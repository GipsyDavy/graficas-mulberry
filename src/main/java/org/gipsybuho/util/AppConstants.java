package org.gipsybuho.util;

import java.awt.Color;

public final class AppConstants {
    private AppConstants() {
        // Evita la instanciación
    }

    // --- Configuración de Conexión con Ollama (IA Local) ---
    // Define la dirección del servidor, el endpoint de generación y el modelo por defecto
    public static final String OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String OLLAMA_API_URL = OLLAMA_BASE_URL + "/api/generate";
    public static final String OLLAMA_MODEL = "llama3.2";

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

    // Colores en formato hexadecimal (para Word y JavaFX)
    public static final String COLOR_MULBERRY_HEX = "6B2D5E";
    public static final String COLOR_BLACK_HEX = "000000";
    public static final String COLOR_WHITE_HEX = "FFFFFF";
    public static final String COLOR_GRAY_HEX = "888888";
    public static final String COLOR_LIGHT_GRAY_HEX = "F5F5F5"; // Para fondos claros
    public static final String COLOR_BORDER_GRAY_HEX = "C8C8C8"; // Para bordes
    public static final String COLOR_ALT_ROW_HEX = "F5EEF4"; // Color alterno para filas de tabla
    public static final String COLOR_ALERT_ROW_HEX = "FFEBEB"; // Color para filas de alerta (ej. bajo stock)
    public static final String COLOR_DARK_GRAY_HEX = "333333"; // Para texto de cabecera de empresa
    public static final String COLOR_LIGHT_PURPLE_BG_HEX = "F0E6EF"; // Fondo claro para burbujas IA/chips
    public static final String COLOR_DARK_BLUE_TEXT_HEX = "1A1A2E"; // Color de texto oscuro general
    public static final String COLOR_MEDIUM_GRAY_HEX = "555"; // Color de texto gris medio
    public static final String COLOR_LIGHT_GRAY_BG_HEX = "FAFAFA"; // Fondo gris claro para mensajes de sistema
    public static final String COLOR_LIGHT_BLUE_BORDER_HEX = "E2E8F0"; // Borde azul claro
    public static final String COLOR_ORANGE_HEX = "E67E22"; // Color naranja para estados
    public static final String COLOR_GREEN_HEX = "27AE60"; // Color verde para estados
    public static final String COLOR_RED_HEX = "E74C3C"; // Color rojo para errores/estados
    public static final String COLOR_SUCCESS_HEX = "1A5C2A"; // Color de texto éxito
    public static final String COLOR_ERROR_HEX = "7F1D1D"; // Color de texto error
    public static final String COLOR_SUCCESS_BG_HEX = "DCFCE7"; // Fondo éxito
    public static final String COLOR_SUCCESS_BORDER_HEX = "86EFAC"; // Borde éxito
    public static final String COLOR_ERROR_BG_HEX = "FEE2E2"; // Fondo error
    public static final String COLOR_ERROR_BORDER_HEX = "FCA5A5"; // Borde error

    // Colores AWT para PDFService
    public static final Color AWT_COLOR_MULBERRY = new Color(107, 45, 94);
    public static final Color AWT_COLOR_GRIS_CLARO = new Color(245, 245, 245);
    public static final Color AWT_COLOR_GRIS_BORDE = new Color(200, 200, 200);
    public static final Color AWT_COLOR_GREEN_SUCCESS = new Color(39, 174, 96);
    public static final Color AWT_COLOR_RED_ERROR = new Color(231, 76, 60);
    public static final Color AWT_COLOR_ORANGE_WARNING = new Color(243, 156, 18);
    public static final Color AWT_COLOR_WHITE = Color.WHITE;
    public static final Color AWT_COLOR_GRAY = Color.GRAY;

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
    public static final String DOC_TITLE_INFORME_ESTADISTICAS = "INFORME DE ESTADÍSTICAS ";

