package org.gipsybuho.service;

import org.apache.poi.ss.usermodel.*;
import org.gipsybuho.dao.*;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Importación genérica desde Excel para cualquier módulo del ERP.
 * Detecta automáticamente el tipo de datos (Materiales, Clientes, Empleados,
 * Presupuestos, Pedidos, Albaranes) según las columnas encontradas en cada hoja.
 */
public class ImportarDatosService {

    // ── Tipo detectado por hoja ───────────────────────────────────────────────

    public enum TipoImportacion {
        MATERIALES, CLIENTES, EMPLEADOS, PRESUPUESTOS, PEDIDOS, ALBARANES, GENERICO;

        public String etiqueta() {
            return switch (this) {
                case MATERIALES   -> "Materiales/Tarifas";
                case CLIENTES     -> "Clientes";
                case EMPLEADOS    -> "Empleados";
                case PRESUPUESTOS -> "Presupuestos";
                case PEDIDOS      -> "Pedidos";
                case ALBARANES    -> "Albaranes";
                case GENERICO     -> "Genérico (sin importar)";
            };
        }

        public static TipoImportacion fromString(String s) {
            if (s == null || s.isBlank()) return null;
            return switch (s.toLowerCase().trim()) {
                case "materiales", "material", "tarifas_proveedor", "tarifas" -> MATERIALES;
                case "clientes", "cliente"     -> CLIENTES;
                case "empleados", "empleado"   -> EMPLEADOS;
                case "presupuestos", "presupuesto" -> PRESUPUESTOS;
                case "pedidos", "pedido"       -> PEDIDOS;
                case "albaranes", "albaran", "albarán" -> ALBARANES;
                default -> GENERICO;
            };
        }
    }

    // ── DAOs ──────────────────────────────────────────────────────────────────

    private final MaterialDAO    materialDAO    = new MaterialDAO();
    private final ClienteDAO     clienteDAO     = new ClienteDAO();
    private final EmpleadoDAO    empleadoDAO    = new EmpleadoDAO();
    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAO();
    private final PedidoDAO      pedidoDAO      = new PedidoDAO();
    private final AlbaranDAO     albaranDAO     = new AlbaranDAO();

    // ── Etiquetas amigables de columna ────────────────────────────────────────

    private static final Map<String, String> ETIQUETA = Map.ofEntries(
        Map.entry("nombre",        "Nombre"),        Map.entry("apellidos",   "Apellidos"),
        Map.entry("nif",           "NIF/CIF"),       Map.entry("email",       "Email"),
        Map.entry("telefono",      "Teléfono"),      Map.entry("direccion",   "Dirección"),
        Map.entry("ciudad",        "Ciudad"),         Map.entry("provincia",   "Provincia"),
        Map.entry("cp",            "C.P."),           Map.entry("tipo",        "Tipo"),
        Map.entry("precio",        "Precio"),         Map.entry("total",       "Total"),
        Map.entry("iva",           "IVA%"),           Map.entry("referencia",  "Referencia"),
        Map.entry("proveedor",     "Proveedor"),      Map.entry("categoria",   "Categoría"),
        Map.entry("unidad",        "Unidad"),         Map.entry("stock",       "Stock"),
        Map.entry("stockMinimo",   "Stock mín."),     Map.entry("gramaje",     "Gramaje"),
        Map.entry("formato",       "Formato"),        Map.entry("salarioBase", "Salario base"),
        Map.entry("irpf",          "IRPF%"),          Map.entry("iban",        "IBAN"),
        Map.entry("fechaAlta",     "Fecha alta"),     Map.entry("horasSemanales", "Horas sem."),
        Map.entry("numDocumento",  "Nº documento"),   Map.entry("clienteNombre",  "Cliente"),
        Map.entry("fechaEntrega",  "Fecha entrega"),  Map.entry("estado",      "Estado"),
        Map.entry("fecha",         "Fecha"),           Map.entry("notas",       "Notas"),
        Map.entry("razonSocial",   "Razón Social"),    Map.entry("concepto",    "Concepto")
    );

    // ── Records internos ──────────────────────────────────────────────────────

    private record InfoHoja(String nombre, TipoImportacion tipo,
                             Map<String, Integer> colMap, int headerRow) {}

    private static class Cont {
        int c = 0, a = 0, o = 0;
        final List<String> err = new ArrayList<>();
    }

    @FunctionalInterface
    private interface DaoLoader<T> { List<T> load() throws Exception; }

    // ── Records públicos de resultado ─────────────────────────────────────────

