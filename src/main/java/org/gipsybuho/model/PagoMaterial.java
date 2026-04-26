package org.gipsybuho.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PagoMaterial {

    private int       id;
    private int       materialId;
    private String    materialNombre;
    private String    proveedor;
    private String    numeroFactura;
    private LocalDate fechaCompra;
    private double    cantidadComprada;
    private String    unidad;
    private double    importeTotal;
    private String    formaPago;
    private LocalDate fechaVencimiento;
    private String    estado;          // "pendiente" | "pagado"
    private LocalDate fechaPago;
    private String    notas;

    // ── Estado efectivo ───────────────────────────────────────────────────────
    // "pagado" | "vencido" | "proximo" (≤7 días) | "pendiente"
    public String getEstadoEfectivo() {
        if ("pagado".equals(estado)) return "pagado";
        if (fechaVencimiento == null)  return "pendiente";
        LocalDate hoy = LocalDate.now();
        if (fechaVencimiento.isBefore(hoy))                         return "vencido";
        if (!fechaVencimiento.isAfter(hoy.plusDays(7)))             return "proximo";
        return "pendiente";
    }

    public long getDiasRestantes() {
        if ("pagado".equals(estado) || fechaVencimiento == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int       getId()                { return id; }
    public void      setId(int id)          { this.id = id; }
    public int       getMaterialId()        { return materialId; }
    public void      setMaterialId(int v)   { this.materialId = v; }
    public String    getMaterialNombre()    { return materialNombre; }
    public void      setMaterialNombre(String v) { this.materialNombre = v; }
    public String    getProveedor()         { return proveedor; }
    public void      setProveedor(String v) { this.proveedor = v; }
    public String    getNumeroFactura()     { return numeroFactura; }
    public void      setNumeroFactura(String v) { this.numeroFactura = v; }
    public LocalDate getFechaCompra()       { return fechaCompra; }
    public void      setFechaCompra(LocalDate v) { this.fechaCompra = v; }
    public double    getCantidadComprada()  { return cantidadComprada; }
    public void      setCantidadComprada(double v) { this.cantidadComprada = v; }
    public String    getUnidad()            { return unidad; }
    public void      setUnidad(String v)    { this.unidad = v; }
    public double    getImporteTotal()      { return importeTotal; }
    public void      setImporteTotal(double v) { this.importeTotal = v; }
    public String    getFormaPago()         { return formaPago; }
    public void      setFormaPago(String v) { this.formaPago = v; }
    public LocalDate getFechaVencimiento()  { return fechaVencimiento; }
    public void      setFechaVencimiento(LocalDate v) { this.fechaVencimiento = v; }
    public String    getEstado()            { return estado; }
    public void      setEstado(String v)    { this.estado = v; }
    public LocalDate getFechaPago()         { return fechaPago; }
    public void      setFechaPago(LocalDate v) { this.fechaPago = v; }
    public String    getNotas()             { return notas; }
    public void      setNotas(String v)     { this.notas = v; }
}
