package org.gipsybuho.model;

import java.util.Set;

public enum UserRole {

    ADMINISTRADOR("Administrador", Set.of(
        UserPermissions.DASHBOARD, UserPermissions.CLIENTES, UserPermissions.PRESUPUESTOS,
        UserPermissions.FACTURAS, UserPermissions.ALBARANES, UserPermissions.PEDIDOS,
        UserPermissions.TARIFAS, UserPermissions.MATERIALES, UserPermissions.EMPLEADOS,
        UserPermissions.NOMINAS, UserPermissions.ESTADISTICAS, UserPermissions.CALENDARIO,
        UserPermissions.IA, UserPermissions.IMPORTAR_BACKUP, UserPermissions.EXPORTAR_BACKUP,
        UserPermissions.CONFIGURACION, UserPermissions.COMPRAS
    )),
    COMERCIAL("Comercial", Set.of(
        UserPermissions.DASHBOARD, UserPermissions.CLIENTES, UserPermissions.PRESUPUESTOS,
        UserPermissions.FACTURAS, UserPermissions.ALBARANES, UserPermissions.PEDIDOS,
        UserPermissions.CALENDARIO, UserPermissions.IA, UserPermissions.CONFIGURACION
    )),
    PRODUCCION("Producción", Set.of(
        UserPermissions.DASHBOARD, UserPermissions.PEDIDOS, UserPermissions.MATERIALES,
        UserPermissions.TARIFAS, UserPermissions.CALENDARIO, UserPermissions.IA,
        UserPermissions.CONFIGURACION, UserPermissions.COMPRAS
    )),
    CONTABILIDAD("Contabilidad", Set.of(
        UserPermissions.DASHBOARD, UserPermissions.FACTURAS, UserPermissions.ALBARANES,
        UserPermissions.NOMINAS, UserPermissions.ESTADISTICAS, UserPermissions.IA,
        UserPermissions.CONFIGURACION, UserPermissions.COMPRAS
    ));

    private final String label;
    private final Set<String> defaultPermissions;

    UserRole(String label, Set<String> defaultPermissions) {
        this.label = label;
        this.defaultPermissions = Set.copyOf(defaultPermissions);
    }

    public String getLabel() { return label; }
    public Set<String> getDefaultPermissions() { return defaultPermissions; }
    public String getPermissionsString() { return String.join(",", defaultPermissions); }

    @Override
    public String toString() { return label; }
}
