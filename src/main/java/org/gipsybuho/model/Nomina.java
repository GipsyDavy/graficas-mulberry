package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.util.List;
import java.util.Map;

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

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    public static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    private static EntityImportSpec buildSpec() {
        // empleado_nombre y empleado_apellidos son campos virtuales del CSV:
        // no existen en Nomina, se usan para resolver empleadoId antes de guardar.
        var syn = Map.ofEntries(
            Map.entry("empleado_nombre", List.of("empleado nombre", "nombre empleado", "trabajador", "nombre trabajador", "empleado", "employee name")),
            Map.entry("empleado_apellidos", List.of("empleado apellidos", "apellidos empleado", "apellidos trabajador", "apellido empleado", "surname", "lastname")),
            Map.entry("mes", List.of("mes", "month", "periodo mes")),
            Map.entry("anio", List.of("anio", "año", "year", "ejercicio")),
            Map.entry("salario_base", List.of("salario base", "salario", "sueldo base", "base")),
            Map.entry("complementos", List.of("complementos", "plus", "pluses", "bonus")),
            Map.entry("horas_extra_normales", List.of("horas extra normales", "horas extra", "h extra normales", "horas normales extra")),
            Map.entry("precio_hora_extra", List.of("precio hora extra", "importe hora extra", "valor hora extra", "precio h extra")),
            Map.entry("horas_extra_festivas", List.of("horas extra festivas", "h extra festivas", "horas festivas")),
            Map.entry("precio_hora_festiva", List.of("precio hora festiva", "importe hora festiva", "valor hora festiva", "precio h festiva")),
            Map.entry("percepciones_no_salariales", List.of("percepciones no salariales", "no salarial", "dietas", "indemnizaciones")),
            Map.entry("total_bruto", List.of("total bruto", "bruto", "devengos", "total devengado")),
            Map.entry("irpf_porcentaje", List.of("irpf porcentaje", "irpf %", "porcentaje irpf", "retencion irpf")),
            Map.entry("irpf_importe", List.of("irpf importe", "importe irpf", "retencion", "retencion importe")),
            Map.entry("ss_trabajador", List.of("ss trabajador", "seguridad social trabajador", "cotizacion trabajador", "ss empleado")),
            Map.entry("total_deducciones", List.of("total deducciones", "deducciones", "descuentos")),
            Map.entry("neto", List.of("neto", "liquido", "liquido neto", "a percibir")),
            Map.entry("ss_empresa", List.of("ss empresa", "seguridad social empresa", "cotizacion empresa")),
            Map.entry("coste_total_empresa", List.of("coste total empresa", "coste empresa", "total coste empresa", "coste total"))
        );
        return new EntityImportSpec("Nominas", List.of(
            new FieldSpec("empleado_nombre", "Nombre del empleado", true),
            new FieldSpec("empleado_apellidos", "Apellidos del empleado", false),
            new FieldSpec("mes", "Mes", true),
            new FieldSpec("anio", "Año", true),
            new FieldSpec("salario_base", "Salario base", false),
            new FieldSpec("complementos", "Complementos", false),
            new FieldSpec("horas_extra_normales", "Horas extra normales", false),
            new FieldSpec("precio_hora_extra", "Precio hora extra", false),
            new FieldSpec("horas_extra_festivas", "Horas extra festivas", false),
            new FieldSpec("precio_hora_festiva", "Precio hora festiva", false),
            new FieldSpec("percepciones_no_salariales", "Percepciones no salariales", false),
            new FieldSpec("total_bruto", "Total bruto", false),
            new FieldSpec("irpf_porcentaje", "IRPF (%)", false),
            new FieldSpec("irpf_importe", "IRPF importe", false),
            new FieldSpec("ss_trabajador", "SS trabajador", false),
            new FieldSpec("total_deducciones", "Total deducciones", false),
            new FieldSpec("neto", "Neto", false),
            new FieldSpec("ss_empresa", "SS empresa", false),
            new FieldSpec("coste_total_empresa", "Coste total empresa", false)
        ), h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn), DuplicatePolicy.SKIP_IF_EXISTS);
    }

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
