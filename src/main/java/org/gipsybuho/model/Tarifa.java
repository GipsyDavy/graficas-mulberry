package org.gipsybuho.model;

public class Tarifa {
    private int id;
    private String tecnica;
    private String nombre;
    private String descripcion;
    private double precioUnit;
    private double precioSetup;
    private int minimoUnidades;
    private boolean activa;
    private String updatedAt;

    public Tarifa() {}

    public Tarifa(String tecnica, String nombre, String descripcion,
                  double precioUnit, double precioSetup, int minimoUnidades) {
        this.tecnica = tecnica;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioUnit = precioUnit;
        this.precioSetup = precioSetup;
        this.minimoUnidades = minimoUnidades;
        this.activa = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTecnica() { return tecnica; }
    public void setTecnica(String tecnica) { this.tecnica = tecnica; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public double getPrecioUnit() { return precioUnit; }
    public void setPrecioUnit(double precioUnit) { this.precioUnit = precioUnit; }
    public double getPrecioSetup() { return precioSetup; }
    public void setPrecioSetup(double precioSetup) { this.precioSetup = precioSetup; }
    public int getMinimoUnidades() { return minimoUnidades; }
    public void setMinimoUnidades(int minimoUnidades) { this.minimoUnidades = minimoUnidades; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return nombre != null ? nombre : ""; }
}
