package org.gipsybuho.util;

public final class AppConstants {
    private AppConstants() {
        // Evita la instanciación
    }

    // Nombres de la aplicación
    public static final String APP_NAME = "Graficas Mulberry";
    public static final String COMPANY_NAME = "GRÁFICAS MULBERRY";

    // Claves de configuración de la empresa
    public static final String CONFIG_EMPRESA_NOMBRE = "empresa_nombre";
    public static final String CONFIG_EMPRESA_NIF = "empresa_nif";
    public static final String CONFIG_EMPRESA_DIRECCION = "empresa_direccion";
    public static final String CONFIG_EMPRESA_CIUDAD = "empresa_ciudad";
    public static final String CONFIG_EMPRESA_CP = "empresa_cp";
    public static final String CONFIG_EMPRESA_TELEFONO = "empresa_telefono";
    public static final String CONFIG_EMPRESA_EMAIL = "empresa_email";
    public static final String CONFIG_EMPRESA_WEB = "empresa_web";

    // Colores en formato hexadecimal (para Word)
    public static final String COLOR_MULBERRY_HEX = "6B2D5E";
    public static final String COLOR_BLACK_HEX = "000000";
    public static final String COLOR_WHITE_HEX = "FFFFFF";
    public static final String COLOR_GRAY_HEX = "888888";
    public static final String COLOR_LIGHT_GRAY_HEX = "F5F5F5"; // Para fondos claros
    public static final String COLOR_BORDER_GRAY_HEX = "C8C8C8"; // Para bordes
    public static final String COLOR_ALT_ROW_HEX = "F5EEF4"; // Color alterno para filas de tabla
    public static final String COLOR_ALERT_ROW_HEX = "FFEBEB"; // Color para filas de alerta (ej. bajo stock)
    public static final String COLOR_DARK_GRAY_HEX = "333333"; // Para texto de cabecera de empresa

    // Etiquetas comunes
    public static final String LABEL_TEL = "Tel: ";
    public static final String LABEL_NIF = "NIF: ";
    public static final String LABEL_IVA_PORCENTAJE = "IVA%: ";
    public static final String LABEL_TOTAL = "Total: ";
    public static final String LABEL_BASE_IMPONIBLE = "Base imponible: ";
    public static final String LABEL_IVA = "IVA: ";
    public static final String LABEL_EURO_SYMBOL = " €";
    public static final String LABEL_PERCENT_SYMBOL = "%";
    public static final String LABEL_UNIDAD = "ud";

    // Mensajes de estado
    public static final String ESTADO_BORRADOR = "borrador";
    public static final String ESTADO_ENVIADO = "enviado";
    public static final String ESTADO_ACEPTADO = "aceptado";
    public static final String ESTADO_RECHAZADO = "rechazado";
    public static final String ESTADO_FACTURADO = "facturado";
    public static final String ESTADO_PENDIENTE = "pendiente";
    public static final String ESTADO_PAGADO = "pagado";
    public static final String ESTADO_ANULADO = "anulado";
    public static final String ESTADO_COMPLETADO = "completado";
    public static final String ESTADO_CANCELADO = "cancelado";
    public static final String STATUS_BAJO_MINIMO = "BAJO MÍNIMO";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ACTIVO = "ACTIVO";
    public static final String STATUS_BAJA = "BAJA";
    public static final String LABEL_SI = "Sí";
    public static final String LABEL_NO = "No";

    // Títulos de documentos
    public static final String DOC_TITLE_PRESUPUESTO = "PRESUPUESTO";
    public static final String DOC_TITLE_FACTURA = "FACTURA";
    public static final String DOC_TITLE_ALBARAN = "ALBARÁN DE ENTREGA";
    public static final String DOC_TITLE_NOMINA = "RECIBO DE NÓMINA";
    public static final String DOC_TITLE_PEDIDO = "PEDIDO DE TRABAJO";
    public static final String DOC_TITLE_FICHA_CLIENTE = "FICHA DE CLIENTE";
    public static final String DOC_TITLE_FICHA_TARIFA = "FICHA DE TARIFA";
    public static final String DOC_TITLE_FICHA_MATERIAL = "FICHA DE MATERIAL";
    public static final String DOC_TITLE_FICHA_EMPLEADO = "FICHA DE EMPLEADO";

