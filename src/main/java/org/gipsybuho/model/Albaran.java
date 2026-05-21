package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Albaran {
    private int id;
    private String numero;
    private int clienteId;
    private String clienteNombre;
    private String fecha;
    private int facturaId;
    private String facturaNumero;
    private int pedidoId;
    private String pedidoNumero;
    private String estado; // pendiente, entregado, firmado
    private String observaciones;
    private String createdAt;
    private List<LineaAlbaran> lineas = new ArrayList<>();

    public Albaran() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public int getFacturaId() { return facturaId; }
    public void setFacturaId(int facturaId) { this.facturaId = facturaId; }
    public String getFacturaNumero() { return facturaNumero; }
    public void setFacturaNumero(String facturaNumero) { this.facturaNumero = facturaNumero; }
    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }
    public String getPedidoNumero() { return pedidoNumero; }
    public void setPedidoNumero(String pedidoNumero) { this.pedidoNumero = pedidoNumero; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<LineaAlbaran> getLineas() { return lineas; }
    public void setLineas(List<LineaAlbaran> lineas) { this.lineas = lineas; }

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    private static EntityImportSpec buildSpec() {
        var syn = Map.ofEntries(
            Map.entry("cliente_nif",       List.of("cliente nif", "nif cliente", "cif cliente", "dni cliente", "cliente cif", "tax id")),
            Map.entry("cliente_nombre",    List.of("cliente nombre", "nombre cliente", "cliente", "razon social", "empresa")),
            Map.entry("cliente_apellidos", List.of("cliente apellidos", "apellidos cliente", "apellido cliente", "surname", "lastname")),
            Map.entry("numero",            List.of("numero albaran", "num albaran", "albaran", "referencia albaran", "nº albaran")),
            Map.entry("factura_numero",    List.of("factura numero", "numero factura", "num factura", "factura", "ref factura", "nº factura")),
            Map.entry("pedido_numero",     List.of("pedido numero", "numero pedido", "num pedido", "pedido", "ref pedido", "nº pedido")),
            Map.entry("fecha",             List.of("fecha", "fecha albaran", "fecha emision", "date")),
            Map.entry("estado",            List.of("estado", "status", "situacion")),
            Map.entry("observaciones",     List.of("observaciones", "obs", "notas", "comentarios", "notes")),
            Map.entry("descripcion",       List.of("descripcion", "detalle", "concepto", "description", "producto", "articulo")),
            Map.entry("cantidad",          List.of("cantidad", "qty", "uds", "unidades")),
            Map.entry("unidad",            List.of("unidad", "medida", "unit"))
        );
        ColumnMatcher matcher = h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn);

        EntityImportSpec specLinea = new EntityImportSpec("Líneas Albarán", List.of(
            new FieldSpec("descripcion", "Descripción", true),
            new FieldSpec("cantidad",    "Cantidad",    true),
            new FieldSpec("unidad",      "Unidad",      false)
        ), matcher, DuplicatePolicy.SKIP_IF_EXISTS);

        return new EntityImportSpec(
            "Albaranes",
            List.of(
                new FieldSpec("cliente_nif",       "NIF/CIF del cliente",   false),
                new FieldSpec("cliente_nombre",    "Nombre del cliente",    false),
                new FieldSpec("cliente_apellidos", "Apellidos del cliente", false),
                new FieldSpec("numero",            "Número",                true),
                new FieldSpec("factura_numero",    "Nº de factura",         false),
                new FieldSpec("pedido_numero",     "Nº de pedido",          false),
                new FieldSpec("fecha",             "Fecha",                 false),
                new FieldSpec("estado",            "Estado",                false),
                new FieldSpec("observaciones",     "Observaciones",         false)
            ),
            matcher,
            DuplicatePolicy.SKIP_IF_EXISTS,
            "numero",   // claveAgrupacion
            "lineas",   // campoLineas
            specLinea
        );
    }
}
