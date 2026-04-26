package org.gipsybuho.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PagoPedido {

    private int       id;
    private int       pedidoId;
    private String    pedidoNumero;
    private String    clienteNombre;
    private String    concepto;
    private double    importe;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String    formaPago;
    private String    estado;          // pendiente / pagado
    private LocalDate fechaPago;
    private String    notas;

    // "pagado" | "vencido" | "proximo" (≤7 días) | "pendiente"
    public String getEstadoEfectivo() {
        if ("pagado".equals(estado)) return "pagado";
        if (fechaVencimiento == null) return "pendiente";
        LocalDate hoy = LocalDate.now();
        if (fechaVencimiento.isBefore(hoy))              return "vencido";
        if (!fechaVencimiento.isAfter(hoy.plusDays(7)))  return "proximo";
        return "pendiente";
    }

    public long getDiasRestantes() {
        if ("pagado".equals(estado) || fechaVencimiento == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }
    public int    getPedidoId()                    { return pedidoId; }
    public void   setPedidoId(int v)               { this.pedidoId = v; }
    public String getPedidoNumero()                { return pedidoNumero; }
    public void   setPedidoNumero(String v)        { this.pedidoNumero = v; }
    public String getClienteNombre()               { return clienteNombre; }
    public void   setClienteNombre(String v)       { this.clienteNombre = v; }
    public String getConcepto()                    { return concepto; }
    public void   setConcepto(String v)            { this.concepto = v; }
    public double getImporte()                     { return importe; }
    public void   setImporte(double v)             { this.importe = v; }
    public LocalDate getFechaEmision()             { return fechaEmision; }
    public void   setFechaEmision(LocalDate v)     { this.fechaEmision = v; }
    public LocalDate getFechaVencimiento()         { return fechaVencimiento; }
    public void   setFechaVencimiento(LocalDate v) { this.fechaVencimiento = v; }
    public String getFormaPago()                   { return formaPago; }
    public void   setFormaPago(String v)           { this.formaPago = v; }
    public String getEstado()                      { return estado; }
    public void   setEstado(String v)              { this.estado = v; }
    public LocalDate getFechaPago()                { return fechaPago; }
    public void   setFechaPago(LocalDate v)        { this.fechaPago = v; }
    public String getNotas()                       { return notas; }
    public void   setNotas(String v)               { this.notas = v; }
}
