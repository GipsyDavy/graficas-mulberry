package org.gipsybuho.util;

import java.awt.Color;

/**
 * AppConstants: Centraliza todas las constantes del sistema ERP y el Asistente IA.
 * VERSIÓN FINAL: Fusión completa garantizada.
 */
public final class AppConstants {
    private AppConstants() {
        // Evita la instanciación
    }

    // --- Configuración de Conexión con Ollama (IA Local) ---
    public static final String OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String OLLAMA_API_URL = OLLAMA_BASE_URL + "/api/generate";
    public static final String OLLAMA_MODEL = "llama3.2";

    // --- Nombres de la aplicación ---
    public static final String APP_NAME = "Graficas Mulberry";
    public static final String COMPANY_NAME = "GRÁFICAS MULBERRY";

    // --- Claves de configuración de la empresa (Originales) ---
    public static final String CONFIG_EMPRESA_NOMBRE = "empresa_nombre";
    public static final String CONFIG_EMPRESA_NIF = "empresa_nif";
    public static final String CONFIG_EMPRESA_DIRECCION = "empresa_direccion";
    public static final String CONFIG_EMPRESA_CIUDAD = "empresa_ciudad";
    public static final String CONFIG_EMPRESA_CP = "empresa_cp";
    public static final String CONFIG_EMPRESA_TELEFONO = "empresa_telefono";
    public static final String CONFIG_EMPRESA_EMAIL = "empresa_email";
    public static final String CONFIG_EMPRESA_WEB = "empresa_web";

    // --- Colores Hexadecimales (JavaFX) ---
    public static final String COLOR_MULBERRY_HEX = "6B2D5E";
    public static final String COLOR_BLACK_HEX = "000000";
    public static final String COLOR_WHITE_HEX = "FFFFFF";
    public static final String COLOR_GRAY_HEX = "888888";
    public static final String COLOR_LIGHT_GRAY_HEX = "F5F5F5";
    public static final String COLOR_BORDER_GRAY_HEX = "C8C8C8";
    public static final String COLOR_ALT_ROW_HEX = "F5EEF4";
    public static final String COLOR_ALERT_ROW_HEX = "FFEBEB";
    public static final String COLOR_DARK_GRAY_HEX = "333333";
    public static final String COLOR_LIGHT_PURPLE_BG_HEX = "F0E6EF";
    public static final String COLOR_DARK_BLUE_TEXT_HEX = "1A1A2E";
    public static final String COLOR_MEDIUM_GRAY_HEX = "555";
    public static final String COLOR_LIGHT_GRAY_BG_HEX = "FAFAFA";
    public static final String COLOR_LIGHT_BLUE_BORDER_HEX = "E2E8F0";
    public static final String COLOR_ORANGE_HEX = "E67E22";
    public static final String COLOR_GREEN_HEX = "27AE60";
    public static final String COLOR_RED_HEX = "E74C3C";
    public static final String COLOR_SUCCESS_HEX = "1A5C2A";
    public static final String COLOR_ERROR_HEX = "7F1D1D";
    public static final String COLOR_SUCCESS_BG_HEX = "DCFCE7";
    public static final String COLOR_SUCCESS_BORDER_HEX = "86EFAC";
    public static final String COLOR_ERROR_BG_HEX = "FEE2E2";
    public static final String COLOR_ERROR_BORDER_HEX = "FCA5A5";
    public static final String COLOR_BG_LIGHT_BLUE_HEX = "F0F4F8";
    public static final String COLOR_DARK_BLUE_GRAY_HEX = "374151";

    // --- Colores AWT (Originales para PDFs) ---
    public static final Color AWT_COLOR_MULBERRY = new Color(107, 45, 94);
    public static final Color AWT_COLOR_GRIS_CLARO = new Color(245, 245, 245);
    public static final Color AWT_COLOR_GRIS_BORDE = new Color(200, 200, 200);
    public static final Color AWT_COLOR_GREEN_SUCCESS = new Color(39, 174, 96);
    public static final Color AWT_COLOR_RED_ERROR = new Color(231, 76, 60);
    public static final Color AWT_COLOR_ORANGE_WARNING = new Color(243, 156, 18);
    public static final Color AWT_COLOR_WHITE = Color.WHITE;
    public static final Color AWT_COLOR_GRAY = Color.GRAY;

    // --- Etiquetas comunes (Originales) ---
    public static final String LABEL_TEL = "Tel: ";
    public static final String LABEL_NIF = "NIF: ";
    public static final String LABEL_IVA_PORCENTAJE = "IVA%: ";
    public static final String LABEL_TOTAL = "Total: ";
    public static final String LABEL_BASE_IMPONIBLE = "Base imponible: ";
    public static final String LABEL_IVA = "IVA: ";
    public static final String LABEL_EURO_SYMBOL = " €";
    public static final String LABEL_PERCENT_SYMBOL = "%";
    public static final String LABEL_UNIDAD = "ud";

