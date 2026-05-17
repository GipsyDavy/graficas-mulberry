package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.util.List;
import java.util.Map;

public class Empleado {
    private int id;
    private String nombre;
    private String apellidos;
    private String nif;
    private String categoria;
    private double salarioBase;
    private String fechaAlta;
    private String fechaBaja;
    private String iban;
    private double irpf;
    private boolean activo;
    private String telefono;
    private String email;
    private String direccion;

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    private static EntityImportSpec buildSpec() {
        // 11 entradas → Map.ofEntries() obligatorio (Map.of() admite máximo 10 pares)
        var syn = Map.ofEntries(
            Map.entry("nombre",       List.of("nombre", "name", "trabajador")),
            Map.entry("apellido",     List.of("apellidos", "apellido", "surname", "lastname")),
            Map.entry("nif",          List.of("nif", "dni", "nie", "cif", "documento")),
            Map.entry("categoria",    List.of("categoria", "puesto", "cargo", "category")),
            Map.entry("salario_base", List.of("salario base", "salario", "sueldo", "salary")),
            Map.entry("fecha_alta",   List.of("fecha alta", "contratacion", "inicio", "alta", "hire date")),
            Map.entry("iban",         List.of("cuenta bancaria", "iban", "cuenta", "bank account")),
            Map.entry("irpf",         List.of("retencion irpf", "retencion", "irpf", "withholding")),
            Map.entry("telefono",     List.of("telefono", "tel", "phone", "movil")),
            Map.entry("email",        List.of("correo electronico", "email", "correo", "mail")),
            Map.entry("direccion",    List.of("direccion", "domicilio", "address"))
        );
        return new EntityImportSpec("Empleados", List.of(
            new FieldSpec("nombre",       "Nombre",         true),
            new FieldSpec("apellido",     "Apellidos",      false),
            new FieldSpec("nif",          "NIF/DNI",        false),
            new FieldSpec("categoria",    "Categoría",      false),
            new FieldSpec("salario_base", "Salario base",   false),
            new FieldSpec("fecha_alta",   "Fecha de alta",  false),
            new FieldSpec("iban",         "IBAN",           false),
            new FieldSpec("irpf",         "IRPF (%)",       false),
            new FieldSpec("telefono",     "Teléfono",       false),
            new FieldSpec("email",        "Email",          false),
            new FieldSpec("direccion",    "Dirección",      false)
        ), h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn), DuplicatePolicy.SKIP_IF_EXISTS);
    }

    public Empleado() {}

    public Empleado(String nombre, String nif, String categoria, double salarioBase,
                    String fechaAlta, String iban, double irpf) {
        this.nombre = nombre;
        this.nif = nif;
        this.categoria = categoria;
        this.salarioBase = salarioBase;
        this.fechaAlta = fechaAlta;
        this.iban = iban;
        this.irpf = irpf;
        this.activo = true;
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
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }
    public String getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(String fechaAlta) { this.fechaAlta = fechaAlta; }
    public String getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(String fechaBaja) { this.fechaBaja = fechaBaja; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public double getIrpf() { return irpf; }
    public void setIrpf(double irpf) { this.irpf = irpf; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    @Override
    public String toString() { return getNombreCompleto(); }
}