    // Títulos de sección
    public static final String SECTION_TITLE_DATOS_PRESUPUESTO = "DATOS DEL PRESUPUESTO";
    public static final String SECTION_TITLE_DETALLE_PRESUPUESTO = "DETALLE DEL PRESUPUESTO";
    public static final String SECTION_TITLE_NOTAS = "Notas"; // Mantener en minúscula para PDFService
    public static final String SECTION_TITLE_CONDICIONES = "Condiciones"; // Mantener en minúscula para PDFService
    public static final String SECTION_TITLE_CLIENTE = "CLIENTE";
    public static final String SECTION_TITLE_DATOS_FACTURA = "DATOS DE LA FACTURA";
    public static final String SECTION_TITLE_DETALLE_FACTURA = "DETALLE DE LA FACTURA";
    public static final String SECTION_TITLE_FORMA_PAGO = "Forma de pago"; // Mantener en minúscula para PDFService
    public static final String SECTION_TITLE_DATOS_ALBARAN = "DATOS DEL ALBARÁN";
    public static final String SECTION_TITLE_ARTICULOS = "ARTÍCULOS";
    public static final String SECTION_TITLE_OBSERVACIONES = "Observaciones"; // Mantener en minúscula para PDFService
    public static final String SECTION_TITLE_DATOS_EMPLEADO = "DATOS DEL EMPLEADO";
    public static final String SECTION_TITLE_PERCEPCIONES = "PERCEPCIONES";
    public static final String SECTION_TITLE_DEDUCCIONES = "DEDUCCIONES";
    public static final String SECTION_TITLE_LIQUIDO_PERCIBIR = "LÍQUIDO A PERCIBIR";
    public static final String SECTION_TITLE_DATOS_PEDIDO = "DATOS DEL PEDIDO";
    public static final String SECTION_TITLE_DESCRIPCION_TRABAJO = "Descripción del trabajo"; // Mantener en minúscula para PDFService
    public static final String SECTION_TITLE_IMPORTES = "IMPORTES";
    public static final String SECTION_TITLE_DATOS_TARIFA = "DATOS DE LA TARIFA";
    public static final String SECTION_TITLE_DATOS_MATERIAL = "DATOS DEL MATERIAL";
    public static final String SECTION_TITLE_FINANCIERO = "FINANCIERO";
    public static final String SECTION_TITLE_MATERIALES = "MATERIALES";
    public static final String SECTION_TITLE_PRODUCCION = "PRODUCCIÓN";

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
    public static final String HEADER_UNIDAD_HEADER = "Unidad"; // Renombrado para evitar conflicto con LABEL_UNIDAD
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
    public static final String HEADER_MES = "Mes";
    public static final String HEADER_INGRESOS_EUR = "Ingresos €";
    public static final String HEADER_GASTOS_MAT_EUR = "Gastos mat. €";
    public static final String HEADER_NOMINAS_EUR = "Nóminas €";
    public static final String HEADER_BALANCE_EUR = "Balance €";
    public static final String HEADER_MATERIAL = "Material";
    public static final String HEADER_UDS_CONSUMIDAS = "Uds. consumidas";
    public static final String HEADER_ARTICULOS = "Artículos";
    public static final String HEADER_STOCK = "Stock";
    public static final String HEADER_FACTURACION_EUR = "Facturación €";
    public static final String HEADER_CANTIDAD = "Cantidad";
    public static final String HEADER_LINEAS = "Líneas";
    public static final String HEADER_PEDIDOS = "Pedidos";
    public static final String HEADER_TIPO_CLIENTE = "Tipo";


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

    public static final String COLOR_BG_LIGHT_BLUE_HEX = "F0F4F8"; // Fondo azul muy claro
    public static final String COLOR_DARK_BLUE_GRAY_HEX = "374151"; // Texto gris azulado oscuro
    public static final String COLOR_PURPLE_DEEP_HEX = "5D4A7A"; // Púrpura profundo para botones
    public static final String COLOR_GREEN_DARK_HEX = "2C7A3C"; // Verde oscuro para botones
    public static final String COLOR_DARK_GRAY_TEXT_HEX = "666"; // Gris oscuro para texto secundario
    