    // --- Estados de documentos (Originales) ---
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

    // --- Títulos de documentos (Originales) ---
    public static final String DOC_TITLE_PRESUPUESTO = "PRESUPUESTO";
    public static final String DOC_TITLE_FACTURA = "FACTURA";
    public static final String DOC_TITLE_ALBARAN = "ALBARÁN DE ENTREGA";

    // --- Formatos (Originales) ---
    public static final String FORMAT_CURRENCY = "%.2f" + LABEL_EURO_SYMBOL;

    // --- Constantes de Interfaz IA (Nuevas) ---
    public static final String TITLE_IA_ASSISTANT = "Asistente IA Local";
    public static final String SUBTITLE_IA_ASSISTANT = "Powered by Ollama — IA 100% local, sin enviar datos a Internet";
    public static final String TEXT_IA_TITULO = TITLE_IA_ASSISTANT;
    public static final String TEXT_IA_SUBTITULO = SUBTITLE_IA_ASSISTANT;
    public static final String TEXT_PROMPT_INPUT = "Escribe aquí... (Shift+Enter para nueva línea)";
    public static final String TEXT_PROMPT_MODELO = "Modelo";
    public static final String TEXT_BTN_ENVIAR = "Enviar ▶";
    public static final String TEXT_BTN_LIMPIAR = "🗑 Limpiar";
    public static final String TEXT_BTN_EXPORTAR = "💾 Exportar";
    public static final String TEXT_BTN_GESTION_MODELOS = "⚙  Modelos";
    public static final String TEXT_BTN_INSTALAR_OLLAMA = "⬇  Instalar Ollama";
    public static final String TEXT_CB_CONTEXTO_ERP = "📊 Datos ERP";
    public static final String TEXT_ESTADO_VERIFICANDO = "⏳ Verificando conexión...";
    public static final String TEXT_ESTADO_CONECTADO = "🟢 Ollama Conectado";
    public static final String TEXT_ESTADO_DESCONECTADO = "🔴 Ollama no encontrado";

    // --- Mensajes de Chat IA (Nuevas) ---
    public static final String MSG_BIENVENIDA_IA = "¡Hola! Soy el asistente de Gráficas Mulberry. ¿En qué puedo ayudarte hoy?";
    public static final String MSG_CHAT_REINICIADO = "Chat reiniciado.";
    public static final String MSG_ERROR_PREFIX = "Error: ";

    // --- Estilos CSS IA (Nuevas) ---
    public static final String STYLE_ESTADO_BAR = "-fx-background-color:#F0F4F8; -fx-background-radius:8;";
    public static final String STYLE_CHAT_SCROLL = "-fx-background-color: white; -fx-border-color:#DDD;";
    public static final String STYLE_CHIP_SUGERENCIA = "-fx-font-size:10; -fx-background-radius:15;";
    public static final String STYLE_BURBUJA_USUARIO = "-fx-background-radius:15 15 2 15;";
    public static final String STYLE_BURBUJA_IA = "-fx-background-color:#F1F1F1; -fx-background-radius:15 15 15 2;";
    public static final String STYLE_MSG_SISTEMA = "-fx-text-fill:#777; -fx-font-style:italic;";

    // --- Exportación IA (Nuevas) ---
    public static final String TEXT_TITULO_EXPORTAR = "Guardar Chat";
    public static final String TEXT_FILTER_PDF = "Documento PDF";
    public static final String TEXT_FILTER_WORD = "Documento Word";
    public static final String TEXT_ERROR_EXPORTAR_TITULO = "Error al exportar";
    public static final String TEXT_ERROR_EXPORTAR_HEADER = "No se pudo guardar el archivo";
    public static final String TEXT_ERROR_EXPORTAR_CONTENT = "Asegúrate de que el archivo no esté abierto en otro programa.";

    // --- Sugerencias Contextuales (INTEGRAL: Originales + IA) ---
    public static final String[] SUGERENCIAS_IA = {"Precios de serigrafía", "¿Stock de papel?", "Estado de facturas"};
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

    // --- Miscelánea y Debug (Originales + IA) ---
    public static final String MSG_NO_OPEN_WINDOW_FOR_MODULE = "No hay ninguna ventana abierta para ese módulo.";
    public static final String DEBUG_GESTION_MODELOS = "DEBUG: Solicitada apertura de gestión de modelos.";
    public static final String DEBUG_REDIR_OLLAMA = "Redirigiendo a la descarga de Ollama...";
}