    public record AnalisisExcel(
        int totalHojas, int hojasConDatos, int totalFilas,
        List<String> nombresHojas,
        Map<String, TipoImportacion> tipoPorHoja,
        Map<String, List<String>> columnasPorHoja,
        Map<String, Integer> filasPorHoja,
        int estimadosCrear, int estimadosActualizar, int estimadosSinNombre
    ) {
        public String resumenCorto() {
            return hojasConDatos + " hoja(s) · ~" + totalFilas + " fila(s)"
                + " · estimación: " + estimadosCrear + " nuevos, "
                + estimadosActualizar + " a actualizar";
        }

        public String resumenCompleto() {
            StringBuilder sb = new StringBuilder();
            sb.append("Análisis completado (sin cambios en la base de datos):\n\n");
            sb.append("Hojas analizadas: ").append(hojasConDatos).append(" de ").append(totalHojas).append("\n");
            for (String hoja : nombresHojas) {
                TipoImportacion tipo = tipoPorHoja.getOrDefault(hoja, TipoImportacion.GENERICO);
                List<String> cols = columnasPorHoja.getOrDefault(hoja, List.of());
                int filas = filasPorHoja.getOrDefault(hoja, 0);
                sb.append("  • «").append(hoja).append("» → ").append(tipo.etiqueta())
                  .append(" (").append(filas).append(" filas)\n")
                  .append("    Columnas detectadas: ").append(String.join(", ", cols)).append("\n");
            }
            sb.append("\nTotal filas de datos: ~").append(totalFilas).append("\n\n");
            sb.append("Estimación de cambios al importar:\n");
            sb.append("  ✨ Registros nuevos a crear: ").append(estimadosCrear).append("\n");
            sb.append("  🔄 Registros a actualizar: ").append(estimadosActualizar).append("\n");
            if (estimadosSinNombre > 0)
                sb.append("  ⚠ Filas sin identificador (se omitirían): ").append(estimadosSinNombre).append("\n");
            sb.append("\nCuando estés listo, pide que se ejecute la importación real.");
            return sb.toString().trim();
        }
    }

    public record ResultadoImportacion(
        int creados, int actualizados, int omitidos,
        List<String> errores, List<String> resumenPorHoja
    ) {
        public String resumen() {
            StringBuilder sb = new StringBuilder();
            sb.append("✨ ").append(creados).append(" nuevo(s)  ·  ")
              .append("🔄 ").append(actualizados).append(" actualizado(s)  ·  ")
              .append(omitidos).append(" omitido(s)");
            if (!resumenPorHoja.isEmpty()) {
                sb.append("\n\n");
                resumenPorHoja.forEach(r -> sb.append("  • ").append(r).append("\n"));
            }
            if (!errores.isEmpty()) {
                sb.append("\n⚠ Problemas (").append(errores.size()).append("):\n");
                errores.stream().limit(5).forEach(e -> sb.append("  • ").append(e).append("\n"));
                if (errores.size() > 5)
                    sb.append("  … y ").append(errores.size() - 5).append(" más.");
            }
            return sb.toString().trim();
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    public AnalisisExcel analizarExcel(File archivo, AccionERP accion) throws Exception {
        TipoImportacion tipoForzado = tipoExplicitoDe(accion);
        String provDef = proveedorDefaultDe(accion);

        List<Material> matExist = safeLoad(materialDAO::findAll);
        List<Cliente>  cliExist = safeLoad(clienteDAO::findAll);
        List<Empleado> empExist = safeLoad(empleadoDAO::findAll);

        int totalHojas, hojasConDatos = 0, totalFilas = 0;
        int estCrear = 0, estActualizar = 0, estSinId = 0;
        List<String> nombresHojas = new ArrayList<>();
        Map<String, TipoImportacion> tipoPH  = new LinkedHashMap<>();
        Map<String, List<String>>    colsPH  = new LinkedHashMap<>();
        Map<String, Integer>         filasPH = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook wb = WorkbookFactory.create(fis)) {

            totalHojas = wb.getNumberOfSheets();
            List<InfoHoja> hojas = descubrirHojas(wb, accion);

            for (InfoHoja info : hojas) {
                TipoImportacion tipo = tipoForzado != null ? tipoForzado : info.tipo();
                hojasConDatos++;
                nombresHojas.add(info.nombre());
                tipoPH.put(info.nombre(), tipo);
                colsPH.put(info.nombre(), columnasFriendly(info.colMap()));

                Sheet sheet = wb.getSheet(info.nombre());
                int filasHoja = 0;

                for (int ri = info.headerRow() + 1; ri <= sheet.getLastRowNum(); ri++) {
                    Row row = sheet.getRow(ri);
                    if (row == null || !tieneContenido(row)) continue;

                    String id = resolverIdentificador(row, info.colMap(), tipo);
                    if (!esValido(id)) { estSinId++; continue; }
                    filasHoja++;
                    totalFilas++;

                    boolean existe = estimarExistencia(id, row, info.colMap(), tipo,
                        matExist, cliExist, empExist, provDef);
                    if (existe) estActualizar++; else estCrear++;
                }
                filasPH.put(info.nombre(), filasHoja);
            }
            return new AnalisisExcel(totalHojas, hojasConDatos, totalFilas, nombresHojas,
                tipoPH, colsPH, filasPH, estCrear, estActualizar, estSinId);
        }
    }

    public ResultadoImportacion importarDesdeExcel(File archivo, AccionERP accion) throws Exception {
        TipoImportacion tipoForzado = tipoExplicitoDe(accion);
        boolean actualizar   = accion.data == null || accion.data.actualizarExistentes == null || accion.data.actualizarExistentes;
        boolean crearNuevos  = accion.data == null || accion.data.crearNuevos == null          || accion.data.crearNuevos;
        String  provDef      = proveedorDefaultDe(accion);

        List<Material> matExist = safeLoad(materialDAO::findAll);
        List<Cliente>  cliExist = safeLoad(clienteDAO::findAll);
        List<Empleado> empExist = safeLoad(empleadoDAO::findAll);

        int totC = 0, totA = 0, totO = 0;
        List<String> todosErr = new ArrayList<>();
        List<String> resumHojas = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook wb = WorkbookFactory.create(fis)) {

            for (InfoHoja info : descubrirHojas(wb, accion)) {
                TipoImportacion tipo = tipoForzado != null ? tipoForzado : info.tipo();
                Sheet sheet = wb.getSheet(info.nombre());
                Cont cont = new Cont();

                switch (tipo) {
                    case MATERIALES   -> importarMateriales(sheet, info, cont, matExist, provDef, actualizar, crearNuevos);
                    case CLIENTES     -> importarClientes(sheet, info, cont, cliExist, actualizar, crearNuevos);
                    case EMPLEADOS    -> importarEmpleados(sheet, info, cont, empExist, actualizar, crearNuevos);
                    case PRESUPUESTOS -> importarPresupuestos(sheet, info, cont);
                    case PEDIDOS      -> importarPedidos(sheet, info, cont);
                    case ALBARANES    -> importarAlbaranes(sheet, info, cont);
                    default           -> cont.o += contarFilas(sheet, info.headerRow());
                }

                todosErr.addAll(cont.err);
                totC += cont.c; totA += cont.a; totO += cont.o;
                resumHojas.add(String.format("«%s» [%s]: %d creado(s), %d actualizado(s)%s",
                    info.nombre(), tipo.etiqueta(), cont.c, cont.a,
                    cont.err.isEmpty() ? "" : ", " + cont.err.size() + " error(es)"));
            }
        }
        return new ResultadoImportacion(totC, totA, totO, todosErr, resumHojas);
    }

