package org.gipsybuho.model;

import org.gipsybuho.service.importer.ColumnMatcher;
import org.gipsybuho.service.importer.DuplicatePolicy;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Presupuesto {
    private int id;
    private String numero;
    private int clienteId;
    private String clienteNombre; // campo auxiliar para mostrar en tabla
    private String fecha;
    private String fechaValidez;
    private String estado; // borrador, enviado, aceptado, rechazado, facturado
    private double baseImponible;
    private double ivaPorcentaje;
    private double ivaImporte;
    private double total;
    private String notas;
    private String condiciones;
    private String createdAt;
    private List<LineaPresupuesto> lineas = new ArrayList<>();

    public static final EntityImportSpec IMPORT_SPEC = buildSpec();

    private static EntityImportSpec buildSpec() {
        var syn = Map.ofEntries(
            Map.entry("cliente_nif",       List.of("cliente nif", "nif cliente", "cif cliente", "dni cliente", "cliente cif", "nif", "cif", "dni", "tax id")),
            Map.entry("cliente_nombre",    List.of("cliente nombre", "nombre cliente", "cliente", "razon social", "empresa")),
            Map.entry("cliente_apellidos", List.of("cliente apellidos", "apellidos cliente", "apellido cliente", "surname", "lastname")),
            Map.entry("numero",            List.of("numero", "nº presupuesto", "num presupuesto", "presupuesto", "numero presupuesto", "referencia presupuesto")),
            Map.entry("fecha",             List.of("fecha", "fecha presupuesto", "date")),
            Map.entry("fecha_validez",     List.of("fecha validez", "validez", "valido hasta", "fecha valido hasta", "expiracion")),
            Map.entry("estado",            List.of("estado", "status", "situacion")),
            Map.entry("iva_porcentaje",    List.of("iva porcentaje", "iva %", "porcentaje iva", "iva")),
            Map.entry("notas",             List.of("notas", "observaciones", "comentarios", "notes")),
            Map.entry("condiciones",       List.of("condiciones", "terminos", "condiciones de pago", "terms")),
            Map.entry("descripcion",       List.of("descripcion", "detalle", "concepto", "description")),
            Map.entry("tecnica",           List.of("tecnica", "proceso", "technique", "tipo", "type")),
            Map.entry("cantidad",          List.of("cantidad", "qty", "uds", "unidades")),
            Map.entry("precio_unit",       List.of("precio unitario", "precio unidad", "precio unit", "precio", "price", "rate", "pvp")),
            Map.entry("descuento",         List.of("descuento", "discount", "dto", "dto %", "descuento %"))
        );
        ColumnMatcher matcher = h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn);

        EntityImportSpec specLinea = new EntityImportSpec("Líneas Presupuesto", List.of(
            new FieldSpec("descripcion", "Descripción",       true),
            new FieldSpec("tecnica",     "Técnica",           false),
            new FieldSpec("cantidad",    "Cantidad",          true),
            new FieldSpec("precio_unit", "Precio unitario",   true),
            new FieldSpec("descuento",   "Descuento (%)",     false)
        ), matcher, DuplicatePolicy.SKIP_IF_EXISTS);

        return new EntityImportSpec(
            "Presupuestos",
            List.of(
                new FieldSpec("cliente_nif",       "NIF/CIF del cliente",   false),
                new FieldSpec("cliente_nombre",    "Nombre del cliente",    false),
                new FieldSpec("cliente_apellidos", "Apellidos del cliente", false),
                new FieldSpec("numero",            "Número",                true),
                new FieldSpec("fecha",             "Fecha",                 false),
                new FieldSpec("fecha_validez",     "Fecha validez",         false),
                new FieldSpec("estado",            "Estado",                false),
                new FieldSpec("iva_porcentaje",    "IVA (%)",               false),
                new FieldSpec("notas",             "Notas",                 false),
                new FieldSpec("condiciones",       "Condiciones",           false)
            ),
            matcher,
            DuplicatePolicy.SKIP_IF_EXISTS,
            "numero",   // claveAgrupacion
            "lineas",   // campoLineas
            specLinea
        );
    }

    public Presupuesto() {}

    public void calcularTotales() {
        baseImponible = lineas.stream().mapToDouble(LineaPresupuesto::getTotal).sum();
        ivaImporte = baseImponible * (ivaPorcentaje / 100.0);
        total = baseImponible + ivaImporte;
    }

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
    public String getFechaValidez() { return fechaValidez; }
    public void setFechaValidez(String fechaValidez) { this.fechaValidez = fechaValidez; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public double getBaseImponible() { return baseImponible; }
    public void setBaseImponible(double baseImponible) { this.baseImponible = baseImponible; }
    public double getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(double ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public double getIvaImporte() { return ivaImporte; }
    public void setIvaImporte(double ivaImporte) { this.ivaImporte = ivaImporte; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getCondiciones() { return condiciones; }
    public void setCondiciones(String condiciones) { this.condiciones = condiciones; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<LineaPresupuesto> getLineas() { return lineas; }
    public void setLineas(List<LineaPresupuesto> lineas) { this.lineas = lineas; }
}
