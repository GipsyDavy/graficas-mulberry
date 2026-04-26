package org.gipsybuho.model;

public class LineaAlbaran {
    private int id;
    private int albaranId;
    private String descripcion;
    private int cantidad;
    private String unidad;
    private int orden;

    public LineaAlbaran() {}

    public LineaAlbaran(String descripcion, int cantidad, String unidad) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.unidad = unidad;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAlbaranId() { return albaranId; }
    public void setAlbaranId(int albaranId) { this.albaranId = albaranId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