    // ── Descubrimiento de hojas ───────────────────────────────────────────────

    private List<InfoHoja> descubrirHojas(Workbook wb, AccionERP accion) {
        List<String> filtro = hojasAImportarDe(accion);
        List<InfoHoja> resultado = new ArrayList<>();
        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            Sheet sheet = wb.getSheetAt(si);
            String nombre = sheet.getSheetName();
            if (!debeIncluirHoja(nombre, filtro)) continue;
            int hRow = detectarFilaCabecera(sheet);
            if (hRow < 0) continue;
            Map<String, Integer> colMap = detectarColumnas(sheet.getRow(hRow));
            if (!tieneColumnaIdentificadora(colMap)) continue;
            resultado.add(new InfoHoja(nombre, inferirTipo(colMap), colMap, hRow));
        }
        return resultado;
    }

    // ── Handlers por tipo ─────────────────────────────────────────────────────

    private void importarMateriales(Sheet sheet, InfoHoja info, Cont cont,
                                     List<Material> exist, String provDef,
                                     boolean actualizar, boolean crearNuevos) {
        Map<String, Integer> c = info.colMap();
        for (int ri = info.headerRow() + 1; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null || !tieneContenido(row)) continue;
            try {
                String nombre = resolverNombreMaterial(row, c);
                if (!esValido(nombre)) { cont.o++; continue; }

                String prov = provDef;
                if (c.containsKey("proveedor")) { String p = getString(row, c.get("proveedor")); if (esValido(p)) prov = p; }
                final String nF = nombre.trim(), pF = prov;

                Material existente = exist.stream().filter(m -> {
                    if (!m.getNombre().equalsIgnoreCase(nF)) return false;
                    if (pF != null && m.getProveedor() != null) return m.getProveedor().equalsIgnoreCase(pF);
                    return true;
                }).findFirst().orElse(null);

                if (existente != null) {
                    if (!actualizar) { cont.o++; continue; }
                    double pr = getDouble(row, c.get("precio"));
                    if (pr > 0)                            existente.setPrecioUnidad(pr);
                    if (esValido(getString(row, c.get("referencia")))) existente.setReferencia(getString(row, c.get("referencia")));
                    if (esValido(getString(row, c.get("categoria"))))  existente.setCategoria(getString(row, c.get("categoria")));
                    if (esValido(getString(row, c.get("unidad"))))     existente.setUnidad(getString(row, c.get("unidad")));
                    if (esValido(pF))                      existente.setProveedor(pF);
                    materialDAO.save(existente);
                    cont.a++;
                } else {
                    if (!crearNuevos) { cont.o++; continue; }
                    Material m = new Material();
                    m.setNombre(nF);
                    String ref = getString(row, c.get("referencia")); if (esValido(ref)) m.setReferencia(ref);
                    String cat = getString(row, c.get("categoria"));
                    m.setCategoria(esValido(cat) ? cat : "sustratos");
                    m.setStockActual(getDouble(row, c.get("stock")));
                    double stMin = getDouble(row, c.get("stockMinimo"));
                    m.setStockMinimo(stMin > 0 ? stMin : 1.0);
                    String ud = getString(row, c.get("unidad")); m.setUnidad(esValido(ud) ? ud : "ud");
                    m.setPrecioUnidad(getDouble(row, c.get("precio")));
                    m.setProveedor(esValido(pF) ? pF : "");
                    materialDAO.save(m);
                    exist.add(m);
                    cont.c++;
                }
            } catch (Exception e) { cont.err.add("Mat r" + (ri + 1) + ": " + msg(e)); }
        }
    }

    private void importarClientes(Sheet sheet, InfoHoja info, Cont cont,
                                   List<Cliente> exist, boolean actualizar, boolean crearNuevos) {
        Map<String, Integer> c = info.colMap();
        for (int ri = info.headerRow() + 1; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null || !tieneContenido(row)) continue;
            try {
                String nombre = getString(row, c.get("nombre"));
                if (!esValido(nombre)) { cont.o++; continue; }
                String nif = getString(row, c.get("nif"));
                String aps = getString(row, c.get("apellidos"));
                final String nF = nombre.trim(), nifF = esValido(nif) ? nif.trim() : null, apsF = esValido(aps) ? aps.trim() : null;

                Cliente existente = exist.stream().filter(cli -> {
                    if (nifF != null && cli.getNif() != null) return cli.getNif().equalsIgnoreCase(nifF);
                    String full = apsF != null ? nF + " " + apsF : nF;
                    return cli.getNombreCompleto().equalsIgnoreCase(full);
                }).findFirst().orElse(null);

                if (existente != null) {
                    if (!actualizar) { cont.o++; continue; }
                    rellenarCliente(existente, row, c);
                    clienteDAO.save(existente);
                    cont.a++;
                } else {
                    if (!crearNuevos) { cont.o++; continue; }
                    Cliente cli = new Cliente();
                    cli.setNombre(nF);
                    if (esValido(apsF)) cli.setApellidos(apsF);
                    cli.setTipo(inferirTipoCliente(getString(row, c.get("tipo")), nifF));
                    if (esValido(nifF)) cli.setNif(nifF);
                    rellenarCliente(cli, row, c);
                    clienteDAO.save(cli);
                    exist.add(cli);
                    cont.c++;
                }
            } catch (Exception e) { cont.err.add("Cli r" + (ri + 1) + ": " + msg(e)); }
        }
    }

    private void rellenarCliente(Cliente cli, Row row, Map<String, Integer> c) {
        set(cli::setEmail,     getString(row, c.get("email")));
        set(cli::setTelefono,  getString(row, c.get("telefono")));
        set(cli::setDireccion, getString(row, c.get("direccion")));
        set(cli::setCiudad,    getString(row, c.get("ciudad")));
        set(cli::setCp,        getString(row, c.get("cp")));
        set(cli::setNotas,     getString(row, c.get("notas")));
    }

    private void importarEmpleados(Sheet sheet, InfoHoja info, Cont cont,
                                    List<Empleado> exist, boolean actualizar, boolean crearNuevos) {
        Map<String, Integer> c = info.colMap();
        for (int ri = info.headerRow() + 1; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null || !tieneContenido(row)) continue;
            try {
                String nombre = getString(row, c.get("nombre"));
                if (!esValido(nombre)) { cont.o++; continue; }
                String nif = getString(row, c.get("nif")), aps = getString(row, c.get("apellidos"));
                final String nF = nombre.trim(), nifF = esValido(nif) ? nif.trim() : null, apsF = esValido(aps) ? aps.trim() : null;

                Empleado existente = exist.stream().filter(emp -> {
                    if (nifF != null && emp.getNif() != null) return emp.getNif().equalsIgnoreCase(nifF);
                    String full = apsF != null ? nF + " " + apsF : nF;
                    return emp.getNombreCompleto().equalsIgnoreCase(full);
                }).findFirst().orElse(null);

                if (existente != null) {
                    if (!actualizar) { cont.o++; continue; }
                    rellenarEmpleado(existente, row, c);
                    empleadoDAO.save(existente);
                    cont.a++;
                } else {
                    if (!crearNuevos) { cont.o++; continue; }
                    Empleado emp = new Empleado();
                    emp.setNombre(nF);
                    if (esValido(apsF)) emp.setApellidos(apsF);
                    if (esValido(nifF)) emp.setNif(nifF);
                    emp.setActivo(true);
                    rellenarEmpleado(emp, row, c);
                    empleadoDAO.save(emp);
                    exist.add(emp);
                    cont.c++;
                }
            } catch (Exception e) { cont.err.add("Emp r" + (ri + 1) + ": " + msg(e)); }
        }
    }

    private void rellenarEmpleado(Empleado emp, Row row, Map<String, Integer> c) {
        double sal = getDouble(row, c.get("salarioBase")); if (sal > 0) emp.setSalarioBase(sal);
        double irpf = getDouble(row, c.get("irpf"));      if (irpf > 0) emp.setIrpf(irpf);
        set(emp::setCategoria, getString(row, c.get("categoria")));
        set(emp::setIban,      getString(row, c.get("iban")));
        set(emp::setEmail,     getString(row, c.get("email")));
        set(emp::setTelefono,  getString(row, c.get("telefono")));
        set(emp::setDireccion, getString(row, c.get("direccion")));
        String fa = getFechaStr(row, c.get("fechaAlta")); if (esValido(fa)) emp.setFechaAlta(fa);
    }

    private void importarPresupuestos(Sheet sheet, InfoHoja info, Cont cont) {
        Map<String, Integer> c = info.colMap();
        for (int ri = info.headerRow() + 1; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null || !tieneContenido(row)) continue;
            try {
                String cliente = primerValido(getString(row, c.get("clienteNombre")), getString(row, c.get("nombre")));
                if (!esValido(cliente)) { cont.o++; continue; }
                Presupuesto p = new Presupuesto();
                p.setNumero(DatabaseManager.generarNumeroPresupuesto());
                p.setFecha(getFechaStr(row, c.get("fecha")));
                p.setEstado("borrador");
                p.setClienteNombre(cliente.trim());
                double ivaV = getDouble(row, c.get("iva")); p.setIvaPorcentaje(ivaV > 0 ? ivaV : 21.0);
                p.setNotas(buildNotasImport(row, c));
                presupuestoDAO.save(p);
                cont.c++;
            } catch (Exception e) { cont.err.add("Pres r" + (ri + 1) + ": " + msg(e)); }
        }
    }

    private void importarPedidos(Sheet sheet, InfoHoja info, Cont cont) {
        Map<String, Integer> c = info.colMap();
        for (int ri = info.headerRow() + 1; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null || !tieneContenido(row)) continue;
            try {
                String cliente = primerValido(getString(row, c.get("clienteNombre")), getString(row, c.get("nombre")));
                if (!esValido(cliente)) { cont.o++; continue; }
                Pedido p = new Pedido();
                p.setNumero(DatabaseManager.generarNumeroPedido());
                String fechaStr = getFechaStr(row, c.get("fecha"));
                try { p.setFecha(LocalDate.parse(fechaStr)); } catch (Exception ex) { p.setFecha(LocalDate.now()); }
                String est = getString(row, c.get("estado")); p.setEstado(esValido(est) ? est : "pendiente");
                p.setClienteNombre(cliente.trim());
                p.setIvaPorcentaje(21.0);
                double total = getDouble(row, c.get("total")); if (total > 0) p.setImporteTotal(total);
                String fEnt = getFechaStr(row, c.get("fechaEntrega"));
                if (esValido(fEnt)) { try { p.setFechaEntregaPrevista(LocalDate.parse(fEnt)); } catch (Exception ignored) {} }
                p.setNotas(buildNotasImport(row, c));
                pedidoDAO.save(p);
                cont.c++;
            } catch (Exception e) { cont.err.add("Ped r" + (ri + 1) + ": " + msg(e)); }
        }
    }

    private void importarAlbaranes(Sheet sheet, InfoHoja info, Cont cont) {
        Map<String, Integer> c = info.colMap();
        for (int ri = info.headerRow() + 1; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null || !tieneContenido(row)) continue;
            try {
                String cliente = primerValido(getString(row, c.get("clienteNombre")), getString(row, c.get("nombre")));
                if (!esValido(cliente)) { cont.o++; continue; }
                Albaran a = new Albaran();
                a.setNumero(DatabaseManager.generarNumeroAlbaran());
                a.setFecha(getFechaStr(row, c.get("fecha")));
                String est = getString(row, c.get("estado")); a.setEstado(esValido(est) ? est : "pendiente");
                a.setClienteNombre(cliente.trim());
                a.setObservaciones(buildNotasImport(row, c));
                albaranDAO.save(a);
                cont.c++;
            } catch (Exception e) { cont.err.add("Alb r" + (ri + 1) + ": " + msg(e)); }
        }
    }

    // ── Detección de columnas ─────────────────────────────────────────────────

    private Map<String, Integer> detectarColumnas(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (headerRow == null) return map;
        for (Cell cell : headerRow) {
            String h = getCellString(cell).toLowerCase().trim();
            int col  = cell.getColumnIndex();
            if (h.isBlank()) continue;

            // Documentos — ANTES de "nombre" genérico
            if (matches(h, "nombre cliente", "razón social cliente", "cliente nombre", "nombre del cliente"))
                map.putIfAbsent("clienteNombre", col);
            else if (matches(h, "número presupuesto", "num presupuesto", "nº presupuesto",
                                "número pedido", "num pedido", "nº pedido",
                                "número albarán", "nº albarán", "num albarán", "número documento"))
                map.putIfAbsent("numDocumento", col);
            else if (matches(h, "fecha entrega", "entrega prevista", "fecha de entrega", "plazo entrega"))
                map.putIfAbsent("fechaEntrega", col);

            // Empleados
            else if (matches(h, "salario base", "sueldo base", "salario bruto", "salario mensual", "salario"))
                map.putIfAbsent("salarioBase", col);
            else if (matches(h, "iban", "cuenta bancaria", "cuenta corriente"))
                map.putIfAbsent("iban", col);
            else if (matches(h, "irpf", "retención", "retencion", "% irpf", "irpf%"))
                map.putIfAbsent("irpf", col);
            else if (matches(h, "fecha alta", "fecha contrato", "fecha incorporación", "alta empresa"))
                map.putIfAbsent("fechaAlta", col);
            else if (matches(h, "horas semanales", "jornada", "horas semana", "horas/semana"))
                map.putIfAbsent("horasSemanales", col);

            // Financiero — ANTES de "precio" para que "importe total" no colisione con "importe"
            else if (matches(h, "total", "importe total", "total iva", "total con iva"))
                map.putIfAbsent("total", col);
            else if (matches(h, "iva%", "% iva", "tipo iva", " iva"))
                map.putIfAbsent("iva", col);

            // Materiales / precios
            else if (matches(h, "precio resma", "precio/resma", "precio pliego", "precio/pliego",
                                "precio kg", "precio/kg", "precio ud", "precio unidad",
                                "precio", "coste", "costo", "pvp", "p.v.p", "importe", "tarifa", "rate", "price"))
                map.putIfAbsent("precio", col);
            else if (matches(h, "gramaje", "gramage", "gsm", "g/m2", "g/m²", "gramos", "grs", "gr/m", "peso papel"))
                map.putIfAbsent("gramaje", col);
            else if (matches(h, "formato", "tamaño", "tamano", "medidas", "dimensión", "dimension", "medida hoja", "pliego"))
                map.putIfAbsent("formato", col);
            else if (matches(h, "stock min", "stock mínimo", "stock minimo", "stock_min", "mínimo", "minimo"))
                map.putIfAbsent("stockMinimo", col);
            else if (matches(h, "stock", "existencias", "disponible", "qty", "inventario"))
                map.putIfAbsent("stock", col);

            // Contacto (cliente + empleado)
            else if (matches(h, "nif", "dni", "cif", "n.i.f", "c.i.f", "d.n.i", "documento"))
                map.putIfAbsent("nif", col);
            else if (matches(h, "email", "correo", "e-mail", "mail", "correo electrónico"))
                map.putIfAbsent("email", col);
            else if (matches(h, "teléfono", "telefono", "tel.", "telf.", "móvil", "movil", "phone"))
                map.putIfAbsent("telefono", col);
            else if (matches(h, "dirección", "direccion", "domicilio", "calle", "address"))
                map.putIfAbsent("direccion", col);
            else if (matches(h, "ciudad", "población", "localidad", "municipio", "city"))
                map.putIfAbsent("ciudad", col);
            else if (matches(h, "provincia", "prov."))
                map.putIfAbsent("provincia", col);
            else if (matches(h, "código postal", "cod postal", "c.p.", "postal"))
                map.putIfAbsent("cp", col);

            // Genéricos — AL FINAL para no capturar términos más específicos
            else if (matches(h, "apellidos", "apellido"))
                map.putIfAbsent("apellidos", col);
            else if (matches(h, "razón social", "razon social")) {
                map.putIfAbsent("nombre", col);
                map.putIfAbsent("razonSocial", col);
            } else if (matches(h, "nombre", "descripcion", "descripción", "material", "producto",
                                "artículo", "articulo", "item", "papel", "soporte", "sustrato",
                                "substrato", "tipo de papel", "tipo papel", "tipo soporte",
                                "tipo material", "estucado"))
                map.putIfAbsent("nombre", col);
            else if (matches(h, "referencia", "ref.", "ref ", "código", "codigo", "sku", "art.", "cód"))
                map.putIfAbsent("referencia", col);
            else if (matches(h, "proveedor", "suministrador", "fabricante", "marca", "supplier"))
                map.putIfAbsent("proveedor", col);
            else if (matches(h, "categoria", "categoría", "familia", "grupo", "gama", "tipo", "sección"))
                map.putIfAbsent("categoria", col);
            else if (matches(h, "unidad", "u.m.", "ud.", "unit", "u/m"))
                map.putIfAbsent("unidad", col);
            else if (matches(h, "estado", "status", "situación"))
                map.putIfAbsent("estado", col);
            else if (matches(h, "fecha", "date", "fecha doc", "fecha emisión"))
                map.putIfAbsent("fecha", col);
            else if (matches(h, "concepto", "concepto/descripción", "descripción servicio", "concepto factura"))
                map.putIfAbsent("concepto", col);
            else if (matches(h, "notas", "observaciones", "comentarios", "nota", "obs"))
                map.putIfAbsent("notas", col);
            else if (matches(h, "tipo cliente", "tipo empresa", "tipo persona"))
                map.putIfAbsent("tipo", col);
            else if (matches(h, "cantidad", "qty", "cant."))
                map.putIfAbsent("stock", col);
        }
        return map;
    }

    // ── Inferencia de tipo ────────────────────────────────────────────────────

    private TipoImportacion inferirTipo(Map<String, Integer> c) {
        Map<TipoImportacion, Integer> score = new EnumMap<>(TipoImportacion.class);
        for (TipoImportacion t : TipoImportacion.values()) score.put(t, 0);

        // MATERIALES
        if (c.containsKey("gramaje"))     score.merge(TipoImportacion.MATERIALES, 5, Integer::sum);
        if (c.containsKey("formato"))     score.merge(TipoImportacion.MATERIALES, 4, Integer::sum);
        if (c.containsKey("stockMinimo")) score.merge(TipoImportacion.MATERIALES, 3, Integer::sum);
        if (c.containsKey("referencia"))  score.merge(TipoImportacion.MATERIALES, 2, Integer::sum);
        if (c.containsKey("unidad"))      score.merge(TipoImportacion.MATERIALES, 2, Integer::sum);
        if (c.containsKey("stock"))       score.merge(TipoImportacion.MATERIALES, 2, Integer::sum);
        if (c.containsKey("proveedor") && c.containsKey("precio")
                && !c.containsKey("nif") && !c.containsKey("email"))
            score.merge(TipoImportacion.MATERIALES, 3, Integer::sum);

        // CLIENTES
        if (c.containsKey("nif"))         score.merge(TipoImportacion.CLIENTES, 4, Integer::sum);
        if (c.containsKey("razonSocial")) score.merge(TipoImportacion.CLIENTES, 4, Integer::sum);
        if (c.containsKey("email"))       score.merge(TipoImportacion.CLIENTES, 3, Integer::sum);
        if (c.containsKey("ciudad"))      score.merge(TipoImportacion.CLIENTES, 2, Integer::sum);
        if (c.containsKey("cp"))          score.merge(TipoImportacion.CLIENTES, 2, Integer::sum);
        if (c.containsKey("provincia"))   score.merge(TipoImportacion.CLIENTES, 2, Integer::sum);
        if (c.containsKey("telefono"))    score.merge(TipoImportacion.CLIENTES, 2, Integer::sum);
        if (c.containsKey("direccion"))   score.merge(TipoImportacion.CLIENTES, 2, Integer::sum);

        // EMPLEADOS
        if (c.containsKey("iban"))           score.merge(TipoImportacion.EMPLEADOS, 6, Integer::sum);
        if (c.containsKey("salarioBase"))    score.merge(TipoImportacion.EMPLEADOS, 5, Integer::sum);
        if (c.containsKey("irpf"))           score.merge(TipoImportacion.EMPLEADOS, 5, Integer::sum);
        if (c.containsKey("horasSemanales")) score.merge(TipoImportacion.EMPLEADOS, 4, Integer::sum);
        if (c.containsKey("fechaAlta"))      score.merge(TipoImportacion.EMPLEADOS, 3, Integer::sum);

        // PRESUPUESTOS
        if (c.containsKey("total") && c.containsKey("iva"))
            score.merge(TipoImportacion.PRESUPUESTOS, 5, Integer::sum);
        if (c.containsKey("clienteNombre") && c.containsKey("total"))
            score.merge(TipoImportacion.PRESUPUESTOS, 4, Integer::sum);
        if (c.containsKey("numDocumento") && c.containsKey("total"))
            score.merge(TipoImportacion.PRESUPUESTOS, 3, Integer::sum);
        if (c.containsKey("concepto"))       score.merge(TipoImportacion.PRESUPUESTOS, 2, Integer::sum);
        if (c.containsKey("total"))          score.merge(TipoImportacion.PRESUPUESTOS, 1, Integer::sum);

        // PEDIDOS
        if (c.containsKey("fechaEntrega"))   score.merge(TipoImportacion.PEDIDOS, 5, Integer::sum);
        if (c.containsKey("estado") && c.containsKey("numDocumento"))
            score.merge(TipoImportacion.PEDIDOS, 4, Integer::sum);
        if (c.containsKey("estado"))         score.merge(TipoImportacion.PEDIDOS, 1, Integer::sum);

        // ALBARANES
        if (c.containsKey("numDocumento") && !c.containsKey("total"))
            score.merge(TipoImportacion.ALBARANES, 3, Integer::sum);

        return score.entrySet().stream()
            .filter(e -> e.getValue() >= 2 && e.getKey() != TipoImportacion.GENERICO)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(TipoImportacion.GENERICO);
    }

    // ── Resolvers ─────────────────────────────────────────────────────────────

    private String resolverNombreMaterial(Row row, Map<String, Integer> c) {
        String base = getString(row, c.get("nombre"));
        if (!esValido(base)) return null;
        StringBuilder sb = new StringBuilder(base.trim());
        if (c.containsKey("gramaje")) {
            String g = getString(row, c.get("gramaje"));
            if (esValido(g)) {
                String suf;
                try { suf = (int) Double.parseDouble(g.replace(",", ".").replaceAll("[^\\d.]", "")) + "g"; }
                catch (NumberFormatException e) { suf = g.trim().endsWith("g") ? g.trim() : g.trim() + "g"; }
                if (!sb.toString().toLowerCase().contains(suf.toLowerCase())) sb.append(" ").append(suf);
            }
        }
        if (c.containsKey("formato")) {
            String f = getString(row, c.get("formato"));
            if (esValido(f) && !sb.toString().toLowerCase().contains(f.trim().toLowerCase()))
                sb.append(" ").append(f.trim());
        }
        return sb.toString();
    }

    private String resolverIdentificador(Row row, Map<String, Integer> c, TipoImportacion tipo) {
        return tipo == TipoImportacion.MATERIALES
            ? resolverNombreMaterial(row, c)
            : primerValido(getString(row, c.get("nombre")),
                           getString(row, c.get("clienteNombre")),
                           getString(row, c.get("numDocumento")));
    }

    private boolean estimarExistencia(String id, Row row, Map<String, Integer> c, TipoImportacion tipo,
                                       List<Material> mat, List<Cliente> cli, List<Empleado> emp,
                                       String provDef) {
        return switch (tipo) {
            case MATERIALES -> {
                String prov = provDef;
                if (c.containsKey("proveedor")) { String p = getString(row, c.get("proveedor")); if (esValido(p)) prov = p; }
                final String pF = prov, nF = id;
                yield mat.stream().anyMatch(m -> {
                    if (!m.getNombre().equalsIgnoreCase(nF)) return false;
                    if (pF != null && m.getProveedor() != null) return m.getProveedor().equalsIgnoreCase(pF);
                    return true;
                });
            }
            case CLIENTES -> {
                String nif = getString(row, c.get("nif"));
                final String nifF = esValido(nif) ? nif.trim() : null;
                yield cli.stream().anyMatch(cl -> {
                    if (nifF != null && cl.getNif() != null) return cl.getNif().equalsIgnoreCase(nifF);
                    return cl.getNombreCompleto().equalsIgnoreCase(id);
                });
            }
            case EMPLEADOS -> {
                String nif = getString(row, c.get("nif"));
                final String nifF = esValido(nif) ? nif.trim() : null;
                yield emp.stream().anyMatch(e -> {
                    if (nifF != null && e.getNif() != null) return e.getNif().equalsIgnoreCase(nifF);
                    return e.getNombreCompleto().equalsIgnoreCase(id);
                });
            }
            default -> false; // documentos: siempre nuevos
        };
    }

    private String getFechaStr(Row row, Integer colIndex) {
        if (colIndex == null || row == null) return LocalDate.now().toString();
        Cell cell = row.getCell(colIndex);
        if (cell == null) return LocalDate.now().toString();
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try { return cell.getLocalDateTimeCellValue().toLocalDate().toString(); } catch (Exception ignored) {}
        }
        String s = getCellString(cell).trim();
        if (!s.isBlank()) {
            try { LocalDate.parse(s); return s; } catch (Exception ignored) {}
            try {
                String[] p = s.split("[/\\-.]");
                if (p.length == 3)
                    return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0])).toString();
            } catch (Exception ignored) {}
        }
        return LocalDate.now().toString();
    }

    private String buildNotasImport(Row row, Map<String, Integer> c) {
        StringBuilder sb = new StringBuilder("Importado desde Excel.");
        String numRef = getString(row, c.get("numDocumento")); if (esValido(numRef)) sb.append(" Ref.: ").append(numRef).append(".");
        double total  = getDouble(row, c.get("total"));        if (total > 0)         sb.append(String.format(" Total ref.: %.2f €.", total));
        String notas  = getString(row, c.get("notas"));        if (esValido(notas))   sb.append(" ").append(notas);
        return sb.toString();
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private int detectarFilaCabecera(Sheet sheet) {
        for (Row row : sheet) { if (tieneContenido(row)) return row.getRowNum(); }
        return -1;
    }

    private boolean tieneColumnaIdentificadora(Map<String, Integer> c) {
        return c.containsKey("nombre") || c.containsKey("clienteNombre") || c.containsKey("numDocumento");
    }

    private int contarFilas(Sheet sheet, int headerRow) {
        int n = 0;
        for (int ri = headerRow + 1; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri); if (row != null && tieneContenido(row)) n++;
        }
        return n;
    }

    private List<String> columnasFriendly(Map<String, Integer> colMap) {
        return colMap.keySet().stream().map(k -> ETIQUETA.getOrDefault(k, k)).collect(Collectors.toList());
    }

    private String inferirTipoCliente(String valor, String nif) {
        if (esValido(valor) && valor.trim().toLowerCase().contains("particular")) return "particular";
        if (nif != null && nif.length() >= 1 && Character.isLetter(nif.charAt(0))) return "empresa";
        return "empresa";
    }

    private String primerValido(String... valores) {
        for (String v : valores) { if (esValido(v)) return v; }
        return null;
    }

    @FunctionalInterface interface Setter { void set(String v); }
    private void set(Setter setter, String value) { if (esValido(value)) setter.set(value); }

    private <T> List<T> safeLoad(DaoLoader<T> loader) {
        try { return loader.load(); } catch (Exception e) { return new ArrayList<>(); }
    }

    private List<String> hojasAImportarDe(AccionERP accion) {
        return (accion.data != null && accion.data.hojasAImportar != null && !accion.data.hojasAImportar.isEmpty())
            ? accion.data.hojasAImportar : null;
    }

    private String proveedorDefaultDe(AccionERP accion) {
        return (accion.data != null && esValido(accion.data.proveedor)) ? accion.data.proveedor.trim() : null;
    }

    private TipoImportacion tipoExplicitoDe(AccionERP accion) {
        if (accion.data == null || !esValido(accion.data.tipoImportacion)) return null;
        return TipoImportacion.fromString(accion.data.tipoImportacion);
    }

    private boolean debeIncluirHoja(String nombre, List<String> filtro) {
        if (filtro == null) return true;
        return filtro.stream().anyMatch(h -> h.equalsIgnoreCase(nombre));
    }

    private boolean matches(String header, String... keywords) {
        for (String kw : keywords) { if (header.contains(kw)) return true; }
        return false;
    }

    private boolean tieneContenido(Row row) {
        for (Cell cell : row) { if (!getCellString(cell).isBlank()) return true; }
        return false;
    }

    private boolean esValido(String s) { return s != null && !s.isBlank(); }

    private String getString(Row row, Integer colIndex) {
        if (colIndex == null || row == null) return null;
        return getCellString(row.getCell(colIndex));
    }

    private double getDouble(Row row, Integer colIndex) {
        if (colIndex == null || row == null) return 0.0;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell))
            return cell.getNumericCellValue();
        try {
            String s = getCellString(cell).replace(",", ".").replaceAll("[^\\d.]", "");
            return s.isBlank() ? 0.0 : Double.parseDouble(s);
        } catch (NumberFormatException ignored) { return 0.0; }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    try { yield cell.getLocalDateTimeCellValue().toLocalDate().toString(); }
                    catch (Exception e) { yield ""; }
                }
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) && !Double.isInfinite(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { try { yield cell.getStringCellValue(); } catch (Exception e2) { yield ""; } }
            }
            default -> "";
        };
    }

    private String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}