    // Títulos de sección
    public static final String SECTION_TITLE_DATOS_PRESUPUESTO = "DATOS DEL PRESUPUESTO";
    public static final String SECTION_TITLE_DETALLE_PRESUPUESTO = "DETALLE DEL PRESUPUESTO";
    public static final String SECTION_TITLE_NOTAS = "NOTAS";
    public static final String SECTION_TITLE_CONDICIONES = "CONDICIONES";
    public static final String SECTION_TITLE_CLIENTE = "CLIENTE";
    public static final String SECTION_TITLE_DATOS_FACTURA = "DATOS DE LA FACTURA";
    public static final String SECTION_TITLE_DETALLE_FACTURA = "DETALLE DE LA FACTURA";
    public static final String SECTION_TITLE_FORMA_PAGO = "FORMA DE PAGO";
    public static final String SECTION_TITLE_DATOS_ALBARAN = "DATOS DEL ALBARÁN";
    public static final String SECTION_TITLE_ARTICULOS = "ARTÍCULOS";
    public static final String SECTION_TITLE_OBSERVACIONES = "OBSERVACIONES";
    public static final String SECTION_TITLE_DATOS_EMPLEADO = "DATOS DEL EMPLEADO";
    public static final String SECTION_TITLE_PERCEPCIONES = "PERCEPCIONES";
    public static final String SECTION_TITLE_DEDUCCIONES = "DEDUCCIONES";
    public static final String SECTION_TITLE_LIQUIDO_PERCIBIR = "LÍQUIDO A PERCIBIR";
    public static final String SECTION_TITLE_DATOS_PEDIDO = "DATOS DEL PEDIDO";
    public static final String SECTION_TITLE_DESCRIPCION_TRABAJO = "DESCRIPCIÓN DEL TRABAJO";
    public static final String SECTION_TITLE_IMPORTES = "IMPORTES";
    public static final String SECTION_TITLE_DATOS_TARIFA = "DATOS DE LA TARIFA";
    public static final String SECTION_TITLE_DATOS_MATERIAL = "DATOS DEL MATERIAL";

    // Encabezados de tabla comunes
    public static final String HEADER_NUMERO = "Número";
    public static final String HEADER_CLIENTE = "Cliente";
    public static final String HEADER_FECHA = "Fecha";
    public static final String HEADER_VALIDEZ = "Validez";
    public static final String HEADER_ESTADO = "Estado";
    public static final String HEADER_BASE = "Base";
    public static final String HEADER_IVA_PCT = "IVA%";
    public static final String HEADER_TOTAL_EUR = "Total"; // Renombrado para evitar conflicto con LABEL_TOTAL
    public static final String HEADER_APELLIDOS = "Apellidos";
    public static final String HEADER_TIPO = "Tipo";
    public static final String HEADER_NIF_CIF = "NIF/CIF";
    public static final String HEADER_TELEFONO = "Teléfono";
    public static final String HEADER_EMAIL = "Email";
    public static final String HEADER_CIUDAD = "Ciudad";
    public static final String HEADER_VENCIMIENTO = "Vencimiento";
    public static final String HEADER_FORMA_PAGO = "Forma pago";
    public static final String HEADER_FACTURA = "Factura";
    public static final String HEADER_PEDIDO = "Pedido";
    public static final String HEADER_OBSERVACIONES = "Observaciones";
    public static final String HEADER_ENTREGA_PREV = "Entrega prev.";
    public static final String HEADER_DESCRIPCION = "Descripción";
    public static final String HEADER_PENDIENTE = "Pendiente";
    public static final String HEADER_NOMBRE = "Nombre";
    public static final String HEADER_REFERENCIA = "Referencia";
    public static final String HEADER_CATEGORIA = "Categoría";
    public static final String HEADER_STOCK_ACTUAL = "Stock actual";
    public static final String HEADER_STOCK_MIN = "Stock mín.";
    public static final String HEADER_PRECIO_UD = "Precio/ud.";
    public static final String HEADER_PROVEEDOR = "Proveedor";
    public static final String HEADER_ALERTA = "Alerta";
    public static final String HEADER_SETUP = "Setup (€)";
    public static final String HEADER_MIN_UDS = "Mín. uds.";
    public static final String HEADER_ACTIVA = "Activa";
    public static final String HEADER_EMPLEADO = "Empleado";
    public static final String HEADER_PERIODO = "Período";
    public static final String HEADER_SALARIO_BASE = "Salario base";
    public static final String HEADER_BRUTO = "Bruto";
    public static final String HEADER_SS_TRAB = "SS Trab.";
    public static final String HEADER_IRPF_PCT = "IRPF%";
    public static final String HEADER_IRPF_EUR = "IRPF €";
    public static final String HEADER_NETO = "Neto";
    public static final String HEADER_COSTE_EMPRESA = "Coste empresa";
    public static final String HEADER_FECHA_ALTA = "Fecha alta";

