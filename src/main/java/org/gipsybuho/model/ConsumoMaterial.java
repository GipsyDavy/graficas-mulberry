package org.gipsybuho.model;

public class ConsumoMaterial {
    private int id;
    private String tecnica;
    private int materialId;
    private String materialNombre;
    private String unidad;
    private double cantidadPorUnidad;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTecnica() { return tecnica; }
    public void setTecnica(String tecnica) { this.tecnica = tecnica; }
    public int getMaterialId() { return materialId; }
    public void setMaterialId(int materialId) { this.materialId = materialId; }
    public String getMaterialNombre() { return materialNombre; }
    public void setMaterialNombre(String materialNombre) { this.materialNombre = materialNombre; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public double getCantidadPorUnidad() { return cantidadPorUnidad; }
    public void setCantidadPorUnidad(double cantidadPorUnidad) { this.cantidadPorUnidad = cantidadPorUnidad; }

    @Override
    public String toString() { return tecnica + " → " + materialNombre; }
}