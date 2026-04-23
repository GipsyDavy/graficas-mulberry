package org.gipsybuho.model;

public class Nomina {
    private int id;
    private int empleadoId;
    private String empleadoNombre;
    private int mes;
    private int anio;
    private double salarioBase;
    private double complementos;
    private int horasExtraNormales;
    private double precioHoraExtra;
    private int horasExtraFestivas;
    private double precioHoraFestiva;
    private double percepcionesNoSalariales;
    private double totalBruto;
    private double irpfPorcentaje;
    private double irpfImporte;
    private double ssTrabajador;
    private double totalDeducciones;
    private double neto;
    private double ssEmpresa;
    private double costeTotalEmpresa;
    private String createdAt;

    public static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    public Nomina() {}

    public String getPeriodo() {
        if (mes >= 1 && mes <= 12) return MESES[mes - 1] + " " + anio;
        return mes + "/" + anio;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    public String getEmpleadoNombre() { return empleadoNombre; }
    public void setEmpleadoNombre(String empleadoNombre) { this.empleadoNombre = empleadoNombre; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }
    public double getComplementos() { return complementos; }
    public void setComplementos(double complementos) { this.complementos = complementos; }
    public int getHorasExtraNormales() { return horasExtraNormales; }
    public void setHorasExtraNormales(int horasExtraNormales) { this.horasExtraNormales = horasExtraNormales; }
    public double getPrecioHoraExtra() { return precioHoraExtra; }
    public void setPrecioHoraExtra(double precioHoraExtra) { this.precioHoraExtra = precioHoraExtra; }
    public int getHorasExtraFestivas() { return horasExtraFestivas; }
    public void setHorasExtraFestivas(int horasExtraFestivas) { this.horasExtraFestivas = horasExtraFestivas; }
    public double getPrecioHoraFestiva() { return precioHoraFestiva; }
    public void setPrecioHoraFestiva(double precioHoraFestiva) { this.precioHoraFestiva = precioHoraFestiva; }
    public double getPercepcionesNoSalariales() { return percepcionesNoSalariales; }
    public void setPercepcionesNoSalariales(double percepcionesNoSalariales) { this.percepcionesNoSalariales = percepcionesNoSalariales; }
    public double getTotalBruto() { return totalBruto; }
    public void setTotalBruto(double totalBruto) { this.totalBruto = totalBruto; }
    public double getIrpfPorcentaje() { return irpfPorcentaje; }
    public void setIrpfPorcentaje(double irpfPorcentaje) { this.irpfPorcentaje = irpfPorcentaje; }
    public double getIrpfImporte() { return irpfImporte; }
    public void setIrpfImporte(double irpfImporte) { this.irpfImporte = irpfImporte; }
    public double getSsTrabajador() { return ssTrabajador; }
    public void setSsTrabajador(double ssTrabajador) { this.ssTrabajador = ssTrabajador; }
    public double getTotalDeducciones() { return totalDeducciones; }
    public void setTotalDeducciones(double totalDeducciones) { this.totalDeducciones = totalDeducciones; }
    public double getNeto() { return neto; }
    public void setNeto(double neto) { this.neto = neto; }
    public double getSsEmpresa() { return ssEmpresa; }
    public void setSsEmpresa(double ssEmpresa) { this.ssEmpresa = ssEmpresa; }
    public double getCosteTotalEmpresa() { return costeTotalEmpresa; }
    public void setCosteTotalEmpresa(double costeTotalEmpresa) { this.costeTotalEmpresa = costeTotalEmpresa; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
