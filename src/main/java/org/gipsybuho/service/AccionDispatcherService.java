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
import org.gipsybuho.dao.TarifaDAO;
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
import org.gipsybuho.model.Tarifa;

import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
    private final TarifaDAO         tarifaDAO      = new TarifaDAO();
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
            // ── Acciones originales ────────────────────────────────────────
            case "crear_presupuesto"    -> crearPresupuesto(accion);
            case "crear_cliente"        -> crearCliente(accion);
            case "consultar_cliente"    -> consultarCliente(accion);
            case "consultar_materiales" -> consultarMateriales(accion);
            case "exportar_backup"      -> exportarBackup();
            case "generar_factura"      -> generarFactura(accion);
            case "crear_albaran"        -> crearAlbaran(accion);
            case "crear_pedido"         -> crearPedido(accion);
            case "actualizar_stock"     -> actualizarStock(accion);
            case "calcular_nomina"      -> calcularNomina(accion);
            case "generar_estadistica"  -> pendiente("Generar Estadística");
            case "agendar_evento"       -> agendarEvento(accion);
            case "importar_datos_excel" -> importarDatosExcel(accion);
            // ── Clientes ───────────────────────────────────────────────────
            case "editar_cliente"       -> editarCliente(accion);
            case "borrar_cliente"       -> borrarCliente(accion);
            case "listar_clientes"      -> listarClientes(accion);
            // ── Presupuestos ───────────────────────────────────────────────
            case "editar_presupuesto"   -> editarPresupuesto(accion);
            case "borrar_presupuesto"   -> borrarPresupuesto(accion);
            case "listar_presupuestos"  -> listarPresupuestos(accion);
            // ── Facturas ───────────────────────────────────────────────────
            case "editar_factura"       -> editarFactura(accion);
            case "borrar_factura"       -> borrarFactura(accion);
            case "listar_facturas"      -> listarFacturas(accion);
            // ── Albaranes ──────────────────────────────────────────────────
            case "editar_albaran"       -> editarAlbaran(accion);
            case "borrar_albaran"       -> borrarAlbaran(accion);
            case "listar_albaranes"     -> listarAlbaranes(accion);
            // ── Pedidos ────────────────────────────────────────────────────
            case "editar_pedido"        -> editarPedido(accion);
            case "borrar_pedido"        -> borrarPedido(accion);
            case "listar_pedidos"       -> listarPedidos(accion);
            // ── Tarifas ────────────────────────────────────────────────────
            case "crear_tarifa"         -> crearTarifa(accion);
            case "editar_tarifa"        -> editarTarifa(accion);
            case "borrar_tarifa"        -> borrarTarifa(accion);
            case "listar_tarifas"       -> listarTarifas(accion);
            // ── Materiales ─────────────────────────────────────────────────
            case "crear_material"       -> crearMaterial(accion);
            case "editar_material"      -> editarMaterial(accion);
            case "borrar_material"      -> borrarMaterial(accion);
            case "listar_materiales"    -> listarMaterialesDetalle(accion);
            // ── Empleados ──────────────────────────────────────────────────
            case "crear_empleado"       -> crearEmpleado(accion);
            case "editar_empleado"      -> editarEmpleado(accion);
            case "borrar_empleado"      -> borrarEmpleado(accion);
            case "listar_empleados"     -> listarEmpleados(accion);
            // ── Nóminas ────────────────────────────────────────────────────
            case "editar_nomina"        -> editarNomina(accion);
            case "borrar_nomina"        -> borrarNomina(accion);
            case "listar_nominas"       -> listarNominas(accion);
            // ── abrir/cerrar módulo se gestiona en IAView ──────────────────
            case "abrir_modulo", "cerrar_modulo" ->
                ResultadoAccion.ok("Módulo gestionado", "La ventana fue procesada por el asistente.");
            default -> ResultadoAccion.error(
                "Acción desconocida: " + accion.action,
                "Esta acción no está implementada aún.");
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLIENTES
    // ══════════════════════════════════════════════════════════════════════════

    private ResultadoAccion crearCliente(AccionERP accion) {
        try {
            if (accion.data == null || (accion.data.clienteNombre == null || accion.data.clienteNombre.isBlank())
                    && (accion.data.nombre == null || accion.data.nombre.isBlank())) {
                return ResultadoAccion.error("Falta el nombre del cliente",
                    "Indica el nombre (y opcionalmente apellidos) del cliente a crear.");
            }
            Cliente c = new Cliente();
            String nombreCompleto = accion.data.clienteNombre != null && !accion.data.clienteNombre.isBlank()
                ? accion.data.clienteNombre.trim() : accion.data.nombre.trim();
            int espacio = nombreCompleto.indexOf(' ');
            if (espacio > 0) {
                c.setNombre(nombreCompleto.substring(0, espacio));
                c.setApellidos(nombreCompleto.substring(espacio + 1));
            } else {
                c.setNombre(nombreCompleto);
            }
            if (accion.data.apellidos != null) c.setApellidos(accion.data.apellidos);
            if (accion.data.nif != null)       c.setNif(accion.data.nif);
            if (accion.data.email != null)     c.setEmail(accion.data.email);
            if (accion.data.telefono != null)  c.setTelefono(accion.data.telefono);
            if (accion.data.ciudad != null)    c.setCiudad(accion.data.ciudad);
            c.setTipo(accion.data.tipo != null ? accion.data.tipo : "empresa");
            if (accion.data.observaciones != null) c.setNotas(accion.data.observaciones);
            clienteDAO.save(c);
            return ResultadoAccion.ok(
                "👤 Cliente «" + c.getNombreCompleto() + "» creado correctamente.",
                String.format("ID: %d · Tipo: %s · Email: %s · NIF: %s",
                    c.getId(), c.getTipo(),
                    c.getEmail() != null ? c.getEmail() : "—",
                    c.getNif() != null ? c.getNif() : "—"));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el cliente", e.getMessage());
        }
    }

    private ResultadoAccion editarCliente(AccionERP accion) {
        try {
            if (accion.data == null) {
                return ResultadoAccion.error("Sin datos", "Indica el ID o nombre del cliente a editar.");
            }
            Object resolucion = resolverCliente(accion.data);
            if (resolucion == null) {
                return ResultadoAccion.error("Cliente no encontrado",
                    "No se encontró ningún cliente con los datos indicados.");
            }
            if (resolucion instanceof String msg) {
                return ResultadoAccion.error("Cliente no encontrado", msg);
            }
            Cliente c = (Cliente) resolucion;
            applyClienteFields(c, accion.data);
            clienteDAO.save(c);
            return ResultadoAccion.ok(
                "✅ Cliente «" + c.getNombreCompleto() + "» actualizado.",
                String.format("ID: %d · Tipo: %s · Email: %s · NIF: %s",
                    c.getId(), c.getTipo() != null ? c.getTipo() : "—",
                    c.getEmail() != null ? c.getEmail() : "—",
                    c.getNif() != null ? c.getNif() : "—"));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar el cliente", e.getMessage());
        }
    }

    private ResultadoAccion borrarCliente(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID del cliente",
                    "Indica el campo 'id' con el número del cliente a eliminar.");
            }
            Cliente c = clienteDAO.findById(accion.data.id);
            if (c == null) {
                return ResultadoAccion.error("Cliente no encontrado",
                    "No existe ningún cliente con ID " + accion.data.id + ".");
            }
            String nombre = c.getNombreCompleto();
            clienteDAO.delete(accion.data.id);
            return ResultadoAccion.ok(
                "🗑 Cliente «" + nombre + "» eliminado correctamente.",
                "ID " + accion.data.id + " eliminado de la base de datos.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar el cliente", e.getMessage());
        }
    }

    private ResultadoAccion consultarCliente(AccionERP accion) {
        try {
            String termino = accion.data != null ? accion.data.clienteNombre : null;
            if (termino == null || termino.isBlank()) {
                return ResultadoAccion.error("Falta el término de búsqueda",
                    "Indica el nombre o datos del cliente.");
            }
            List<Cliente> resultados = clienteDAO.search(termino);
            if (resultados.isEmpty()) {
                return ResultadoAccion.ok("🔍 No se encontraron clientes con «" + termino + "».",
                    "Prueba con otro término o crea el cliente.");
            }
            String lista = resultados.stream().limit(5)
                .map(c -> String.format("• %s (ID: %d)", c.getNombreCompleto(), c.getId()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("🔍 Se encontraron " + resultados.size() + " cliente(s):", lista);
        } catch (Exception e) {
            return ResultadoAccion.error("Error en la búsqueda", e.getMessage());
        }
    }

    private ResultadoAccion listarClientes(AccionERP accion) {
        try {
            List<Cliente> todos = clienteDAO.findAll();
            if (todos.isEmpty()) {
                return ResultadoAccion.ok("No hay clientes registrados.",
                    "La base de datos no tiene ningún cliente todavía.");
            }
            String termino = accion.data != null ? accion.data.clienteNombre : null;
            List<Cliente> lista = (termino != null && !termino.isBlank())
                ? clienteDAO.search(termino) : todos;
            String resumen = lista.stream().limit(20)
                .map(c -> String.format("• ID %d: %s · %s · %s",
                    c.getId(), c.getNombreCompleto(),
                    c.getTipo() != null ? c.getTipo() : "—",
                    c.getEmail() != null ? c.getEmail() : "—"))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok(
                "👥 " + lista.size() + " cliente(s):",
                resumen + (lista.size() > 20 ? "\n… y " + (lista.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar clientes", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRESUPUESTOS
    // ══════════════════════════════════════════════════════════════════════════

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
                        new LineaPresupuesto(construirDescripcion(item),
                            item.tecnica != null ? item.tecnica : "",
                            item.cantidad, item.precioUnitario, item.mermaPorcentaje)
                    ).collect(Collectors.toList()));
                }
            }
            p.calcularTotales();
            presupuestoDAO.save(p);
            return ResultadoAccion.ok(
                "✅ Presupuesto " + p.getNumero() + " creado en estado 'borrador'.",
                String.format("Nº %s · Cliente: %s · Total: %.2f €",
                    p.getNumero(),
                    p.getClienteNombre() != null ? p.getClienteNombre() : "Sin asignar",
                    p.getTotal()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el presupuesto", e.getMessage());
        }
    }

    private ResultadoAccion editarPresupuesto(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID del presupuesto",
                    "Indica el campo 'id' del presupuesto a editar.");
            }
            Presupuesto p = presupuestoDAO.findById(accion.data.id);
            if (p == null) {
                return ResultadoAccion.error("Presupuesto no encontrado",
                    "No existe presupuesto con ID " + accion.data.id + ".");
            }
            if (accion.data.estado != null)       p.setEstado(accion.data.estado);
            if (accion.data.clienteNombre != null) p.setClienteNombre(accion.data.clienteNombre);
            if (accion.data.observaciones != null) p.setNotas(accion.data.observaciones);
            if (accion.data.ivaPorcentaje > 0)    p.setIvaPorcentaje(accion.data.ivaPorcentaje);
            if (accion.data.fecha != null && !accion.data.fecha.isBlank()) p.setFecha(accion.data.fecha);
            presupuestoDAO.save(p);
            return ResultadoAccion.ok(
                "✅ Presupuesto " + p.getNumero() + " actualizado.",
                String.format("Estado: %s · Cliente: %s · Total: %.2f €",
                    p.getEstado(), p.getClienteNombre() != null ? p.getClienteNombre() : "—", p.getTotal()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar el presupuesto", e.getMessage());
        }
    }

    private ResultadoAccion borrarPresupuesto(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' del presupuesto a eliminar.");
            }
            Presupuesto p = presupuestoDAO.findById(accion.data.id);
            if (p == null) {
                return ResultadoAccion.error("No encontrado", "No existe presupuesto con ID " + accion.data.id + ".");
            }
            presupuestoDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Presupuesto " + p.getNumero() + " eliminado.",
                "ID " + accion.data.id + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar el presupuesto", e.getMessage());
        }
    }

    private ResultadoAccion listarPresupuestos(AccionERP accion) {
        try {
            List<Presupuesto> todos = presupuestoDAO.findAll();
            if (todos.isEmpty()) {
                return ResultadoAccion.ok("No hay presupuestos registrados.", "");
            }
            String filtroEstado = accion.data != null ? accion.data.estado : null;
            List<Presupuesto> lista = filtroEstado != null && !filtroEstado.isBlank()
                ? todos.stream().filter(p -> filtroEstado.equalsIgnoreCase(p.getEstado())).collect(Collectors.toList())
                : todos;
            String resumen = lista.stream().limit(20)
                .map(p -> String.format("• ID %d: %s · %s · %s · %.2f €",
                    p.getId(), p.getNumero(), p.getEstado(),
                    p.getClienteNombre() != null ? p.getClienteNombre() : "—", p.getTotal()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("📋 " + lista.size() + " presupuesto(s):",
                resumen + (lista.size() > 20 ? "\n… y " + (lista.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar presupuestos", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FACTURAS
    // ══════════════════════════════════════════════════════════════════════════

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
                        new LineaFactura(construirDescripcion(item),
                            item.tecnica != null ? item.tecnica : "",
                            item.cantidad, item.precioUnitario, item.mermaPorcentaje)
                    ).collect(Collectors.toList()));
                }
            }
            f.calcularTotales();
            facturaDAO.save(f);
            return ResultadoAccion.ok(
                "✅ Factura " + f.getNumero() + " generada en estado 'pendiente'.",
                String.format("Nº %s · Cliente: %s · Total: %.2f € · Vence: %s",
                    f.getNumero(),
                    f.getClienteNombre() != null ? f.getClienteNombre() : "Sin asignar",
                    f.getTotal(), f.getFechaVencimiento()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al generar la factura", e.getMessage());
        }
    }

    private ResultadoAccion editarFactura(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID de la factura",
                    "Indica el campo 'id' de la factura a editar.");
            }
            Factura f = facturaDAO.findById(accion.data.id);
            if (f == null) {
                return ResultadoAccion.error("Factura no encontrada",
                    "No existe factura con ID " + accion.data.id + ".");
            }
            if (accion.data.estado != null)          f.setEstado(accion.data.estado);
            if (accion.data.formaPago != null)        f.setFormaPago(accion.data.formaPago);
            if (accion.data.fechaVencimiento != null) f.setFechaVencimiento(accion.data.fechaVencimiento);
            if (accion.data.clienteNombre != null)   f.setClienteNombre(accion.data.clienteNombre);
            if (accion.data.observaciones != null)   f.setNotas(accion.data.observaciones);
            if (accion.data.ivaPorcentaje > 0)       f.setIvaPorcentaje(accion.data.ivaPorcentaje);
            facturaDAO.save(f);
            return ResultadoAccion.ok(
                "✅ Factura " + f.getNumero() + " actualizada.",
                String.format("Estado: %s · Forma pago: %s · Vence: %s",
                    f.getEstado(), f.getFormaPago(), f.getFechaVencimiento()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar la factura", e.getMessage());
        }
    }

    private ResultadoAccion borrarFactura(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' de la factura a eliminar.");
            }
            Factura f = facturaDAO.findById(accion.data.id);
            if (f == null) {
                return ResultadoAccion.error("No encontrada", "No existe factura con ID " + accion.data.id + ".");
            }
            facturaDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Factura " + f.getNumero() + " eliminada.",
                "ID " + accion.data.id + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar la factura", e.getMessage());
        }
    }

    private ResultadoAccion listarFacturas(AccionERP accion) {
        try {
            List<Factura> todos = facturaDAO.findAll();
            if (todos.isEmpty()) {
                return ResultadoAccion.ok("No hay facturas registradas.", "");
            }
            String filtroEstado = accion.data != null ? accion.data.estado : null;
            List<Factura> lista = filtroEstado != null && !filtroEstado.isBlank()
                ? todos.stream().filter(f -> filtroEstado.equalsIgnoreCase(f.getEstado())).collect(Collectors.toList())
                : todos;
            String resumen = lista.stream().limit(20)
                .map(f -> String.format("• ID %d: %s · %s · %s · %.2f €",
                    f.getId(), f.getNumero(), f.getEstado(),
                    f.getClienteNombre() != null ? f.getClienteNombre() : "—", f.getTotal()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("🧾 " + lista.size() + " factura(s):",
                resumen + (lista.size() > 20 ? "\n… y " + (lista.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar facturas", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ALBARANES
    // ══════════════════════════════════════════════════════════════════════════

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
                        String unidad = item.soporte != null && !item.soporte.isBlank() ? item.soporte : "ud";
                        return new LineaAlbaran(construirDescripcion(item), item.cantidad, unidad);
                    }).collect(Collectors.toList()));
                }
            }
            albaranDAO.save(a);
            return ResultadoAccion.ok(
                "✅ Albarán " + a.getNumero() + " creado en estado 'pendiente'.",
                String.format("Nº %s · Cliente: %s · %d línea(s)",
                    a.getNumero(),
                    a.getClienteNombre() != null ? a.getClienteNombre() : "Sin asignar",
                    a.getLineas().size()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el albarán", e.getMessage());
        }
    }

    private ResultadoAccion editarAlbaran(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID del albarán",
                    "Indica el campo 'id' del albarán a editar.");
            }
            Albaran a = albaranDAO.findById(accion.data.id);
            if (a == null) {
                return ResultadoAccion.error("Albarán no encontrado",
                    "No existe albarán con ID " + accion.data.id + ".");
            }
            if (accion.data.estado != null)       a.setEstado(accion.data.estado);
            if (accion.data.clienteNombre != null) a.setClienteNombre(accion.data.clienteNombre);
            if (accion.data.observaciones != null) a.setObservaciones(accion.data.observaciones);
            albaranDAO.save(a);
            return ResultadoAccion.ok(
                "✅ Albarán " + a.getNumero() + " actualizado.",
                String.format("Estado: %s · Cliente: %s", a.getEstado(),
                    a.getClienteNombre() != null ? a.getClienteNombre() : "—"));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar el albarán", e.getMessage());
        }
    }

    private ResultadoAccion borrarAlbaran(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' del albarán a eliminar.");
            }
            Albaran a = albaranDAO.findById(accion.data.id);
            if (a == null) {
                return ResultadoAccion.error("No encontrado", "No existe albarán con ID " + accion.data.id + ".");
            }
            albaranDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Albarán " + a.getNumero() + " eliminado.",
                "ID " + accion.data.id + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar el albarán", e.getMessage());
        }
    }

    private ResultadoAccion listarAlbaranes(AccionERP accion) {
        try {
            List<Albaran> todos = albaranDAO.findAll();
            if (todos.isEmpty()) {
                return ResultadoAccion.ok("No hay albaranes registrados.", "");
            }
            String filtroEstado = accion.data != null ? accion.data.estado : null;
            List<Albaran> lista = filtroEstado != null && !filtroEstado.isBlank()
                ? todos.stream().filter(a -> filtroEstado.equalsIgnoreCase(a.getEstado())).collect(Collectors.toList())
                : todos;
            String resumen = lista.stream().limit(20)
                .map(a -> String.format("• ID %d: %s · %s · %s",
                    a.getId(), a.getNumero(), a.getEstado(),
                    a.getClienteNombre() != null ? a.getClienteNombre() : "—"))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("📦 " + lista.size() + " albarán(es):",
                resumen + (lista.size() > 20 ? "\n… y " + (lista.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar albaranes", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PEDIDOS
    // ══════════════════════════════════════════════════════════════════════════

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
                    try { p.setFechaEntregaPrevista(LocalDate.parse(accion.data.fecha)); } catch (Exception ignored) {}
                }
                if (accion.data.items != null && !accion.data.items.isEmpty()) {
                    p.setDescripcion(accion.data.items.stream()
                        .map(this::construirDescripcion).collect(Collectors.joining(" | ")));
                }
            }
            pedidoDAO.save(p);
            String entrega = p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().toString() : "Sin definir";
            return ResultadoAccion.ok(
                "✅ Pedido " + p.getNumero() + " creado en estado 'pendiente'.",
                String.format("Nº %s · Cliente: %s · Importe: %.2f € · Entrega: %s",
                    p.getNumero(), p.getClienteNombre() != null ? p.getClienteNombre() : "Sin asignar",
                    p.getImporteTotal(), entrega));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el pedido", e.getMessage());
        }
    }

    private ResultadoAccion editarPedido(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID del pedido",
                    "Indica el campo 'id' del pedido a editar.");
            }
            Pedido p = pedidoDAO.findById(accion.data.id);
            if (p == null) {
                return ResultadoAccion.error("Pedido no encontrado",
                    "No existe pedido con ID " + accion.data.id + ".");
            }
            if (accion.data.estado != null)        p.setEstado(accion.data.estado);
            if (accion.data.clienteNombre != null)  p.setClienteNombre(accion.data.clienteNombre);
            if (accion.data.observaciones != null)  p.setNotas(accion.data.observaciones);
            if (accion.data.totalEstimado > 0)     p.setImporteTotal(accion.data.totalEstimado);
            if (accion.data.fecha != null && !accion.data.fecha.isBlank()) {
                try { p.setFechaEntregaPrevista(LocalDate.parse(accion.data.fecha)); } catch (Exception ignored) {}
            }
            pedidoDAO.save(p);
            return ResultadoAccion.ok(
                "✅ Pedido " + p.getNumero() + " actualizado.",
                String.format("Estado: %s · Cliente: %s · Importe: %.2f €",
                    p.getEstado(), p.getClienteNombre() != null ? p.getClienteNombre() : "—",
                    p.getImporteTotal()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar el pedido", e.getMessage());
        }
    }

    private ResultadoAccion borrarPedido(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' del pedido a eliminar.");
            }
            Pedido p = pedidoDAO.findById(accion.data.id);
            if (p == null) {
                return ResultadoAccion.error("No encontrado", "No existe pedido con ID " + accion.data.id + ".");
            }
            pedidoDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Pedido " + p.getNumero() + " eliminado.",
                "ID " + accion.data.id + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar el pedido", e.getMessage());
        }
    }

    private ResultadoAccion listarPedidos(AccionERP accion) {
        try {
            List<Pedido> todos = pedidoDAO.findAll();
            if (todos.isEmpty()) {
                return ResultadoAccion.ok("No hay pedidos registrados.", "");
            }
            String filtroEstado = accion.data != null ? accion.data.estado : null;
            List<Pedido> lista = filtroEstado != null && !filtroEstado.isBlank()
                ? todos.stream().filter(p -> filtroEstado.equalsIgnoreCase(p.getEstado())).collect(Collectors.toList())
                : todos;
            String resumen = lista.stream().limit(20)
                .map(p -> String.format("• ID %d: %s · %s · %s · %.2f €",
                    p.getId(), p.getNumero(), p.getEstado(),
                    p.getClienteNombre() != null ? p.getClienteNombre() : "—", p.getImporteTotal()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("🛒 " + lista.size() + " pedido(s):",
                resumen + (lista.size() > 20 ? "\n… y " + (lista.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar pedidos", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TARIFAS
    // ══════════════════════════════════════════════════════════════════════════

    private ResultadoAccion crearTarifa(AccionERP accion) {
        try {
            if (accion.data == null || (accion.data.nombre == null || accion.data.nombre.isBlank())) {
                return ResultadoAccion.error("Falta el nombre de la tarifa",
                    "Indica 'nombre' y 'tecnica' de la tarifa a crear.");
            }
            Tarifa t = new Tarifa();
            t.setNombre(accion.data.nombre);
            t.setTecnica(accion.data.tecnica != null ? accion.data.tecnica : "General");
            t.setDescripcion(accion.data.descripcion != null ? accion.data.descripcion : "");
            t.setPrecioUnit(accion.data.precioUnitario);
            t.setPrecioSetup(accion.data.precioSetup);
            t.setMinimoUnidades(accion.data.minimoUnidades > 0 ? accion.data.minimoUnidades : 1);
            t.setActiva(accion.data.activa == null || accion.data.activa);
            tarifaDAO.save(t);
            return ResultadoAccion.ok(
                "✅ Tarifa «" + t.getNombre() + "» creada.",
                String.format("ID: %d · Técnica: %s · Precio/ud: %.2f € · Setup: %.2f €",
                    t.getId(), t.getTecnica(), t.getPrecioUnit(), t.getPrecioSetup()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear la tarifa", e.getMessage());
        }
    }

    private ResultadoAccion editarTarifa(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID de la tarifa",
                    "Indica el campo 'id' de la tarifa a editar.");
            }
            Tarifa t = tarifaDAO.findById(accion.data.id);
            if (t == null) {
                return ResultadoAccion.error("Tarifa no encontrada",
                    "No existe tarifa con ID " + accion.data.id + ".");
            }
            if (accion.data.nombre != null && !accion.data.nombre.isBlank()) t.setNombre(accion.data.nombre);
            if (accion.data.tecnica != null)   t.setTecnica(accion.data.tecnica);
            if (accion.data.descripcion != null) t.setDescripcion(accion.data.descripcion);
            if (accion.data.precioUnitario > 0) t.setPrecioUnit(accion.data.precioUnitario);
            if (accion.data.precioSetup > 0)   t.setPrecioSetup(accion.data.precioSetup);
            if (accion.data.minimoUnidades > 0) t.setMinimoUnidades(accion.data.minimoUnidades);
            if (accion.data.activa != null)    t.setActiva(accion.data.activa);
            tarifaDAO.save(t);
            return ResultadoAccion.ok(
                "✅ Tarifa «" + t.getNombre() + "» actualizada.",
                String.format("Técnica: %s · Precio/ud: %.2f € · Activa: %s",
                    t.getTecnica(), t.getPrecioUnit(), t.isActiva() ? "Sí" : "No"));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar la tarifa", e.getMessage());
        }
    }

    private ResultadoAccion borrarTarifa(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' de la tarifa a eliminar.");
            }
            Tarifa t = tarifaDAO.findById(accion.data.id);
            if (t == null) {
                return ResultadoAccion.error("No encontrada", "No existe tarifa con ID " + accion.data.id + ".");
            }
            tarifaDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Tarifa «" + t.getNombre() + "» eliminada.",
                "ID " + accion.data.id + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar la tarifa", e.getMessage());
        }
    }

    private ResultadoAccion listarTarifas(AccionERP accion) {
        try {
            List<Tarifa> todas = tarifaDAO.findAll();
            if (todas.isEmpty()) {
                return ResultadoAccion.ok("No hay tarifas registradas.", "");
            }
            String filtroTecnica = accion.data != null ? accion.data.tecnica : null;
            List<Tarifa> lista = filtroTecnica != null && !filtroTecnica.isBlank()
                ? todas.stream().filter(t -> filtroTecnica.equalsIgnoreCase(t.getTecnica())).collect(Collectors.toList())
                : todas;
            String resumen = lista.stream().limit(20)
                .map(t -> String.format("• ID %d: %s · %s · %.2f €/ud · Setup: %.2f €",
                    t.getId(), t.getNombre(), t.getTecnica(), t.getPrecioUnit(), t.getPrecioSetup()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("🏷 " + lista.size() + " tarifa(s):",
                resumen + (lista.size() > 20 ? "\n… y " + (lista.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar tarifas", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MATERIALES
    // ══════════════════════════════════════════════════════════════════════════

    private ResultadoAccion consultarMateriales(AccionERP accion) {
        try {
            List<Material> bajoStock = materialDAO.findBajoStock();
            List<Material> todos = materialDAO.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("Total de materiales: ").append(todos.size()).append("\n");
            if (!bajoStock.isEmpty()) {
                sb.append("⚠ Bajo stock (").append(bajoStock.size()).append("):\n");
                bajoStock.forEach(m -> sb.append("  • ").append(m.getNombre())
                    .append(" — stock: ").append(m.getStockActual()).append(" ").append(m.getUnidad()).append("\n"));
            } else {
                sb.append("✅ Todos los materiales tienen stock suficiente.");
            }
            return ResultadoAccion.ok("🗃 Informe de materiales:", sb.toString());
        } catch (Exception e) {
            return ResultadoAccion.error("Error al consultar materiales", e.getMessage());
        }
    }

    private ResultadoAccion crearMaterial(AccionERP accion) {
        try {
            if (accion.data == null || (accion.data.nombre == null || accion.data.nombre.isBlank())) {
                return ResultadoAccion.error("Falta el nombre del material",
                    "Indica 'nombre', 'unidad' y opcionalmente 'categoria', 'stock_actual', 'stock_minimo'.");
            }
            Material m = new Material();
            m.setNombre(accion.data.nombre);
            m.setReferencia(accion.data.referencia != null ? accion.data.referencia : "");
            m.setCategoria(accion.data.categoria != null ? accion.data.categoria : "consumibles");
            m.setUnidad(accion.data.unidad != null ? accion.data.unidad : "ud");
            m.setStockActual(accion.data.stockActual);
            m.setStockMinimo(accion.data.stockMinimo);
            m.setPrecioUnidad(accion.data.precioUnidad);
            m.setProveedor(accion.data.proveedor != null ? accion.data.proveedor : "");
            materialDAO.save(m);
            return ResultadoAccion.ok(
                "✅ Material «" + m.getNombre() + "» creado.",
                String.format("ID: %d · Categoría: %s · Stock: %.1f %s · Mín: %.1f",
                    m.getId(), m.getCategoria(), m.getStockActual(), m.getUnidad(), m.getStockMinimo()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al crear el material", e.getMessage());
        }
    }

    private ResultadoAccion editarMaterial(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID del material",
                    "Indica el campo 'id' del material a editar.");
            }
            Material m = materialDAO.findById(accion.data.id);
            if (m == null) {
                return ResultadoAccion.error("Material no encontrado",
                    "No existe material con ID " + accion.data.id + ".");
            }
            if (accion.data.nombre != null && !accion.data.nombre.isBlank()) m.setNombre(accion.data.nombre);
            if (accion.data.referencia != null) m.setReferencia(accion.data.referencia);
            if (accion.data.categoria != null)  m.setCategoria(accion.data.categoria);
            if (accion.data.unidad != null)     m.setUnidad(accion.data.unidad);
            if (accion.data.stockActual > 0 || (accion.data.observaciones != null && accion.data.observaciones.contains("0")))
                m.setStockActual(accion.data.stockActual);
            if (accion.data.stockMinimo > 0)   m.setStockMinimo(accion.data.stockMinimo);
            if (accion.data.precioUnidad > 0)  m.setPrecioUnidad(accion.data.precioUnidad);
            if (accion.data.proveedor != null)  m.setProveedor(accion.data.proveedor);
            materialDAO.save(m);
            return ResultadoAccion.ok(
                "✅ Material «" + m.getNombre() + "» actualizado.",
                String.format("Stock: %.1f %s · Mín: %.1f · Precio: %.2f €",
                    m.getStockActual(), m.getUnidad(), m.getStockMinimo(), m.getPrecioUnidad()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar el material", e.getMessage());
        }
    }

    private ResultadoAccion borrarMaterial(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' del material a eliminar.");
            }
            Material m = materialDAO.findById(accion.data.id);
            if (m == null) {
                return ResultadoAccion.error("No encontrado", "No existe material con ID " + accion.data.id + ".");
            }
            materialDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Material «" + m.getNombre() + "» eliminado.",
                "ID " + accion.data.id + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar el material", e.getMessage());
        }
    }

    private ResultadoAccion listarMaterialesDetalle(AccionERP accion) {
        try {
            List<Material> todos = materialDAO.findAll();
            if (todos.isEmpty()) {
                return ResultadoAccion.ok("No hay materiales registrados.", "");
            }
            String filtroCategoria = accion.data != null ? accion.data.categoria : null;
            List<Material> lista = filtroCategoria != null && !filtroCategoria.isBlank()
                ? todos.stream().filter(m -> filtroCategoria.equalsIgnoreCase(m.getCategoria())).collect(Collectors.toList())
                : todos;
            String resumen = lista.stream().limit(20)
                .map(m -> String.format("• ID %d: %s · %s · Stock: %.1f %s%s",
                    m.getId(), m.getNombre(), m.getCategoria(),
                    m.getStockActual(), m.getUnidad(),
                    m.isBajoStock() ? " ⚠" : ""))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("📦 " + lista.size() + " material(es):",
                resumen + (lista.size() > 20 ? "\n… y " + (lista.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar materiales", e.getMessage());
        }
    }

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
                String desc = accion.data.observaciones != null ? accion.data.observaciones : "Ajuste desde Asistente IA";
                materialDAO.ajustarStock(encontrado.getId(), cantidad, tipo, desc);
                resultados.add(String.format("✅ %s: %s%.0f %s",
                    encontrado.getNombre(), tipo.equals("entrada") ? "+" : "-", cantidad, encontrado.getUnidad()));
            }
            if (resultados.isEmpty()) {
                return ResultadoAccion.error("Sin resultados", "No se pudo identificar ningún material de la lista.");
            }
            return ResultadoAccion.ok("🗃 Stock actualizado:", String.join("\n", resultados));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al actualizar stock", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMPLEADOS
    // ══════════════════════════════════════════════════════════════════════════

    private ResultadoAccion crearEmpleado(AccionERP accion) {
        try {
            if (accion.data == null || (accion.data.nombre == null || accion.data.nombre.isBlank())
                    && (accion.data.clienteNombre == null || accion.data.clienteNombre.isBlank())) {
                return ResultadoAccion.error("Falta el nombre del empleado",
                    "Indica 'nombre' (y opcionalmente 'apellidos', 'nif', 'categoria', 'salario_base', 'irpf').");
            }
            Empleado e = new Empleado();
            String nombreCompleto = accion.data.nombre != null && !accion.data.nombre.isBlank()
                ? accion.data.nombre.trim() : accion.data.clienteNombre.trim();
            int espacio = nombreCompleto.indexOf(' ');
            if (espacio > 0 && (accion.data.apellidos == null || accion.data.apellidos.isBlank())) {
                e.setNombre(nombreCompleto.substring(0, espacio));
                e.setApellidos(nombreCompleto.substring(espacio + 1));
            } else {
                e.setNombre(nombreCompleto);
            }
            if (accion.data.apellidos != null && !accion.data.apellidos.isBlank())
                e.setApellidos(accion.data.apellidos);
            if (accion.data.nif != null)       e.setNif(accion.data.nif);
            if (accion.data.email != null)     e.setEmail(accion.data.email);
            if (accion.data.telefono != null)  e.setTelefono(accion.data.telefono);
            if (accion.data.categoria != null) e.setCategoria(accion.data.categoria);
            else e.setCategoria("Operario");
            if (accion.data.salarioBase > 0)  e.setSalarioBase(accion.data.salarioBase);
            if (accion.data.irpf > 0)         e.setIrpf(accion.data.irpf);
            if (accion.data.iban != null)     e.setIban(accion.data.iban);
            e.setFechaAlta(accion.data.fechaAlta != null ? accion.data.fechaAlta : LocalDate.now().toString());
            e.setActivo(true);
            empleadoDAO.save(e);
            return ResultadoAccion.ok(
                "✅ Empleado «" + e.getNombreCompleto() + "» creado.",
                String.format("ID: %d · Categoría: %s · Salario base: %.2f € · IRPF: %.1f%%",
                    e.getId(), e.getCategoria(), e.getSalarioBase(), e.getIrpf()));
        } catch (Exception ex) {
            return ResultadoAccion.error("Error al crear el empleado", ex.getMessage());
        }
    }

    private ResultadoAccion editarEmpleado(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID del empleado",
                    "Indica el campo 'id' del empleado a editar.");
            }
            Empleado emp = empleadoDAO.findById(accion.data.id);
            if (emp == null) {
                return ResultadoAccion.error("Empleado no encontrado",
                    "No existe empleado con ID " + accion.data.id + ".");
            }
            if (accion.data.nombre != null && !accion.data.nombre.isBlank()) emp.setNombre(accion.data.nombre);
            if (accion.data.apellidos != null) emp.setApellidos(accion.data.apellidos);
            if (accion.data.nif != null)       emp.setNif(accion.data.nif);
            if (accion.data.email != null)     emp.setEmail(accion.data.email);
            if (accion.data.telefono != null)  emp.setTelefono(accion.data.telefono);
            if (accion.data.categoria != null) emp.setCategoria(accion.data.categoria);
            if (accion.data.salarioBase > 0)  emp.setSalarioBase(accion.data.salarioBase);
            if (accion.data.irpf > 0)         emp.setIrpf(accion.data.irpf);
            if (accion.data.iban != null)     emp.setIban(accion.data.iban);
            if (accion.data.activa != null)   emp.setActivo(accion.data.activa);
            empleadoDAO.save(emp);
            return ResultadoAccion.ok(
                "✅ Empleado «" + emp.getNombreCompleto() + "» actualizado.",
                String.format("Categoría: %s · Salario: %.2f € · IRPF: %.1f%%",
                    emp.getCategoria(), emp.getSalarioBase(), emp.getIrpf()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar el empleado", e.getMessage());
        }
    }

    private ResultadoAccion borrarEmpleado(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' del empleado a eliminar.");
            }
            Empleado emp = empleadoDAO.findById(accion.data.id);
            if (emp == null) {
                return ResultadoAccion.error("No encontrado", "No existe empleado con ID " + accion.data.id + ".");
            }
            empleadoDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Empleado «" + emp.getNombreCompleto() + "» eliminado.",
                "ID " + accion.data.id + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar el empleado", e.getMessage());
        }
    }

    private ResultadoAccion listarEmpleados(AccionERP accion) {
        try {
            List<Empleado> todos = empleadoDAO.findAll();
            if (todos.isEmpty()) {
                return ResultadoAccion.ok("No hay empleados registrados.", "");
            }
            String resumen = todos.stream().limit(20)
                .map(e -> String.format("• ID %d: %s · %s · %.2f €/mes",
                    e.getId(), e.getNombreCompleto(),
                    e.getCategoria() != null ? e.getCategoria() : "—", e.getSalarioBase()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("👥 " + todos.size() + " empleado(s):",
                resumen + (todos.size() > 20 ? "\n… y " + (todos.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar empleados", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NÓMINAS
    // ══════════════════════════════════════════════════════════════════════════

    private ResultadoAccion calcularNomina(AccionERP accion) {
        try {
            String nombreEmpleado = accion.data != null ? accion.data.clienteNombre : null;
            if (nombreEmpleado == null && accion.data != null) nombreEmpleado = accion.data.nombre;
            if (nombreEmpleado == null || nombreEmpleado.isBlank()) {
                return ResultadoAccion.error("Falta el empleado",
                    "Indica el nombre del empleado en 'cliente_nombre' o 'nombre'.");
            }
            List<Empleado> todos = empleadoDAO.findAll();
            String termino = nombreEmpleado.toLowerCase();
            Empleado empleado = todos.stream()
                .filter(e -> e.getNombreCompleto().toLowerCase().contains(termino)
                          || termino.contains(e.getNombre().toLowerCase()))
                .findFirst().orElse(null);
            if (empleado == null) {
                return ResultadoAccion.error("Empleado no encontrado",
                    "No se encontró «" + nombreEmpleado + "».\nDisponibles: "
                    + todos.stream().map(Empleado::getNombreCompleto).collect(Collectors.joining(", ")));
            }
            LocalDate fechaNomina = resolverFechaLocal(accion.data);
            double complementos = accion.data != null && accion.data.totalEstimado > 0
                ? accion.data.totalEstimado : 0.0;
            Nomina nomina = nominaService.calcular(
                empleado, fechaNomina.getMonthValue(), fechaNomina.getYear(),
                complementos, 0, 0.0, 0, 0.0, 0.0);
            nominaDAO.save(nomina);
            return ResultadoAccion.ok(
                "💰 Nómina calculada para " + empleado.getNombreCompleto() + " — " + nomina.getPeriodo(),
                String.format("Bruto: %.2f €  ·  SS: %.2f €  ·  IRPF %.1f%%: %.2f €  ·  Neto: %.2f €",
                    nomina.getTotalBruto(), nomina.getSsTrabajador(),
                    nomina.getIrpfPorcentaje(), nomina.getIrpfImporte(), nomina.getNeto()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al calcular la nómina", e.getMessage());
        }
    }

    private ResultadoAccion editarNomina(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID de la nómina",
                    "Indica el campo 'id' de la nómina a editar.");
            }
            Nomina n = nominaDAO.findById(accion.data.id);
            if (n == null) {
                return ResultadoAccion.error("Nómina no encontrada",
                    "No existe nómina con ID " + accion.data.id + ".");
            }
            Empleado emp = empleadoDAO.findById(n.getEmpleadoId());
            if (emp == null) {
                return ResultadoAccion.error("Empleado no encontrado",
                    "No se puede editar la nómina porque el empleado asociado no existe.");
            }
            double complementos = accion.data.totalEstimado > 0 ? accion.data.totalEstimado : n.getComplementos();
            Nomina recalculada = nominaService.calcular(
                emp, n.getMes(), n.getAnio(), complementos,
                n.getHorasExtraNormales(), n.getPrecioHoraExtra(),
                n.getHorasExtraFestivas(), n.getPrecioHoraFestiva(),
                n.getPercepcionesNoSalariales());
            recalculada.setId(n.getId());
            nominaDAO.save(recalculada);
            return ResultadoAccion.ok(
                "✅ Nómina ID " + n.getId() + " recalculada.",
                String.format("Periodo: %s · Bruto: %.2f € · Neto: %.2f €",
                    recalculada.getPeriodo(), recalculada.getTotalBruto(), recalculada.getNeto()));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al editar la nómina", e.getMessage());
        }
    }

    private ResultadoAccion borrarNomina(AccionERP accion) {
        try {
            if (accion.data == null || accion.data.id <= 0) {
                return ResultadoAccion.error("Falta el ID", "Indica el campo 'id' de la nómina a eliminar.");
            }
            Nomina n = nominaDAO.findById(accion.data.id);
            if (n == null) {
                return ResultadoAccion.error("No encontrada", "No existe nómina con ID " + accion.data.id + ".");
            }
            nominaDAO.delete(accion.data.id);
            return ResultadoAccion.ok("🗑 Nómina ID " + accion.data.id + " eliminada.",
                "Periodo: " + n.getPeriodo() + " eliminado permanentemente.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error al eliminar la nómina", e.getMessage());
        }
    }

    private ResultadoAccion listarNominas(AccionERP accion) {
        try {
            List<Nomina> todas = nominaDAO.findAll();
            if (todas.isEmpty()) {
                return ResultadoAccion.ok("No hay nóminas registradas.", "");
            }
            String resumen = todas.stream().limit(20)
                .map(n -> String.format("• ID %d: %s · %s · Bruto: %.2f € · Neto: %.2f €",
                    n.getId(), n.getPeriodo(),
                    n.getEmpleadoNombre() != null ? n.getEmpleadoNombre() : "—",
                    n.getTotalBruto(), n.getNeto()))
                .collect(Collectors.joining("\n"));
            return ResultadoAccion.ok("💰 " + todas.size() + " nómina(s):",
                resumen + (todas.size() > 20 ? "\n… y " + (todas.size() - 20) + " más." : ""));
        } catch (Exception e) {
            return ResultadoAccion.error("Error al listar nóminas", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CALENDARIO / BACKUP / EXCEL
    // ══════════════════════════════════════════════════════════════════════════

    private ResultadoAccion agendarEvento(AccionERP accion) {
        try {
            LocalDate fecha = resolverFechaLocal(accion.data);
            String titulo = accion.previewSummary != null && !accion.previewSummary.isBlank()
                ? accion.previewSummary
                : (accion.data != null && accion.data.observaciones != null
                    ? accion.data.observaciones : "Evento sin título");
            if (titulo.length() > 80) titulo = titulo.substring(0, 77) + "…";
            StringBuilder sb = new StringBuilder();
            if (accion.data != null && accion.data.observaciones != null) sb.append(accion.data.observaciones);
            if (accion.data != null && accion.data.items != null && !accion.data.items.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n");
                accion.data.items.forEach(item -> sb.append("• ").append(construirDescripcion(item)).append("\n"));
            }
            NotaCalendario evento = new NotaCalendario(fecha, titulo, sb.toString().trim());
            calendarioDAO.guardar(evento);
            return ResultadoAccion.ok("📅 Evento agendado para el " + fecha + ".", "Título: " + titulo);
        } catch (Exception e) {
            return ResultadoAccion.error("Error al agendar el evento", e.getMessage());
        }
    }

    private ResultadoAccion exportarBackup() {
        return ResultadoAccion.pendiente(
            "💾 Para exportar el backup, ve a Exportar / Backup en el menú lateral.");
    }

    private ResultadoAccion importarDatosExcel(AccionERP accion) {
        boolean dryRun = accion.data != null && Boolean.TRUE.equals(accion.data.dryRun);
        try {
            CompletableFuture<Optional<File>> futureArchivo = new CompletableFuture<>();
            Platform.runLater(() -> {
                FileChooser fc = new FileChooser();
                fc.setTitle(dryRun ? "Seleccionar Excel a analizar (modo prueba)" : "Seleccionar Excel a importar");
                fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Excel (*.xlsx, *.xls, *.xlsm)", "*.xlsx", "*.xls", "*.xlsm"),
                    new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
                futureArchivo.complete(Optional.ofNullable(fc.showOpenDialog(null)));
            });
            Optional<File> archivoOpt = futureArchivo.get(120, TimeUnit.SECONDS);
            if (archivoOpt.isEmpty()) {
                return ResultadoAccion.ok("Operación cancelada", "No se seleccionó ningún archivo.");
            }
            File archivo = archivoOpt.get();
            ImportarDatosService svc = new ImportarDatosService();
            if (dryRun) {
                ImportarDatosService.AnalisisExcel analisis = svc.analizarExcel(archivo, accion);
                return ResultadoAccion.ok("🔍 Análisis previo: " + archivo.getName(), analisis.resumenCompleto());
            }
            ImportarDatosService.AnalisisExcel analisis = svc.analizarExcel(archivo, accion);
            ImportarDatosService.ResultadoImportacion resultado = svc.importarDesdeExcel(archivo, accion);
            boolean hayResultados = resultado.creados() + resultado.actualizados() > 0;
            boolean hayErrores = !resultado.errores().isEmpty();
            String titulo = hayResultados ? "📥 Importación completada: " + archivo.getName()
                : (hayErrores ? "⚠ Sin registros importados: " + archivo.getName() : "📥 Sin cambios: " + archivo.getName());
            String detalle = "Vista previa → " + analisis.resumenCorto() + "\n\n" + resultado.resumen();
            return (hayResultados || !hayErrores)
                ? ResultadoAccion.ok(titulo, detalle)
                : ResultadoAccion.error(titulo, detalle);
        } catch (java.util.concurrent.TimeoutException e) {
            return ResultadoAccion.error("Tiempo agotado", "El selector de archivos no respondió.");
        } catch (Exception e) {
            return ResultadoAccion.error("Error en la importación",
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private ResultadoAccion pendiente(String nombreAccion) {
        return ResultadoAccion.pendiente(
            "⚙ «" + nombreAccion + "» — ve al módulo correspondiente en el menú lateral.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Object resolverCliente(AccionERP.Data data) throws Exception {
        if (data.id > 0) {
            return clienteDAO.findById(data.id);
        }
        String termino = data.clienteNombre != null ? data.clienteNombre : data.nombre;
        if (termino == null || termino.isBlank()) return null;
        List<Cliente> encontrados = clienteDAO.search(termino);
        if (encontrados.isEmpty()) return "No se encontró ningún cliente con el nombre «" + termino + "».";
        if (encontrados.size() > 1) {
            String lista = encontrados.stream().limit(5)
                .map(c -> "• " + c.getNombreCompleto() + " (ID: " + c.getId() + ")")
                .collect(Collectors.joining("\n"));
            return "Hay " + encontrados.size() + " clientes con ese nombre. Especifica el ID:\n" + lista;
        }
        return encontrados.get(0);
    }

    private void applyClienteFields(Cliente c, AccionERP.Data data) {
        if (data.clienteNombre != null && !data.clienteNombre.isBlank()) {
            String nc = data.clienteNombre.trim();
            int esp = nc.indexOf(' ');
            if (esp > 0) { c.setNombre(nc.substring(0, esp)); c.setApellidos(nc.substring(esp + 1)); }
            else c.setNombre(nc);
        }
        if (data.nombre != null && !data.nombre.isBlank()) c.setNombre(data.nombre);
        if (data.apellidos != null)    c.setApellidos(data.apellidos);
        if (data.nif != null)          c.setNif(data.nif);
        if (data.email != null)        c.setEmail(data.email);
        if (data.telefono != null)     c.setTelefono(data.telefono);
        if (data.ciudad != null)       c.setCiudad(data.ciudad);
        if (data.tipo != null)         c.setTipo(data.tipo);
        if (data.observaciones != null) c.setNotas(data.observaciones);
    }

    private static int parsearClienteId(AccionERP.Data data) {
        if (data == null || data.clienteId == null || data.clienteId.isBlank()) return 0;
        try { return Integer.parseInt(data.clienteId); } catch (NumberFormatException ignored) { return 0; }
    }

    private static String resolverFechaStr(AccionERP.Data data) {
        return (data != null && data.fecha != null && !data.fecha.isBlank())
            ? data.fecha : LocalDate.now().toString();
    }

    private static LocalDate resolverFechaLocal(AccionERP.Data data) {
        if (data != null && data.fecha != null && !data.fecha.isBlank()) {
            try { return LocalDate.parse(data.fecha); } catch (Exception ignored) {}
        }
        return LocalDate.now();
    }

    private String construirDescripcion(AccionERP.Item item) {
        StringBuilder sb = new StringBuilder();
        if (item.descripcion != null) sb.append(item.descripcion);
        boolean tieneSoporte = item.soporte != null && !item.soporte.isBlank();
        if (tieneSoporte || item.gramaje > 0) {
            sb.append(" (");
            if (tieneSoporte) sb.append(item.soporte);
            if (item.gramaje > 0) { if (tieneSoporte) sb.append(", "); sb.append((int) item.gramaje).append("g"); }
            sb.append(")");
        }
        if (item.colores > 0) sb.append(" — ").append(item.colores).append(" color").append(item.colores > 1 ? "es" : "");
        if (item.pantones != null && !item.pantones.isEmpty()) sb.append(" [").append(String.join(", ", item.pantones)).append("]");
        if (item.notas != null && !item.notas.isBlank()) sb.append(". ").append(item.notas);
        return sb.toString();
    }
}
