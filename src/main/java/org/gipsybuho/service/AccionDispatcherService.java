package org.gipsybuho.service;

import org.gipsybuho.dao.AlbaranDAO;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.EmpleadoDAO;
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.dao.NominaDAO;
import org.gipsybuho.dao.NotaCalendarioDAO;
import org.gipsybuho.dao.PedidoDAO;
import org.gipsybuho.dao.PresupuestoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.AccionERP;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.LineaAlbaran;
import org.gipsybuho.model.LineaFactura;
import org.gipsybuho.model.LineaPresupuesto;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.model.NotaCalendario;
import org.gipsybuho.model.Pedido;
import org.gipsybuho.model.Presupuesto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccionDispatcherService {

    private final PresupuestoDAO    presupuestoDAO = new PresupuestoDAO();
    private final FacturaDAO        facturaDAO     = new FacturaDAO();
    private final AlbaranDAO        albaranDAO     = new AlbaranDAO();
    private final PedidoDAO         pedidoDAO      = new PedidoDAO();
    private final ClienteDAO        clienteDAO     = new ClienteDAO();
    private final MaterialDAO       materialDAO    = new MaterialDAO();
    private final EmpleadoDAO       empleadoDAO    = new EmpleadoDAO();
    private final NominaDAO         nominaDAO      = new NominaDAO();
    private final NotaCalendarioDAO calendarioDAO  = new NotaCalendarioDAO();
    private final NominaService     nominaService  = new NominaService();

    public record ResultadoAccion(boolean exito, String mensaje, String detalle) {
        public static ResultadoAccion ok(String mensaje, String detalle) {
            return new ResultadoAccion(true, mensaje, detalle);
        }
        public static ResultadoAccion error(String mensaje, String detalle) {
            return new ResultadoAccion(false, mensaje, detalle);
        }
        public static ResultadoAccion pendiente(String mensaje) {
            return new ResultadoAccion(true, mensaje, "");
        }
    }

    public ResultadoAccion ejecutar(AccionERP accion) {
        if (accion == null || accion.action == null) {
            return ResultadoAccion.error("Acción inválida", "El objeto AccionERP está vacío.");
        }
        return switch (accion.action) {
            case "crear_presupuesto"   -> crearPresupuesto(accion);
            case "crear_cliente"       -> crearCliente(accion);
            case "consultar_cliente"   -> consultarCliente(accion);
            case "consultar_materiales"-> consultarMateriales(accion);
            case "exportar_backup"     -> exportarBackup();
            case "generar_factura"     -> generarFactura(accion);
            case "crear_albaran"       -> crearAlbaran(accion);
            case "crear_pedido"        -> crearPedido(accion);
            case "actualizar_stock"    -> actualizarStock(accion);
            case "calcular_nomina"     -> calcularNomina(accion);
            case "generar_estadistica" -> pendiente("Generar Estadística");
            case "agendar_evento"      -> agendarEvento(accion);
            default -> ResultadoAccion.error(
                "Acción desconocida: " + accion.action,
                "Esta acción no está implementada aún.");
        };
    }

    // ── Crear Presupuesto ─────────────────────────────────────────────────────

    private ResultadoAccion crearPresupuesto(AccionERP accion) {
        try {
            Presupuesto p = new Presupuesto();
            p.setNumero(DatabaseManager.generarNumeroPresupuesto());
            p.setFecha(resolverFechaStr(accion.data));
            p.setEstado("borrador");
            p.setIvaPorcentaje(21.0);

            if (accion.data != null) {
                if (accion.data.clienteNombre != null) p.setClienteNombre(accion.data.clienteNombre);
                if (accion.data.observaciones != null) p.setNotas(accion.data.observaciones);
                int cid = parsearClienteId(accion.data);
                if (cid > 0) p.setClienteId(cid);
                if (accion.data.items != null) {
                    p.setLineas(accion.data.items.stream().map(item ->
                        new LineaPresupuesto(
                            construirDescripcion(item),
                            item.tecnica != null ? item.tecnica : "",
                            item.cantidad,
                            item.precioUnitario,
                            item.mermaPorcentaje)
                    ).collect(Collectors.toList()));
                }
            }

            p.calcularTotales();
            presupuestoDAO.save(p);

            return ResultadoAccion.ok(
                "✅ Presupuesto " + p.getNumero() + " creado correctamente en estado 'borrador'.",
                String.format("Nº %s · Cliente: %s · Total: %.2f € (IVA 21%% incl.)",
                    p.getNumero(),
                    p.getClienteNombre() != null ? p.getClienteNombre() : "Sin asignar",
                    p.getTotal()));

        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el presupuesto", e.getMessage());
        }
    }

    // ── Generar Factura ───────────────────────────────────────────────────────

    private ResultadoAccion generarFactura(AccionERP accion) {
        try {
            Factura f = new Factura();
            f.setNumero(DatabaseManager.generarNumeroFactura());
            f.setFecha(resolverFechaStr(accion.data));
            f.setFechaVencimiento(LocalDate.now().plusDays(30).toString());
            f.setEstado("pendiente");
            f.setFormaPago("Transferencia bancaria");
            f.setIvaPorcentaje(21.0);

            if (accion.data != null) {
                if (accion.data.clienteNombre != null) f.setClienteNombre(accion.data.clienteNombre);
                if (accion.data.observaciones != null) f.setNotas(accion.data.observaciones);
                int cid = parsearClienteId(accion.data);
                if (cid > 0) f.setClienteId(cid);
                if (accion.data.items != null) {
                    f.setLineas(accion.data.items.stream().map(item ->
                        new LineaFactura(
                            construirDescripcion(item),
                            item.tecnica != null ? item.tecnica : "",
                            item.cantidad,
                            item.precioUnitario,
                            item.mermaPorcentaje)
                    ).collect(Collectors.toList()));
                }
            }

            f.calcularTotales();
            facturaDAO.save(f);

            return ResultadoAccion.ok(
                "✅ Factura " + f.getNumero() + " generada correctamente en estado 'pendiente'.",
                String.format("Nº %s · Cliente: %s · Total: %.2f € (IVA 21%% incl.) · Vence: %s",
                    f.getNumero(),
                    f.getClienteNombre() != null ? f.getClienteNombre() : "Sin asignar",
                    f.getTotal(),
                    f.getFechaVencimiento()));

        } catch (Exception e) {
            return ResultadoAccion.error("Error al generar la factura", e.getMessage());
        }
    }

    // ── Crear Albarán ─────────────────────────────────────────────────────────

    private ResultadoAccion crearAlbaran(AccionERP accion) {
        try {
            Albaran a = new Albaran();
            a.setNumero(DatabaseManager.generarNumeroAlbaran());
            a.setFecha(resolverFechaStr(accion.data));
            a.setEstado("pendiente");

            if (accion.data != null) {
                if (accion.data.clienteNombre != null) a.setClienteNombre(accion.data.clienteNombre);
                if (accion.data.observaciones != null) a.setObservaciones(accion.data.observaciones);
                int cid = parsearClienteId(accion.data);
                if (cid > 0) a.setClienteId(cid);
                if (accion.data.items != null) {
                    a.setLineas(accion.data.items.stream().map(item -> {
                        String unidad = item.soporte != null && !item.soporte.isBlank()
                            ? item.soporte : "ud";
                        return new LineaAlbaran(construirDescripcion(item), item.cantidad, unidad);
                    }).collect(Collectors.toList()));
                }
            }

            albaranDAO.save(a);

            return ResultadoAccion.ok(
                "✅ Albarán " + a.getNumero() + " creado correctamente en estado 'pendiente'.",
                String.format("Nº %s · Cliente: %s · %d línea(s)",
                    a.getNumero(),
                    a.getClienteNombre() != null ? a.getClienteNombre() : "Sin asignar",
                    a.getLineas().size()));

        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el albarán", e.getMessage());
        }
    }

    // ── Crear Pedido ──────────────────────────────────────────────────────────

    private ResultadoAccion crearPedido(AccionERP accion) {
        try {
            Pedido p = new Pedido();
            p.setNumero(DatabaseManager.generarNumeroPedido());
            p.setFecha(LocalDate.now());
            p.setEstado("pendiente");
            p.setIvaPorcentaje(21.0);

            if (accion.data != null) {
                if (accion.data.clienteNombre != null) p.setClienteNombre(accion.data.clienteNombre);
                if (accion.data.observaciones != null) p.setNotas(accion.data.observaciones);
                if (accion.data.totalEstimado > 0)    p.setImporteTotal(accion.data.totalEstimado);
                int cid = parsearClienteId(accion.data);
                if (cid > 0) p.setClienteId(cid);
                if (accion.data.fecha != null && !accion.data.fecha.isBlank()) {
                    try { p.setFechaEntregaPrevista(LocalDate.parse(accion.data.fecha)); }
                    catch (Exception ignored) {}
                }
                if (accion.data.items != null && !accion.data.items.isEmpty()) {
                    p.setDescripcion(accion.data.items.stream()
                        .map(this::construirDescripcion)
                        .collect(Collectors.joining(" | ")));
                }
            }

            pedidoDAO.save(p);

            String entrega = p.getFechaEntregaPrevista() != null
                ? p.getFechaEntregaPrevista().toString() : "Sin definir";
            return ResultadoAccion.ok(
                "✅ Pedido " + p.getNumero() + " creado correctamente en estado 'pendiente'.",
                String.format("Nº %s · Cliente: %s · Importe: %.2f € · Entrega prevista: %s",
                    p.getNumero(),
                    p.getClienteNombre() != null ? p.getClienteNombre() : "Sin asignar",
                    p.getImporteTotal(), entrega));

        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el pedido", e.getMessage());
        }
    }

    // ── Actualizar Stock ──────────────────────────────────────────────────────

    private ResultadoAccion actualizarStock(AccionERP accion) {
        if (accion.data == null || accion.data.items == null || accion.data.items.isEmpty()) {
            return ResultadoAccion.error("Sin datos",
                "Indica el material y la cantidad a ajustar (entrada o salida).");
        }
        try {
            List<Material> todos = materialDAO.findAll();
            List<String> resultados = new ArrayList<>();

            for (AccionERP.Item item : accion.data.items) {
                if (item.descripcion == null || item.descripcion.isBlank()) continue;

                String termino = item.descripcion.toLowerCase();
                Material encontrado = todos.stream()
                    .filter(m -> m.getNombre().toLowerCase().contains(termino)
                              || termino.contains(m.getNombre().toLowerCase()))
                    .findFirst().orElse(null);

                if (encontrado == null) {
                    resultados.add("⚠ Material no encontrado: «" + item.descripcion + "»");
                    continue;
                }

                String tipo = (item.notas != null && item.notas.toLowerCase().contains("salida"))
                    ? "salida" : "entrada";
                double cantidad = Math.abs(item.cantidad > 0 ? item.cantidad : 1);
                String descripcionMovimiento = accion.data.observaciones != null
                    ? accion.data.observaciones : "Ajuste desde Asistente IA";

                materialDAO.ajustarStock(encontrado.getId(), cantidad, tipo, descripcionMovimiento);
                resultados.add(String.format("✅ %s: %s%.0f %s (nuevo stock: %.1f)",
                    encontrado.getNombre(),
                    tipo.equals("entrada") ? "+" : "-",
                    cantidad,
                    encontrado.getUnidad(),
                    tipo.equals("entrada")
                        ? encontrado.getStockActual() + cantidad
                        : encontrado.getStockActual() - cantidad));
            }

            if (resultados.isEmpty())
                return ResultadoAccion.error("Sin resultados",
                    "No se pudo identificar ningún material de la lista.");

            return ResultadoAccion.ok(
                "🗃 Stock actualizado correctamente:",
                String.join("\n", resultados));

        } catch (Exception e) {
            return ResultadoAccion.error("Error al actualizar stock", e.getMessage());
        }
    }

    // ── Agendar Evento en Calendario ──────────────────────────────────────────

    private ResultadoAccion agendarEvento(AccionERP accion) {
        try {
            LocalDate fecha = resolverFechaLocal(accion.data);

            String titulo = accion.previewSummary != null && !accion.previewSummary.isBlank()
                ? accion.previewSummary
                : (accion.data != null && accion.data.observaciones != null
                    ? accion.data.observaciones : "Evento sin título");
            if (titulo.length() > 80) titulo = titulo.substring(0, 77) + "…";

            StringBuilder sb = new StringBuilder();
            if (accion.data != null && accion.data.observaciones != null)
                sb.append(accion.data.observaciones);
            if (accion.data != null && accion.data.items != null && !accion.data.items.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n");
                accion.data.items.forEach(item ->
                    sb.append("• ").append(construirDescripcion(item)).append("\n"));
            }

            NotaCalendario evento = new NotaCalendario(fecha, titulo, sb.toString().trim());
            calendarioDAO.guardar(evento);

            return ResultadoAccion.ok(
                "📅 Evento agendado para el " + fecha + ".",
                "Título: " + titulo);

        } catch (Exception e) {
            return ResultadoAccion.error("Error al agendar el evento", e.getMessage());
        }
    }

    // ── Calcular Nómina ───────────────────────────────────────────────────────

    private ResultadoAccion calcularNomina(AccionERP accion) {
        try {
            String nombreEmpleado = accion.data != null ? accion.data.clienteNombre : null;
            if (nombreEmpleado == null || nombreEmpleado.isBlank()) {
                return ResultadoAccion.error("Falta el empleado",
                    "Indica el nombre del empleado en el campo 'cliente_nombre' del JSON.");
            }

            List<Empleado> todos = empleadoDAO.findAll();
            String termino = nombreEmpleado.toLowerCase();
            Empleado empleado = todos.stream()
                .filter(e -> e.getNombreCompleto().toLowerCase().contains(termino)
                          || termino.contains(e.getNombre().toLowerCase()))
                .findFirst().orElse(null);

            if (empleado == null) {
                return ResultadoAccion.error("Empleado no encontrado",
                    "No se encontró ningún empleado con el nombre «" + nombreEmpleado + "».\n"
                    + "Empleados disponibles: " + todos.stream()
                        .map(Empleado::getNombreCompleto)
                        .collect(Collectors.joining(", ")));
            }

            LocalDate fechaNomina = resolverFechaLocal(accion.data);
            double complementos = accion.data != null && accion.data.totalEstimado > 0
                ? accion.data.totalEstimado : 0.0;

            Nomina nomina = nominaService.calcular(
                empleado, fechaNomina.getMonthValue(), fechaNomina.getYear(),
                complementos, 0, 0.0, 0, 0.0, 0.0);
            nominaDAO.save(nomina);

            return ResultadoAccion.ok(
                "💰 Nómina calculada para " + empleado.getNombreCompleto()
                    + " — " + nomina.getPeriodo(),
                String.format(
                    "Bruto: %.2f €  ·  SS trabajador: %.2f €  ·  IRPF %.1f%%: %.2f €  ·  Neto: %.2f €",
                    nomina.getTotalBruto(), nomina.getSsTrabajador(),
                    nomina.getIrpfPorcentaje(), nomina.getIrpfImporte(),
                    nomina.getNeto()));

        } catch (Exception e) {
            return ResultadoAccion.error("Error al calcular la nómina", e.getMessage());
        }
    }

    // ── Consultar Cliente ─────────────────────────────────────────────────────

    private ResultadoAccion consultarCliente(AccionERP accion) {
        try {
            String termino = accion.data != null ? accion.data.clienteNombre : null;
            if (termino == null || termino.isBlank()) {
                return ResultadoAccion.error("Falta el término de búsqueda",
                    "Indica el nombre o datos del cliente.");
            }
            List<Cliente> resultados = clienteDAO.search(termino);
            if (resultados.isEmpty()) {
                return ResultadoAccion.ok(
                    "🔍 No se encontraron clientes con «" + termino + "».",
                    "Prueba con otro término o crea el cliente.");
            }
            String lista = resultados.stream()
                .limit(5)
                .map(c -> String.format("• %s (ID: %d)", c.getNombreCompleto(), c.getId()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok(
                "🔍 Se encontraron " + resultados.size() + " cliente(s):",
                lista);
        } catch (Exception e) {
            return ResultadoAccion.error("Error en la búsqueda", e.getMessage());
        }
    }

    // ── Consultar Materiales ──────────────────────────────────────────────────

    private ResultadoAccion consultarMateriales(AccionERP accion) {
        try {
            List<Material> bajoStock = materialDAO.findBajoStock();
            List<Material> todos = materialDAO.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("Total de materiales: ").append(todos.size()).append("\n");
            if (!bajoStock.isEmpty()) {
                sb.append("⚠ Bajo stock (").append(bajoStock.size()).append("):\n");
                bajoStock.forEach(m -> sb.append("  • ").append(m.getNombre())
                    .append(" — stock: ").append(m.getStockActual())
                    .append(" ").append(m.getUnidad()).append("\n"));
            } else {
                sb.append("✅ Todos los materiales tienen stock suficiente.");
            }
            return ResultadoAccion.ok("🗃 Informe de materiales:", sb.toString());
        } catch (Exception e) {
            return ResultadoAccion.error("Error al consultar materiales", e.getMessage());
        }
    }

    // ── Exportar Backup ───────────────────────────────────────────────────────

    private ResultadoAccion exportarBackup() {
        return ResultadoAccion.pendiente(
            "💾 Para exportar el backup, ve a Exportar / Backup en el menú lateral. " +
            "Desde allí puedes guardar todos los datos en formato JSON.");
    }

    // ── Stub genérico ─────────────────────────────────────────────────────────

    private ResultadoAccion pendiente(String nombreAccion) {
        return ResultadoAccion.pendiente(
            "⚙ «" + nombreAccion + "» está registrada. Para completarla, " +
            "ve al módulo correspondiente en el menú lateral. " +
            "La integración directa desde el chat llegará próximamente.");
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Parsea clienteId desde Data. Devuelve 0 si ausente o no es un número válido.
     */
    private static int parsearClienteId(AccionERP.Data data) {
        if (data == null || data.clienteId == null || data.clienteId.isBlank()) return 0;
        try { return Integer.parseInt(data.clienteId); }
        catch (NumberFormatException ignored) { return 0; }
    }

    /**
     * Devuelve data.fecha si está presente, o la fecha de hoy como String ISO.
     */
    private static String resolverFechaStr(AccionERP.Data data) {
        return (data != null && data.fecha != null && !data.fecha.isBlank())
            ? data.fecha : LocalDate.now().toString();
    }

    /**
     * Devuelve data.fecha como LocalDate si es válida, o LocalDate.now() como fallback.
     */
    private static LocalDate resolverFechaLocal(AccionERP.Data data) {
        if (data != null && data.fecha != null && !data.fecha.isBlank()) {
            try { return LocalDate.parse(data.fecha); }
            catch (Exception ignored) {}
        }
        return LocalDate.now();
    }

    /**
     * Construye una descripción textual rica a partir de un Item del JSON de la IA.
     * El soporte y el gramaje se agrupan en un único bloque de paréntesis cuando
     * al menos uno de los dos está presente.
     */
    private String construirDescripcion(AccionERP.Item item) {
        StringBuilder sb = new StringBuilder();
        if (item.descripcion != null) sb.append(item.descripcion);

        boolean tieneSoporte = item.soporte != null && !item.soporte.isBlank();
        if (tieneSoporte || item.gramaje > 0) {
            sb.append(" (");
            if (tieneSoporte) sb.append(item.soporte);
            if (item.gramaje > 0) {
                if (tieneSoporte) sb.append(", ");
                sb.append((int) item.gramaje).append("g");
            }
            sb.append(")");
        }

        if (item.colores > 0)
            sb.append(" — ").append(item.colores).append(" color").append(item.colores > 1 ? "es" : "");
        if (item.pantones != null && !item.pantones.isEmpty())
            sb.append(" [").append(String.join(", ", item.pantones)).append("]");
        if (item.notas != null && !item.notas.isBlank())
            sb.append(". ").append(item.notas);

        return sb.toString();
    }
}
