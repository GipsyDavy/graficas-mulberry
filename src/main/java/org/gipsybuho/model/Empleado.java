package org.gipsybuho.model;

public class Empleado {
    private int id;
    private String nombre;
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
    public String toString() { return nombre != null ? nombre : ""; }
}
