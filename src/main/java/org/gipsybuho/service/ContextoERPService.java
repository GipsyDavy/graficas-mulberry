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
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.model.NotaCalendario;
import org.gipsybuho.model.Pedido;
import org.gipsybuho.model.Presupuesto;
import org.gipsybuho.model.Tarifa;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Recopila datos actuales de la BD y los formatea como bloque de texto
 * para inyectarlos como contexto en los mensajes a Ollama.
 * El resultado se cachea 8 minutos para evitar consultas repetidas.
 */
public class ContextoERPService {

    private static final long CACHE_MILLIS = 8 * 60 * 1000L;

    private static final int TOP_N_TRANSACCIONAL = 5;
    private static final int MIN_LARGO_NOMBRE_DETALLE = 3;

    private final PresupuestoDAO    presupuestoDAO;
    private final FacturaDAO        facturaDAO;
    private final PedidoDAO         pedidoDAO;
    private final MaterialDAO       materialDAO;
    private final ClienteDAO        clienteDAO;
    private final EmpleadoDAO       empleadoDAO;
    private final TarifaDAO         tarifaDAO;
    private final AlbaranDAO        albaranDAO;
    private final NominaDAO         nominaDAO;
    private final NotaCalendarioDAO calendarioDAO;

    private volatile String cachedContexto = null;
    private volatile long   cacheTimestamp = 0L;

