package org.gipsybuho.service;

import org.gipsybuho.dao.*;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.*;
import org.gipsybuho.dao.DynamicColumnValueDAO;
import org.gipsybuho.service.importer.*;
import org.gipsybuho.util.TypedValueFormatter;
import java.time.format.DateTimeParseException;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Importa filas ya parseadas y mapeadas a una entidad del dominio.
 *
 * <p>Flujo:
 * <ol>
 *   <li>Fase 1 — mapearValores: renombra las claves-header a claves-campo y sanitiza strings.</li>
 *   <li>Fase 2 — validarTodas: comprueba NOT NULL, tipos numéricos y longitudes en memoria, sin BD.</li>
 *   <li>Fase 3 — insertarFilas: aplica DuplicatePolicy e inserta/actualiza en una transacción única.</li>
 * </ol>
 *
 * <p>El caller es responsable del parseo (use {@code ImportService.parseFile()}) y del mapeo
 * interactivo (use {@code ColumnMappingDialog}). Esta clase recibe las filas ya en memoria.
 */
public class EntityImportService {

    private static final int MAX_FILAS     = 10_000;
    private static final int MAX_LEN       = 255;
    private static final int MAX_LEN_LIBRE = 1_000; // notas, descripcion

    private static final Set<String> CAMPOS_NUMERICOS = Set.of(
        "stock_actual", "stock_minimo", "precio_unidad",
        "salario_base", "irpf", "precio_unit", "precio_setup", "minimo_unidades",
        "mes", "anio", "complementos", "horas_extra_normales", "precio_hora_extra",
        "horas_extra_festivas", "precio_hora_festiva", "percepciones_no_salariales",
        "total_bruto", "irpf_porcentaje", "irpf_importe", "ss_trabajador",
        "total_deducciones", "neto", "ss_empresa", "coste_total_empresa",
        "importe_total", "iva_porcentaje"
    );
    private static final Set<String> CAMPOS_FECHA = Set.of(
        "fecha", "fecha_validez", "fecha_vencimiento", "fecha_entrega_prevista", "fecha_entrega_real"
    );
    private static final Set<String> CAMPOS_LIBRES = Set.of("notas", "descripcion", "condiciones", "observaciones");

    /** Fila válida que ha superado la fase 2, con su número original de fila (1-based). */
    private record ValidRow(int numero, Map<String, String> vals) {}

    /** Grupo de filas que comparten la misma clave de agrupación en specs parent-child. */
    private record ValidGroup(String clave, List<ValidRow> filas) {}

    /**
     * @param spec    descriptor de la entidad (Material.IMPORT_SPEC, etc.)
     * @param filas   filas parseadas del archivo (clave = header del archivo, valor = texto)
     * @param mapping mapa header-archivo → clave-campo (vacío de entradas con valor "(ignorar)")
     * @param policy  política aplicada cuando la clave de negocio ya existe en BD
     * @return resumen detallado de la importación
     */
    public ImportResult importar(EntityImportSpec spec,
                                  List<Map<String, String>> filas,
                                  Map<String, String> mapping,
                                  DuplicatePolicy policy) throws Exception {
        if (filas.size() > MAX_FILAS)
            throw new IllegalArgumentException(
                "El archivo supera el límite de " + MAX_FILAS + " filas por importación.");

        if (spec.esParentChild() && policy == DuplicatePolicy.UPDATE_EXISTING) {
            throw new IllegalArgumentException(
                "UPDATE_EXISTING no está permitido para entidades parent-child: " + spec.nombre()
                    + ". El motor borraría líneas hijas manualmente añadidas. Use SKIP_IF_EXISTS.");
        }
        if (spec.esParentChild() && policy == DuplicatePolicy.CREATE_NEW) {
            throw new IllegalArgumentException(
                "CREATE_NEW no está permitido para entidades parent-child: " + spec.nombre()
                    + ". La clave de agrupación es el identificador natural; duplicarla no tiene semántica clara. Use SKIP_IF_EXISTS.");
        }

        Instant start = Instant.now();

        // Fase 1: renombrar + sanitizar (sin tocar BD)
        List<Map<String, String>> mapeadas = mapearValores(filas, mapping);

        // Fase 2: validar todas las filas en memoria, separar válidas de inválidas
        List<ValidRow> validas = new ArrayList<>();
        List<RowError> errores  = new ArrayList<>();
        for (int i = 0; i < mapeadas.size(); i++) {
            List<RowError> fe = validarFila(spec, mapeadas.get(i), i + 1);
            if (fe.isEmpty()) {
                validas.add(new ValidRow(i + 1, mapeadas.get(i)));
            } else {
                errores.addAll(fe);
            }
        }

        // Fase 3: insertar/actualizar en una única transacción SQLite
        int insertadas;
        int actualizadas;
        int filasOk;
        if (spec.esParentChild()) {
            // Fase 2.5: agrupar filas válidas por clave de agrupación
            List<ValidGroup> grupos = agruparEnFase2_5(spec, validas, errores);
            int[] cnt = insertarGrupos(spec, grupos, policy, errores);
            insertadas = cnt[0];
            actualizadas = cnt[1];
            filasOk = cnt[2];
        } else {
            int[] cnt = insertarFilas(spec, validas, policy, errores);
            insertadas = cnt[0];
            actualizadas = cnt[1];
            filasOk = cnt[0] + cnt[1];
        }

        return new ImportResult(
            filas.size(),
            insertadas,
            actualizadas,
            filas.size() - filasOk,
            Collections.unmodifiableList(errores),
            Duration.between(start, Instant.now())
        );
    }

    // ── Fase 1: mapear headers → claves de campo + sanitizar ─────────────────

    private List<Map<String, String>> mapearValores(List<Map<String, String>> filas,
                                                     Map<String, String> mapping) {
        List<Map<String, String>> out = new ArrayList<>(filas.size());
        for (Map<String, String> fila : filas) {
            Map<String, String> m = new LinkedHashMap<>();
            for (var e : mapping.entrySet()) {
                m.put(e.getValue(), sanitizar(fila.getOrDefault(e.getKey(), "")));
            }
            out.add(m);
        }
        return out;
    }

