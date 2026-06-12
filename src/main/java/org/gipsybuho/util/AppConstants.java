package org.gipsybuho.util;

import java.awt.Color;

/**
 * AppConstants: Centraliza todas las constantes del sistema ERP y el Asistente IA.
 */
public final class AppConstants {
    private AppConstants() {
        // Evita la instanciación de una clase de utilidades
    }

    // =========================================================================
    // 1. CONFIGURACIÓN TÉCNICA IA (API OLLAMA)
    // =========================================================================
    public static final String OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String OLLAMA_API_URL  = OLLAMA_BASE_URL + "/api/generate";
    public static final String OLLAMA_MODEL    = "llama3.2";
    public static final int OLLAMA_TIMEOUT_SEC = 30; // Vital para evitar bloqueos de red

    /** Prompt de sistema: Define la identidad y el conocimiento del asistente */
    public static final String SYSTEM_PROMPT_IA = """
        Eres Mulberry Assistant, el asistente experto del ERP Gráficas Mulberry.
        Tu función es ayudar con presupuestos, facturas, clientes y gestión de stock.
        Responde de forma profesional, clara y en español.
        Si el usuario te pregunta por datos del ERP, utiliza el contexto proporcionado.
        """;

    // =========================================================================
    // 2. NOMBRES DE LA APLICACIÓN Y CONFIGURACIÓN DE EMPRESA (ORIGINALES)
    // =========================================================================
    public static final String APP_NAME     = "Graficas Mulberry";
    public static final String APP_VERSION  = "v13.5.0";
    public static final String COMPANY_NAME = "GRÁFICAS MULBERRY";

    public static final String CONFIG_EMPRESA_NOMBRE    = "empresa_nombre";
    public static final String CONFIG_EMPRESA_NIF       = "empresa_nif";
    public static final String CONFIG_EMPRESA_DIRECCION = "empresa_direccion";
    public static final String CONFIG_EMPRESA_CIUDAD    = "empresa_ciudad";
    public static final String CONFIG_EMPRESA_CP        = "empresa_cp";
    public static final String CONFIG_EMPRESA_TELEFONO  = "empresa_telefono";
    public static final String CONFIG_EMPRESA_EMAIL     = "empresa_email";
    public static final String CONFIG_EMPRESA_WEB       = "empresa_web";

    // =========================================================================
    // 3. PALETA DE COLORES (JAVAFX HEXADECIMAL - ORIGINALES)
    // =========================================================================
    public static final String COLOR_MULBERRY_HEX          = "6B2D5E";
    public static final String COLOR_BLACK_HEX             = "000000";
    public static final String COLOR_WHITE_HEX             = "FFFFFF";
    public static final String COLOR_GRAY_HEX              = "888888";
    public static final String COLOR_LIGHT_GRAY_HEX        = "F5F5F5";
    public static final String COLOR_BORDER_GRAY_HEX       = "C8C8C8";
    public static final String COLOR_ALT_ROW_HEX           = "F5EEF4";
    public static final String COLOR_ALERT_ROW_HEX         = "FFEBEB";
    public static final String COLOR_DARK_GRAY_HEX         = "333333";
    public static final String COLOR_LIGHT_PURPLE_BG_HEX   = "F0E6EF";
    public static final String COLOR_DARK_BLUE_TEXT_HEX    = "1A1A2E";
    public static final String COLOR_SUCCESS_HEX           = "1A5C2A";
    public static final String COLOR_ERROR_HEX             = "7F1D1D";
    public static final String COLOR_BG_LIGHT_BLUE_HEX      = "F0F4F8";
    public static final String COLOR_DARK_BLUE_GRAY_HEX    = "374151";

    // =========================================================================
    // 4. COLORES AWT (ORIGINALES PARA PDFS)
    // =========================================================================
    public static final Color AWT_COLOR_MULBERRY      = new Color(107, 45, 94);
    public static final Color AWT_COLOR_GRIS_CLARO    = new Color(245, 245, 245);
    public static final Color AWT_COLOR_GRIS_BORDE    = new Color(200, 200, 200);
    public static final Color AWT_COLOR_GREEN_SUCCESS = new Color(39, 174, 96);
    public static final Color AWT_COLOR_RED_ERROR     = new Color(231, 76, 60);
    public static final Color AWT_COLOR_ORANGE_WARNING = new Color(243, 156, 18);
    public static final Color AWT_COLOR_WHITE         = Color.WHITE;
    public static final Color AWT_COLOR_GRAY          = Color.GRAY;

    // =========================================================================
    // 5. ETIQUETAS, ESTADOS Y TÍTULOS DE DOCUMENTO (ORIGINALES)
    // =========================================================================
    public static final String LABEL_TEL            = "Tel: ";
    public static final String LABEL_NIF            = "NIF: ";
    public static final String LABEL_IVA_PORCENTAJE = "IVA%: ";
    public static final String LABEL_TOTAL          = "Total: ";
    public static final String LABEL_BASE_IMPONIBLE = "Base imponible: ";
    public static final String LABEL_IVA            = "IVA: ";
    public static final String LABEL_EURO_SYMBOL    = " €";
    public static final String LABEL_PERCENT_SYMBOL = "%";
    public static final String LABEL_UNIDAD         = "ud";

