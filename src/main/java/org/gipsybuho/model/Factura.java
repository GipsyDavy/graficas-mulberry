package org.gipsybuho.model;

import java.util.ArrayList;
import java.util.List;

public class Factura {
    private int id;
    private String numero;
    private int presupuestoId;
    private int clienteId;
    private String clienteNombre;
    private String fecha;
    private String fechaVencimiento;
    private String estado; // pendiente, pagada, vencida, anulada
    private String formaPago;
    private double baseImponible;
    private double ivaPorcentaje;
    private double ivaImporte;
    private double total;
    private String notas;
    private String createdAt;
    private List<LineaFactura> lineas = new ArrayList<>();

    public Factura() {}

    public void calcularTotales() {
        baseImponible = lineas.stream().mapToDouble(LineaFactura::getTotal).sum();
        ivaImporte = baseImponible * (ivaPorcentaje / 100.0);
        total = baseImponible + ivaImporte;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public int getPresupuestoId() { return presupuestoId; }
    public void setPresupuestoId(int presupuestoId) { this.presupuestoId = presupuestoId; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public double getBaseImponible() { return baseImponible; }
    public void setBaseImponible(double baseImponible) { this.baseImponible = baseImponible; }
    public double getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(double ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public double getIvaImporte() { return ivaImporte; }
    public void setIvaImporte(double ivaImporte) { this.ivaImporte = ivaImporte; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<LineaFactura> getLineas() { return lineas; }
    public void setLineas(List<LineaFactura> lineas) { this.lineas = lineas; }
}
