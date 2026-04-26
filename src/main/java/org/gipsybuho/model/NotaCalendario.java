package org.gipsybuho.model;

import java.time.LocalDate;

public class NotaCalendario {

    private int id;
    private LocalDate fecha;
    private String titulo;
    private String nota;

    public NotaCalendario() {}

    public NotaCalendario(LocalDate fecha, String titulo, String nota) {
        this.fecha = fecha;
        this.titulo = titulo;
        this.nota = nota;
    }

    public int getId()              { return id; }
    public void setId(int id)       { this.id = id; }
    public LocalDate getFecha()     { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTitulo()       { return titulo; }
    public void setTitulo(String t) { this.titulo = t; }
    public String getNota()         { return nota; }
    public void setNota(String n)   { this.nota = n; }
}
