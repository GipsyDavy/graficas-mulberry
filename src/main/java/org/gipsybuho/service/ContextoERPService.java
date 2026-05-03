package org.gipsybuho.service;

import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.dao.PedidoDAO;
import org.gipsybuho.dao.PresupuestoDAO;
import org.gipsybuho.model.Pedido;

import java.time.LocalDate;
import java.util.List;

/**
 * Recopila datos actuales de la BD y los formatea como bloque de texto
 * para inyectarlos como contexto en los mensajes a Ollama.
 */
public class ContextoERPService {

    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAO();
    private final FacturaDAO     facturaDAO     = new FacturaDAO();
    private final PedidoDAO      pedidoDAO      = new PedidoDAO();
    private final MaterialDAO    materialDAO    = new MaterialDAO();
    private final ClienteDAO     clienteDAO     = new ClienteDAO();

    /**
     * Construye un bloque de texto con los datos actuales del ERP.
     * Cada sección se protege con try-catch para que un fallo aislado
     * no rompa el resto del contexto.
     */
    public String construirContexto() {
        LocalDate hoy = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        sb.append("=== DATOS ACTUALES DEL ERP — ").append(hoy).append(" ===\n\n");

        // ── Presupuestos ──────────────────────────────────────────────────────
        try {
            int borrador  = presupuestoDAO.countByEstado("borrador");
            int enviado   = presupuestoDAO.countByEstado("enviado");
            int aceptado  = presupuestoDAO.countByEstado("aceptado");
            sb.append("PRESUPUESTOS  → ")
              .append(borrador).append(" en borrador · ")
              .append(enviado).append(" enviados · ")
              .append(aceptado).append(" aceptados\n");
        } catch (Exception ignored) {
            sb.append("PRESUPUESTOS  → (datos no disponibles)\n");
        }

        // ── Facturas ──────────────────────────────────────────────────────────
        try {
            int pendientes  = facturaDAO.countByEstado("pendiente");
            int pagadas     = facturaDAO.countByEstado("pagada");
            double totalAnio = facturaDAO.totalFacturadoAnio(hoy.getYear());
            sb.append("FACTURAS      → ")
              .append(pendientes).append(" pendientes de cobro · ")
              .append(pagadas).append(" pagadas | ")
              .append("Facturado ").append(hoy.getYear()).append(": ")
              .append(String.format("%.2f €", totalAnio)).append("\n");
        } catch (Exception ignored) {
            sb.append("FACTURAS      → (datos no disponibles)\n");
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
                })
                .count();
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
            int bajoStock = materialDAO.countBajoStock();
            sb.append("MATERIALES    → ").append(total).append(" en catálogo");
            if (bajoStock > 0) sb.append(" · ⚠ ").append(bajoStock).append(" con stock bajo");
            else               sb.append(" · ✅ todos con stock suficiente");
            sb.append("\n");
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

        sb.append("\n=== FIN CONTEXTO ERP ===\n")
          .append("Usa estos datos para responder preguntas sobre el estado actual del negocio. ")
          .append("No inventes cifras: usa exactamente los valores indicados arriba.");

        return sb.toString();
    }
}
