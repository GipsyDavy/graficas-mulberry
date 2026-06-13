package org.gipsybuho.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

public class DatabaseManager {

    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static Connection connection;

    private static String buildDbUrl() {
        String override = System.getProperty("graficas.mulberry.db.url");
        if (override != null && !override.isBlank()) {
            return override;
        }
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
            connection = DriverManager.getConnection(buildDbUrl());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }

    public static void initialize() throws SQLException {
        Connection conn = getConnection();
        createTables(conn);
        runMigrations(conn);
        insertDatosIniciales(conn);
    }

    private static void runMigrations(Connection conn) {
        String[] migrations = {
            "ALTER TABLE clientes ADD COLUMN apellido TEXT",
            "ALTER TABLE empleados ADD COLUMN apellido TEXT",
            "ALTER TABLE clientes ADD COLUMN apellidos TEXT",
            "ALTER TABLE empleados ADD COLUMN apellidos TEXT",
            "UPDATE clientes SET apellidos = apellido WHERE apellidos IS NULL AND apellido IS NOT NULL",
            "UPDATE empleados SET apellidos = apellido WHERE apellidos IS NULL AND apellido IS NOT NULL",
            """
            CREATE TABLE IF NOT EXISTS albaranes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero TEXT UNIQUE NOT NULL,
                cliente_id INTEGER REFERENCES clientes(id),
                fecha TEXT NOT NULL,
                factura_id INTEGER REFERENCES facturas(id) ON DELETE SET NULL,
                pedido_id INTEGER REFERENCES pedidos(id) ON DELETE SET NULL,
                estado TEXT DEFAULT 'pendiente',
                observaciones TEXT,
                created_at TEXT DEFAULT (datetime('now'))
            )""",
            """
            CREATE TABLE IF NOT EXISTS lineas_albaran (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                albaran_id INTEGER REFERENCES albaranes(id) ON DELETE CASCADE,
                descripcion TEXT NOT NULL,
                cantidad INTEGER DEFAULT 1,
                unidad TEXT DEFAULT 'ud',
                orden INTEGER DEFAULT 0
            )""",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('prefijo_albaran', 'ALB')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('siguiente_albaran', '1')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('musica_playlist', '')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('musica_volumen', '50')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('musica_loop', '1')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('musica_autoplay', '0')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('ia_voz_activada', '0')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('tts_voz', 'Windows femenina')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('asistente_visual_activo', '1')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('asistente_visual_voz', '0')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('asistente_visual_personaje', 'Búho')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('asistente_visual_tamano', '58')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('asistente_visual_x', '24')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('asistente_visual_y', '24')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('asistente_visual_instalador_animado', '1')",
            "DELETE FROM config WHERE clave IN ('session_timeout_minutes','max_login_attempts','login_lockout_minutes')",
            "ALTER TABLE usuarios ADD COLUMN last_login TEXT",
            "ALTER TABLE usuarios ADD COLUMN security_question TEXT",
            "ALTER TABLE usuarios ADD COLUMN security_answer_hash TEXT",
            "ALTER TABLE tarifas ADD COLUMN usa_tiempo INTEGER DEFAULT 0",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('prefijo_pedido', 'PED')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('factura_vencimiento_dias', '30')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('factura_forma_pago', 'Transferencia')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('doc_logo_path', '')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('doc_pie_legal', '')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('doc_texto_presupuesto', '')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('doc_texto_factura', '')",
            "INSERT OR IGNORE INTO config (clave, valor) VALUES ('doc_texto_albaran', '')",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_clientes_nif ON clientes(nif)",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_empleados_nif ON empleados(nif)",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_materiales_referencia ON materiales(referencia)",
            "ALTER TABLE usuarios ADD COLUMN login_failed INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE usuarios ADD COLUMN login_locked_until TEXT",
            "ALTER TABLE usuarios ADD COLUMN recovery_failed INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE usuarios ADD COLUMN recovery_locked_until TEXT"
        };
        for (String sql : migrations) {
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
            } catch (SQLException e) {
                // Solo ignorar si la columna ya existe (ALTER TABLE ADD COLUMN repetido).
                // Cualquier otro error de migración se registra — no silenciar fallos reales.
                if (e.getMessage() == null || !e.getMessage().contains("duplicate column name")) {
                    System.err.println("Error en migración: " + e.getMessage() + " | SQL: " + sql.substring(0, Math.min(80, sql.length())));
                }
            }
        }
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
        // Si existe el esquema antiguo de auth (columna initial_admin), lo eliminamos
        try (ResultSet cols = conn.getMetaData().getColumns(null, null, "usuarios", "initial_admin")) {
            if (cols.next()) {
                try (Statement drop = conn.createStatement()) {
                    drop.execute("DROP TABLE IF EXISTS log_accesos");
                    drop.execute("DROP TABLE IF EXISTS usuarios");
                }
            }
        }

        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL,
                    permissions TEXT NOT NULL DEFAULT '',
                    created_at TEXT DEFAULT (datetime('now')),
                    last_login TEXT,
                    security_question TEXT,
                    security_answer_hash TEXT,
                    login_failed INTEGER NOT NULL DEFAULT 0,
                    login_locked_until TEXT,
                    recovery_failed INTEGER NOT NULL DEFAULT 0,
                    recovery_locked_until TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    apellidos TEXT,
                    tipo TEXT DEFAULT 'empresa',
                    nif TEXT UNIQUE,
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
                    apellidos TEXT,
                    nif TEXT UNIQUE,
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
                CREATE TABLE IF NOT EXISTS tarifa_tramos (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    tarifa_id      INTEGER NOT NULL REFERENCES tarifas(id) ON DELETE CASCADE,
                    tiempo_minutos INTEGER NOT NULL,
                    precio_tiempo  REAL    NOT NULL,
                    UNIQUE (tarifa_id, tiempo_minutos)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS materiales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    referencia TEXT UNIQUE,
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
                CREATE TABLE IF NOT EXISTS consumo_material_tecnica (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tecnica TEXT NOT NULL,
                    material_id INTEGER REFERENCES materiales(id) ON DELETE CASCADE,
                    cantidad_por_unidad REAL NOT NULL DEFAULT 0
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

            st.execute("""
                CREATE TABLE IF NOT EXISTS column_configs (
                    table_name TEXT NOT NULL,
                    column_name TEXT NOT NULL,
                    label TEXT NOT NULL,
                    base_column INTEGER DEFAULT 0,
                    visible INTEGER DEFAULT 1,
                    created_at TEXT DEFAULT (datetime('now')),
                    PRIMARY KEY (table_name, column_name)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS notas_calendario (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fecha TEXT NOT NULL,
                    titulo TEXT NOT NULL,
                    nota TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS pedidos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero TEXT UNIQUE NOT NULL,
                    cliente_id INTEGER REFERENCES clientes(id),
                    fecha TEXT NOT NULL,
                    fecha_entrega_prevista TEXT,
                    fecha_entrega_real TEXT,
                    estado TEXT DEFAULT 'pendiente',
                    descripcion TEXT,
                    importe_total REAL DEFAULT 0.0,
                    iva_porcentaje REAL DEFAULT 21.0,
                    notas TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS pagos_pedido (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pedido_id INTEGER REFERENCES pedidos(id) ON DELETE CASCADE,
                    concepto TEXT DEFAULT 'Pago',
                    importe REAL DEFAULT 0.0,
                    fecha_emision TEXT,
                    fecha_vencimiento TEXT NOT NULL,
                    forma_pago TEXT DEFAULT 'Transferencia bancaria',
                    estado TEXT DEFAULT 'pendiente',
                    fecha_pago TEXT,
                    notas TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS pagos_material (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    material_id INTEGER REFERENCES materiales(id) ON DELETE SET NULL,
                    proveedor TEXT,
                    numero_factura TEXT,
                    fecha_compra TEXT NOT NULL,
                    cantidad_comprada REAL DEFAULT 0,
                    unidad TEXT,
                    importe_total REAL DEFAULT 0,
                    forma_pago TEXT DEFAULT 'Contado',
                    fecha_vencimiento TEXT NOT NULL,
                    estado TEXT DEFAULT 'pendiente',
                    fecha_pago TEXT,
                    notas TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS albaranes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero TEXT UNIQUE NOT NULL,
                    cliente_id INTEGER REFERENCES clientes(id),
                    fecha TEXT NOT NULL,
                    factura_id INTEGER REFERENCES facturas(id) ON DELETE SET NULL,
                    pedido_id INTEGER REFERENCES pedidos(id) ON DELETE SET NULL,
                    estado TEXT DEFAULT 'pendiente',
                    observaciones TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS lineas_albaran (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    albaran_id INTEGER REFERENCES albaranes(id) ON DELETE CASCADE,
                    descripcion TEXT NOT NULL,
                    cantidad INTEGER DEFAULT 1,
                    unidad TEXT DEFAULT 'ud',
                    orden INTEGER DEFAULT 0
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
                ('prefijo_pedido', 'PED'),
                ('prefijo_albaran', 'ALB'),
                ('siguiente_presupuesto', '1'),
                ('siguiente_factura', '1'),
                ('siguiente_pedido', '1'),
                ('siguiente_albaran', '1'),
                ('musica_playlist', ''),
                ('musica_volumen', '50'),
                ('musica_loop', '1'),
                ('musica_autoplay', '0'),
                ('ia_voz_activada', '0'),
                ('tts_voz', 'Windows femenina'),
                ('asistente_visual_activo', '1'),
                ('asistente_visual_voz', '0'),
                ('asistente_visual_personaje', 'Búho'),
                ('asistente_visual_tamano', '58'),
                ('asistente_visual_x', '24'),
                ('asistente_visual_y', '24'),
                ('asistente_visual_instalador_animado', '1')
                """);

            // Datos de ejemplo: solo se insertan en la primera ejecución
            boolean primeraVez;
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM config WHERE clave='datos_ejemplo_insertados'")) {
                primeraVez = rs.next() && rs.getInt(1) == 0;
            }

            if (primeraVez) {
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

                st.execute("INSERT INTO config (clave, valor) VALUES ('datos_ejemplo_insertados','1')");
            }
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
        int sig = parseConfigInt("siguiente_presupuesto", 1);
        String prefijo = getConfig("prefijo_presupuesto");
        String anio = String.valueOf(java.time.LocalDate.now().getYear());
        setConfig("siguiente_presupuesto", String.valueOf(sig + 1));
        return String.format("%s-%s-%04d", prefijo, anio, sig);
    }