    /** trim + eliminar caracteres de control ASCII (salvo \t, \n, \r). Sin truncado de longitud. */
    private String sanitizar(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]", "");
    }

    // ── Fase 2: validar en memoria, sin BD ───────────────────────────────────

    private List<RowError> validarFila(EntityImportSpec spec, Map<String, String> vals, int num) {
        List<RowError> out = new ArrayList<>();
        for (FieldSpec f : spec.campos()) {
            String v = vals.getOrDefault(f.clave(), "");

            if (f.obligatorio() && v.isBlank()) {
                out.add(new RowError(num, f.clave(), v, ErrorTipo.NULL_OBLIGATORIO,
                    "Campo obligatorio vacío: " + f.etiqueta()));
                continue;
            }

            if (v.isBlank()) continue;

            int limite = CAMPOS_LIBRES.contains(f.clave()) ? MAX_LEN_LIBRE : MAX_LEN;
            if (v.length() > limite) {
                out.add(new RowError(num, f.clave(), v, ErrorTipo.EXCEDE_LONGITUD,
                    "Supera " + limite + " caracteres (" + v.length() + ")"));
                continue;
            }

            if (CAMPOS_NUMERICOS.contains(f.clave()) && !esNumerico(v)) {
                out.add(new RowError(num, f.clave(), v, ErrorTipo.TIPO_INVALIDO,
                    "No es un número válido: '" + v + "'"));
            }
            if (CAMPOS_FECHA.contains(f.clave()) && !esIsoFechaValida(v)) {
                out.add(new RowError(num, f.clave(), v, ErrorTipo.TIPO_INVALIDO,
                    "Fecha no válida (formato esperado: yyyy-MM-dd): '" + v + "'"));
            }
        }
        return out;
    }

    /**
     * Fase 2.5: agrupa filas válidas por la clave declarada en {@code spec.claveAgrupacion()}.
     * Filas con clave de agrupación blank se descartan con RowError individual (no contaminan ningún grupo).
     * Preserva el orden de aparición de las claves en el CSV.
     */
    private List<ValidGroup> agruparEnFase2_5(EntityImportSpec spec, List<ValidRow> validas, List<RowError> errores) {
        String claveCampo = spec.claveAgrupacion();
        LinkedHashMap<String, List<ValidRow>> mapa = new LinkedHashMap<>();
        for (ValidRow vr : validas) {
            String valorClave = vr.vals().getOrDefault(claveCampo, "").trim();
            if (valorClave.isBlank()) {
                errores.add(new RowError(vr.numero(), claveCampo, "", ErrorTipo.NULL_OBLIGATORIO,
                    "Falta clave de agrupación '" + claveCampo + "' en la fila"));
                continue;
            }
            mapa.computeIfAbsent(valorClave, k -> new ArrayList<>()).add(vr);
        }
        List<ValidGroup> grupos = new ArrayList<>(mapa.size());
        for (var e : mapa.entrySet()) {
            List<ValidRow> filas = e.getValue();
            List<RowError> inconsistencias = detectarInconsistenciaGrupo(spec, e.getKey(), filas);
            if (!inconsistencias.isEmpty()) {
                errores.addAll(inconsistencias);
                continue;
            }
            grupos.add(new ValidGroup(e.getKey(), filas));
        }
        return grupos;
    }

    /**
     * Compara cada fila del grupo contra la primera en todos los campos de {@code spec.campos()}.
     * Si alguna fila difiere, devuelve un RowError por cada fila del grupo (la "canon" y las divergentes),
     * indicando el primer campo discrepante encontrado. El caller descarta el grupo completo.
     *
     * <p>Política A de la decisión D1: todos los campos de la cabecera del spec cuentan.
     * Comparación con equals() directo, sin normalización (la sanitización Fase 1 ya ha hecho trim).
     *
     * @return lista vacía si el grupo es coherente; lista con un RowError por fila si hay inconsistencia.
     */
    private List<RowError> detectarInconsistenciaGrupo(EntityImportSpec spec, String clave, List<ValidRow> filas) {
        if (filas.size() < 2) return List.of();
        ValidRow canon = filas.get(0);
        for (int i = 1; i < filas.size(); i++) {
            ValidRow otra = filas.get(i);
            for (FieldSpec f : spec.campos()) {
                String vCanon = canon.vals().getOrDefault(f.clave(), "");
                String vOtra = otra.vals().getOrDefault(f.clave(), "");
                if (!vCanon.equals(vOtra)) {
                    List<RowError> out = new ArrayList<>(filas.size());
                    String mensaje = "Inconsistencia en grupo '" + clave + "' entre filas "
                        + canon.numero() + " y " + otra.numero()
                        + ": campo '" + f.clave() + "' tiene valores '" + vCanon + "' vs '" + vOtra + "'";
                    for (ValidRow vr : filas) {
                        out.add(new RowError(vr.numero(), f.clave(), vr.vals().getOrDefault(f.clave(), ""),
                            ErrorTipo.OTRO, mensaje));
                    }
                    return out;
                }
            }
        }
        return List.of();
    }

    private boolean esNumerico(String s) {
        return TypedValueFormatter.parseDecimal(s).isPresent();
    }

    private boolean esIsoFechaValida(String s) {
        try {
            LocalDate.parse(s.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // ── Fase 3: transacción única ─────────────────────────────────────────────

    private int[] insertarFilas(EntityImportSpec spec, List<ValidRow> validas,
                                         DuplicatePolicy policy, List<RowError> errores) throws Exception {
        int insertadas = 0, actualizadas = 0;
        Connection conn = DatabaseManager.getConnection();
        boolean prevAC = conn.getAutoCommit();

        // Columnas extra: claves del mapping que no están en el spec (campos dinámicos creados durante el diálogo)
        Set<String> specClaves = spec.campos().stream().map(FieldSpec::clave).collect(Collectors.toSet());
        Set<String> extraClaves = validas.isEmpty() ? Set.of() :
            validas.get(0).vals().keySet().stream()
                .filter(k -> !specClaves.contains(k))
                .collect(Collectors.toSet());
        String tableName = spec.tableName();
        boolean tieneExtras = !extraClaves.isEmpty() && tableName != null;
        DynamicColumnValueDAO valueDAO = tieneExtras ? new DynamicColumnValueDAO(conn) : null;

        conn.setAutoCommit(false);
        try {
            for (ValidRow vr : validas) {
                Savepoint sp = conn.setSavepoint();
                try {
                    int[] r = procesarFila(conn, spec, vr, policy, errores);
                    insertadas   += r[0];
                    actualizadas += r[1];
                    int entityId  = r[2];
                    if (tieneExtras && entityId > 0) {
                        Map<String, String> extraVals = new LinkedHashMap<>();
                        for (String k : extraClaves) {
                            String v = vr.vals().getOrDefault(k, "");
                            if (v != null && !v.isBlank()) extraVals.put(k, v);
                        }
                        if (!extraVals.isEmpty()) valueDAO.updateValues(tableName, entityId, extraVals);
                    }
                } catch (SQLException sqle) {
                    conn.rollback(sp);
                    errores.add(new RowError(vr.numero(), null, null, ErrorTipo.OTRO,
                        "Error de BD al guardar la fila: " + sqle.getMessage()));
                }
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(prevAC);
        }
        return new int[]{insertadas, actualizadas};
    }

    /**
     * Fase 3 alternativa para specs parent-child: ejecuta cada grupo dentro de su propio savepoint.
     * Cabecera + líneas se insertan o se revierten como un átomo transaccional.
     *
     * @return int[]{entidadesInsertadas, entidadesActualizadas, filasConsumidasOk}
     */
    private int[] insertarGrupos(EntityImportSpec spec, List<ValidGroup> grupos,
                                  DuplicatePolicy policy, List<RowError> errores) throws Exception {
        int insertadas = 0, actualizadas = 0, filasOk = 0;
        Connection conn = DatabaseManager.getConnection();
        boolean prevAC = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            for (ValidGroup g : grupos) {
                Savepoint sp = conn.setSavepoint();
                try {
                    int[] r = procesarGrupo(conn, spec, g, policy, errores);
                    insertadas += r[0];
                    actualizadas += r[1];
                    filasOk += r[2];
                } catch (SQLException sqle) {
                    conn.rollback(sp);
                    int numFila = g.filas().isEmpty() ? 0 : g.filas().get(0).numero();
                    errores.add(new RowError(numFila, null, g.clave(), ErrorTipo.OTRO,
                        "Error de BD al guardar el grupo '" + g.clave() + "': " + sqle.getMessage()));
                }
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(prevAC);
        }
        return new int[]{insertadas, actualizadas, filasOk};
    }

    // Devuelve int[]{insertadas, actualizadas, entityId}
    // entityId > 0 cuando la fila fue insertada o actualizada; 0 cuando fue omitida o hay error.
    private int[] procesarFila(Connection conn, EntityImportSpec spec, ValidRow vr,
                                DuplicatePolicy policy, List<RowError> errores) throws SQLException {
        return switch (spec.nombre()) {
            case "Materiales" -> procesarMaterial(conn, vr, policy, errores);
            case "Empleados"  -> procesarEmpleado(conn, vr, policy, errores);
            case "Clientes"   -> procesarCliente(conn, vr, policy, errores);
            case "Tarifas"    -> procesarTarifa(conn, vr, policy, errores);
            case "Nominas"    -> procesarNomina(conn, vr, policy, errores);
            case "Pedidos"    -> procesarPedido(conn, vr, policy, errores);
            default -> throw new IllegalArgumentException("Entidad no soportada: " + spec.nombre());
        };
    }

    /**
     * Dispatcher de specs parent-child. Cada entidad parent-child se añade como case conforme se implementa
     * (3C-paso-3 Presupuesto, Bloque 4 Factura, Bloque 5 Albarán pendiente).
     *
     * <p>Contrato del método llamado por entidad: devolver int[]{entidadesInsertadas, entidadesActualizadas, filasConsumidasOk}.
     * Fallos post-INSERT-cabecera deben lanzar SQLException para que el savepoint del grupo rollee atómicamente.
     * Fallos pre-INSERT (FK de cabecera, etc.) pueden retornar {0,0,0} añadiendo el RowError correspondiente.
     */
    private int[] procesarGrupo(Connection conn, EntityImportSpec spec, ValidGroup g,
                                 DuplicatePolicy policy, List<RowError> errores) throws SQLException {
        return switch (spec.nombre()) {
            case "Presupuestos" -> procesarPresupuesto(conn, spec, g, policy, errores);
            case "Facturas"     -> procesarFactura(conn, spec, g, policy, errores);
            case "Albaranes"    -> procesarAlbaran(conn, spec, g, policy, errores);
            default -> throw new IllegalArgumentException(
                "Entidad parent-child no soportada: " + spec.nombre());
        };
    }

    // ── Material ──────────────────────────────────────────────────────────────

    private int[] procesarMaterial(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                    List<RowError> errores) throws SQLException {
        MaterialDAO dao = new MaterialDAO(conn);
        Material m = ensamblarMaterial(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(m);
            return new int[]{1, 0, m.getId()};
        }

        // Clave de negocio: referencia si existe; si no, nombre (los archivos reales
        // de material rara vez traen referencia y descartarlos dejaba 0 importados).
        String ref = m.getReferencia();
        int existingId;
        if (ref != null && !ref.isBlank()) {
            existingId = buscarId(conn, "SELECT id FROM materiales WHERE referencia=?", ref);
        } else {
            existingId = buscarId(conn, "SELECT id FROM materiales WHERE nombre=?", m.getNombre());
        }
        if (existingId == 0) {
            dao.save(m);
            return new int[]{1, 0, m.getId()};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0, 0};
        }
        // UPDATE_EXISTING: cargar existente y sobreescribir solo los campos importados
        Material existente = dao.findById(existingId);
        aplicarValoresMaterial(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1, existingId};
    }

    private Material ensamblarMaterial(Map<String, String> vals) {
        Material m = new Material();
        aplicarValoresMaterial(m, vals);
        return m;
    }

    private void aplicarValoresMaterial(Material m, Map<String, String> vals) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "nombre"        -> m.setNombre(v);
                case "referencia"    -> m.setReferencia(v);
                case "categoria"     -> m.setCategoria(v);
                case "stock_actual"  -> m.setStockActual(toDouble(v));
                case "stock_minimo"  -> m.setStockMinimo(toDouble(v));
                case "unidad"        -> { if (esUnidadMaterialValida(v)) m.setUnidad(v); }
                case "precio_unidad" -> m.setPrecioUnidad(toDouble(v));
                case "proveedor"     -> m.setProveedor(v);
            }
        });
    }

    private boolean esUnidadMaterialValida(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank() || v.length() > 20) return false;
        if (v.matches("[+-]?\\d+(?:[.,]\\d+)?")) return false;
        String lower = v.toLowerCase(Locale.ROOT);
        return lower.matches("[a-záéíóúüñ0-9./²³ -]+")
            && lower.matches(".*[a-záéíóúüñ].*");
    }

    // ── Empleado ──────────────────────────────────────────────────────────────

    private int[] procesarEmpleado(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                    List<RowError> errores) throws SQLException {
        EmpleadoDAO dao = new EmpleadoDAO(conn);
        Empleado emp = ensamblarEmpleado(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(emp);
            return new int[]{1, 0, emp.getId()};
        }

        String nif = emp.getNif();
        if (nif == null || nif.isBlank()) {
            errores.add(new RowError(vr.numero(), "nif", nif, ErrorTipo.OTRO,
                "Sin clave de negocio ('nif') requerida por la política " + policy));
            return new int[]{0, 0, 0};
        }

        int existingId = buscarId(conn, "SELECT id FROM empleados WHERE nif=?", nif);
        if (existingId == 0) {
            dao.save(emp);
            return new int[]{1, 0, emp.getId()};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0, 0};
        }
        Empleado existente = dao.findById(existingId);
        aplicarValoresEmpleado(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1, existingId};
    }

    private Empleado ensamblarEmpleado(Map<String, String> vals) {
        Empleado emp = new Empleado();
        emp.setActivo(true); // DAOs insertan activo explícitamente; sin esto el empleado queda inactivo
        aplicarValoresEmpleado(emp, vals);
        return emp;
    }

    private void aplicarValoresEmpleado(Empleado emp, Map<String, String> vals) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "nombre"       -> emp.setNombre(v);
                case "apellido"     -> emp.setApellidos(v);
                case "nif"          -> emp.setNif(v);
                case "categoria"    -> emp.setCategoria(v);
                case "salario_base" -> emp.setSalarioBase(toDouble(v));
                case "fecha_alta"   -> emp.setFechaAlta(v);
                case "iban"         -> emp.setIban(v);
                case "irpf"         -> emp.setIrpf(toDouble(v));
                case "telefono"     -> emp.setTelefono(v);
                case "email"        -> emp.setEmail(v);
                case "direccion"    -> emp.setDireccion(v);
            }
        });
    }

    // ── Cliente ───────────────────────────────────────────────────────────────

    private int[] procesarCliente(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                   List<RowError> errores) throws SQLException {
        ClienteDAO dao = new ClienteDAO(conn);
        Cliente c = ensamblarCliente(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(c);
            return new int[]{1, 0, c.getId()};
        }

        String nif = c.getNif();
        if (nif == null || nif.isBlank()) {
            errores.add(new RowError(vr.numero(), "nif", nif, ErrorTipo.OTRO,
                "Sin clave de negocio ('nif') requerida por la política " + policy));
            return new int[]{0, 0, 0};
        }

        int existingId = buscarId(conn, "SELECT id FROM clientes WHERE nif=?", nif);
        if (existingId == 0) {
            dao.save(c);
            return new int[]{1, 0, c.getId()};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0, 0};
        }
        Cliente existente = dao.findById(existingId);
        aplicarValoresCliente(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1, existingId};
    }

    private Cliente ensamblarCliente(Map<String, String> vals) {
        Cliente c = new Cliente();
        aplicarValoresCliente(c, vals);
        return c;
    }

    private void aplicarValoresCliente(Cliente c, Map<String, String> vals) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "nombre"    -> c.setNombre(v);
                case "apellido"  -> c.setApellidos(v);
                case "tipo"      -> c.setTipo(v);
                case "nif"       -> c.setNif(v);
                case "direccion" -> c.setDireccion(v);
                case "ciudad"    -> c.setCiudad(v);
                case "cp"        -> c.setCp(v);
                case "telefono"  -> c.setTelefono(v);
                case "email"     -> c.setEmail(v);
                case "notas"     -> c.setNotas(v);
            }
        });
    }

    // ── Tarifa ────────────────────────────────────────────────────────────────

    private int[] procesarTarifa(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                  List<RowError> errores) throws SQLException {
        TarifaDAO dao = new TarifaDAO(conn);
        Tarifa t = ensamblarTarifa(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(t);
            return new int[]{1, 0, t.getId()};
        }

        String tecnica = t.getTecnica();
        String nombre  = t.getNombre();
        int minimoUnidades = t.getMinimoUnidades();
        // tecnica y nombre son NOT NULL en el spec, pero check defensivo para UPDATE
        if ((tecnica == null || tecnica.isBlank()) || (nombre == null || nombre.isBlank())) {
            errores.add(new RowError(vr.numero(), "tecnica+nombre",
                tecnica + "/" + nombre, ErrorTipo.OTRO,
                "Sin clave de negocio ('tecnica'+'nombre') requerida por la política " + policy));
            return new int[]{0, 0, 0};
        }

        // TODO: añadir UNIQUE(tecnica, nombre,minimo_unidades) en migración futura para defensa en profundidad
        int existingId = buscarIdTarifa(conn, tecnica, nombre, minimoUnidades);
        if (existingId == 0) {
            dao.save(t);
            return new int[]{1, 0, t.getId()};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0, 0};
        }
        Tarifa existente = dao.findById(existingId);
        aplicarValoresTarifa(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1, existingId};
    }

    private Tarifa ensamblarTarifa(Map<String, String> vals) {
        Tarifa t = new Tarifa();
        t.setActiva(true); // DAOs insertan activa explícitamente; sin esto la tarifa queda oculta
        aplicarValoresTarifa(t, vals);
        return t;
    }

    private void aplicarValoresTarifa(Tarifa t, Map<String, String> vals) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "tecnica"         -> t.setTecnica(v);
                case "nombre"          -> t.setNombre(v);
                case "descripcion"     -> t.setDescripcion(v);
                case "precio_unit"     -> t.setPrecioUnit(toDouble(v));
                case "precio_setup"    -> t.setPrecioSetup(toDouble(v));
                case "minimo_unidades" -> t.setMinimoUnidades(toInt(v));
            }
        });
    }

    // ── Nomina ───────────────────────────────────────────────────────────────

    private int[] procesarNomina(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                  List<RowError> errores) throws SQLException {
        NominaDAO dao = new NominaDAO(conn);
        Nomina n = ensamblarNomina(vr.vals(), conn, errores, vr.numero());
        if (n == null) return new int[]{0, 0, 0};

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(n);
            return new int[]{1, 0, n.getId()};
        }

        int empleadoId = n.getEmpleadoId();
        int mes = n.getMes();
        int anio = n.getAnio();
        if (empleadoId <= 0 || mes <= 0 || anio <= 0) {
            errores.add(new RowError(vr.numero(), "empleado_id+mes+anio",
                empleadoId + "/" + mes + "/" + anio, ErrorTipo.OTRO,
                "Sin clave de negocio ('empleado_id'+'mes'+'anio') requerida por la política " + policy));
            return new int[]{0, 0, 0};
        }

        int existingId = buscarIdNomina(conn, empleadoId, mes, anio);
        if (existingId == 0) {
            dao.save(n);
            return new int[]{1, 0, n.getId()};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0, 0};
        }
        Nomina existente = dao.findById(existingId);
        aplicarValoresNomina(existente, vr.vals());
        existente.setEmpleadoId(empleadoId);
        existente.setMes(mes);
        existente.setAnio(anio);
        dao.save(existente);
        return new int[]{0, 1, existingId};
    }

    private Nomina ensamblarNomina(Map<String, String> vals, Connection conn,
                                    List<RowError> errores, int numFila) throws SQLException {
        String nombre = vals.getOrDefault("empleado_nombre", "");
        String apellidos = vals.getOrDefault("empleado_apellidos", "");
        int empleadoId = resolverEmpleadoId(conn, nombre, apellidos, errores, numFila);
        if (empleadoId <= 0) return null;

        Nomina n = new Nomina();
        n.setEmpleadoId(empleadoId);
        aplicarValoresNomina(n, vals);
        return n;
    }

    private void aplicarValoresNomina(Nomina n, Map<String, String> vals) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "mes"                         -> n.setMes(toInt(v));
                case "anio"                        -> n.setAnio(toInt(v));
                case "salario_base"                -> n.setSalarioBase(toDouble(v));
                case "complementos"                -> n.setComplementos(toDouble(v));
                case "horas_extra_normales"        -> n.setHorasExtraNormales(toInt(v));
                case "precio_hora_extra"           -> n.setPrecioHoraExtra(toDouble(v));
                case "horas_extra_festivas"        -> n.setHorasExtraFestivas(toInt(v));
                case "precio_hora_festiva"         -> n.setPrecioHoraFestiva(toDouble(v));
                case "percepciones_no_salariales"  -> n.setPercepcionesNoSalariales(toDouble(v));
                case "total_bruto"                 -> n.setTotalBruto(toDouble(v));
                case "irpf_porcentaje"             -> n.setIrpfPorcentaje(toDouble(v));
                case "irpf_importe"                -> n.setIrpfImporte(toDouble(v));
                case "ss_trabajador"               -> n.setSsTrabajador(toDouble(v));
                case "total_deducciones"           -> n.setTotalDeducciones(toDouble(v));
                case "neto"                        -> n.setNeto(toDouble(v));
                case "ss_empresa"                  -> n.setSsEmpresa(toDouble(v));
                case "coste_total_empresa"         -> n.setCosteTotalEmpresa(toDouble(v));
            }
        });
    }

    private int resolverEmpleadoId(Connection conn, String nombre, String apellidos,
                                   List<RowError> errores, int numFila) throws SQLException {
        // Sin filtro activo: las nóminas históricas pueden pertenecer a empleados inactivos.
        return resolverFkPorNombre(conn, "empleados", "nombre", "apellidos",
            nombre, apellidos, null, "Empleado", "la nómina", errores, numFila);
    }

    // ── Pedido ───────────────────────────────────────────────────────────────

    private int[] procesarPedido(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                  List<RowError> errores) throws SQLException {
        PedidoDAO dao = new PedidoDAO(conn);
        Pedido p = ensamblarPedido(vr.vals(), conn, errores, vr.numero());
        if (p == null) return new int[]{0, 0, 0};

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(p);
            return new int[]{1, 0, p.getId()};
        }

        String numero = p.getNumero();
        if (numero == null || numero.isBlank()) {
            errores.add(new RowError(vr.numero(), "numero", numero, ErrorTipo.OTRO,
                "Sin clave de negocio ('numero') requerida por la política " + policy));
            return new int[]{0, 0, 0};
        }

        int existingId = buscarId(conn, "SELECT id FROM pedidos WHERE numero=?", numero);
        if (existingId == 0) {
            dao.save(p);
            return new int[]{1, 0, p.getId()};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0, 0};
        }
        Pedido existente = dao.findById(existingId);
        int erroresAntes = errores.size();
        aplicarValoresPedido(existente, vr.vals(), errores, vr.numero());
        if (errores.size() > erroresAntes) {
            return new int[]{0, 0, 0};
        }
        existente.setClienteId(p.getClienteId());
        dao.save(existente);
        return new int[]{0, 1, existingId};
    }

    private Pedido ensamblarPedido(Map<String, String> vals, Connection conn,
                                   List<RowError> errores, int numFila) throws SQLException {
        String nif = vals.getOrDefault("cliente_nif", "");
        String nombre = vals.getOrDefault("cliente_nombre", "");
        String apellidos = vals.getOrDefault("cliente_apellidos", "");
        int clienteId = resolverClienteId(conn, nif, nombre, apellidos, errores, numFila);
        if (clienteId <= 0) return null;

        Pedido p = new Pedido();
        p.setClienteId(clienteId);
        int erroresAntes = errores.size();
        aplicarValoresPedido(p, vals, errores, numFila);
        if (errores.size() > erroresAntes) return null;
        return p;
    }

    private void aplicarValoresPedido(Pedido p, Map<String, String> vals,
                                      List<RowError> errores, int numFila) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "numero"                  -> p.setNumero(v);
                case "fecha"                   -> p.setFecha(toLocalDate(v, "fecha", numFila, errores));
                case "fecha_entrega_prevista"  -> p.setFechaEntregaPrevista(toLocalDate(v, "fecha_entrega_prevista", numFila, errores));
                case "fecha_entrega_real"      -> p.setFechaEntregaReal(toLocalDate(v, "fecha_entrega_real", numFila, errores));
                case "estado"                  -> p.setEstado(v);
                case "descripcion"             -> p.setDescripcion(v);
                case "importe_total"           -> p.setImporteTotal(toDouble(v));
                case "iva_porcentaje"          -> p.setIvaPorcentaje(toDouble(v));
                case "notas"                   -> p.setNotas(v);
            }
        });
    }

    private int resolverClienteId(Connection conn, String nif, String nombre, String apellidos,
                                  List<RowError> errores, int numFila) throws SQLException {
        String nifLimpio = nif != null ? nif.trim() : "";
        if (!nifLimpio.isBlank()) {
            int id = buscarId(conn, "SELECT id FROM clientes WHERE nif=?", nifLimpio);
            if (id > 0) return id;
            errores.add(new RowError(numFila, "cliente_nif", nifLimpio, ErrorTipo.OTRO,
                "Cliente con nif '" + nifLimpio + "' no encontrado para el pedido"));
            return -1;
        }

        String nombreLimpio = nombre != null ? nombre.trim() : "";
        if (nombreLimpio.isBlank()) {
            errores.add(new RowError(numFila, "cliente_nombre", "", ErrorTipo.OTRO,
                "El pedido no especifica ni cliente_nif ni cliente_nombre"));
            return -1;
        }

        // Clientes no tiene flag de activo aplicado en importación; se resuelve sin filtro extra.
        return resolverFkPorNombre(conn, "clientes", "nombre", "apellidos",
            nombre, apellidos, null, "Cliente", "el pedido", errores, numFila);
    }

    private int resolverFkPorNombre(Connection conn,
                                    String tabla,
                                    String columnaNombre,
                                    String columnaApellidos,
                                    String nombre,
                                    String apellidos,
                                    String filtroExtraSql,
                                    String etiquetaEntidad,
                                    String contextoEntidad,
                                    List<RowError> errores,
                                    int numFila) throws SQLException {
        validarIdentificadorSql(tabla);
        validarIdentificadorSql(columnaNombre);
        validarIdentificadorSql(columnaApellidos);
        if (filtroExtraSql != null && !filtroExtraSql.matches("[A-Za-z_][A-Za-z0-9_]*\\s*=\\s*[0-9]+")) {
            throw new IllegalArgumentException("Filtro SQL no válido para resolver FK: " + filtroExtraSql);
        }

        String nombreLimpio = nombre != null ? nombre.trim() : "";
        String apellidosLimpios = apellidos != null ? apellidos.trim() : "";
        String sql = "SELECT id FROM " + tabla
            + " WHERE lower(trim(" + columnaNombre + "))=lower(trim(?))";
        if (!apellidosLimpios.isBlank()) {
            sql += " AND lower(trim(COALESCE(" + columnaApellidos + ",'')))=lower(trim(?))";
        }
        if (filtroExtraSql != null && !filtroExtraSql.isBlank()) {
            sql += " AND " + filtroExtraSql;
        }

        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreLimpio);
            if (!apellidosLimpios.isBlank()) ps.setString(2, apellidosLimpios);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id"));
        }

        String valor = apellidosLimpios.isBlank() ? nombreLimpio : nombreLimpio + " " + apellidosLimpios;
        String campo = etiquetaEntidad.toLowerCase(Locale.ROOT) + "_nombre";
        if (ids.isEmpty()) {
            errores.add(new RowError(numFila, campo, valor, ErrorTipo.OTRO,
                etiquetaEntidad + " no encontrado para " + contextoEntidad + ": " + valor));
            return -1;
        }
        if (ids.size() > 1) {
            errores.add(new RowError(numFila, campo, valor, ErrorTipo.OTRO,
                etiquetaEntidad + " ambiguo para " + contextoEntidad + ": " + valor + " (" + ids.size() + " coincidencias)"));
            return -1;
        }
        return ids.get(0);
    }

    // ── Presupuesto (parent-child) ───────────────────────────────────────────

    private int[] procesarPresupuesto(Connection conn, EntityImportSpec spec, ValidGroup g,
                                       DuplicatePolicy policy, List<RowError> errores) throws SQLException {
        // Cabecera = primera fila del grupo (ya validada coherente por detectarInconsistenciaGrupo).
        ValidRow cabecera = g.filas().get(0);
        Map<String, String> vals = cabecera.vals();
        int numFila = cabecera.numero();

        // FK cliente (mismo contrato que Pedido: nif explícito sin fallback; si vacío, nombre+apellidos).
        String nif = vals.getOrDefault("cliente_nif", "");
        String nombre = vals.getOrDefault("cliente_nombre", "");
        String apellidos = vals.getOrDefault("cliente_apellidos", "");
        int clienteId = resolverClienteId(conn, nif, nombre, apellidos, errores, numFila);
        if (clienteId <= 0) return new int[]{0, 0, 0};

        // Duplicado por 'numero'. UPDATE_EXISTING y CREATE_NEW están bloqueados al inicio de importar()
        // para parent-child, así que aquí sólo se llega con SKIP_IF_EXISTS.
        String numero = vals.getOrDefault("numero", "");
        int existingId = buscarId(conn, "SELECT id FROM presupuestos WHERE numero=?", numero);
        if (existingId > 0) {
            return new int[]{0, 0, g.filas().size()};
        }

        Presupuesto p = ensamblarPresupuesto(clienteId, vals, g.filas(), errores, numFila);
        if (p == null) return new int[]{0, 0, 0};

        new PresupuestoDAO(conn).save(p);
        return new int[]{1, 0, g.filas().size()};
    }

    private Presupuesto ensamblarPresupuesto(int clienteId, Map<String, String> valsCabecera,
                                              List<ValidRow> filas, List<RowError> errores, int numFila) {
        Presupuesto p = new Presupuesto();
        p.setClienteId(clienteId);
        int erroresAntes = errores.size();
        aplicarValoresPresupuestoCabecera(p, valsCabecera, errores, numFila);
        if (errores.size() > erroresAntes) return null;

        // Defaults si llegaron vacíos del CSV.
        if (p.getFecha() == null || p.getFecha().isBlank()) p.setFecha(LocalDate.now().toString());
        if (p.getEstado() == null || p.getEstado().isBlank()) p.setEstado("borrador");
        if (p.getIvaPorcentaje() == 0.0) p.setIvaPorcentaje(21.0);

        // Líneas: una por cada fila del grupo, en orden CSV.
        List<LineaPresupuesto> lineas = new ArrayList<>(filas.size());
        for (ValidRow vr : filas) {
            lineas.add(ensamblarLineaPresupuesto(vr.vals()));
        }
        p.setLineas(lineas);
        p.calcularTotales();
        return p;
    }

    private void aplicarValoresPresupuestoCabecera(Presupuesto p, Map<String, String> vals,
                                                    List<RowError> errores, int numFila) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "numero"          -> p.setNumero(v);
                case "fecha"           -> p.setFecha(v);
                case "fecha_validez"   -> p.setFechaValidez(v);
                case "estado"          -> p.setEstado(v);
                case "iva_porcentaje"  -> p.setIvaPorcentaje(toDouble(v));
                case "notas"           -> p.setNotas(v);
                case "condiciones"     -> p.setCondiciones(v);
                default -> { /* campos de línea o desconocidos: ignorar a nivel cabecera */ }
            }
        });
    }

    private LineaPresupuesto ensamblarLineaPresupuesto(Map<String, String> vals) {
        LineaPresupuesto l = new LineaPresupuesto();
        l.setDescripcion(vals.getOrDefault("descripcion", ""));
        l.setTecnica(vals.getOrDefault("tecnica", ""));
        l.setCantidad(toInt(vals.getOrDefault("cantidad", "0")));
        l.setPrecioUnit(toDouble(vals.getOrDefault("precio_unit", "0")));
        l.setDescuento(toDouble(vals.getOrDefault("descuento", "0")));
        l.calcularTotal();
        return l;
    }

    // ── Factura (parent-child) ──────────────────────────────────────────────

    private int[] procesarFactura(Connection conn, EntityImportSpec spec, ValidGroup g,
                                   DuplicatePolicy policy, List<RowError> errores) throws SQLException {
        // Cabecera = primera fila del grupo (ya validada coherente por detectarInconsistenciaGrupo).
        ValidRow cabecera = g.filas().get(0);
        Map<String, String> vals = cabecera.vals();
        int numFila = cabecera.numero();

        // FK cliente (mismo contrato que Pedido/Presupuesto: nif explícito sin fallback; si vacío, nombre+apellidos).
        String nif = vals.getOrDefault("cliente_nif", "");
        String nombre = vals.getOrDefault("cliente_nombre", "");
        String apellidos = vals.getOrDefault("cliente_apellidos", "");
        int clienteId = resolverClienteId(conn, nif, nombre, apellidos, errores, numFila);
        if (clienteId <= 0) return new int[]{0, 0, 0};

        // FK opcional presupuesto. Si CSV no la trae → 0 (preexistente, no se llama al setter).
        // Si la trae y no existe en BD → ERROR, descartar grupo entero.
        String presNumero = vals.getOrDefault("presupuesto_numero", "");
        int presupuestoId = resolverPresupuestoIdPorNumero(conn, presNumero, errores, numFila);
        if (presNumero != null && !presNumero.isBlank() && presupuestoId <= 0) {
            return new int[]{0, 0, 0};
        }

        // Duplicado por 'numero'. UPDATE_EXISTING y CREATE_NEW están bloqueados al inicio de importar()
        // para parent-child, así que aquí sólo se llega con SKIP_IF_EXISTS.
        String numero = vals.getOrDefault("numero", "");
        int existingId = buscarId(conn, "SELECT id FROM facturas WHERE numero=?", numero);
        if (existingId > 0) {
            return new int[]{0, 0, g.filas().size()};
        }

        Factura f = ensamblarFactura(clienteId, presupuestoId, vals, g.filas(), errores, numFila);
        if (f == null) return new int[]{0, 0, 0};

        new FacturaDAO(conn).save(f);
        return new int[]{1, 0, g.filas().size()};
    }

    private Factura ensamblarFactura(int clienteId, int presupuestoId, Map<String, String> valsCabecera,
                                      List<ValidRow> filas, List<RowError> errores, int numFila) {
        Factura f = new Factura();
        f.setClienteId(clienteId);
        if (presupuestoId > 0) f.setPresupuestoId(presupuestoId);
        int erroresAntes = errores.size();
        aplicarValoresFacturaCabecera(f, valsCabecera, errores, numFila);
        if (errores.size() > erroresAntes) return null;

        // Defaults si llegaron vacíos del CSV. La BD impone NOT NULL en facturas.fecha.
        if (f.getFecha() == null || f.getFecha().isBlank()) f.setFecha(LocalDate.now().toString());
        if (f.getEstado() == null || f.getEstado().isBlank()) f.setEstado("pendiente");
        if (f.getIvaPorcentaje() == 0.0) f.setIvaPorcentaje(21.0);

        // Líneas: una por cada fila del grupo, en orden CSV.
        List<LineaFactura> lineas = new ArrayList<>(filas.size());
        for (ValidRow vr : filas) {
            lineas.add(ensamblarLineaFactura(vr.vals()));
        }
        f.setLineas(lineas);
        f.calcularTotales();
        return f;
    }

    private void aplicarValoresFacturaCabecera(Factura f, Map<String, String> vals,
                                                List<RowError> errores, int numFila) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "numero"             -> f.setNumero(v);
                case "fecha"              -> f.setFecha(v);
                case "fecha_vencimiento"  -> f.setFechaVencimiento(v);
                case "estado"             -> f.setEstado(v);
                case "forma_pago"         -> f.setFormaPago(v);
                case "iva_porcentaje"     -> f.setIvaPorcentaje(toDouble(v));
                case "notas"              -> f.setNotas(v);
                default -> { /* cliente_*, presupuesto_numero, campos de línea o desconocidos: ignorar a nivel cabecera */ }
            }
        });
    }

    private LineaFactura ensamblarLineaFactura(Map<String, String> vals) {
        LineaFactura l = new LineaFactura();
        l.setDescripcion(vals.getOrDefault("descripcion", ""));
        l.setTecnica(vals.getOrDefault("tecnica", ""));
        l.setCantidad(toInt(vals.getOrDefault("cantidad", "0")));
        l.setPrecioUnit(toDouble(vals.getOrDefault("precio_unit", "0")));
        l.setDescuento(toDouble(vals.getOrDefault("descuento", "0")));
        l.calcularTotal();
        return l;
    }

    /**
     * Resuelve un número de presupuesto a su id de BD.
     * @return id si la cadena no es blank y el presupuesto existe; 0 si la cadena es blank (FK opcional no informada);
     *         -1 si la cadena es no-blank pero el presupuesto no existe (añade RowError).
     */
    private int resolverPresupuestoIdPorNumero(Connection conn, String presNumero,
                                                List<RowError> errores, int numFila) throws SQLException {
        if (presNumero == null || presNumero.isBlank()) return 0;
        int id = buscarId(conn, "SELECT id FROM presupuestos WHERE numero=?", presNumero.trim());
        if (id > 0) return id;
        errores.add(new RowError(numFila, "presupuesto_numero", presNumero, ErrorTipo.OTRO,
            "Presupuesto con numero '" + presNumero + "' no encontrado para la factura"));
        return -1;
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Busca el id de un registro por una sola clave de negocio. Devuelve 0 si no existe. */
    private int buscarId(Connection conn, String sql, String valor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Busca el id de una tarifa por su clave compuesta (tecnica, nombre, minimo_unidades). */
    private int buscarIdTarifa(Connection conn, String tecnica, String nombre, int minimoUnidades) throws SQLException {
        String sql = "SELECT id FROM tarifas WHERE tecnica=? AND nombre=? AND minimo_unidades=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tecnica);
            ps.setString(2, nombre);
            ps.setInt(3, minimoUnidades);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Busca el id de una nómina por su clave compuesta (empleado_id, mes, anio). */
    private int buscarIdNomina(Connection conn, int empleadoId, int mes, int anio) throws SQLException {
        String sql = "SELECT id FROM nominas WHERE empleado_id=? AND mes=? AND anio=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empleadoId);
            ps.setInt(2, mes);
            ps.setInt(3, anio);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void validarIdentificadorSql(String identificador) {
        if (identificador == null || !identificador.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Identificador SQL no válido para resolver FK: " + identificador);
        }
    }

    private double toDouble(String s) {
        return TypedValueFormatter.parseDecimal(s).map(java.math.BigDecimal::doubleValue).orElse(0.0);
    }

    private int toInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9\\-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalDate toLocalDate(String s, String campo, int numFila, List<RowError> errores) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            errores.add(new RowError(numFila, campo, s, ErrorTipo.TIPO_INVALIDO,
                    "Fecha no válida (formato esperado: yyyy-MM-dd): '" + s + "'"));
            return null;
        }
    }

    // ── Albarán (parent-child) ──────────────────────────────────────────────

    private int[] procesarAlbaran(Connection conn, EntityImportSpec spec, ValidGroup g,
                                   DuplicatePolicy policy, List<RowError> errores) throws SQLException {
        ValidRow cabecera = g.filas().get(0);
        Map<String, String> vals = cabecera.vals();
        int numFila = cabecera.numero();

        // FK cliente (mismo contrato que Pedido/Presupuesto/Factura).
        String nif = vals.getOrDefault("cliente_nif", "");
        String nombre = vals.getOrDefault("cliente_nombre", "");
        String apellidos = vals.getOrDefault("cliente_apellidos", "");
        int clienteId = resolverClienteId(conn, nif, nombre, apellidos, errores, numFila);
        if (clienteId <= 0) return new int[]{0, 0, 0};

        // FK opcional factura. Patrón D5: si vacío → 0; si informado y no existe → ERROR.
        String facturaNumero = vals.getOrDefault("factura_numero", "");
        int facturaId = resolverFacturaIdPorNumero(conn, facturaNumero, errores, numFila);
        if (facturaNumero != null && !facturaNumero.isBlank() && facturaId <= 0) {
            return new int[]{0, 0, 0};
        }

        // FK opcional pedido. Mismo patrón.
        String pedidoNumero = vals.getOrDefault("pedido_numero", "");
        int pedidoId = resolverPedidoIdPorNumero(conn, pedidoNumero, errores, numFila);
        if (pedidoNumero != null && !pedidoNumero.isBlank() && pedidoId <= 0) {
            return new int[]{0, 0, 0};
        }

        // Duplicado por 'numero'. UPDATE_EXISTING y CREATE_NEW están bloqueados al inicio de importar()
        // para parent-child, así que aquí sólo se llega con SKIP_IF_EXISTS.
        String numero = vals.getOrDefault("numero", "");
        int existingId = buscarId(conn, "SELECT id FROM albaranes WHERE numero=?", numero);
        if (existingId > 0) {
            return new int[]{0, 0, g.filas().size()};
        }

        Albaran a = ensamblarAlbaran(clienteId, facturaId, pedidoId, vals, g.filas(), errores, numFila);
        if (a == null) return new int[]{0, 0, 0};

        new AlbaranDAO(conn).save(a);
        return new int[]{1, 0, g.filas().size()};
    }

    private Albaran ensamblarAlbaran(int clienteId, int facturaId, int pedidoId,
                                      Map<String, String> valsCabecera, List<ValidRow> filas,
                                      List<RowError> errores, int numFila) {
        Albaran a = new Albaran();
        a.setClienteId(clienteId);
        if (facturaId > 0) a.setFacturaId(facturaId);
        if (pedidoId > 0) a.setPedidoId(pedidoId);
        int erroresAntes = errores.size();
        aplicarValoresAlbaranCabecera(a, valsCabecera, errores, numFila);
        if (errores.size() > erroresAntes) return null;

        // Defaults si llegaron vacíos del CSV. La BD impone NOT NULL en albaranes.fecha.
        if (a.getFecha() == null || a.getFecha().isBlank()) a.setFecha(LocalDate.now().toString());
        // 'estado': SQLite no aplica DEFAULT cuando se pasa NULL explícito vía setString,
        // así que se aplica en Java. Simetría con Factura (D-F-EST) y Presupuesto.
        if (a.getEstado() == null || a.getEstado().isBlank()) a.setEstado("pendiente");

        // Líneas: una por cada fila del grupo, en orden CSV.
        List<LineaAlbaran> lineas = new ArrayList<>(filas.size());
        for (ValidRow vr : filas) {
            lineas.add(ensamblarLineaAlbaran(vr.vals()));
        }
        a.setLineas(lineas);
        return a;
    }

    private void aplicarValoresAlbaranCabecera(Albaran a, Map<String, String> vals,
                                                List<RowError> errores, int numFila) {
        vals.forEach((clave, v) -> {
            if (v == null || v.isBlank()) return;
            switch (clave) {
                case "numero"        -> a.setNumero(v);
                case "fecha"         -> a.setFecha(v);
                case "estado"        -> a.setEstado(v);
                case "observaciones" -> a.setObservaciones(v);
                default -> { /* cliente_*, factura_numero, pedido_numero, campos de línea o desconocidos: ignorar a nivel cabecera */ }
            }
        });
    }

    private LineaAlbaran ensamblarLineaAlbaran(Map<String, String> vals) {
        LineaAlbaran l = new LineaAlbaran();
        l.setDescripcion(vals.getOrDefault("descripcion", ""));
        l.setCantidad(toInt(vals.getOrDefault("cantidad", "0")));
        String unidad = vals.getOrDefault("unidad", "");
        if (!unidad.isBlank()) l.setUnidad(unidad);
        return l;
    }

    /**
     * Resuelve un número de factura a su id de BD.
     * @return id si la cadena no es blank y la factura existe; 0 si la cadena es blank (FK opcional no informada);
     *         -1 si la cadena es no-blank pero la factura no existe (añade RowError).
     */
    private int resolverFacturaIdPorNumero(Connection conn, String facturaNumero,
                                            List<RowError> errores, int numFila) throws SQLException {
        if (facturaNumero == null || facturaNumero.isBlank()) return 0;
        int id = buscarId(conn, "SELECT id FROM facturas WHERE numero=?", facturaNumero.trim());
        if (id > 0) return id;
        errores.add(new RowError(numFila, "factura_numero", facturaNumero, ErrorTipo.OTRO,
            "Factura con numero '" + facturaNumero + "' no encontrada para el albaran"));
        return -1;
    }

    /**
     * Resuelve un número de pedido a su id de BD.
     * @return id si la cadena no es blank y el pedido existe; 0 si la cadena es blank (FK opcional no informada);
     *         -1 si la cadena es no-blank pero el pedido no existe (añade RowError).
     */
    private int resolverPedidoIdPorNumero(Connection conn, String pedidoNumero,
                                           List<RowError> errores, int numFila) throws SQLException {
        if (pedidoNumero == null || pedidoNumero.isBlank()) return 0;
        int id = buscarId(conn, "SELECT id FROM pedidos WHERE numero=?", pedidoNumero.trim());
        if (id > 0) return id;
        errores.add(new RowError(numFila, "pedido_numero", pedidoNumero, ErrorTipo.OTRO,
            "Pedido con numero '" + pedidoNumero + "' no encontrado para el albaran"));
        return -1;
    }
}
