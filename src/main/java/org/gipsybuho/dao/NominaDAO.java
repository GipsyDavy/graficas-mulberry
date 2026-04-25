package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Nomina;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NominaDAO {

    public List<Nomina> findAll() throws SQLException {
        List<Nomina> list = new ArrayList<>();
        String sql = """
            SELECT n.*, (e.nombre || COALESCE(' ' || NULLIF(e.apellido, ''), '')) as empleado_nombre
            FROM nominas n
            JOIN empleados e ON n.empleado_id = e.id
            ORDER BY n.anio DESC, n.mes DESC, e.nombre
            """;
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Nomina> findByPeriodo(int mes, int anio) throws SQLException {
        List<Nomina> list = new ArrayList<>();
        String sql = """
            SELECT n.*, (e.nombre || COALESCE(' ' || NULLIF(e.apellido, ''), '')) as empleado_nombre
            FROM nominas n
            JOIN empleados e ON n.empleado_id = e.id
            WHERE n.mes=? AND n.anio=?
            ORDER BY e.nombre
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, mes); ps.setInt(2, anio);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Nomina findById(int id) throws SQLException {
        String sql = """
            SELECT n.*, (e.nombre || COALESCE(' ' || NULLIF(e.apellido, ''), '')) as empleado_nombre
            FROM nominas n
            JOIN empleados e ON n.empleado_id = e.id
            WHERE n.id=?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void save(Nomina n) throws SQLException {
        if (n.getId() == 0) insert(n); else update(n);
    }

    private void insert(Nomina n) throws SQLException {
        String sql = """
            INSERT INTO nominas (empleado_id,mes,anio,salario_base,complementos,
            horas_extra_normales,precio_hora_extra,horas_extra_festivas,precio_hora_festiva,
            percepciones_no_salariales,total_bruto,irpf_porcentaje,irpf_importe,
            ss_trabajador,total_deducciones,neto,ss_empresa,coste_total_empresa)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, n);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) n.setId(keys.getInt(1));
        }
    }

    private void update(Nomina n) throws SQLException {
        String sql = """
            UPDATE nominas SET empleado_id=?,mes=?,anio=?,salario_base=?,complementos=?,
            horas_extra_normales=?,precio_hora_extra=?,horas_extra_festivas=?,precio_hora_festiva=?,
            percepciones_no_salariales=?,total_bruto=?,irpf_porcentaje=?,irpf_importe=?,
            ss_trabajador=?,total_deducciones=?,neto=?,ss_empresa=?,coste_total_empresa=?
            WHERE id=?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, n);
            ps.setInt(19, n.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM nominas WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void set(PreparedStatement ps, Nomina n) throws SQLException {
        ps.setInt(1, n.getEmpleadoId());
        ps.setInt(2, n.getMes());
        ps.setInt(3, n.getAnio());
        ps.setDouble(4, n.getSalarioBase());
        ps.setDouble(5, n.getComplementos());
        ps.setInt(6, n.getHorasExtraNormales());
        ps.setDouble(7, n.getPrecioHoraExtra());
        ps.setInt(8, n.getHorasExtraFestivas());
        ps.setDouble(9, n.getPrecioHoraFestiva());
        ps.setDouble(10, n.getPercepcionesNoSalariales());
        ps.setDouble(11, n.getTotalBruto());
        ps.setDouble(12, n.getIrpfPorcentaje());
        ps.setDouble(13, n.getIrpfImporte());
        ps.setDouble(14, n.getSsTrabajador());
        ps.setDouble(15, n.getTotalDeducciones());
        ps.setDouble(16, n.getNeto());
        ps.setDouble(17, n.getSsEmpresa());
        ps.setDouble(18, n.getCosteTotalEmpresa());
    }

    private Nomina map(ResultSet rs) throws SQLException {
        Nomina n = new Nomina();
        n.setId(rs.getInt("id"));
        n.setEmpleadoId(rs.getInt("empleado_id"));
        n.setEmpleadoNombre(rs.getString("empleado_nombre"));
        n.setMes(rs.getInt("mes"));
        n.setAnio(rs.getInt("anio"));
        n.setSalarioBase(rs.getDouble("salario_base"));
        n.setComplementos(rs.getDouble("complementos"));
        n.setHorasExtraNormales(rs.getInt("horas_extra_normales"));
        n.setPrecioHoraExtra(rs.getDouble("precio_hora_extra"));
        n.setHorasExtraFestivas(rs.getInt("horas_extra_festivas"));
        n.setPrecioHoraFestiva(rs.getDouble("precio_hora_festiva"));
        n.setPercepcionesNoSalariales(rs.getDouble("percepciones_no_salariales"));
        n.setTotalBruto(rs.getDouble("total_bruto"));
        n.setIrpfPorcentaje(rs.getDouble("irpf_porcentaje"));
        n.setIrpfImporte(rs.getDouble("irpf_importe"));
        n.setSsTrabajador(rs.getDouble("ss_trabajador"));
        n.setTotalDeducciones(rs.getDouble("total_deducciones"));
        n.setNeto(rs.getDouble("neto"));
        n.setSsEmpresa(rs.getDouble("ss_empresa"));
        n.setCosteTotalEmpresa(rs.getDouble("coste_total_empresa"));
        n.setCreatedAt(rs.getString("created_at"));
        return n;
    }
}
