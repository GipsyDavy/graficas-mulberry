package org.gipsybuho.model;

public class TarifaTramo {
    private int id;
    private int tarifaId;
    private int tiempoMinutos;
    private double precioTiempo;

    public TarifaTramo() {}

    public TarifaTramo(int tarifaId, int tiempoMinutos, double precioTiempo) {
        this.tarifaId = tarifaId;
        this.tiempoMinutos = tiempoMinutos;
        this.precioTiempo = precioTiempo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTarifaId() { return tarifaId; }
    public void setTarifaId(int tarifaId) { this.tarifaId = tarifaId; }
    public int getTiempoMinutos() { return tiempoMinutos; }
    public void setTiempoMinutos(int tiempoMinutos) { this.tiempoMinutos = tiempoMinutos; }
    public double getPrecioTiempo() { return precioTiempo; }
    public void setPrecioTiempo(double precioTiempo) { this.precioTiempo = precioTiempo; }
}
