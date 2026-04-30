package org.gipsybuho.service;

import org.gipsybuho.db.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class EstadisticasService {

    private static final String[] MESES = {
        "Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"
    };

    // ── Ingresos mensuales (facturas no anuladas) ─────────────────────────────

    public static Map<String, Double> ingresosPorMes(int anio) {
        return queryMensualStr(
            "SELECT strftime('%m',fecha), COALESCE(SUM(total),0) " +
            "FROM facturas WHERE strftime('%Y',fecha)=? AND estado!='anulada' GROUP BY 1",
            anio);
    }

    // ── Gastos de materiales por mes ──────────────────────────────────────────

    public static Map<String, Double> gastosMaterialPorMes(int anio) {
        return queryMensualStr(
            "SELECT strftime('%m',fecha_compra), COALESCE(SUM(importe_total),0) " +
            "FROM pagos_material WHERE fecha_compra IS NOT NULL " +
            "AND strftime('%Y',fecha_compra)=? GROUP BY 1",
            anio);
    }

    // ── Gastos de nóminas por mes ─────────────────────────────────────────────

    public static Map<String, Double> gastosNominasPorMes(int anio) {
        Map<String, Double> result = mesMapa();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT mes, COALESCE(SUM(neto),0) FROM nominas WHERE anio=? GROUP BY mes")) {
            ps.setInt(1, anio);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int m = rs.getInt(1);
                if (m >= 1 && m <= 12) result.put(MESES[m - 1], rs.getDouble(2));
            }
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Materiales más consumidos (por movimientos de salida) ─────────────────

    public static Map<String, Double> materialesMasUsados(int limite) {
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT m.nombre, COALESCE(SUM(mv.cantidad),0) " +
                "FROM movimientos_material mv " +
                "JOIN materiales m ON mv.material_id=m.id " +
                "WHERE mv.tipo='salida' " +
                "GROUP BY m.nombre ORDER BY 2 DESC LIMIT ?")) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Materiales por categoría (número de artículos) ────────────────────────

    public static Map<String, Double> materialesPorCategoria() {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COALESCE(NULLIF(categoria,''),'Sin categoría'), COUNT(*) " +
                "FROM materiales GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) result.put(capitalize(rs.getString(1)), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Stock actual de materiales (top N por uso) ────────────────────────────

    public static Map<String, Double> stockMateriales(int limite) {
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT nombre, stock_actual FROM materiales " +
                "ORDER BY stock_actual DESC LIMIT ?")) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Nuevos clientes por mes ───────────────────────────────────────────────

    public static Map<String, Double> clientesPorMes(int anio) {
        return queryMensualStr(
            "SELECT strftime('%m',created_at), COUNT(*) " +
            "FROM clientes WHERE strftime('%Y',created_at)=? GROUP BY 1",
            anio);
    }

    // ── Clientes por tipo (empresa / particular) ──────────────────────────────

    public static Map<String, Double> clientesPorTipo() {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COALESCE(NULLIF(tipo,''),'Sin tipo'), COUNT(*) " +
                "FROM clientes GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) result.put(capitalize(rs.getString(1)), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Top clientes por facturación total ────────────────────────────────────

    public static Map<String, Double> topClientesPorFacturacion(int limite) {
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT (c.nombre||COALESCE(' '||NULLIF(c.apellidos,''),'')) as nombre, " +
                "COALESCE(SUM(f.total),0) " +
                "FROM facturas f JOIN clientes c ON f.cliente_id=c.id " +
                "WHERE f.estado!='anulada' " +
                "GROUP BY f.cliente_id ORDER BY 2 DESC LIMIT ?")) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Técnicas más usadas por ingresos (líneas de factura) ─────────────────

    public static Map<String, Double> tecnicasPorIngresos() {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COALESCE(NULLIF(tecnica,''),'Sin técnica'), COALESCE(SUM(total),0) " +
                "FROM lineas_factura GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) result.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Técnicas más usadas por número de pedidos/líneas ─────────────────────

    public static Map<String, Double> tecnicasPorVolumen() {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COALESCE(NULLIF(tecnica,''),'Sin técnica'), COUNT(*) " +
                "FROM lineas_factura GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) result.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Pedidos por estado ────────────────────────────────────────────────────

    public static Map<String, Double> pedidosPorEstado() {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT estado, COUNT(*) FROM pedidos GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) result.put(capitalize(rs.getString(1)), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Estado de facturas ────────────────────────────────────────────────────

    public static Map<String, Double> facturasPorEstado() {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT estado, COUNT(*) FROM facturas GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) result.put(capitalize(rs.getString(1)), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Presupuestos por estado ───────────────────────────────────────────────

    public static Map<String, Double> presupuestosPorEstado() {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT estado, COUNT(*) FROM presupuestos GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) result.put(capitalize(rs.getString(1)), rs.getDouble(2));
        } catch (SQLException ignored) {}
        return result;
    }

    // ── Facturación acumulada mensual (año actual) ────────────────────────────

    public static Map<String, Double> facturacionAcumulada(int anio) {
        Map<String, Double> mensual = ingresosPorMes(anio);
        Map<String, Double> acumulada = new LinkedHashMap<>();
        double acum = 0;
        for (Map.Entry<String, Double> e : mensual.entrySet()) {
            acum += e.getValue();
            acumulada.put(e.getKey(), acum);
        }
        return acumulada;
    }

    // ── KPIs globales ─────────────────────────────────────────────────────────

    public static double totalIngresosAnio(int anio) {
        return scalarStr("SELECT COALESCE(SUM(total),0) FROM facturas " +
            "WHERE strftime('%Y',fecha)=? AND estado!='anulada'", anio);
    }

    public static double totalGastosMaterialAnio(int anio) {
        return scalarStr("SELECT COALESCE(SUM(importe_total),0) FROM pagos_material " +
            "WHERE fecha_compra IS NOT NULL AND strftime('%Y',fecha_compra)=?", anio);
    }

    public static double totalNominasAnio(int anio) {
        return scalarInt("SELECT COALESCE(SUM(neto),0) FROM nominas WHERE anio=?", anio);
    }

    public static int totalClientesAnio(int anio) {
        return (int) scalarStr("SELECT COUNT(*) FROM clientes WHERE strftime('%Y',created_at)=?", anio);
    }

    public static int totalClientesAcumulado() {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM clientes")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    // ── Años con datos en la BD ───────────────────────────────────────────────

    public static List<Integer> aniosDisponibles() {
        Set<Integer> set = new TreeSet<>(Comparator.reverseOrder());
        int actual = LocalDate.now().getYear();
        set.add(actual);
        try (Statement st = DatabaseManager.getConnection().createStatement()) {
            for (String q : new String[]{
                "SELECT DISTINCT strftime('%Y',fecha) FROM facturas WHERE fecha IS NOT NULL",
                "SELECT DISTINCT strftime('%Y',created_at) FROM clientes WHERE created_at IS NOT NULL",
                "SELECT DISTINCT strftime('%Y',fecha_compra) FROM pagos_material WHERE fecha_compra IS NOT NULL"
            }) {
                try (ResultSet rs = st.executeQuery(q)) {
                    while (rs.next()) {
                        String s = rs.getString(1);
                        if (s != null && !s.isBlank()) try { set.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
                    }
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ignored) {}
        return new ArrayList<>(set);
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    private static Map<String, Double> mesMapa() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (String s : MESES) m.put(s, 0.0);
        return m;
    }

    private static Map<String, Double> queryMensualStr(String sql, int anio) {
        Map<String, Double> result = mesMapa();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, String.valueOf(anio));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String mesStr = rs.getString(1);
                if (mesStr != null) {
                    int m = Integer.parseInt(mesStr.trim());
                    if (m >= 1 && m <= 12) result.put(MESES[m - 1], rs.getDouble(2));
                }
            }
        } catch (SQLException ignored) {}
        return result;
    }

    private static double scalarStr(String sql, int anio) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, String.valueOf(anio));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    private static double scalarInt(String sql, int anio) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anio);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).replace('_', ' ');
    }
}
