package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.util.List;
import java.util.Map;

public class Tarifa {
    private int id;
    private String tecnica;
    private String nombre;
    private String descripcion;
    private double precioUnit;
    private double precioSetup;
    private int minimoUnidades;
    private boolean activa;
    private boolean usaTiempo;
    private String updatedAt;

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    private static EntityImportSpec buildSpec() {
        var syn = Map.of(
            "tecnica",         List.of("tecnica", "proceso", "technique", "tipo", "type"),
            "nombre",          List.of("nombre", "name", "servicio", "tarifa"),
            "descripcion",     List.of("descripcion", "detalle", "description"),
            "precio_unit",     List.of("precio unitario", "precio unidad", "precio unit", "precio", "price", "rate", "pvp"),
            "precio_setup",    List.of("precio setup", "setup", "configuracion", "arranque", "preparacion"),
            "minimo_unidades", List.of("minimo unidades", "cantidad minima", "minimo", "min qty", "minimum")
        );
        return new EntityImportSpec("Tarifas", List.of(
            new FieldSpec("tecnica",         "Técnica",            true),
            new FieldSpec("nombre",          "Nombre",             true),
            new FieldSpec("descripcion",     "Descripción",        false),
            new FieldSpec("precio_unit",     "Precio unitario",    false),
            new FieldSpec("precio_setup",    "Precio de setup",    false),
            new FieldSpec("minimo_unidades", "Mínimo de unidades", false)
        ), h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn), DuplicatePolicy.SKIP_IF_EXISTS);
    }

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
    public boolean isUsaTiempo() { return usaTiempo; }
    public void setUsaTiempo(boolean usaTiempo) { this.usaTiempo = usaTiempo; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return nombre != null ? nombre : ""; }
}
