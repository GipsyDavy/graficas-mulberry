package org.gipsybuho.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccionERP {

    public String action;
    public Data data;

    @JsonProperty("preview_summary")
    public String previewSummary;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("cliente_id")     public String clienteId;
        @JsonProperty("cliente_nombre") public String clienteNombre;
        public List<Item> items;
        @JsonProperty("total_estimado") public double totalEstimado;
        public String observaciones;
        public String fecha;

        // Campos para importar_datos_excel
        @JsonProperty("tipo_importacion")               public String tipoImportacion;
        public String proveedor;
        @JsonProperty("archivo_nombre")                 public String archivoNombre;
        @JsonProperty("hojas_a_importar")               public List<String> hojasAImportar;
        @JsonProperty("actualizar_registros_existentes") public Boolean actualizarExistentes;
        @JsonProperty("crear_nuevos_registros")          public Boolean crearNuevos;
        @JsonProperty("dry_run")                         public Boolean dryRun;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        public String descripcion;
        public int cantidad;
        public String tecnica;
        public int colores;
        public List<String> pantones;
        public String soporte;
        public double gramaje;
        @JsonProperty("precio_unitario")   public double precioUnitario;
        @JsonProperty("merma_porcentaje")  public double mermaPorcentaje;
        public String notas;
    }

    public String accionLabel() {
        if (action == null) return "Acción desconocida";
        return switch (action) {
            case "crear_presupuesto"   -> "Crear Presupuesto";
            case "generar_factura"     -> "Generar Factura";
            case "crear_albaran"       -> "Crear Albarán";
            case "crear_pedido"        -> "Crear Pedido";
            case "actualizar_stock"    -> "Actualizar Stock";
            case "crear_cliente"       -> "Crear Cliente";
            case "calcular_nomina"     -> "Calcular Nómina";
            case "generar_estadistica" -> "Generar Estadística";
            case "agendar_evento"      -> "Agendar Evento";
            case "consultar_materiales"  -> "Consultar Materiales";
            case "consultar_cliente"     -> "Consultar Cliente";
            case "exportar_backup"       -> "Exportar Backup";
            case "importar_datos_excel"  -> "Importar Datos Excel";
            default -> action;
        };
    }

    /** Color principal de la acción (para cabecera y acentos del panel). */
    public String colorHex() {
        if (action == null) return "#6B2D5E";
        return switch (action) {
            case "crear_presupuesto"   -> "#6B2D5E"; // Mulberry
            case "generar_factura"     -> "#1565C0"; // Azul
            case "crear_albaran"       -> "#E65100"; // Naranja
            case "crear_pedido"        -> "#00695C"; // Verde azulado
            case "actualizar_stock"    -> "#B71C1C"; // Rojo
            case "crear_cliente"       -> "#2E7D32"; // Verde
            case "calcular_nomina"     -> "#263238"; // Gris oscuro
            case "generar_estadistica" -> "#6A1B9A"; // Violeta
            case "agendar_evento"      -> "#00838F"; // Turquesa
            case "consultar_materiales"-> "#546E7A"; // Gris azulado
            case "consultar_cliente"   -> "#546E7A"; // Gris azulado
            case "exportar_backup"      -> "#4E342E"; // Marrón
            case "importar_datos_excel" -> "#1A6B3C"; // Verde oscuro
            default -> "#6B2D5E";
        };
    }

    /** Versión clara del color para fondos de cabecera y summary. */
    public String colorFondoHex() {
        if (action == null) return "#F5E6F0";
        return switch (action) {
            case "crear_presupuesto"   -> "#F5E6F0";
            case "generar_factura"     -> "#E3F2FD";
            case "crear_albaran"       -> "#FFF3E0";
            case "crear_pedido"        -> "#E0F2F1";
            case "actualizar_stock"    -> "#FFEBEE";
            case "crear_cliente"       -> "#E8F5E9";
            case "calcular_nomina"     -> "#ECEFF1";
            case "generar_estadistica" -> "#F3E5F5";
            case "agendar_evento"      -> "#E0F7FA";
            case "consultar_materiales"-> "#ECEFF1";
            case "consultar_cliente"   -> "#ECEFF1";
            case "exportar_backup"      -> "#EFEBE9";
            case "importar_datos_excel" -> "#E8F5E9"; // Verde claro
            default -> "#F5E6F0";
        };
    }

    public String accionIcono() {
        if (action == null) return "📋";
        return switch (action) {
            case "crear_presupuesto"   -> "📋";
            case "generar_factura"     -> "🧾";
            case "crear_albaran"       -> "📦";
            case "crear_pedido"        -> "🛒";
            case "actualizar_stock"    -> "🗃";
            case "crear_cliente"       -> "👤";
            case "calcular_nomina"     -> "💰";
            case "generar_estadistica" -> "📊";
            case "agendar_evento"      -> "📅";
            case "consultar_materiales"-> "🔍";
            case "consultar_cliente"   -> "🔍";
            case "exportar_backup"      -> "💾";
            case "importar_datos_excel" -> "📥";
            default -> "⚙";
        };
    }
}
