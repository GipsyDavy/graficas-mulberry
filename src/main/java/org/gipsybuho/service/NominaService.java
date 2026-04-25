package org.gipsybuho.service;

import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Nomina;

public class NominaService {

    // Porcentajes SS 2024 (España)
    public static final double SS_TRABAJADOR_CONTINGENCIAS = 4.70;
    public static final double SS_TRABAJADOR_DESEMPLEO = 1.55;
    public static final double SS_TRABAJADOR_FORMACION = 0.10;
    public static final double SS_TRABAJADOR_TOTAL = SS_TRABAJADOR_CONTINGENCIAS + SS_TRABAJADOR_DESEMPLEO + SS_TRABAJADOR_FORMACION;

    public static final double SS_EMPRESA_CONTINGENCIAS = 23.60;
    public static final double SS_EMPRESA_DESEMPLEO = 5.50;
    public static final double SS_EMPRESA_FOGASA = 0.20;
    public static final double SS_EMPRESA_FORMACION = 0.60;
    public static final double SS_EMPRESA_TOTAL = SS_EMPRESA_CONTINGENCIAS + SS_EMPRESA_DESEMPLEO + SS_EMPRESA_FOGASA + SS_EMPRESA_FORMACION;

    public Nomina calcular(Empleado empleado, int mes, int anio,
                           double complementos,
                           int horasExtraNormales, double precioHoraExtra,
                           int horasExtraFestivas, double precioHoraFestiva,
                           double percepcionesNoSalariales) {
        Nomina n = new Nomina();
        n.setEmpleadoId(empleado.getId());
        n.setEmpleadoNombre(empleado.getNombreCompleto());
        n.setMes(mes);
        n.setAnio(anio);
        n.setSalarioBase(empleado.getSalarioBase());
        n.setComplementos(complementos);
        n.setHorasExtraNormales(horasExtraNormales);
        n.setPrecioHoraExtra(precioHoraExtra);
        n.setHorasExtraFestivas(horasExtraFestivas);
        n.setPrecioHoraFestiva(precioHoraFestiva);
        n.setPercepcionesNoSalariales(percepcionesNoSalariales);
        n.setIrpfPorcentaje(empleado.getIrpf());

        recalcular(n);
        return n;
    }

    public void recalcular(Nomina n) {
        double importeHorasNormales = n.getHorasExtraNormales() * n.getPrecioHoraExtra();
        double importeHorasFestivas = n.getHorasExtraFestivas() * n.getPrecioHoraFestiva();

        double salarioSujeto = n.getSalarioBase() + n.getComplementos() + importeHorasNormales + importeHorasFestivas;
        double totalBruto = salarioSujeto + n.getPercepcionesNoSalariales();
        n.setTotalBruto(redondear(totalBruto));

        // Deducciones sobre base salarial (sin percepciones no salariales para SS)
        double baseSS = salarioSujeto;
        double ssTrabajador = baseSS * (SS_TRABAJADOR_TOTAL / 100.0);
        n.setSsTrabajador(redondear(ssTrabajador));

        double irpf = salarioSujeto * (n.getIrpfPorcentaje() / 100.0);
        n.setIrpfImporte(redondear(irpf));

        double totalDeducciones = ssTrabajador + irpf;
        n.setTotalDeducciones(redondear(totalDeducciones));

        double neto = totalBruto - totalDeducciones;
        n.setNeto(redondear(neto));

        // Coste empresa
        double ssEmpresa = baseSS * (SS_EMPRESA_TOTAL / 100.0);
        n.setSsEmpresa(redondear(ssEmpresa));
        n.setCosteTotalEmpresa(redondear(salarioSujeto + ssEmpresa));
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public String resumenDesglose(Nomina n) {
        return String.format("""
            PERCEPCIONES SALARIALES
            ─────────────────────────────────────
            Salario base:                 %8.2f €
            Complementos:                 %8.2f €
            Horas extra normales (%d):    %8.2f €
            Horas extra festivas (%d):    %8.2f €
            Percepciones no salariales:   %8.2f €
            ─────────────────────────────────────
            TOTAL DEVENGADO:              %8.2f €

            DEDUCCIONES
            ─────────────────────────────────────
            S.S. trabajador (%.2f%%):     %8.2f €
            IRPF (%.2f%%):                %8.2f €
            ─────────────────────────────────────
            TOTAL DEDUCCIONES:            %8.2f €

            ═════════════════════════════════════
            LÍQUIDO A PERCIBIR:           %8.2f €
            ═════════════════════════════════════

            COSTE EMPRESA
            ─────────────────────────────────────
            S.S. empresa (%.2f%%):        %8.2f €
            COSTE TOTAL EMPRESA:          %8.2f €
            """,
            n.getSalarioBase(), n.getComplementos(),
            n.getHorasExtraNormales(), n.getHorasExtraNormales() * n.getPrecioHoraExtra(),
            n.getHorasExtraFestivas(), n.getHorasExtraFestivas() * n.getPrecioHoraFestiva(),
            n.getPercepcionesNoSalariales(),
            n.getTotalBruto(),
            SS_TRABAJADOR_TOTAL, n.getSsTrabajador(),
            n.getIrpfPorcentaje(), n.getIrpfImporte(),
            n.getTotalDeducciones(),
            n.getNeto(),
            SS_EMPRESA_TOTAL, n.getSsEmpresa(),
            n.getCosteTotalEmpresa()
        );
    }
}
