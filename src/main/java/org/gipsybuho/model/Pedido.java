package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Pedido {

    private int       id;
    private String    numero;
    private int       clienteId;
    private String    clienteNombre;
    private LocalDate fecha;
    private LocalDate fechaEntregaPrevista;
    private LocalDate fechaEntregaReal;
    private String    estado;          // pendiente / en_proceso / listo / entregado / cancelado
    private String    descripcion;
    private double    importeTotal;
    private double    ivaPorcentaje;
    private String    notas;
    // Computed from pagos_pedido via SQL
    private double    importePagado;
    private int       pagosVencidos;

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    private static EntityImportSpec buildSpec() {
        // cliente_nif, cliente_nombre y cliente_apellidos son campos virtuales del CSV:
        // no existen en Pedido, se usan para resolver clienteId antes de guardar.
        // Si viene cliente_nif y no existe, la importación falla sin fallback por nombre.
        var syn = Map.ofEntries(
            Map.entry("cliente_nif", List.of("cliente nif", "nif cliente", "cif cliente", "dni cliente", "cliente cif", "nif", "cif", "dni", "tax id")),
            Map.entry("cliente_nombre", List.of("cliente nombre", "nombre cliente", "cliente", "razon social", "empresa")),
            Map.entry("cliente_apellidos", List.of("cliente apellidos", "apellidos cliente", "apellido cliente", "surname", "lastname")),
            Map.entry("numero", List.of("numero", "nº pedido", "num pedido", "pedido", "numero pedido", "referencia pedido")),
            Map.entry("fecha", List.of("fecha", "fecha pedido", "date")),
            Map.entry("fecha_entrega_prevista", List.of("fecha entrega prevista", "entrega prevista", "fecha prevista", "prevision entrega")),
            Map.entry("fecha_entrega_real", List.of("fecha entrega real", "entrega real", "fecha real", "fecha entregado")),
            Map.entry("estado", List.of("estado", "status", "situacion")),
            Map.entry("descripcion", List.of("descripcion", "detalle", "concepto", "description")),
            Map.entry("importe_total", List.of("importe total", "total", "importe", "total pedido")),
            Map.entry("iva_porcentaje", List.of("iva porcentaje", "iva %", "porcentaje iva", "iva")),
            Map.entry("notas", List.of("notas", "observaciones", "comentarios", "notes"))
        );
        return new EntityImportSpec("Pedidos", List.of(
            new FieldSpec("cliente_nif", "NIF/CIF del cliente", false),
            new FieldSpec("cliente_nombre", "Nombre del cliente", false),
            new FieldSpec("cliente_apellidos", "Apellidos del cliente", false),
            new FieldSpec("numero", "Número", true),
            new FieldSpec("fecha", "Fecha", false),
            new FieldSpec("fecha_entrega_prevista", "Fecha entrega prevista", false),
            new FieldSpec("fecha_entrega_real", "Fecha entrega real", false),
            new FieldSpec("estado", "Estado", false),
            new FieldSpec("descripcion", "Descripción", false),
            new FieldSpec("importe_total", "Importe total", false),
            new FieldSpec("iva_porcentaje", "IVA (%)", false),
            new FieldSpec("notas", "Notas", false)
        ), h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn), DuplicatePolicy.SKIP_IF_EXISTS);
    }

    public double getImportePendiente() {
        return Math.max(0, importeTotal - importePagado);
    }

    public String getEstadoDisplay() {
        return switch (estado != null ? estado : "") {
            case "en_proceso" -> "En proceso";
            case "listo"      -> "Listo";
            case "entregado"  -> "Entregado";
            case "cancelado"  -> "Cancelado";
            default           -> "Pendiente";
        };
    }

    public int    getId()                         { return id; }
    public void   setId(int id)                   { this.id = id; }
    public String getNumero()                     { return numero; }
    public void   setNumero(String v)             { this.numero = v; }
    public int    getClienteId()                  { return clienteId; }
    public void   setClienteId(int v)             { this.clienteId = v; }
    public String getClienteNombre()              { return clienteNombre; }
    public void   setClienteNombre(String v)      { this.clienteNombre = v; }
    public LocalDate getFecha()                   { return fecha; }
    public void   setFecha(LocalDate v)           { this.fecha = v; }
    public LocalDate getFechaEntregaPrevista()    { return fechaEntregaPrevista; }
    public void   setFechaEntregaPrevista(LocalDate v) { this.fechaEntregaPrevista = v; }
    public LocalDate getFechaEntregaReal()        { return fechaEntregaReal; }
    public void   setFechaEntregaReal(LocalDate v){ this.fechaEntregaReal = v; }
    public String getEstado()                     { return estado; }
    public void   setEstado(String v)             { this.estado = v; }
    public String getDescripcion()                { return descripcion; }
    public void   setDescripcion(String v)        { this.descripcion = v; }
    public double getImporteTotal()               { return importeTotal; }
    public void   setImporteTotal(double v)       { this.importeTotal = v; }
    public double getIvaPorcentaje()              { return ivaPorcentaje; }
    public void   setIvaPorcentaje(double v)      { this.ivaPorcentaje = v; }
    public String getNotas()                      { return notas; }
    public void   setNotas(String v)              { this.notas = v; }
    public double getImportePagado()              { return importePagado; }
    public void   setImportePagado(double v)      { this.importePagado = v; }
    public int    getPagosVencidos()              { return pagosVencidos; }
    public void   setPagosVencidos(int v)         { this.pagosVencidos = v; }
}
