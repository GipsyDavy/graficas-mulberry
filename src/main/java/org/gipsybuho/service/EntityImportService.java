package org.gipsybuho.service;

import org.gipsybuho.dao.*;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.*;
import org.gipsybuho.service.importer.*;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Importa filas ya parseadas y mapeadas a una entidad del dominio.
 *
 * <p>Flujo:
 * <ol>
 *   <li>Fase 1 — mapearValores: renombra las claves-header a claves-campo y sanitiza strings.</li>
 *   <li>Fase 2 — validarTodas: comprueba NOT NULL, tipos numéricos y longitudes en memoria, sin BD.</li>
 *   <li>Fase 3 — insertarEnTransaccion: aplica DuplicatePolicy e inserta/actualiza en una transacción única.</li>
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
        "salario_base", "irpf", "precio_unit", "precio_setup", "minimo_unidades"
    );
    private static final Set<String> CAMPOS_LIBRES = Set.of("notas", "descripcion");

    /** Fila válida que ha superado la fase 2, con su número original de fila (1-based). */
    private record ValidRow(int numero, Map<String, String> vals) {}

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
        int[] cnt = insertarEnTransaccion(spec, validas, policy, errores);

        return new ImportResult(
            filas.size(),
            cnt[0],                             // insertadas
            cnt[1],                             // actualizadas
            filas.size() - cnt[0] - cnt[1],     // descartadas = total − insertadas − actualizadas
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
        }
        return out;
    }

    private boolean esNumerico(String s) {
        try {
            Double.parseDouble(s.replace(",", ".").replaceAll("[^0-9.\\-]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ── Fase 3: transacción única ─────────────────────────────────────────────

    private int[] insertarEnTransaccion(EntityImportSpec spec, List<ValidRow> validas,
                                         DuplicatePolicy policy, List<RowError> errores) throws Exception {
        int insertadas = 0, actualizadas = 0;
        Connection conn = DatabaseManager.getConnection();
        boolean prevAC = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            for (ValidRow vr : validas) {
                Savepoint sp = conn.setSavepoint();
                try {
                    int[] r = procesarFila(conn, spec, vr, policy, errores);
                    insertadas   += r[0];
                    actualizadas += r[1];
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

    private int[] procesarFila(Connection conn, EntityImportSpec spec, ValidRow vr,
                                DuplicatePolicy policy, List<RowError> errores) throws SQLException {
        return switch (spec.nombre()) {
            case "Materiales" -> procesarMaterial(conn, vr, policy, errores);
            case "Empleados"  -> procesarEmpleado(conn, vr, policy, errores);
            case "Clientes"   -> procesarCliente(conn, vr, policy, errores);
            case "Tarifas"    -> procesarTarifa(conn, vr, policy, errores);
            default -> throw new IllegalArgumentException("Entidad no soportada: " + spec.nombre());
        };
    }

    // ── Material ──────────────────────────────────────────────────────────────

    private int[] procesarMaterial(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                    List<RowError> errores) throws SQLException {
        MaterialDAO dao = new MaterialDAO();
        Material m = ensamblarMaterial(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(m);
            return new int[]{1, 0};
        }

        String ref = m.getReferencia();
        if (ref == null || ref.isBlank()) {
            errores.add(new RowError(vr.numero(), "referencia", ref, ErrorTipo.OTRO,
                "Sin clave de negocio ('referencia') requerida por la política " + policy));
            return new int[]{0, 0};
        }

        int existingId = buscarId(conn, "SELECT id FROM materiales WHERE referencia=?", ref);
        if (existingId == 0) {
            dao.save(m);
            return new int[]{1, 0};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0};
        }
        // UPDATE_EXISTING: cargar existente y sobreescribir solo los campos importados
        Material existente = dao.findById(existingId);
        aplicarValoresMaterial(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1};
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
                case "unidad"        -> m.setUnidad(v);
                case "precio_unidad" -> m.setPrecioUnidad(toDouble(v));
                case "proveedor"     -> m.setProveedor(v);
            }
        });
    }

    // ── Empleado ──────────────────────────────────────────────────────────────

    private int[] procesarEmpleado(Connection conn, ValidRow vr, DuplicatePolicy policy,
                                    List<RowError> errores) throws SQLException {
        EmpleadoDAO dao = new EmpleadoDAO();
        Empleado emp = ensamblarEmpleado(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(emp);
            return new int[]{1, 0};
        }

        String nif = emp.getNif();
        if (nif == null || nif.isBlank()) {
            errores.add(new RowError(vr.numero(), "nif", nif, ErrorTipo.OTRO,
                "Sin clave de negocio ('nif') requerida por la política " + policy));
            return new int[]{0, 0};
        }

        int existingId = buscarId(conn, "SELECT id FROM empleados WHERE nif=?", nif);
        if (existingId == 0) {
            dao.save(emp);
            return new int[]{1, 0};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0};
        }
        Empleado existente = dao.findById(existingId);
        aplicarValoresEmpleado(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1};
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
        ClienteDAO dao = new ClienteDAO();
        Cliente c = ensamblarCliente(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(c);
            return new int[]{1, 0};
        }

        String nif = c.getNif();
        if (nif == null || nif.isBlank()) {
            errores.add(new RowError(vr.numero(), "nif", nif, ErrorTipo.OTRO,
                "Sin clave de negocio ('nif') requerida por la política " + policy));
            return new int[]{0, 0};
        }

        int existingId = buscarId(conn, "SELECT id FROM clientes WHERE nif=?", nif);
        if (existingId == 0) {
            dao.save(c);
            return new int[]{1, 0};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0};
        }
        Cliente existente = dao.findById(existingId);
        aplicarValoresCliente(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1};
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
        TarifaDAO dao = new TarifaDAO();
        Tarifa t = ensamblarTarifa(vr.vals());

        if (policy == DuplicatePolicy.CREATE_NEW) {
            dao.save(t);
            return new int[]{1, 0};
        }

        String tecnica = t.getTecnica();
        String nombre  = t.getNombre();
        // tecnica y nombre son NOT NULL en el spec, pero check defensivo para UPDATE
        if ((tecnica == null || tecnica.isBlank()) || (nombre == null || nombre.isBlank())) {
            errores.add(new RowError(vr.numero(), "tecnica+nombre",
                tecnica + "/" + nombre, ErrorTipo.OTRO,
                "Sin clave de negocio ('tecnica'+'nombre') requerida por la política " + policy));
            return new int[]{0, 0};
        }

        // TODO: añadir UNIQUE(tecnica, nombre) en migración futura para defensa en profundidad
        int existingId = buscarIdTarifa(conn, tecnica, nombre);
        if (existingId == 0) {
            dao.save(t);
            return new int[]{1, 0};
        }
        if (policy == DuplicatePolicy.SKIP_IF_EXISTS) {
            return new int[]{0, 0};
        }
        Tarifa existente = dao.findById(existingId);
        aplicarValoresTarifa(existente, vr.vals());
        dao.save(existente);
        return new int[]{0, 1};
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

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Busca el id de un registro por una sola clave de negocio. Devuelve 0 si no existe. */
    private int buscarId(Connection conn, String sql, String valor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Busca el id de una tarifa por su clave compuesta (tecnica, nombre). */
    private int buscarIdTarifa(Connection conn, String tecnica, String nombre) throws SQLException {
        String sql = "SELECT id FROM tarifas WHERE tecnica=? AND nombre=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tecnica);
            ps.setString(2, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double toDouble(String s) {
        try {
            return Double.parseDouble(s.replace(",", ".").replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int toInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9\\-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
