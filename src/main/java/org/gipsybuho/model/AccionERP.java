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
        // ── Campos originales ──────────────────────────────────────────────
        @JsonProperty("cliente_id")     public String clienteId;
        @JsonProperty("cliente_nombre") public String clienteNombre;
        public List<Item> items;
        @JsonProperty("total_estimado") public double totalEstimado;
        public String observaciones;
        public String fecha;

        // ── Campos para importar_datos_excel ───────────────────────────────
        @JsonProperty("tipo_importacion")                public String tipoImportacion;
        public String proveedor;
        @JsonProperty("archivo_nombre")                  public String archivoNombre;
        @JsonProperty("hojas_a_importar")                public List<String> hojasAImportar;
        @JsonProperty("actualizar_registros_existentes") public Boolean actualizarExistentes;
        @JsonProperty("crear_nuevos_registros")          public Boolean crearNuevos;
        @JsonProperty("dry_run")                         public Boolean dryRun;

        // ── Campos CRUD generales ──────────────────────────────────────────
        public int id;          // ID del registro a editar/borrar
        public String nombre;   // nombre del material, empleado, tarifa
        public String apellidos;
        public String nif;
        public String email;
        public String telefono;
        public String ciudad;
        public String tipo;     // tipo de cliente (empresa/particular) o módulo
        public String estado;   // estado del documento
        public String modulo;   // nombre del módulo para abrir_modulo / cerrar_modulo
        public String descripcion;

        // ── Campos de tarifas ──────────────────────────────────────────────
        public String tecnica;                                              // Serigrafía, DTF…
        @JsonProperty("precio_unitario") public double precioUnitario;
        @JsonProperty("precio_setup")    public double precioSetup;
        @JsonProperty("minimo_unidades") public int minimoUnidades;
        public Boolean activa;

        // ── Campos de empleados ────────────────────────────────────────────
        @JsonProperty("salario_base") public double salarioBase;
        public double irpf;
        public String categoria;
        @JsonProperty("fecha_alta")   public String fechaAlta;
        public String iban;

        // ── Campos de materiales ───────────────────────────────────────────
        public String unidad;
        @JsonProperty("stock_actual") public double stockActual;
        @JsonProperty("stock_minimo") public double stockMinimo;
        public String referencia;
        @JsonProperty("precio_unidad") public double precioUnidad;

        // ── Campos de documentos (facturas, pedidos, nóminas) ──────────────
        @JsonProperty("forma_pago")        public String formaPago;
        @JsonProperty("fecha_vencimiento") public String fechaVencimiento;
        @JsonProperty("iva_porcentaje")    public double ivaPorcentaje;
        public String periodo;
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
            // ── Acciones originales ────────────────────────────────────────
            case "crear_presupuesto"    -> "Crear Presupuesto";
            case "generar_factura"      -> "Generar Factura";
            case "crear_albaran"        -> "Crear Albarán";
            case "crear_pedido"         -> "Crear Pedido";
            case "actualizar_stock"     -> "Actualizar Stock";
            case "crear_cliente"        -> "Crear Cliente";
            case "calcular_nomina"      -> "Calcular Nómina";
            case "generar_estadistica"  -> "Generar Estadística";
            case "agendar_evento"       -> "Agendar Evento";
            case "consultar_materiales" -> "Consultar Materiales";
            case "consultar_cliente"    -> "Consultar Cliente";
            case "exportar_backup"      -> "Exportar Backup";
            case "importar_datos_excel" -> "Importar Datos Excel";
            // ── Clientes ───────────────────────────────────────────────────
            case "editar_cliente"       -> "Editar Cliente";
            case "borrar_cliente"       -> "Eliminar Cliente";
            case "listar_clientes"      -> "Listar Clientes";
            // ── Presupuestos ───────────────────────────────────────────────
            case "editar_presupuesto"   -> "Editar Presupuesto";
            case "borrar_presupuesto"   -> "Eliminar Presupuesto";
            case "listar_presupuestos"  -> "Listar Presupuestos";
            // ── Facturas ───────────────────────────────────────────────────
            case "editar_factura"       -> "Editar Factura";
            case "borrar_factura"       -> "Eliminar Factura";
            case "listar_facturas"      -> "Listar Facturas";
            // ── Albaranes ──────────────────────────────────────────────────
            case "editar_albaran"       -> "Editar Albarán";
            case "borrar_albaran"       -> "Eliminar Albarán";
            case "listar_albaranes"     -> "Listar Albaranes";
            // ── Pedidos ────────────────────────────────────────────────────
            case "editar_pedido"        -> "Editar Pedido";
            case "borrar_pedido"        -> "Eliminar Pedido";
            case "listar_pedidos"       -> "Listar Pedidos";
            // ── Tarifas ────────────────────────────────────────────────────
            case "crear_tarifa"         -> "Crear Tarifa";
            case "editar_tarifa"        -> "Editar Tarifa";
            case "borrar_tarifa"        -> "Eliminar Tarifa";
            case "listar_tarifas"       -> "Listar Tarifas";
            // ── Materiales ─────────────────────────────────────────────────
            case "crear_material"       -> "Crear Material";
            case "editar_material"      -> "Editar Material";
            case "borrar_material"      -> "Eliminar Material";
            case "listar_materiales"    -> "Listar Materiales";
            // ── Empleados ──────────────────────────────────────────────────
            case "crear_empleado"       -> "Crear Empleado";
            case "editar_empleado"      -> "Editar Empleado";
            case "borrar_empleado"      -> "Eliminar Empleado";
            case "listar_empleados"     -> "Listar Empleados";
            // ── Nóminas ────────────────────────────────────────────────────
            case "editar_nomina"        -> "Editar Nómina";
            case "borrar_nomina"        -> "Eliminar Nómina";
            case "listar_nominas"       -> "Listar Nóminas";
            // ── Módulos ────────────────────────────────────────────────────
            case "abrir_modulo"         -> "Abrir Módulo";
            case "cerrar_modulo"        -> "Cerrar Módulo";
            default -> action;
        };
    }

    public String colorHex() {
        if (action == null) return "#6B2D5E";
        return switch (action) {
            case "crear_presupuesto", "editar_presupuesto", "borrar_presupuesto", "listar_presupuestos"
                                        -> "#6B2D5E";
            case "generar_factura", "editar_factura", "borrar_factura", "listar_facturas"
                                        -> "#1565C0";
            case "crear_albaran", "editar_albaran", "borrar_albaran", "listar_albaranes"
                                        -> "#E65100";
            case "crear_pedido", "editar_pedido", "borrar_pedido", "listar_pedidos"
                                        -> "#00695C";
            case "actualizar_stock"     -> "#B71C1C";
            case "crear_cliente", "editar_cliente", "borrar_cliente", "listar_clientes",
                 "consultar_cliente"    -> "#2E7D32";
            case "calcular_nomina", "editar_nomina", "borrar_nomina", "listar_nominas"
                                        -> "#263238";
            case "generar_estadistica"  -> "#6A1B9A";
            case "agendar_evento"       -> "#00838F";
            case "consultar_materiales", "crear_material", "editar_material",
                 "borrar_material", "listar_materiales"
                                        -> "#546E7A";
            case "crear_tarifa", "editar_tarifa", "borrar_tarifa", "listar_tarifas"
                                        -> "#E65100";
            case "crear_empleado", "editar_empleado", "borrar_empleado", "listar_empleados"
                                        -> "#4A148C";
            case "exportar_backup"      -> "#4E342E";
            case "importar_datos_excel" -> "#1A6B3C";
            case "abrir_modulo"         -> "#6A1B9A";
            case "cerrar_modulo"        -> "#546E7A";
            default -> "#6B2D5E";
        };
    }

    public String colorFondoHex() {
        if (action == null) return "#F5E6F0";
        return switch (action) {
            case "crear_presupuesto", "editar_presupuesto", "borrar_presupuesto", "listar_presupuestos"
                                        -> "#F5E6F0";
            case "generar_factura", "editar_factura", "borrar_factura", "listar_facturas"
                                        -> "#E3F2FD";
            case "crear_albaran", "editar_albaran", "borrar_albaran", "listar_albaranes"
                                        -> "#FFF3E0";
            case "crear_pedido", "editar_pedido", "borrar_pedido", "listar_pedidos"
                                        -> "#E0F2F1";
            case "actualizar_stock"     -> "#FFEBEE";
            case "crear_cliente", "editar_cliente", "borrar_cliente", "listar_clientes",
                 "consultar_cliente"    -> "#E8F5E9";
            case "calcular_nomina", "editar_nomina", "borrar_nomina", "listar_nominas"
                                        -> "#ECEFF1";
            case "generar_estadistica"  -> "#F3E5F5";
            case "agendar_evento"       -> "#E0F7FA";
            case "consultar_materiales", "crear_material", "editar_material",
                 "borrar_material", "listar_materiales"
                                        -> "#ECEFF1";
            case "crear_tarifa", "editar_tarifa", "borrar_tarifa", "listar_tarifas"
                                        -> "#FFF3E0";
            case "crear_empleado", "editar_empleado", "borrar_empleado", "listar_empleados"
                                        -> "#EDE7F6";
            case "exportar_backup"      -> "#EFEBE9";
            case "importar_datos_excel" -> "#E8F5E9";
            case "abrir_modulo"         -> "#F3E5F5";
            case "cerrar_modulo"        -> "#ECEFF1";
            default -> "#F5E6F0";
        };
    }

    public String accionIcono() {
        if (action == null) return "📋";
        return switch (action) {
            case "crear_presupuesto"    -> "📋";
            case "editar_presupuesto"   -> "✏ Presupuesto";
            case "borrar_presupuesto"   -> "🗑 Presupuesto";
            case "listar_presupuestos"  -> "📋";
            case "generar_factura"      -> "🧾";
            case "editar_factura"       -> "✏ Factura";
            case "borrar_factura"       -> "🗑 Factura";
            case "listar_facturas"      -> "🧾";
            case "crear_albaran"        -> "📦";
            case "editar_albaran"       -> "✏ Albarán";
            case "borrar_albaran"       -> "🗑 Albarán";
            case "listar_albaranes"     -> "📦";
            case "crear_pedido"         -> "🛒";
            case "editar_pedido"        -> "✏ Pedido";
            case "borrar_pedido"        -> "🗑 Pedido";
            case "listar_pedidos"       -> "🛒";
            case "actualizar_stock"     -> "🗃";
            case "crear_cliente"        -> "👤";
            case "editar_cliente"       -> "✏ Cliente";
            case "borrar_cliente"       -> "🗑 Cliente";
            case "listar_clientes"      -> "👥";
            case "calcular_nomina"      -> "💰";
            case "editar_nomina"        -> "✏ Nómina";
            case "borrar_nomina"        -> "🗑 Nómina";
            case "listar_nominas"       -> "💰";
            case "generar_estadistica"  -> "📊";
            case "agendar_evento"       -> "📅";
            case "consultar_materiales" -> "🔍";
            case "consultar_cliente"    -> "🔍";
            case "crear_material"       -> "📦";
            case "editar_material"      -> "✏ Material";
            case "borrar_material"      -> "🗑 Material";
            case "listar_materiales"    -> "📦";
            case "crear_tarifa"         -> "🏷";
            case "editar_tarifa"        -> "✏ Tarifa";
            case "borrar_tarifa"        -> "🗑 Tarifa";
            case "listar_tarifas"       -> "🏷";
            case "crear_empleado"       -> "👤";
            case "editar_empleado"      -> "✏ Empleado";
            case "borrar_empleado"      -> "🗑 Empleado";
            case "listar_empleados"     -> "👥";
            case "exportar_backup"      -> "💾";
            case "importar_datos_excel" -> "📥";
            case "abrir_modulo"         -> "🪟";
            case "cerrar_modulo"        -> "✖";
            default -> "⚙";
        };
    }
}