    // SQL
    public static final String SQL_PRAGMA_FOREIGN_KEYS_OFF = "PRAGMA foreign_keys = OFF;";
    public static final String SQL_BEGIN_TRANSACTION = "BEGIN TRANSACTION;";
    public static final String SQL_COMMIT = "COMMIT;";
    public static final String SQL_PRAGMA_FOREIGN_KEYS_ON = "PRAGMA foreign_keys = ON;";
    public static final String SQL_SELECT_SQL_FROM_SQLITE_MASTER = "SELECT sql FROM sqlite_master WHERE type='table' AND name=?";
    public static final String SQL_DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    public static final String SQL_SELECT_ALL_FROM = "SELECT * FROM ";
    public static final String SQL_INSERT_INTO = "INSERT INTO ";
    public static final String SQL_VALUES = " VALUES (";
    public static final String SQL_NULL = "NULL";
    public static final String SQL_SINGLE_QUOTE = "'";
    public static final String SQL_DOUBLE_SINGLE_QUOTE = "''";
    public static final String SQL_COMMA_SPACE = ", ";
    public static final String SQL_SEMICOLON = ";";
    public static final String SQL_COUNT_ALL_FROM = "SELECT COUNT(*) FROM ";
    public static final String SQL_SELECT_1_FROM_SQLITE_MASTER = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?";

    // Nombres de tablas
    public static final String TABLE_CONFIG = "config";
    public static final String TABLE_CLIENTES = "clientes";
    public static final String TABLE_EMPLEADOS = "empleados";
    public static final String TABLE_TARIFAS = "tarifas";
    public static final String TABLE_MATERIALES = "materiales";
    public static final String TABLE_CONSUMO_MATERIAL_TECNICA = "consumo_material_tecnica";
    public static final String TABLE_MOVIMIENTOS_MATERIAL = "movimientos_material";
    public static final String TABLE_PAGOS_MATERIAL = "pagos_material";
    public static final String TABLE_PRESUPUESTOS = "presupuestos";
    public static final String TABLE_LINEAS_PRESUPUESTO = "lineas_presupuesto";
    public static final String TABLE_FACTURAS = "facturas";
    public static final String TABLE_LINEAS_FACTURA = "lineas_factura";
    public static final String TABLE_PEDIDOS = "pedidos";
    public static final String TABLE_PAGOS_PEDIDO = "pagos_pedido";
    public static final String TABLE_NOMINAS = "nominas";
    public static final String TABLE_NOTAS_CALENDARIO = "notas_calendario";
    public static final String TABLE_ALBARANES = "albaranes";
    public static final String TABLE_LINEAS_ALBARAN = "lineas_albaran";

    // Formatos
    public static final String FORMAT_CURRENCY = "%.2f" + LABEL_EURO_SYMBOL;
    public static final String FORMAT_PERCENT_0_DECIMAL = "%.0f" + LABEL_PERCENT_SYMBOL;
    public static final String FORMAT_PERCENT_1_DECIMAL = "%.1f" + LABEL_PERCENT_SYMBOL;
    public static final String FORMAT_STOCK_UNIDAD = "%.2f %s";

    // Listado de títulos
    public static final String LISTADO_CLIENTES_TITLE = "Listado de Clientes — " + APP_NAME;
    public static final String LISTADO_FACTURAS_TITLE = "Listado de Facturas — " + APP_NAME;
    public static final String LISTADO_ALBARANES_TITLE = "Listado de Albaranes — " + APP_NAME;
    public static final String LISTADO_PEDIDOS_TITLE = "Listado de Pedidos — " + APP_NAME;
    public static final String LISTADO_MATERIALES_TITLE = "Listado de Materiales — " + APP_NAME;
    public static final String LISTADO_TARIFAS_TITLE = "Listado de Tarifas — " + APP_NAME;
    public static final String LISTADO_NOMINAS_TITLE = "Listado de Nóminas — " + APP_NAME;
    public static final String LISTADO_EMPLEADOS_TITLE = "Listado de Empleados — " + APP_NAME;

    public static final String LABEL_GENERADO = "Generado: ";
    public static final String LABEL_TOTAL_COUNT_PART1 = "  ·  Total: ";
    public static final String LABEL_CLIENTE_PLURAL = " cliente(s)";
    public static final String LABEL_FACTURA_PLURAL = " factura(s)";
    public static final String LABEL_ALBARAN_PLURAL = " albarán(es)";
    public static final String LABEL_PEDIDO_PLURAL = " pedido(s)";
    public static final String LABEL_MATERIAL_PLURAL = " material(es)";
    public static final String LABEL_TARIFA_PLURAL = " tarifa(s)";
    public static final String LABEL_NOMINA_PLURAL = " nómina(s)";
    public static final String LABEL_EMPLEADO_PLURAL = " empleado(s)";

    public static final String LABEL_TEL_DOT = "Tel. ";
}
