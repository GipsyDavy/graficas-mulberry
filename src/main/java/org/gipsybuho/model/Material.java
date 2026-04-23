package org.gipsybuho.model;

public class Material {
    private int id;
    private String nombre;
    private String referencia;
    private String categoria; // tintas, pantallas, sustratos, vinilos, consumibles, bordado
    private double stockActual;
    private double stockMinimo;
    private String unidad;
    private double precioUnidad;
    private String proveedor;
    private String updatedAt;

    public Material() {}

    public Material(String nombre, String referencia, String categoria,
                    double stockActual, double stockMinimo, String unidad,
                    double precioUnidad, String proveedor) {
        this.nombre = nombre;
        this.referencia = referencia;
        this.categoria = categoria;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.unidad = unidad;
        this.precioUnidad = precioUnidad;
        this.proveedor = proveedor;
    }

    public boolean isBajoStock() {
        return stockActual <= stockMinimo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public double getStockActual() { return stockActual; }
    public void setStockActual(double stockActual) { this.stockActual = stockActual; }
    public double getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(double stockMinimo) { this.stockMinimo = stockMinimo; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public double getPrecioUnidad() { return precioUnidad; }
    public void setPrecioUnidad(double precioUnidad) { this.precioUnidad = precioUnidad; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return nombre != null ? nombre : ""; }
}
