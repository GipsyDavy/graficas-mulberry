package org.gipsybuho.model;

public class LineaPresupuesto {
    private int id;
    private int presupuestoId;
    private String descripcion;
    private String tecnica;
    private int cantidad;
    private double precioUnit;
    private double descuento; // porcentaje
    private double total;
    private int orden;

    public LineaPresupuesto() {}

    public LineaPresupuesto(String descripcion, String tecnica, int cantidad,
                            double precioUnit, double descuento) {
        this.descripcion = descripcion;
        this.tecnica = tecnica;
        this.cantidad = cantidad;
        this.precioUnit = precioUnit;
        this.descuento = descuento;
        calcularTotal();
    }

    public void calcularTotal() {
        double base = cantidad * precioUnit;
        total = base - (base * descuento / 100.0);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPresupuestoId() { return presupuestoId; }
    public void setPresupuestoId(int presupuestoId) { this.presupuestoId = presupuestoId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTecnica() { return tecnica; }
    public void setTecnica(String tecnica) { this.tecnica = tecnica; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getPrecioUnit() { return precioUnit; }
    public void setPrecioUnit(double precioUnit) { this.precioUnit = precioUnit; }
    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
