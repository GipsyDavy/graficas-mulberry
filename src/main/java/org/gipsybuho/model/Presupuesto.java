package org.gipsybuho.model;

import java.util.ArrayList;
import java.util.List;

public class Presupuesto {
    private int id;
    private String numero;
    private int clienteId;
    private String clienteNombre; // campo auxiliar para mostrar en tabla
    private String fecha;
    private String fechaValidez;
    private String estado; // borrador, enviado, aceptado, rechazado, facturado
    private double baseImponible;
    private double ivaPorcentaje;
    private double ivaImporte;
    private double total;
    private String notas;
    private String condiciones;
    private String createdAt;
    private List<LineaPresupuesto> lineas = new ArrayList<>();

    public Presupuesto() {}

    public void calcularTotales() {
        baseImponible = lineas.stream().mapToDouble(LineaPresupuesto::getTotal).sum();
        ivaImporte = baseImponible * (ivaPorcentaje / 100.0);
        total = baseImponible + ivaImporte;
    }

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
    public String getFechaValidez() { return fechaValidez; }
    public void setFechaValidez(String fechaValidez) { this.fechaValidez = fechaValidez; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
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
    public String getCondiciones() { return condiciones; }
    public void setCondiciones(String condiciones) { this.condiciones = condiciones; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<LineaPresupuesto> getLineas() { return lineas; }
    public void setLineas(List<LineaPresupuesto> lineas) { this.lineas = lineas; }
}