    // Títulos y etiquetas IA
    public static final String TITLE_IA_ASSISTANT = "Asistente IA Local";
    public static final String SUBTITLE_IA_ASSISTANT = "Powered by Ollama — IA 100% local, sin enviar datos a Internet";
    public static final String LABEL_MODEL = "Modelo:";
    public static final String PROMPT_INPUT_IA = "Escribe tu pregunta aquí... (Enter para enviar, Shift+Enter para nueva línea)";
    public static final String LABEL_QUICK_SUGGESTIONS = "Sugerencias rápidas:";
    public static final String BTN_CLEAN_CHAT = "🗑 Limpiar chat";
    public static final String BTN_EXPORT_CHAT = "💾 Exportar chat";
    public static final String BTN_MODELS = "⚙  Modelos";
    public static final String BTN_INSTALL_OLLAMA = "⬇  Instalar Ollama";
    public static final String BTN_SEND = "Enviar ▶";
    public static final String BTN_DATA_ERP = "📊 Datos ERP";
    public static final String TOOLTIP_CONTEXT_ERP = "Incluir datos actuales del ERP (presupuestos, facturas, pedidos…) en el contexto del asistente para respuestas más precisas.";

    // Mensajes específicos de IAView
    public static final String MSG_WELCOME_IA = "¡Hola! Soy el asistente IA de Gráficas Mulberry. Puedo ayudarte con consultas sobre presupuestos, precios, materiales, nóminas y mucho más.\n\nEscribe tu pregunta abajo para comenzar.";
    public static final String MSG_OLLAMA_READY = "🟢 Ollama listo — %s";
    public static final String MSG_OLLAMA_ERROR = "❌ Error: %s";
    public static final String MSG_OLLAMA_STARTING = "⏳ Iniciando Ollama...";
    public static final String MSG_OLLAMA_CONNECTED_DOWNLOAD_MODEL = "🟡 Ollama conectado — Descarga un modelo: ollama pull llama3.2";
    public static final String MSG_OLLAMA_INSTALLED_NOT_RESPONDING = "🟡 Ollama instalado pero no responde — reiniciando...";
    public static final String MSG_OLLAMA_NOT_INSTALLED = "🔴 Ollama no instalado";
    public static final String MSG_OLLAMA_INSTALL_INSTRUCTIONS = """
            ⚠  Ollama no está instalado en este equipo.

            Haz clic en «⬇ Instalar Ollama» en la barra superior para instalarlo \
            automáticamente. El proceso descargará el instalador oficial y el modelo \
            de IA. Solo necesitas conexión a Internet.""";
    public static final String MSG_NO_CHAT_MESSAGES = "No hay mensajes en el chat para exportar.";
    public static final String DIALOG_TITLE_EXPORT_CHAT = "Exportar chat";
    public static final String DIALOG_HEADER_EXPORT_FORMAT = "Selecciona el formato de exportación";
    public static final String DIALOG_CONTENT_EXPORT_FORMAT = "Formato:";
    public static final String FORMAT_PDF = "PDF";
    public static final String FORMAT_WORD_DOCX = "Word (.docx)";
    public static final String DIALOG_TITLE_SAVE_CHAT = "Guardar chat";
    public static final String FILE_PREFIX_CHAT_EXPORT = "chat-asistente-ia-";
    public static final String FILE_FILTER_PDF = "PDF (*.pdf)";
    public static final String FILE_FILTER_WORD = "Word (*.docx)";
    public static final String MSG_CHAT_EXPORT_SUCCESS = "Chat exportado correctamente:\n";
    public static final String MSG_ERROR_EXPORTING = "Error al exportar";
    public static final String MSG_PROCESSING_ACTION = "Procesando acción...";
    public static final String MSG_PROCESSING = "⏳ Procesando...";
    public static final String MSG_PROCESSING_ALT = "⌛ Procesando...";
    public static final String MSG_ACTION_CANCELLED = "Acción cancelada";
    public static final String MSG_USER_CANCELLED_ACTION = "El usuario canceló la acción antes de ejecutarla.";
    public static final String MSG_MODULE_NOT_SPECIFIED = "Módulo no especificado";
    public static final String MSG_MODULE_LIST = "Indica el nombre del módulo a abrir: clientes, presupuestos, facturas, albaranes, pedidos, tarifas, materiales, empleados, nominas, estadisticas, calendario.";
    public static final String MSG_MODULE_OPENED_PART1 = "Módulo «";
    public static final String MSG_MODULE_OPENED_PART2 = "» ya estaba abierto";
    public static final String MSG_WINDOW_BROUGHT_FRONT = "La ventana se ha traído al frente.";
    public static final String MSG_UNKNOWN_MODULE = "Módulo desconocido: ";
    public static final String MSG_AVAILABLE_MODULES = "Módulos disponibles: clientes · presupuestos · facturas · albaranes · pedidos · tarifas · materiales · empleados · nominas · estadisticas · calendario";
    public static final String MSG_MODULE_OPENED_SUCCESS_PART1 = "✅ Módulo «";
    public static final String MSG_MODULE_OPENED_SUCCESS_PART2 = "» abierto";
    public static final String MSG_POPUP_OPENED_SUCCESS = "Ventana emergente abierta correctamente.";
    public static final String MSG_SPECIFY_MODULE_TO_CLOSE = "Indica el nombre del módulo a cerrar.";
    public static final String MSG_MODULE_CLOSED_SUCCESS_PART1 = "✅ Módulo «";
    public static final String MSG_MODULE_CLOSED_SUCCESS_PART2 = "» cerrado";
    public static final String MSG_WINDOW_CLOSED_SUCCESS = "La ventana se ha cerrado correctamente.";
    public static final String MSG_MODULE_NOT_OPENED_PART1 = "Módulo «";
    public static final String MSG_MODULE_NOT_OPENED_PART2 = "» no estaba abierto";
    public static final String MSG_NO_OPEN_WINDOW_FOR_MODULE = "No hay ninguna ventana abierta para ese módulo.";