    public static String generarNumeroFactura() {
        int sig = parseConfigInt("siguiente_factura", 1);
        String prefijo = getConfig("prefijo_factura");
        String anio = String.valueOf(java.time.LocalDate.now().getYear());
        setConfig("siguiente_factura", String.valueOf(sig + 1));
        return String.format("%s-%s-%04d", prefijo, anio, sig);
    }

    public static String generarNumeroPedido() {
        int sig = parseConfigInt("siguiente_pedido", 1);
        String prefijo = getConfig("prefijo_pedido");
        String anio = String.valueOf(java.time.LocalDate.now().getYear());
        setConfig("siguiente_pedido", String.valueOf(sig + 1));
        return String.format("%s-%s-%04d", prefijo, anio, sig);
    }

    public static String generarNumeroAlbaran() {
        int sig = parseConfigInt("siguiente_albaran", 1);
        String prefijo = getConfig("prefijo_albaran");
        String anio = String.valueOf(java.time.LocalDate.now().getYear());
        setConfig("siguiente_albaran", String.valueOf(sig + 1));
        return String.format("%s-%s-%04d", prefijo, anio, sig);
    }

    private static int parseConfigInt(String clave, int defecto) {
        try { return Integer.parseInt(getConfig(clave)); } catch (NumberFormatException e) { return defecto; }
    }

    public static void addColumn(String tabla, String columna) throws SQLException {
        requireSqlIdentifier(tabla);
        requireSqlIdentifier(columna);
        try (Statement st = getConnection().createStatement()) {
            st.execute("ALTER TABLE " + quoteIdentifier(tabla) + " ADD COLUMN " + quoteIdentifier(columna) + " TEXT");
        }
    }

    public static void dropColumn(String tabla, String columna) throws SQLException {
        requireSqlIdentifier(tabla);
        requireSqlIdentifier(columna);
        try (Statement st = getConnection().createStatement()) {
            st.execute("ALTER TABLE " + quoteIdentifier(tabla) + " DROP COLUMN " + quoteIdentifier(columna));
        }
    }

    public static String quoteIdentifier(String identifier) {
        requireSqlIdentifier(identifier);
        return "\"" + identifier + "\"";
    }

    public static void requireSqlIdentifier(String identifier) {
        if (identifier == null || !SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Identificador SQL no válido: " + identifier);
        }
    }
}
