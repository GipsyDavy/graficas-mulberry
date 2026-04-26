package org.gipsybuho.model;

import java.util.ArrayList;
import java.util.List;

public class Albaran {
    private int id;
    private String numero;
    private int clienteId;
    private String clienteNombre;
    private String fecha;
    private int facturaId;
    private String facturaNumero;
    private int pedidoId;
    private String pedidoNumero;
    private String estado; // pendiente, entregado, firmado
    private String observaciones;
    private String createdAt;
    private List<LineaAlbaran> lineas = new ArrayList<>();

    public Albaran() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public int getFacturaId() { return facturaId; }
    public void setFacturaId(int facturaId) { this.facturaId = facturaId; }
    public String getFacturaNumero() { return facturaNumero; }
    public void setFacturaNumero(String facturaNumero) { this.facturaNumero = facturaNumero; }
    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }
    public String getPedidoNumero() { return pedidoNumero; }
    public void setPedidoNumero(String pedidoNumero) { this.pedidoNumero = pedidoNumero; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<LineaAlbaran> getLineas() { return lineas; }
    public void setLineas(List<LineaAlbaran> lineas) { this.lineas = lineas; }
}