    // Sugerencias contextuales
    public static final String SUGGESTION_CREATE_BUDGET = "📋 Crear presupuesto";
    public static final String SUGGESTION_CREATE_BUDGET_MSG = "Crea un presupuesto con los datos anteriores";
    public static final String SUGGESTION_GENERATE_INVOICE = "🧾 Generar factura";
    public static final String SUGGESTION_GENERATE_INVOICE_MSG = "Genera una factura con esos datos";
    public static final String SUGGESTION_CREATE_ORDER = "🛒 Crear pedido";
    public static final String SUGGESTION_CREATE_ORDER_MSG = "Crea un pedido con esos datos";
    public static final String SUGGESTION_CREATE_CLIENT = "👤 Crear cliente";
    public static final String SUGGESTION_CREATE_CLIENT_MSG = "Crea ese cliente en el sistema";
    public static final String SUGGESTION_VIEW_MATERIALS = "🗃 Ver materiales";
    public static final String SUGGESTION_VIEW_MATERIALS_MSG = "¿Qué materiales están bajo stock?";
    public static final String SUGGESTION_CALCULATE_PAYROLL = "💰 Calcular nómina";
    public static final String SUGGESTION_CALCULATE_PAYROLL_MSG = "Calcula la nómina con esos datos";
    public static final String SUGGESTION_SCHEDULE_EVENT = "📅 Agendar evento";
    public static final String SUGGESTION_SCHEDULE_EVENT_MSG = "Agéndalo en el calendario";
    public static final String SUGGESTION_PROMPT = "¿Quieres que lo haga ahora?";
}
