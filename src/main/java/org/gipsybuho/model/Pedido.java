package org.gipsybuho.model;

import java.time.LocalDate;

public class Pedido {

    private int       id;
    private String    numero;
    private int       clienteId;
    private String    clienteNombre;
    private LocalDate fecha;
    private LocalDate fechaEntregaPrevista;
    private LocalDate fechaEntregaReal;
    private String    estado;          // pendiente / en_proceso / listo / entregado / cancelado
    private String    descripcion;
    private double    importeTotal;
    private double    ivaPorcentaje;
    private String    notas;
    // Computed from pagos_pedido via SQL
    private double    importePagado;
    private int       pagosVencidos;

    public double getImportePendiente() {
        return Math.max(0, importeTotal - importePagado);
    }

    public String getEstadoDisplay() {
        return switch (estado != null ? estado : "") {
            case "en_proceso" -> "En proceso";
            case "listo"      -> "Listo";
            case "entregado"  -> "Entregado";
            case "cancelado"  -> "Cancelado";
            default           -> "Pendiente";
        };
    }

    public int    getId()                         { return id; }
    public void   setId(int id)                   { this.id = id; }
    public String getNumero()                     { return numero; }
    public void   setNumero(String v)             { this.numero = v; }
    public int    getClienteId()                  { return clienteId; }
    public void   setClienteId(int v)             { this.clienteId = v; }
    public String getClienteNombre()              { return clienteNombre; }
    public void   setClienteNombre(String v)      { this.clienteNombre = v; }
    public LocalDate getFecha()                   { return fecha; }
    public void   setFecha(LocalDate v)           { this.fecha = v; }
    public LocalDate getFechaEntregaPrevista()    { return fechaEntregaPrevista; }
    public void   setFechaEntregaPrevista(LocalDate v) { this.fechaEntregaPrevista = v; }
    public LocalDate getFechaEntregaReal()        { return fechaEntregaReal; }
    public void   setFechaEntregaReal(LocalDate v){ this.fechaEntregaReal = v; }
    public String getEstado()                     { return estado; }
    public void   setEstado(String v)             { this.estado = v; }
    public String getDescripcion()                { return descripcion; }
    public void   setDescripcion(String v)        { this.descripcion = v; }
    public double getImporteTotal()               { return importeTotal; }
    public void   setImporteTotal(double v)       { this.importeTotal = v; }
    public double getIvaPorcentaje()              { return ivaPorcentaje; }
    public void   setIvaPorcentaje(double v)      { this.ivaPorcentaje = v; }
    public String getNotas()                      { return notas; }
    public void   setNotas(String v)              { this.notas = v; }
    public double getImportePagado()              { return importePagado; }
    public void   setImportePagado(double v)      { this.importePagado = v; }
    public int    getPagosVencidos()              { return pagosVencidos; }
    public void   setPagosVencidos(int v)         { this.pagosVencidos = v; }
}
