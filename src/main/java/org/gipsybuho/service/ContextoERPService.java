package org.gipsybuho.service;

import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.dao.NotaCalendarioDAO;
import org.gipsybuho.dao.PedidoDAO;
import org.gipsybuho.dao.PresupuestoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.NotaCalendario;
import org.gipsybuho.model.Pedido;

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

    private final PresupuestoDAO    presupuestoDAO;
    private final FacturaDAO        facturaDAO;
    private final PedidoDAO         pedidoDAO;
    private final MaterialDAO       materialDAO;
    private final ClienteDAO        clienteDAO;
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
            int total     = materialDAO.findAll().size();
            List<Material> criticos = materialDAO.findBajoStock();
            sb.append("MATERIALES    → ").append(total).append(" en catálogo");
            if (!criticos.isEmpty()) {
                sb.append(" · ⚠ ").append(criticos.size()).append(" con stock bajo:\n");
                for (Material m : criticos) {
                    sb.append("    - ").append(m.getNombre())
                      .append(" (stock actual: ").append(m.getStockActual())
                      .append(" ").append(m.getUnidad())
                      .append(", mínimo: ").append(m.getStockMinimo()).append(")\n");
                }
                alertas.append("  ⚠ ").append(criticos.size())
                       .append(" material(es) bajo stock mínimo → revisar pedidos\n");
            } else {
                sb.append(" · ✅ todos con stock suficiente\n");
            }
        } catch (Exception ignored) {
            sb.append("MATERIALES    → (datos no disponibles)\n");
        }

        // ── Clientes ──────────────────────────────────────────────────────────
        try {
            int total = clienteDAO.count();
            sb.append("CLIENTES      → ").append(total).append(" registrados\n");
        } catch (Exception ignored) {
            sb.append("CLIENTES      → (datos no disponibles)\n");
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
