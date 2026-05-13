package org.gipsybuho.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UserPermissions {

    public static final String DASHBOARD = "dashboard";
    public static final String CLIENTES = "clientes";
    public static final String PRESUPUESTOS = "presupuestos";
    public static final String FACTURAS = "facturas";
    public static final String ALBARANES = "albaranes";
    public static final String PEDIDOS = "pedidos";
    public static final String TARIFAS = "tarifas";
    public static final String MATERIALES = "materiales";
    public static final String EMPLEADOS = "empleados";
    public static final String NOMINAS = "nominas";
    public static final String ESTADISTICAS = "estadisticas";
    public static final String CALENDARIO = "calendario";
    public static final String IA = "ia";
    public static final String IMPORTAR_BACKUP = "importar_backup";
    public static final String EXPORTAR_BACKUP = "exportar_backup";
    public static final String CONFIGURACION = "configuracion";

    public static final Map<String, String> AVAILABLE = new LinkedHashMap<>();

    static {
        AVAILABLE.put(DASHBOARD, "Panel principal");
        AVAILABLE.put(CLIENTES, "Clientes");
        AVAILABLE.put(PRESUPUESTOS, "Presupuestos");
        AVAILABLE.put(FACTURAS, "Facturas");
        AVAILABLE.put(ALBARANES, "Albaranes");
        AVAILABLE.put(PEDIDOS, "Pedidos");
        AVAILABLE.put(TARIFAS, "Tarifas");
        AVAILABLE.put(MATERIALES, "Materiales");
        AVAILABLE.put(EMPLEADOS, "Empleados");
        AVAILABLE.put(NOMINAS, "Nóminas");
        AVAILABLE.put(ESTADISTICAS, "Estadísticas");
        AVAILABLE.put(CALENDARIO, "Calendario");
        AVAILABLE.put(IA, "Asistente IA");
        AVAILABLE.put(IMPORTAR_BACKUP, "Importar Backup");
        AVAILABLE.put(EXPORTAR_BACKUP, "Exportar / Backup");
        AVAILABLE.put(CONFIGURACION, "Configuración");
    }

    private UserPermissions() {
    }
}
