package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cliente {
    private int id;
    private String nombre;
    private String apellidos;
    private String tipo; // empresa, particular
    private String nif;
    private String direccion;
    private String ciudad;
    private String cp;
    private String telefono;
    private String email;
    private String notas;
    private String createdAt;
    // Columnas adicionales añadidas dinámicamente al importar desde archivos externos
    private Map<String, String> extras = new LinkedHashMap<>();

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    private static EntityImportSpec buildSpec() {
        // Exactamente 10 entradas → Map.of() en su límite
        var syn = Map.of(
            "nombre",    List.of("razon social", "nombre", "empresa", "company", "name"),
            "apellido",  List.of("apellidos", "apellido", "surname", "lastname"),
            "tipo",      List.of("tipo cliente", "tipo", "type"),
            "nif",       List.of("nif", "cif", "dni", "nie", "vat", "tax id"),
            "direccion", List.of("direccion", "domicilio", "address"),
            "ciudad",    List.of("ciudad", "city", "localidad", "municipio"),
            "cp",        List.of("codigo postal", "postal code", "cp", "zip", "postcode"),
            "telefono",  List.of("telefono", "tel", "phone", "movil"),
            "email",     List.of("correo electronico", "email", "correo", "mail"),
            "notas",     List.of("observaciones", "notas", "comentarios", "notes")
        );
        return new EntityImportSpec("Clientes", List.of(
            new FieldSpec("nombre",    "Nombre",    true),
            new FieldSpec("apellido",  "Apellidos", false),
            new FieldSpec("tipo",      "Tipo",      false),
            new FieldSpec("nif",       "NIF/CIF",   false),
            new FieldSpec("direccion", "Dirección", false),
            new FieldSpec("ciudad",    "Ciudad",    false),
            new FieldSpec("cp",        "C.P.",      false),
            new FieldSpec("telefono",  "Teléfono",  false),
            new FieldSpec("email",     "Email",     false),
            new FieldSpec("notas",     "Notas",     false)
        ), h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn), DuplicatePolicy.SKIP_IF_EXISTS);
    }

    public Cliente() {}

    public Cliente(String nombre, String tipo, String nif, String direccion,
                   String ciudad, String cp, String telefono, String email, String notas) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.nif = nif;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.cp = cp;
        this.telefono = telefono;
        this.email = email;
        this.notas = notas;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getNombreCompleto() {
        if (apellidos == null || apellidos.isBlank()) return nombre != null ? nombre : "";
        return nombre + " " + apellidos;
    }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getCp() { return cp; }
    public void setCp(String cp) { this.cp = cp; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Map<String, String> getExtras() { return extras; }
    public void setExtras(Map<String, String> extras) { this.extras = extras != null ? extras : new LinkedHashMap<>(); }
    public String getExtra(String key) { return extras.getOrDefault(key, null); }
    public void setExtra(String key, String value) { if (key != null) extras.put(key, value); }

    @Override
    public String toString() { return getNombreCompleto(); }
}