    public static final String ESTADO_BORRADOR  = "borrador";
    public static final String ESTADO_ENVIADO   = "enviado";
    public static final String ESTADO_ACEPTADO  = "aceptado";
    public static final String ESTADO_RECHAZADO = "rechazado";
    public static final String ESTADO_FACTURADO = "facturado";
    public static final String ESTADO_PAGADO    = "pagado";
    public static final String ESTADO_ANULADO   = "anulado";

    public static final String DOC_TITLE_PRESUPUESTO = "PRESUPUESTO";
    public static final String DOC_TITLE_FACTURA     = "FACTURA";
    public static final String DOC_TITLE_ALBARAN     = "ALBARÁN DE ENTREGA";

    public static final String FORMAT_CURRENCY = "%.2f" + LABEL_EURO_SYMBOL;
    public static final String DATE_FORMAT_ES  = "dd/MM/yyyy";

    // =========================================================================
    // 6. CONSTANTES DE INTERFAZ IA (UI)
    // =========================================================================
    public static final String TITLE_IA_ASSISTANT      = "Asistente IA Local";
    public static final String SUBTITLE_IA_ASSISTANT   = "Powered by Ollama — IA 100% local, privada y segura";
    public static final String TEXT_PROMPT_INPUT       = "Escribe aquí... (Shift+Enter para nueva línea)";
    public static final String TEXT_BTN_ENVIAR         = "Enviar ▶";
    public static final String TEXT_BTN_LIMPIAR        = "🗑 Limpiar";
    public static final String TEXT_BTN_EXPORTAR       = "💾 Exportar";
    public static final String TEXT_BTN_GESTION_MODELOS = "⚙  Modelos";

    public static final String TEXT_ESTADO_CONECTADO    = "🟢 Ollama Conectado";
    public static final String TEXT_ESTADO_DESCONECTADO = "🔴 Ollama no encontrado";
    public static final String TEXT_ESTADO_VERIFICANDO  = "⏳ Verificando conexión...";

    // =========================================================================
    // 7. MENSAJES Y ESTILOS CSS IA (MEJORADOS)
    // =========================================================================
    public static final String MSG_BIENVENIDA_IA   = "¡Hola! Soy el asistente de Gráficas Mulberry. ¿En qué puedo ayudarte hoy?";
    public static final String MSG_ERROR_PREFIX    = "Error: ";
    public static final String MSG_CHAT_REINICIADO = "Historial de chat limpiado.";

    public static final String STYLE_BURBUJA_USUARIO = "-fx-background-color:#0078D7; -fx-background-radius:15 15 2 15; -fx-text-fill:white;";
    public static final String STYLE_BURBUJA_IA      = "-fx-background-color:#F1F1F1; -fx-background-radius:15 15 15 2;";
    public static final String STYLE_MSG_SISTEMA     = "-fx-text-fill:#777; -fx-font-style:italic; -fx-font-size:11px;";
    public static final String STYLE_ESTADO_BAR      = "-fx-background-color:#F0F4F8; -fx-background-radius:8;";

    // =========================================================================
    // 8. LÓGICA DE BASE DE DATOS Y SQL (PIEZA FALTA ANTES)
    // =========================================================================
    public static final String TABLE_CLIENTES     = "clientes";
    public static final String TABLE_PRESUPUESTOS = "presupuestos";
    public static final String TABLE_FACTURAS     = "facturas";
    public static final String TABLE_MATERIALES   = "materiales";

    // =========================================================================
    // 9. SUGERENCIAS CONTEXTUALES (INTEGRAL)
    // =========================================================================
    public static final String SUGGESTION_CREATE_BUDGET      = "📋 Crear presupuesto";
    public static final String SUGGESTION_CREATE_BUDGET_MSG  = "Ayúdame a redactar un presupuesto para un cliente";
    public static final String SUGGESTION_GENERATE_INVOICE   = "🧾 Generar factura";
    public static final String SUGGESTION_GENERATE_INVOICE_MSG = "Genera la factura de este presupuesto";
    public static final String SUGGESTION_VIEW_MATERIALS     = "🗃 Ver materiales";
    public static final String SUGGESTION_VIEW_MATERIALS_MSG = "Muestra los materiales con bajo stock";

    // =========================================================================
    // 10. EXPORTACIÓN Y DEBUG (ORIGINALES + IA)
    // =========================================================================
    public static final String TEXT_TITULO_EXPORTAR       = "Guardar historial de chat";
    public static final String TEXT_ERROR_EXPORTAR_TITULO = "Error al exportar";
    public static final String MSG_NO_OPEN_WINDOW_MODULE  = "Error: No hay una ventana activa para este módulo.";
}