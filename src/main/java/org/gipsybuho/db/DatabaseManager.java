package org.gipsybuho.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = buildDbUrl();
    private static Connection connection;

    private static String buildDbUrl() {
        String appData = System.getenv("LOCALAPPDATA");
        java.io.File dir = new java.io.File(
            (appData != null && !appData.isEmpty()) ? appData : System.getProperty("user.home"),
            "GraficasMulberry"
        );
        dir.mkdirs();
        return "jdbc:sqlite:" + new java.io.File(dir, "graficas_mulberry.db").getAbsolutePath();
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initialize() throws SQLException {
        Connection conn = getConnection();
        createTables(conn);
        insertDatosIniciales(conn);
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión con la base de datos: " + e.getMessage());
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    tipo TEXT DEFAULT 'empresa',
                    nif TEXT,
                    direccion TEXT,
                    ciudad TEXT DEFAULT 'Almería',
                    cp TEXT,
                    telefono TEXT,
                    email TEXT,
                    notas TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS empleados (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    nif TEXT,
                    categoria TEXT DEFAULT 'Operario',
                    salario_base REAL DEFAULT 1200.0,
                    fecha_alta TEXT,
                    fecha_baja TEXT,
                    iban TEXT,
                    irpf REAL DEFAULT 15.0,
                    activo INTEGER DEFAULT 1,
                    telefono TEXT,
                    email TEXT,
                    direccion TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS tarifas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tecnica TEXT NOT NULL,
                    nombre TEXT NOT NULL,
                    descripcion TEXT,
                    precio_unit REAL DEFAULT 0.0,
                    precio_setup REAL DEFAULT 0.0,
                    minimo_unidades INTEGER DEFAULT 1,
                    activa INTEGER DEFAULT 1,
                    updated_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS materiales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    referencia TEXT,
                    categoria TEXT DEFAULT 'consumibles',
                    stock_actual REAL DEFAULT 0.0,
                    stock_minimo REAL DEFAULT 0.0,
                    unidad TEXT DEFAULT 'ud',
                    precio_unidad REAL DEFAULT 0.0,
                    proveedor TEXT,
                    updated_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS movimientos_material (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    material_id INTEGER REFERENCES materiales(id),
                    tipo TEXT,
                    cantidad REAL,
                    fecha TEXT,
                    descripcion TEXT,
                    referencia_pedido TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS presupuestos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero TEXT UNIQUE NOT NULL,
                    cliente_id INTEGER REFERENCES clientes(id),
                    fecha TEXT NOT NULL,
                    fecha_validez TEXT,
                    estado TEXT DEFAULT 'borrador',
                    base_imponible REAL DEFAULT 0.0,
                    iva_porcentaje REAL DEFAULT 21.0,
                    iva_importe REAL DEFAULT 0.0,
                    total REAL DEFAULT 0.0,
                    notas TEXT,
                    condiciones TEXT DEFAULT 'Presupuesto válido por 30 días. Precios sin IVA.',
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS lineas_presupuesto (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    presupuesto_id INTEGER REFERENCES presupuestos(id) ON DELETE CASCADE,
                    descripcion TEXT NOT NULL,
                    tecnica TEXT,
                    cantidad INTEGER DEFAULT 1,
                    precio_unit REAL DEFAULT 0.0,
                    descuento REAL DEFAULT 0.0,
                    total REAL DEFAULT 0.0,
                    orden INTEGER DEFAULT 0
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS facturas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero TEXT UNIQUE NOT NULL,
                    presupuesto_id INTEGER REFERENCES presupuestos(id),
                    cliente_id INTEGER REFERENCES clientes(id),
                    fecha TEXT NOT NULL,
                    fecha_vencimiento TEXT,
                    estado TEXT DEFAULT 'pendiente',
                    forma_pago TEXT DEFAULT 'Transferencia bancaria',
                    base_imponible REAL DEFAULT 0.0,
                    iva_porcentaje REAL DEFAULT 21.0,
                    iva_importe REAL DEFAULT 0.0,
                    total REAL DEFAULT 0.0,
                    notas TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS lineas_factura (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    factura_id INTEGER REFERENCES facturas(id) ON DELETE CASCADE,
                    descripcion TEXT NOT NULL,
                    tecnica TEXT,
                    cantidad INTEGER DEFAULT 1,
                    precio_unit REAL DEFAULT 0.0,
                    descuento REAL DEFAULT 0.0,
                    total REAL DEFAULT 0.0,
                    orden INTEGER DEFAULT 0
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS nominas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    empleado_id INTEGER REFERENCES empleados(id),
                    mes INTEGER NOT NULL,
                    anio INTEGER NOT NULL,
                    salario_base REAL DEFAULT 0.0,
                    complementos REAL DEFAULT 0.0,
                    horas_extra_normales INTEGER DEFAULT 0,
                    precio_hora_extra REAL DEFAULT 15.0,
                    horas_extra_festivas INTEGER DEFAULT 0,
                    precio_hora_festiva REAL DEFAULT 22.0,
                    percepciones_no_salariales REAL DEFAULT 0.0,
                    total_bruto REAL DEFAULT 0.0,
                    irpf_porcentaje REAL DEFAULT 0.0,
                    irpf_importe REAL DEFAULT 0.0,
                    ss_trabajador REAL DEFAULT 0.0,
                    total_deducciones REAL DEFAULT 0.0,
                    neto REAL DEFAULT 0.0,
                    ss_empresa REAL DEFAULT 0.0,
                    coste_total_empresa REAL DEFAULT 0.0,
                    created_at TEXT DEFAULT (datetime('now')),
                    UNIQUE(empleado_id, mes, anio)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS config (
                    clave TEXT PRIMARY KEY,
                    valor TEXT
                )""");
        }
    }

    private static void insertDatosIniciales(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            // Configuración inicial de la empresa
            st.execute("""
                INSERT OR IGNORE INTO config (clave, valor) VALUES
                ('empresa_nombre', 'Gráficas Mulberry'),
                ('empresa_nif', 'PENDIENTE-CONFIGURAR'),
                ('empresa_direccion', 'Calle Río Miño, 54 - 3F'),
                ('empresa_ciudad', 'Huercal de Almería'),
                ('empresa_cp', '04230'),
                ('empresa_telefono', '950 30 61 64'),
                ('empresa_email', 'info@graficasmulberry.com'),
                ('empresa_web', 'www.graficasmulberry.com'),
                ('iva_defecto', '21'),
                ('prefijo_presupuesto', 'PRE'),
                ('prefijo_factura', 'FAC'),
                ('siguiente_presupuesto', '1'),
                ('siguiente_factura', '1')
                """);

            // Tarifas de ejemplo para serigrafía
            st.execute("""
                INSERT OR IGNORE INTO tarifas (id, tecnica, nombre, descripcion, precio_unit, precio_setup, minimo_unidades) VALUES
                (1, 'Serigrafía', 'Serigrafía 1 color', 'Impresión serigráfica en 1 tinta', 2.50, 35.00, 12),
                (2, 'Serigrafía', 'Serigrafía 2 colores', 'Impresión serigráfica en 2 tintas', 3.20, 60.00, 12),
                (3, 'Serigrafía', 'Serigrafía 4 colores', 'Impresión serigráfica a todo color', 4.80, 100.00, 24),
                (4, 'DTF', 'DTF estándar', 'Impresión DTF (Direct to Film)', 3.50, 0.00, 1),
                (5, 'Bordado', 'Bordado básico', 'Bordado computarizado hasta 5.000 puntos', 4.00, 25.00, 6),
                (6, 'Bordado', 'Bordado premium', 'Bordado computarizado hasta 15.000 puntos', 7.00, 40.00, 6),
                (7, 'Vinilo', 'Vinilo textil', 'Corte de vinilo termoadhesivo para textil', 2.80, 0.00, 1),
                (8, 'Sublimación', 'Sublimación full-color', 'Sublimación para tejidos sintéticos', 3.00, 0.00, 1),
                (9, 'Gran Formato', 'Lona PVC 510g', 'Impresión en lona por m²', 18.00, 0.00, 1),
                (10, 'Gran Formato', 'Vinilo adhesivo', 'Vinilo adhesivo exterior por m²', 22.00, 0.00, 1)
                """);

            // Materiales de ejemplo
            st.execute("""
                INSERT OR IGNORE INTO materiales (id, nombre, referencia, categoria, stock_actual, stock_minimo, unidad, precio_unidad) VALUES
                (1, 'Tinta plastisol negra', 'TPL-NEG-01', 'tintas', 5.0, 2.0, 'kg', 12.50),
                (2, 'Tinta plastisol blanca', 'TPL-BLA-01', 'tintas', 8.0, 2.0, 'kg', 11.00),
                (3, 'Emulsión fotosensible', 'EMU-01', 'pantallas', 3.0, 1.0, 'kg', 28.00),
                (4, 'Pantalla 90x120 serigrafía', 'PAN-90-01', 'pantallas', 10.0, 4.0, 'ud', 15.00),
                (5, 'Camiseta blanca 160g', 'CAM-BLA-M', 'sustratos', 150.0, 30.0, 'ud', 3.20),
                (6, 'Camiseta negra 160g', 'CAM-NEG-M', 'sustratos', 80.0, 20.0, 'ud', 3.50),
                (7, 'Vinilo textil negro', 'VIN-TXT-NEG', 'vinilos', 25.0, 5.0, 'm²', 8.00),
                (8, 'Papel DTF A3', 'DTF-A3', 'consumibles', 500.0, 100.0, 'ud', 0.35),
                (9, 'Hilo poliéster negro 5000m', 'HIL-POL-NEG', 'bordado', 12.0, 3.0, 'ud', 6.50),
                (10, 'Quitatintas industrial', 'LIM-QUI-01', 'consumibles', 4.0, 1.0, 'L', 9.80)
                """);
        }
    }

    public static String getConfig(String clave) {
        try (var st = getConnection().prepareStatement("SELECT valor FROM config WHERE clave = ?")) {
            st.setString(1, clave);
            var rs = st.executeQuery();
            return rs.next() ? rs.getString("valor") : "";
        } catch (SQLException e) {
            return "";
        }
    }

    public static void setConfig(String clave, String valor) {
        try (var st = getConnection().prepareStatement(
                "INSERT OR REPLACE INTO config (clave, valor) VALUES (?, ?)")) {
            st.setString(1, clave);
            st.setString(2, valor);
            st.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al guardar configuración '" + clave + "': " + e.getMessage());
        }
    }

    public static String generarNumeroPresupuesto() {
        int sig = Integer.parseInt(getConfig("siguiente_presupuesto"));
        String prefijo = getConfig("prefijo_presupuesto");
        String anio = String.valueOf(java.time.LocalDate.now().getYear());
        setConfig("siguiente_presupuesto", String.valueOf(sig + 1));
        return String.format("%s-%s-%04d", prefijo, anio, sig);
    }

    public static String generarNumeroFactura() {
        int sig = Integer.parseInt(getConfig("siguiente_factura"));
        String prefijo = getConfig("prefijo_factura");
        String anio = String.valueOf(java.time.LocalDate.now().getYear());
        setConfig("siguiente_factura", String.valueOf(sig + 1));
        return String.format("%s-%s-%04d", prefijo, anio, sig);
    }
}
