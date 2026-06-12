package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.util.List;
import java.util.Map;

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

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    private static EntityImportSpec buildSpec() {
        var syn = Map.of(
            "nombre",        List.of("nombre", "material", "articulo", "name", "item", "producto", "modelo", "tipo de papel",
                                      "tipo papel", "familia", "concepto"),
            "referencia",    List.of("referencia", "ref", "codigo", "sku", "code"),
            "categoria",     List.of("categoria", "tipo", "grupo", "category"),
            "stock_actual",  List.of("stock actual", "existencias", "cantidad", "quantity"),
            "stock_minimo",  List.of("stock minimo", "stock min", "minimo stock", "stock", "minimo", "reorder"),
            "unidad",        List.of("unidad", "ud", "und", "medida", "unit", "um"),
            "precio_unidad", List.of("precio unidad", "precio ud", "precio resma", "precio pliego", "precio millar",
                                      "eur resma", "eur pliego", "precio", "coste", "price", "pvp"),
            "proveedor",     List.of("proveedor", "supplier", "vendor", "fabricante")
        );
        return new EntityImportSpec("Materiales", List.of(
            new FieldSpec("nombre",        "Nombre",        true),
            new FieldSpec("referencia",    "Referencia",    false),
            new FieldSpec("categoria",     "Categoría",     false),
            new FieldSpec("stock_actual",  "Stock actual",  false),
            new FieldSpec("stock_minimo",  "Stock mínimo",  false),
            new FieldSpec("unidad",        "Unidad",        false),
            new FieldSpec("precio_unidad", "Precio unidad", false),
            new FieldSpec("proveedor",     "Proveedor",     false)
        ), h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn), DuplicatePolicy.SKIP_IF_EXISTS);
    }

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