    public ContextoERPService() {
        try {
            Connection conn = DatabaseManager.getConnection();
            calendarioDAO = new NotaCalendarioDAO(conn);
            pedidoDAO = new PedidoDAO(conn);
            materialDAO = new MaterialDAO(conn);
            clienteDAO = new ClienteDAO(conn);
            empleadoDAO = new EmpleadoDAO(conn);
            tarifaDAO = new TarifaDAO(conn);
            albaranDAO = new AlbaranDAO(conn);
            nominaDAO = new NominaDAO(conn);
            presupuestoDAO = new PresupuestoDAO(conn);
            facturaDAO = new FacturaDAO(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Devuelve el contexto cacheado o lo regenera si han pasado más de 8 minutos. */
    public String construirContexto() {
        long ahora = System.currentTimeMillis();
        if (cachedContexto != null && (ahora - cacheTimestamp) < CACHE_MILLIS) {
            return cachedContexto;
        }
        String nuevo = generarContexto();
        cachedContexto  = nuevo;
        cacheTimestamp  = ahora;
        return nuevo;
    }

    /** Invalida el caché para forzar una regeneración en la próxima llamada. */
    public void invalidarCache() {
        cacheTimestamp = 0L;
    }

    /**
     * Devuelve el contexto cacheado (resumen + listados de toda la app) y, si el prompt del
     * usuario menciona el nombre de un cliente o empleado concreto, añade su detalle completo.
     * Evita tener que listar siempre todos los registros en detalle (coste de caracteres),
     * resolviendo bajo demanda solo lo que la consulta del usuario necesita.
     */
    public String construirContexto(String promptUsuario) {
        String base = construirContexto();
        String detalle = detalleBajoDemanda(promptUsuario);
        return detalle.isBlank() ? base : base + "\n" + detalle;
    }

    private String detalleBajoDemanda(String promptUsuario) {
        if (promptUsuario == null || promptUsuario.isBlank()) return "";
        String promptLower = promptUsuario.toLowerCase(Locale.of("es", "ES"));
        StringBuilder sb = new StringBuilder();

        try {
            for (Cliente c : clienteDAO.findAll()) {
                if (coincideNombre(promptLower, c.getNombre())) {
                    sb.append("DETALLE CLIENTE → ").append(c.getNombreCompleto());
                    if (c.getCiudad() != null && !c.getCiudad().isBlank()) sb.append(" — ").append(c.getCiudad());
                    if (c.getTelefono() != null && !c.getTelefono().isBlank()) sb.append(" — Tel: ").append(c.getTelefono());
                    if (c.getEmail() != null && !c.getEmail().isBlank()) sb.append(" — ").append(c.getEmail());
                    if (c.getNotas() != null && !c.getNotas().isBlank()) sb.append(" — Notas: ").append(c.getNotas());
                    sb.append("\n");
                }
            }
        } catch (Exception ignored) { /* sección opcional */ }

        try {
            for (Empleado e : empleadoDAO.findAll()) {
                if (coincideNombre(promptLower, e.getNombre())) {
                    sb.append("DETALLE EMPLEADO → ").append(e.getNombreCompleto());
                    if (e.getCategoria() != null && !e.getCategoria().isBlank()) sb.append(" — ").append(e.getCategoria());
                    if (e.getFechaAlta() != null && !e.getFechaAlta().isBlank()) sb.append(" — Alta: ").append(e.getFechaAlta());
                    sb.append("\n");
                }
            }
        } catch (Exception ignored) { /* sección opcional */ }

        if (sb.isEmpty()) return "";
        return "DETALLE BAJO DEMANDA (según tu consulta):\n" + sb;
    }

    private boolean coincideNombre(String promptLower, String nombre) {
        if (nombre == null || nombre.trim().length() < MIN_LARGO_NOMBRE_DETALLE) return false;
        return promptLower.contains(nombre.trim().toLowerCase(Locale.of("es", "ES")));
    }

    // ── Construcción real del contexto ────────────────────────────────────────

    private String generarContexto() {
        LocalDate hoy  = LocalDate.now();
        int anio       = hoy.getYear();
        int mes        = hoy.getMonthValue();
        String mesNombre = hoy.getMonth().getDisplayName(TextStyle.FULL, Locale.of("es", "ES"));

        StringBuilder alertas = new StringBuilder();
        StringBuilder sb      = new StringBuilder();

        sb.append("=== DATOS ACTUALES DEL ERP — ").append(hoy)
          .append(" (").append(mesNombre).append(" ").append(anio).append(") ===\n\n");

        // ── Presupuestos ──────────────────────────────────────────────────────
        try {
            int borrador = presupuestoDAO.countByEstado("borrador");
            int enviado  = presupuestoDAO.countByEstado("enviado");
            int aceptado = presupuestoDAO.countByEstado("aceptado");
            sb.append("PRESUPUESTOS  → ")
              .append(borrador).append(" en borrador · ")
              .append(enviado).append(" enviados · ")
              .append(aceptado).append(" aceptados\n");
            List<Presupuesto> recientes = presupuestoDAO.findAll();
            if (!recientes.isEmpty()) {
                sb.append("  Últimos presupuestos:\n");
                for (Presupuesto p : recientes.subList(0, Math.min(TOP_N_TRANSACCIONAL, recientes.size()))) {
                    sb.append("    - ").append(p.getNumero())
                      .append(" — ").append(p.getClienteNombre())
                      .append(" — ").append(p.getEstado())
                      .append(" — ").append(String.format("%.2f €", p.getTotal())).append("\n");
                }
            }
        } catch (Exception ignored) {
            sb.append("PRESUPUESTOS  → (datos no disponibles)\n");
        }

        // ── Facturas ──────────────────────────────────────────────────────────
        try {
            int    pendientes = facturaDAO.countByEstado("pendiente");
            int    pagadas    = facturaDAO.countByEstado("pagada");
            double totalAnio  = facturaDAO.totalFacturadoAnio(anio);
            double totalMes   = facturaDAO.totalFacturadoMes(anio, mes);
            sb.append("FACTURAS      → ")
              .append(pendientes).append(" pendientes de cobro · ")
              .append(pagadas).append(" pagadas\n")
              .append("  Facturado ").append(mesNombre).append(": ")
              .append(String.format("%.2f €", totalMes))
              .append("  |  Acumulado ").append(anio).append(": ")
              .append(String.format("%.2f €", totalAnio)).append("\n");

            if (pendientes > 0)
                alertas.append("  ⚠ ").append(pendientes)
                       .append(" factura(s) pendientes de cobro\n");

            List<Factura> pendientesDetalle = facturaDAO.findAll().stream()
                .filter(f -> "pendiente".equalsIgnoreCase(f.getEstado()))
                .limit(TOP_N_TRANSACCIONAL)
                .toList();
            if (!pendientesDetalle.isEmpty()) {
                sb.append("  Facturas pendientes de cobro:\n");
                for (Factura f : pendientesDetalle) {
                    sb.append("    - ").append(f.getNumero())
                      .append(" — ").append(f.getClienteNombre())
                      .append(" — ").append(String.format("%.2f €", f.getTotal()))
                      .append(" — vence: ").append(f.getFechaVencimiento()).append("\n");
                }
            }
        } catch (Exception ignored) {
            sb.append("FACTURAS      → (datos no disponibles)\n");
        }

        // ── Top clientes del mes ──────────────────────────────────────────────
        try {
            List<String[]> top = facturaDAO.topClientesMes(anio, mes, 5);
            if (!top.isEmpty()) {
                sb.append("TOP CLIENTES ").append(mesNombre.toUpperCase()).append(":\n");
                int pos = 1;
                for (String[] fila : top) {
                    sb.append("  ").append(pos++).append(". ")
                      .append(fila[0]).append(" — ").append(fila[1]).append("\n");
                }
            }
        } catch (Exception ignored) {
            // sección opcional, silencio en caso de error
        }

        // ── Pedidos ───────────────────────────────────────────────────────────
        try {
            List<Pedido> todos = pedidoDAO.findAll();
            long activos = todos.stream()
                .filter(p -> {
                    String e = p.getEstado();
                    return e != null && (e.equalsIgnoreCase("pendiente")
                        || e.toLowerCase().contains("proceso")
                        || e.equalsIgnoreCase("en produccion")
                        || e.equalsIgnoreCase("en producción"));
                }).count();
            long entregados = todos.stream()
                .filter(p -> p.getEstado() != null
                    && (p.getEstado().equalsIgnoreCase("entregado")
                        || p.getEstado().equalsIgnoreCase("completado")))
                .count();
            sb.append("PEDIDOS       → ")
              .append(activos).append(" activos (pendiente / en proceso) · ")
              .append(entregados).append(" entregados\n");
        } catch (Exception ignored) {
            sb.append("PEDIDOS       → (datos no disponibles)\n");
        }

        // ── Materiales ────────────────────────────────────────────────────────
        try {
            List<Material> todosMateriales = materialDAO.findAll();
            List<Material> criticos        = materialDAO.findBajoStock();
            sb.append("MATERIALES    → ").append(todosMateriales.size()).append(" en catálogo:\n");
            for (Material m : todosMateriales) {
                sb.append("    - ").append(m.getNombre());
                if (m.getCategoria() != null && !m.getCategoria().isBlank())
                    sb.append(" [").append(m.getCategoria()).append("]");
                sb.append(" — stock: ").append(m.getStockActual())
                  .append(" ").append(m.getUnidad()).append("\n");
            }
            if (!criticos.isEmpty()) {
                sb.append("  ⚠ ").append(criticos.size()).append(" con stock bajo mínimo:\n");
                for (Material m : criticos) {
                    sb.append("    - ").append(m.getNombre())
                      .append(" (stock actual: ").append(m.getStockActual())
                      .append(" ").append(m.getUnidad())
                      .append(", mínimo: ").append(m.getStockMinimo()).append(")\n");
                }
                alertas.append("  ⚠ ").append(criticos.size())
                       .append(" material(es) bajo stock mínimo → revisar pedidos\n");
            } else {
                sb.append("  ✅ todos con stock suficiente\n");
            }
        } catch (Exception ignored) {
            sb.append("MATERIALES    → (datos no disponibles)\n");
        }

        // ── Clientes ──────────────────────────────────────────────────────────
        try {
            List<Cliente> clientes = clienteDAO.findAll();
            sb.append("CLIENTES      → ").append(clientes.size()).append(" registrados:\n");
            for (Cliente c : clientes) {
                sb.append("    - ").append(c.getNombreCompleto());
                if (c.getCiudad() != null && !c.getCiudad().isBlank())
                    sb.append(" — ").append(c.getCiudad());
                if (c.getTelefono() != null && !c.getTelefono().isBlank())
                    sb.append(" — Tel: ").append(c.getTelefono());
                sb.append("\n");
            }
        } catch (Exception ignored) {
            sb.append("CLIENTES      → (datos no disponibles)\n");
        }

        // ── Empleados ─────────────────────────────────────────────────────────
        try {
            List<Empleado> empleados = empleadoDAO.findAll();
            sb.append("EMPLEADOS     → ").append(empleados.size()).append(" activos:\n");
            for (Empleado e : empleados) {
                sb.append("    - ").append(e.getNombreCompleto());
                if (e.getCategoria() != null && !e.getCategoria().isBlank())
                    sb.append(" — ").append(e.getCategoria());
                sb.append("\n");
            }
        } catch (Exception ignored) {
            sb.append("EMPLEADOS     → (datos no disponibles)\n");
        }

        // ── Tarifas ───────────────────────────────────────────────────────────
        try {
            List<Tarifa> tarifas = tarifaDAO.findAll();
            sb.append("TARIFAS       → ").append(tarifas.size()).append(" definidas:\n");
            for (Tarifa t : tarifas) {
                sb.append("    - ").append(t.getNombre())
                  .append(" [").append(t.getTecnica()).append("]")
                  .append(" — ").append(String.format("%.2f €/ud", t.getPrecioUnit())).append("\n");
            }
        } catch (Exception ignored) {
            sb.append("TARIFAS       → (datos no disponibles)\n");
        }

        // ── Albaranes ─────────────────────────────────────────────────────────
        try {
            List<Albaran> albaranes = albaranDAO.findAll();
            long pendientesAlbaran = albaranes.stream()
                .filter(a -> a.getEstado() != null && a.getEstado().equalsIgnoreCase("pendiente"))
                .count();
            sb.append("ALBARANES     → ").append(albaranes.size()).append(" en total · ")
              .append(pendientesAlbaran).append(" pendientes\n");
            if (!albaranes.isEmpty()) {
                sb.append("  Últimos albaranes:\n");
                for (Albaran a : albaranes.subList(0, Math.min(TOP_N_TRANSACCIONAL, albaranes.size()))) {
                    sb.append("    - ").append(a.getNumero())
                      .append(" — ").append(a.getClienteNombre())
                      .append(" — ").append(a.getEstado()).append("\n");
                }
            }
        } catch (Exception ignored) {
            sb.append("ALBARANES     → (datos no disponibles)\n");
        }

        // ── Nóminas ───────────────────────────────────────────────────────────
        try {
            List<Nomina> nominasMes = nominaDAO.findAll().stream()
                .filter(n -> n.getAnio() == anio && n.getMes() == mes)
                .toList();
            double costeMes = nominasMes.stream().mapToDouble(Nomina::getCosteTotalEmpresa).sum();
            sb.append("NOMINAS       → ").append(nominasMes.size()).append(" generadas en ").append(mesNombre)
              .append(" · coste total: ").append(String.format("%.2f €", costeMes)).append("\n");
        } catch (Exception ignored) {
            sb.append("NOMINAS       → (datos no disponibles)\n");
        }

        // ── Calendario: próximos 7 días ───────────────────────────────────────
        try {
            List<NotaCalendario> proximas = calendarioDAO.findProximas(7);
            if (!proximas.isEmpty()) {
                sb.append("\nCALENDARIO (próximos 7 días):\n");
                for (NotaCalendario n : proximas) {
                    sb.append("  ").append(n.getFecha())
                      .append(" → ").append(n.getTitulo());
                    if (n.getNota() != null && !n.getNota().isBlank())
                        sb.append(": ").append(n.getNota());
                    sb.append("\n");
                }
            }
        } catch (Exception ignored) {
            // sección opcional, silencio en caso de error
        }

        // ── Alertas destacadas ────────────────────────────────────────────────
        if (!alertas.isEmpty()) {
            int idx = sb.indexOf("\n\n");
            int insertPos = idx >= 0 ? idx + 2 : Math.max(sb.indexOf("\n") + 1, 0);
            sb.insert(insertPos, "ALERTAS ACTIVAS:\n" + alertas + "\n");
        }

        sb.append("\n=== FIN CONTEXTO ERP ===\n")
          .append("Usa estos datos para responder preguntas sobre el estado actual del negocio. ")
          .append("No inventes cifras: usa exactamente los valores indicados arriba.");

        return sb.toString();
    }
}
