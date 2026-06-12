# HANDOFF — Graficas Mulberry · CLIENTE-GATE en curso/casi cerrado
# Versión: 5.2 · Fecha revisión documentación: 12/06/2026 · Último commit funcional: c144f06 · Tests: 127/127 verdes. Cola activa: prueba manual UI import + Gemini gate + Sprint MIGRACION-COMPLEJA.

---

## ESTADO VIVO 2026-06-12 — CLIENTE-GATE

Decisión técnica actual: **apto para valoración controlada del cliente final**,
pero **no entrega final cerrada** hasta completar prueba manual UI de importación
y registrar segunda opinión Gemini o dejarla marcada como pendiente aceptada.

Validación hecha:
```powershell
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
.\mvnw.cmd test
# 127/127 verdes

.\mvnw.cmd clean package -DskipTests
# BUILD SUCCESS
# target\GraficasMulberry-13.5.0.jar
```

Commit nuevo de seguridad:
- `c144f06` — `security(auth): unificar minimo password`

Qué hizo:
- `AuthService.MIN_PASSWORD_LENGTH = 8`.
- `AuthService.isPasswordValid(...)`.
- Defensa en servicio para `registerUser`, `changePassword`,
  `resetPasswordAdmin`, `resetPasswordWithAnswer`.
- UI usa la misma constante en alta admin, login/reset y gestión usuarios.
- `AuthServiceTest` añade 3 tests contra contraseñas cortas.

Security review local:
- VibeSec aplicado a auth, import/export, SQL dinámico, datos sensibles, roles y Ollama.
- Sin P0/P1 nuevo detectado.
- Abiertos no bloqueantes: `SEC-NEW-4`, `SEC-NEW-5`, `COD-NEW-2`.

Documentación nueva:
- `CLIENTE_GATE.md` registra checklist, resultado, bloque Gemini y decisión.
- `continuar.md` actualizado con handoff vivo.

Siguiente paso:
1. Ejecutar `.\mvnw.cmd javafx:run`.
2. Probar manualmente importación con los Excel reales.
3. Pegar bloque Gemini de `CLIENTE_GATE.md` y registrar respuesta.

---

## CÓMO TRATARME

- **Tuteo siempre.** No "usted", no "señor".
- **Crítica sin envolver.** No buscar agradar. Si una decisión mía es mala, dilo directo.
- **Defender la solución correcta sobre la fácil.** No es válido "es más rápido aceptar lo que ya hay" si lo que hay está mal.
- **No pedir permiso para cada paso.** Avanza. Si algo es ambiguo, decide con criterio y declara la asunción.
- **Si te equivocas, recula sin ceremonia.** Una disculpa breve y sigues. No hay que justificarse en exceso.
- **Verifica antes de juzgar.** Lección dura: di por hecho que `Pedido.fecha` era String sin mirar el modelo, y casi se revierte un parseo necesario por mal diagnóstico. Antes de afirmar que una decisión de un agente es mala, lee el código que la motiva.
- **Declara nombres exactos.** Lección 2C: confundí `UPDATE_IF_EXISTS` con `UPDATE_EXISTING` por memoria. El nombre real del enum se verifica leyendo el archivo, no recordando. Aplica a enums, métodos, paquetes, constantes.
- **Verifica que el código compilaría antes de dictarlo.** Lección 3C (dos veces): reescribí `EntityImportSpec` perdiendo los genéricos de `List<FieldSpec>` y luego dicté un `switch` expression sin brazos que devuelvan valor. Ambos fallos triviales en `javac` pero ambos pasaron al bloque sin filtrar. Antes de dictar Java nuevo, tirar mentalmente del compilador.
- **No copiar respuestas viejas del agente.** Lección del fix de 3C-paso-2b: el usuario pegó por error la salida del turno anterior idéntica al actual. Si dos salidas son carácter por carácter iguales, pedir verificación (`findstr` o `type` del archivo afectado) antes de actuar.
- **Antes de declarar emergencia, leer `git log`.** Si `git status` dice algo raro, `git log --oneline` lo confirma o lo desmiente antes que cualquier comando destructivo. No entrar en pánico por un `nothing added to commit` si no se ha mirado el log primero.
- **Pide el archivo antes de redactar bloques sobre métodos concretos.** Lección 3C-paso-2c, 3C-paso-3a, Bloque 4a, reforzada en Bloque 5a.1 y RE-REFORZADA en Sprint B 1c (`FacturaDAO`): no fiarse del recuerdo del handoff sobre la forma exacta de un método o API si la sesión actual no lo ha leído. Pedir y leer es barato; redactar a ciegas un bloque carácter-por-carácter es caro.
- **Releer el bloque redactado antes de pegarlo.** Lección 3C-paso-3a + Sprint B 1c: detecté basura en mi propia redacción al releer (ternario inútil, línea sentinela rara, bloque try-with-resources duplicado + roto). Releer una vez antes de dar por bueno el bloque cazó ambos casos. **Sigue siendo la lección que más se me cuela.**
- **Si un archivo aparece como modificado al arrancar sesión sin commit previo identificable, declararlo explícitamente.** Lección 3C-paso-3b. En el primer pase de cada sesión, verificar `git diff --stat` y declarar scope del archivo no-commiteado antes de empezar a editar nuevos archivos.
- **Si Codex declara un cambio funcional no pedido, parar y pedir el diff antes de aprobar.** Lección 3C-paso-3a. Revisar diff antes del commit y, si el cambio se acepta, declararlo como decisión explícita.
- **Codex prefiere inserción aditiva sobre reemplazo cuando puede.** Lección 4a.1. **Truco aplicado en 4b/5b/6 y validado en todo Sprint B y Sprint D:** incluir las líneas a preservar tanto en `old_str` como en `new_str` blinda el resultado.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Lección 4a.2. Alternativas robustas:
  - `findstr /N /C:"X \"Y\"" archivo` (con `/C:` literal)
  - `Select-String -Path 'archivo' -Pattern 'X .Y.'` (regex, el `.` cubre la comilla)
- **Si una "deuda latente" toca escritura de FK y los tests sintéticos arrancan con `PRAGMA foreign_keys=ON`, NO es latente: es bug seguro.** Lección 4a.2 final.
- **`Nothing to compile - all classes are up to date` NO es prueba de que compila.** Lección 4a.1. Tras editar un archivo, usar `.\mvnw.cmd clean compile` para forzar recompilación real desde cero.
- **Si un `clean compile` falla con `Failed to delete` en `target/classes`, es bloqueo de archivo Windows.** Lección 5a.1. Solución antes de Maven: `Get-Process java | Stop-Process -Force`.
- **No instanciar APIs sin haberlas leído.** Lección 5a.1. Antes de dictar `new X(...)`, leer X.java o un archivo que ya lo use bien.
- **SQLite no aplica DEFAULT cuando se pasa NULL explícito vía `setString`/`setInt`.** Lección 5a.3. **REFORZADA en Sprint D pre-bloque 1a:** ni `setString(n, null)` ni `setNull(n, Types.X)` disparan el DEFAULT. El DEFAULT DDL solo se aplica si la columna se OMITE del INSERT. Patrón canónico cuando se quiere preservar DEFAULT: `setString(n, getX() != null ? getX() : "literal_default")`. Patrón validado en 3 DAOs del Sprint D.
- **No diagnosticar deudas técnicas sin leer el código que las realizaría.** Lección Sprint B análisis (Deuda 9 mal etiquetada). Antes de afirmar "X está roto", leer el código que haría X.
- **Cuando el usuario aporta información casual interpretarla con cuidado.** Sesión Sprint A. Si la frase del usuario es ambigua, preguntar antes de actuar.
- **Las capturas de UI contienen información que reemplaza preguntas.** Sesión Sprint A. Si una captura puede responder, pedirla en vez de pedir descripción manual.
- **NUEVA (Sprint B 1c primer intento):** El truco "corromper datos vía UPDATE para forzar fallo en cadena" no funciona si la BD tiene `NOT NULL` o constraints estrictos. SQLite rechaza el UPDATE igual que el INSERT. **Patrón válido para forzar fallos en tests de rollback de cadenas DAO: colisión `UNIQUE`.** Pre-crear entidad con número conocido + resetear `siguiente_X` en config + invocar `crearDesde*` que vuelve a generar el mismo número y choca contra el UNIQUE en INSERT.
- **NUEVA (Sprint B análisis):** El override `graficas.mulberry.db.url` en `DatabaseManager.buildDbUrl()` es la pieza clave del harness JDBC de tests. Permite redirigir el singleton a una BD efímera (`@TempDir`) sin tocar `DatabaseManager.java`. Patrón canónico de harness:
```java
  @BeforeEach setUp(): closeConnection() + setProperty + initialize()
  @AfterEach tearDown(): closeConnection() + clearProperty
```
- **NUEVA (Sprint B 1b validado):** "Read your own writes" dentro de la misma Connection en SQLite funciona. Dentro de una tx abierta, un `SELECT` ve los cambios pendientes de esa misma tx. Detalle archivado para futuros tests de rollback.
- **NUEVA (Sprint B planning):** Si un sub-bloque encadena dependencias con otros, reordenar para que cada commit deje el código sin bug latente. Sprint B: MaterialDAO debía ir antes que los `crearDesde*` que lo invocan porque hasta el refactor de `ajustarStock`, un `crearDesde*` envuelto en tx lo habría roto. El orden del handoff original no respetaba esto y se ajustó.
- **NUEVA (Sprint D pre-bloque 1a):** Caso flagrante de "decisión cerrada por análisis sin verificar la física del motor". Cerré opción A (`setNull`) tras 6 decisiones de diseño antes de redactar el primer bloque, sin comprobar la semántica real de SQLite con DEFAULT. Codex me lo detectó al leer el INSERT. **Regla derivada:** antes de cerrar una decisión de patrón sobre un comportamiento del motor de BD, verificar el comportamiento real (doc o test aislado), no asumirlo por intuición o por analogía con otros motores. La opción A funcionaría en PostgreSQL/MySQL en strict mode; no en SQLite.
- **NUEVA (Sprint D pre-bloque 1a):** Cuando un DEFAULT DDL es una cadena larga (`"Presupuesto válido por 30 días. Precios sin IVA."`), extraer constante `private static final` en el DAO con comentario referenciando `DatabaseManager.createTables()`. Para cadenas cortas (`"borrador"`, `"pendiente"`, `"Transferencia bancaria"`), literal inline es aceptable. Criterio aplicado en Sprint D.

---

## CIERRE DE SESIÓN 2026-06-12 — IMPORT-ADAPTIVE

### Estado Git
- Rama: `master`.
- Último commit funcional antes de documentación: `63c6592` — `fix(import): reconocer cabeceras comunes en documentos`.
- Working tree funcional al cierre: solo `.claude/settings.local.json` modificado fuera de alcance.
- Commits de la sesión:
  - `322ff50` — `docs: eliminar regla de inicio obsoleta`.
  - `bca51b2` — `fix(import): normalizar tablas laterales`.
  - `83d2018` — `feat(import): expandir matrices de precios`.
  - `e5f30d1` — `fix(import): acotar pivot a matrices de tarifas`.
  - `c871a3b` — `fix(import): permitir mapear lineas parent-child`.
  - `63c6592` — `fix(import): reconocer cabeceras comunes en documentos`.

### Validación
- `.\mvnw.cmd test -q` — 121/121 verdes.
- Probe temporal de solo lectura retirado antes de cerrar.
- Validación real de parser/mapeo:
  - `01_TARJETAS_DE_VISITA.csv` -> 56 filas normalizadas.
  - `02_FOLIOS.xlsx` -> 18 filas normalizadas.
  - `20_CALENDARIOS.xlsx` -> 259 filas normalizadas.
  - `07_MATERIAL.xlsx` -> tabla plana regional, sin pivot indebido.
  - `smoke_albaran.csv` -> `ALBARANES=7/7 requiredMissing=[]`.

### Cambios funcionales cerrados
- `ImportService` detecta regiones y normaliza tablas laterales.
- Matrices de precios de Tarifas se expanden a filas normalizadas: `TECNICA`, `NOMBRE`, `MINIMO_UNIDADES`, `PRECIO_UNIT`.
- El pivot queda acotado a matriz real de Tarifas para no romper Materiales u otros módulos.
- `ColumnMappingDialog` expone campos de líneas en specs parent-child (`Presupuestos`, `Facturas`, `Albaranes`).
- Los documentos reconocen cabeceras comunes: `nif/cif/dni` como `cliente_nif`; `Albaran` reconoce `numero`.

### Security review
- No se añadió SQL.
- No se toca import backup.
- No se ejecuta contenido de CSV/XLSX/XLSB.
- Ollama no es autoridad final; Java mantiene validación local y bloqueo de obligatorios.
- Los cambios amplían mapeos hacia campos existentes y siguen pasando por `EntityImportService`.

### Próximo paso recomendado
1. Validación manual final en la app:
   - `Tarifas > Importar`: `01_TARJETAS_DE_VISITA`, `02_FOLIOS`, `20_CALENDARIOS`.
   - `Materiales > Importar`: `07_MATERIAL.xlsx` y CSV de `Desktop\files`.
   - `Albaranes > Importar`: `smoke_albaran.csv`.
2. Después, retomar `Sprint MIGRACION-COMPLEJA` para libros humanos con secciones internas (`NUEVAS TARIFAS...xlsb`, `PRECIOS PAPEL PROVEEDORES Formulas.xlsx`).

---

## CONTEXTO DEL PROYECTO

**Nombre:** Graficas Mulberry
**Ruta local:** `C:\Users\GipsyDavy\MAVEN\Graficas Mulberry`
**Stack:** Java 21 · JavaFX 21 · SQLite (vía JDBC) · Maven · JUnit 5
**Restricciones duras:** sin Lombok, sin Spring, sin servidores HTTP, sin nuevas dependencias salvo justificación.
**Build commands:** `.\mvnw.cmd compile`, `.\mvnw.cmd clean compile`, `.\mvnw.cmd test`, `.\mvnw.cmd package`, `.\mvnw.cmd javafx:run`. `mvn` directo NO está en el PATH; usar siempre el wrapper.
**Versión Maven:** 3.9.11 vía wrapper.
**Reglas operativas:** ver `CLAUDE.md`. Karpathy-style: cambios quirúrgicos, mínima modificación, YAGNI, no refactorizar lo que no está roto, código auto-documentado.

### Estructura relevante
- `src/main/java/org/gipsybuho/model/` — entidades de dominio.
- `src/main/java/org/gipsybuho/dao/` — acceso a datos JDBC.
- `src/main/java/org/gipsybuho/service/` — lógica de servicios. `EntityImportService` aquí.
- `src/main/java/org/gipsybuho/service/importer/` — tipos auxiliares del importador.
- `src/main/java/org/gipsybuho/ui/` — vistas JavaFX.
- `src/test/java/org/gipsybuho/dao/` — tests JDBC reales con BD efímera por `@TempDir`.
- `src/test/java/org/gipsybuho/service/importer/` — tests del importador.
- `src/main/java/org/gipsybuho/db/DatabaseManager.java` — DDL inline en Java. Activa `PRAGMA foreign_keys = ON`. **Connection es singleton estático.** **Override de URL vía system property `graficas.mulberry.db.url`** (línea 17-20).

### Documentos del proyecto que NO debes ignorar
- `CLAUDE.md` — reglas operativas, Multi-IA, cambios quirúrgicos.
- `MIGRACION_HISTORICO.md` — actualizado en Bloque 6 del sprint anterior (`74f174c`).

### Valores conocidos del enum `DuplicatePolicy`
`SKIP_IF_EXISTS` (default), `UPDATE_EXISTING`, `CREATE_NEW`. **NO existe `UPDATE_IF_EXISTS`** — error frecuente.

### Forma de `DatabaseManager.getConnection()`
```java
private static Connection connection;
public static Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection(buildDbUrl());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
    }
    return connection;
}
```
**Implicación crítica:** todos los DAOs y el servicio comparten el mismo objeto Connection. La atomicidad cross-DAO funciona porque `setAutoCommit(false)` aplica al singleton y los DAOs heredan el estado. **Tras Sprint B, los DAOs detectan tx externa explícitamente y respetan el contrato.**

### Patrón transaccional canónico — IMPLEMENTADO en todos los DAOs de Sprint B
```java
Connection conn = DatabaseManager.getConnection();
boolean externalTx = !conn.getAutoCommit();
if (!externalTx) conn.setAutoCommit(false);
try {
    // trabajo SQL
    if (!externalTx) conn.commit();
} catch (SQLException e) {
    if (!externalTx) conn.rollback();
    throw e;
} finally {
    if (!externalTx) conn.setAutoCommit(true);
}
```
Aplicado en: `PresupuestoDAO.save`, `FacturaDAO.save`, `FacturaDAO.crearDesdePresupuesto`, `AlbaranDAO.save`, `AlbaranDAO.crearDesdeFactura`, `AlbaranDAO.crearDesdePresupuesto`, `MaterialDAO.ajustarStock`.

### Patrón "preservar DEFAULT DDL" — IMPLEMENTADO en columnas TEXT del Sprint D
```java
// Coincide con DEFAULT DDL: 'X'. Ver DatabaseManager.createTables() tabla 'Y'.
// SQLite no aplica DEFAULT cuando se inserta NULL explícito, solo si la columna se omite del INSERT.
ps.setString(n, modelo.getCampo() != null ? modelo.getCampo() : "X");
```
Aplicado en 5 sitios: `PresupuestoDAO.set` (estado, condiciones), `FacturaDAO.set` (estado, forma_pago), `AlbaranDAO.set` (estado). Para cadenas largas (`condiciones`), extraída constante `private static final` en el DAO. **Aplica solo a columnas TEXT.** Defaults numéricos primitivos (ej. `iva_porcentaje DEFAULT 21.0` pisado por `double = 0.0`) NO están cubiertos por Sprint D — requieren cambio de modelo (`double`→`Double`) con blast radius mayor.

### Override de URL para tests JDBC reales
`DatabaseManager.buildDbUrl()` lee `System.getProperty("graficas.mulberry.db.url")` antes de calcular la ruta default en `%LOCALAPPDATA%`. **Esto desbloquea harness JDBC con BD efímera por `@TempDir` sin tocar DatabaseManager.**

Patrón canónico de tests JDBC (replicado en 5 tests del Sprint B + 3 tests nuevos del Sprint D y existente en los `EntityImportService*Test`):
```java
@TempDir Path tempDir;

@BeforeEach void setUp() throws Exception {
    DatabaseManager.closeConnection();
    System.setProperty("graficas.mulberry.db.url", "jdbc:sqlite:" + tempDir.resolve("test.db"));
    DatabaseManager.initialize();
}

@AfterEach void tearDown() {
    DatabaseManager.closeConnection();
    System.clearProperty("graficas.mulberry.db.url");
}
```

---

## WORKFLOW MULTI-IA

**No hay invocación directa por CLI desde mí (Claude consumer).** El flujo es:

1. **Yo (Claude consumer en el chat)** coordino el sprint, redacto bloques autocontenidos.
2. **Tú** pegas el bloque en el chat del agente correspondiente dentro de IntelliJ IDEA.
3. **El agente del IDE** (Claude Code / Codex / Gemini) ejecuta.
4. **Tú** me pegas la respuesta del agente y yo evalúo si seguir, corregir o pedir fix-up.

**Roles según `CLAUDE.md`:**
- **Claude Code (en IDE):** preferente para planificación, revisión final, calidad, seguridad, tests, cumplimiento de reglas.
- **Codex (en IDE):** edición local, ejecución de comandos, parches quirúrgicos. Ejecutor de bloques blindados. Tendencia confirmada a inserción aditiva sobre reemplazo. **Validado en todo Sprint B y Sprint D**: el truco "líneas idénticas en old_str y new_str" funciona sistemáticamente. **Codex detectó por sí solo en Sprint D 1a que la decisión de patrón era inválida tras leer el pre-requisito de lectura.** Confirma valor del pre-requisito de lectura explícito.
- **Gemini (en IDE):** contexto amplio, arquitectura, segunda opinión. Usado UNA VEZ en Bloque 5a para dictamen `BEGIN DEFERRED`. **No usado en Sprint B ni Sprint D.**

**Cómo redactar bloques para los agentes — lecciones consolidadas:**
- Instrucciones cerradas, sin espacio interpretativo.
- Restricciones negativas explícitas.
- Criterio de éxito verificable sin reejecutar.
- Una sola tarea por bloque (o sub-paso).
- Trazabilidad obligatoria al final.
- Declarar nombres literales de enums, métodos, paquetes.
- Verificar que el snippet de Java sería aceptado por `javac` aislado.
- Añadir `git diff --stat` al criterio de éxito.
- Pedir el archivo objetivo antes de redactar bloques no triviales.
- Releer el bloque redactado antes de darlo por bueno.
- Reescritura completa sobre N `str_replace` para edits extensos.
- Inserciones puras a reemplazos cuando las líneas pueden conservarse (truco "líneas idénticas en old_str y new_str").
- **NUEVA Sprint B:** Bloque debe incluir pre-requisito de lectura explícito ("LEE Y VERIFICA") antes de las ediciones. Codex puede entonces parar y reportar si encuentra discrepancia en setters o constraints. **VALIDADO en Sprint D 1a**: el pre-requisito atrapó una decisión de patrón inválida antes de tocar archivos.
- **NUEVA Sprint D:** Para fixes que dependen de la semántica de un motor de BD, el bloque debe declarar explícitamente la semántica asumida en el "Contexto" para que el agente pueda contradecirla si no es cierta. Ej.: "SQLite NO aplica DEFAULT con NULL explícito" → si Codex sospecha lo contrario, lo verifica leyendo doc/código y reporta antes de editar.

**Convención de commits:** un bloque = un commit + push. Mensaje con título imperativo (`feat:`, `fix:`, `docs:`, `test:`) ≤72 chars, línea en blanco, cuerpo con párrafos. Editor configurado: `git config --global core.editor "notepad"`. Multilínea complejo: archivo temporal + `git commit -F archivo.txt`.

---

## AUDITORÍA TÉCNICA COMPLETA — COMPLETADA (2026-06-02)

Auditoría de 8 fases realizada con Claude Code + Gemini + Codex. Resultado: `INFORME-FINAL.md`.

**Hallazgos principales:**
- **P0 (crítico):** SEC-2 — `changePassword` sin verificar contraseña actual.
- **P1 (altos):** SEC-1 UserDAO cierra singleton, SEC-4 mínimo contraseña sin validación ejecutable, SEC-5 runMigrations silencia todo, SEC-6 fallback rol COMERCIAL.
- **P2 (medios):** ARCH-1, COD-2 dead code SQL incorrecto, UI-4 colores hardcodeados en Dashboard, UI-5 IAView sin CommandBar.
- **P3 (backlog):** COD-1/3/4, UI-1/2/3, AP-1, ARCH-2/4. Ver `INFORME-FINAL.md`.

**Tests en esa auditoría:** 72/72 ✅ · **Estado histórico:** superado por sprints posteriores. Estado vigente: 121/121 verdes.
**Archivos nuevos:** `AUDITORIA.md`, `INFORME-FINAL.md`, `FASES.md`, `MACRO-PROMPT-GRAFICAS-MULBERRY.md`, `interfaz.md`, `continuar.md`, `_cajon-desastre/` (20 archivos movidos).

---

## SPRINTS PREVIOS — RESUMEN

- **Sprint Importación CSV** (handoff v3.6, HEAD `74f174c`): 9 entidades importables, 56 tests verdes (2+12+11+9+5+10+7).
- **Sprint A — Smoke Test manual de Albaranes** (sin commit de código): PASA. 3 deudas nuevas registradas (21, 22, 23).
- **Sprint B — Transacciones explícitas en DAOs** (handoff v3.8, HEAD `fbc6fc8`): 5 commits, 12 tests nuevos (9 unit + 3 cross-DAO), 68/68 verdes. Deuda 9 cerrada.
- **Sprint IMPORT-UPGRADE** (HEAD `4bc6c9c`): XLSB/XLSM via `WorkbookFactory`; `EntityImportSpec.tableName()`; `procesarFila()` → `int[3]`; escritura de columnas dinámicas en import; `ColumnMappingDialog` + "➕ Nuevo campo…"; 10 vistas actualizadas. 89/89 verdes. Sin nuevas deudas.

---

## SPRINT ACTUAL — DEFAULTS DDL EN COLUMNAS TEXT (Sprint D) — CERRADO

### Resultado
**ÉXITO COMPLETO.** 3 commits, 3 métodos `set` modificados, 3 tests nuevos, 71/71 verdes al cierre. Cero regresiones.

### Hallazgo principal del análisis

La Deuda 20 estaba parcialmente mal descrita. Realidad:
1. **El bug se manifiesta con `setString(n, null)` Y con `setNull(n, Types.VARCHAR)`** — ambos envían NULL explícito a SQLite, que no aplica el DEFAULT en ninguno de los dos casos.
2. **La "mitigación 5a.3 en Albarán" del handoff v3.7 NO era mitigación arquitectónica**, era setear `estado="pendiente"` explícitamente en los callers `crearDesdeFactura`/`crearDesdePresupuesto`. El bug en `AlbaranDAO.set` persistía para cualquier otro caller (UI directa, futuros). Sprint D 1c lo arregló en el setter.
3. **Defaults numéricos primitivos (`iva_porcentaje DEFAULT 21.0` pisado por `double = 0.0`) NO están cubiertos.** Son técnicamente del mismo bug pero requieren cambio de modelo (`double`→`Double` boxed) con blast radius mayor. Quedan fuera de scope, registrados como Deuda 20-bis.
4. **DAOs no auditados en Sprint D** (PedidoDAO, ClienteDAO, EmpleadoDAO, MaterialDAO escritura inicial, NominaDAO, PagoPedidoDAO, PagoMaterialDAO) tienen DEFAULTs DDL no triviales en sus tablas. **No se sabe si están afectados.** Quedan fuera de scope, registrados como Deuda 20-ter.

### Decisiones de diseño cerradas

| ID | Decisión | Valor cerrado | Notas finales |
|---|---|---|---|
| D-D-PATTERN-v2 | Patrón para preservar DEFAULT en columna TEXT | **C (literal Java-side con ternario + comentario referenciando DDL)** | Opción A (`setNull`) descartada tras pre-bloque 1a — Codex detectó que NULL explícito pisa DEFAULT en SQLite. |
| D-D-CONSTANTE | ¿Constante `private static final` para cadenas largas? | **Sí solo si la cadena es larga** | Aplicado solo a `condiciones` en `PresupuestoDAO`. Estado/forma_pago: literal inline. |
| D-D-COMENTARIO | ¿Comentario inline referenciando DDL? | **Sí, en cada uno de los 5 sitios** | Comentario explica el porqué (SQLite no aplica DEFAULT con NULL explícito) + dónde está el DDL. |
| D-D-SCOPE-IVA | ¿Incluir `iva_porcentaje` primitivo? | **No** | Fuera de scope. Requiere cambio `double`→`Double` con blast radius mayor. Registrado como Deuda 20-bis. |
| D-D-SCOPE-DAOS | ¿Solo 3 DAOs de Sprint B, o todos? | **Solo Presupuesto/Factura/Albaran** | Karpathy: cerrar lo identificable. Resto registrado como Deuda 20-ter. |
| D-D-TESTS | ¿Tests de verificación? | **Uno por DAO** | Construir entidad inline (sin helper que setee los campos), llamar save, recargar, asertar contra los literales del DDL. |
| D-D-COMMITS | ¿Un commit por DAO? | **Sí** | Tres commits, simetría con Sprint B. |
| D-D-ORDEN | Orden de los tres | **Presupuesto → Factura → Albarán** | Sin dependencias entre ellos. Orden por simetría con el handoff. |

### Sub-bloques ejecutados

| Sub-bloque | Commit | Cambios | Tests | Notas |
|---|---|---|---|---|
| **1a** | `0041fa2` | `PresupuestoDAO.set` preserva DEFAULT en estado + condiciones. Constante `DEFAULT_CONDICIONES` añadida. | +1 (`PresupuestoDAOTest`: 2→3) | **Pre-bloque atrapó decisión de patrón inválida.** Original era opción A (`setNull`); Codex reportó que SQLite no aplica DEFAULT con NULL explícito. Replanteado a opción C antes de tocar archivos. |
| **1b** | `e434d31` | `FacturaDAO.set` preserva DEFAULT en estado + forma_pago. Sin constante (cadenas cortas). | +1 (`FacturaDAOTest`: 2→3) | Patrón ya validado en 1a, ejecución limpia. |
| **1c** | `8ef936a` | `AlbaranDAO.set` preserva DEFAULT en estado. | +1 (`AlbaranDAOTest`: 3→4) | El más corto: una sola columna. La "mitigación 5a.3" del handoff v3.7 era solo a nivel caller, no arquitectónica. Sprint D 1c fija el setter; los `a.setEstado("pendiente")` redundantes en `crearDesde*` se mantienen para no ampliar blast radius. |
| **1d** | (este handoff) | `Resumen.md` v3.9 | 0 | Cierre documental. |

### Errores cometidos en esta sesión (para no repetirlos)

1. **Cerré 6 decisiones de diseño tras un análisis "exhaustivo" sin verificar la semántica real de SQLite con DEFAULT y NULL explícito.** La opción A (`setNull`) parecía válida por intuición y por analogía con otros motores. Codex la detectó como inválida al leer el INSERT en el pre-requisito. **El pre-requisito de lectura salvó el sprint.** Lección archivada como nueva regla en CÓMO TRATARME.
2. **No comprobé red-green del primer test (1a).** No se verificó que el test fallara antes del fix. El test pasa ahora, pero queda residual la duda "¿funciona el fix o el test mide algo trivialmente cierto?". Para próximos sprints con test-first, considerar exigir red-green al menos en el primer sub-bloque.

### Archivos modificados en esta sesión

- `src/main/java/org/gipsybuho/dao/PresupuestoDAO.java` — constante DEFAULT_CONDICIONES + 2 ternarios en `set`.
- `src/test/java/org/gipsybuho/dao/PresupuestoDAOTest.java` — +1 test.
- `src/main/java/org/gipsybuho/dao/FacturaDAO.java` — 2 ternarios en `set`.
- `src/test/java/org/gipsybuho/dao/FacturaDAOTest.java` — +1 test.
- `src/main/java/org/gipsybuho/dao/AlbaranDAO.java` — 1 ternario en `set`.
- `src/test/java/org/gipsybuho/dao/AlbaranDAOTest.java` — +1 test.

### Helpers conocidos en los tests JDBC (acumulados sobre Sprint B)

- **`PresupuestoDAOTest`:** `crearCliente()`, `nuevoPresupuesto(int clienteId, String numero)`, `linea(String desc, int cant, double precio)`, `lineaInvalida()`.
- **`FacturaDAOTest`:** `crearCliente()`, `nuevoPresupuesto(int clienteId, String numero)`, `nuevaFactura(int clienteId, String numero)`, `lineaPresupuesto(String desc, int cant, double precio)`, `lineaFactura(String desc, int cant, double precio)`, `lineaFacturaInvalida()`.
- **`AlbaranDAOTest`:** `crearCliente()`, `nuevoAlbaran(int clienteId, String numero)`, `nuevaFactura(int clienteId, String numero)`, `nuevoPresupuesto(int clienteId, String numero)`, `lineaAlbaran(String desc, int cant)`, `lineaAlbaranInvalida()`, `lineaFactura(String desc, int cant, double precio)`, `lineaPresupuesto(String desc, int cant, double precio)`.

**Importante:** los helpers `nuevoX(...)` setean campos por defecto (estado, iva, etc.). Para tests que necesitan campos null, construir la entidad inline en el test, no usar el helper.

---

## ESTADO TÉCNICO AL CIERRE

### Git
- **Rama:** `master`
- **Último commit funcional:** `63c6592` — `fix(import): reconocer cabeceras comunes en documentos`.
- **Working tree funcional:** solo `.claude/settings.local.json` modificado fuera de alcance. Documentación de cierre pendiente hasta commit `docs`.

### Commits de sprints relevantes
| Sprint | Commits | Descripción |
|--------|---------|-------------|
| Sprint D-ter | `5f50954`, `8c2c0d1`, `cfb83fa`, `d19a342` | EmpleadoDAO, MaterialDAO, PagoMaterialDAO, ClienteDAO |
| Sprint SEC | `8060734`, `1c892ea`, `f1e9b01`, `7cd651d` | 5 correcciones de seguridad P0/P1 |
| Sprint COD | `2dd1f11` | Dead code eliminado en AppConstants |
| Sprint UI-A | `467a2b5`, `cc0b2c2`, `5718971` | CSS variables, nuevos componentes, theme-mulberry |
| Sprint UI-B | `632e355`, `ef7ec17` | btn() color param, FadeTransition, AppConstants.APP_VERSION |
| Sprint UI-C | `34bca4c` | IAView command-bar + audit EstadisticasView + UserManagementView |
| Sprint C | `1851216` | Fix resolverEmpleadoId filtro activo=1 (Deuda 2) |
| Deuda 24 | `acc81a3` | 15 tests JDBC: EmpleadoDAO, NominaDAO, PagoMaterialDAO, PedidoDAO, PagoPedidoDAO |
| Sprint UI-D | `d4109c2` | .skeleton-row CSS + ProgressIndicator overlay en Clientes, Facturas, Pedidos |
| Docs estado real | `ccb8e35`, `90413e4` | Actualización documental post UI-D y protocolo obligatorio de inicio |
| Docs Selene / migración compleja | `67ae150` | Consolidación documental y eliminación de manuales antiguos en `installer/` |
| **Sprint IMPORT-UPGRADE** | **`4bc6c9c`** | **XLSB/XLSM via WorkbookFactory; entityId en procesarFila(); escritura columnas dinámicas; ColumnMappingDialog + "➕ Nuevo campo…"; filtros 10 vistas** |
| **Sprint HELP-0** | **`39d060e`** | **HELP-SPEC.md: taxonomía 8 cat., 19 módulos, 81 artículos ★, formato HTML, index.json schema, HelpEntry record, HelpService API, mapa F1** |
| **Sprint HELP-1** | **`65588cf`** | **81 artículos HTML offline + help.css + index.json en 19 módulos (General×5, Importación×9, Backups×5, Clientes×5, Materiales×4, Empleados×4, Presupuestos×6, Facturas×6, Albaranes×5, Pedidos×4, Nóminas×3, Tarifas×3, Exportación×3, IA×5, Asistente×2, Estadísticas×2, Calendario×2, Usuarios×5, Configuración×3)** |
| **Sprint COLUMN-TYPES** | **`e12a687`→`d4db11b`** | **Tipos de dato (TEXTO/NUMÉRICO/PRECIO/FECHA) en columnas dinámicas. Hard delete columnas usuario. Ocultar/mostrar columnas base. Botón "Tipo…" para editar tipo en columnas existentes. DatePicker y TextField filtrado en formularios.** |
| **Sprint WIZARD-VALIDATION** | **`988a8fb`→`979cd06`** | **Paso 3.5 en wizard: validación IA (Ollama) + local de primeras 20 filas. ValidationIssue record. Tabla de incidencias con "🤖 Corregir con IA" / "Ignorar". Bloqueo de Continuar mientras haya ERRORs. Fix: issue no se descarta si IA no pudo corregir. CSS data-table + texto en wizard.** |
| **Sprint COLUMN-FORMAT + IMPORT-REPAIR** | **commiteado antes de esta sesión** | **Formato real de columnas dinámicas tipadas; reparación IA; normalización determinista antes de importar. Revisión Gemini incorporada.** |
| **Sprint IMPORT-PARSER + MAPPING-GUARD** | **commiteado antes de esta sesión** | **Parser real contra archivos del usuario; fallback local aunque Ollama devuelva 0 columnas; bloqueo de obligatorios.** |
| **Auditoría 2026-06-12** | **`3d7f765`→`a352225` + docs posteriores** | **Corregidos SEC-NEW-1/2/3, ARCH-NEW-1, COD-NEW-1 y VULN-SR-001. Abiertos: SEC-NEW-4/5, COD-NEW-2. SEC-NEW-1 reclasificado como defensa en profundidad por código muerto desde UI.** |
| **IMPORT-ADAPTIVE** | **`bca51b2`→`63c6592`** | **Regiones/tablas laterales, pivot seguro de matrices de Tarifas, protección de Materiales, mapeo parent-child y cabeceras comunes en documentos.** |

### Tests
- **121/121 verdes** con `.\mvnw.cmd test -q`. Incluye `ImportServiceParsingTest` (12 tests), `ColumnMappingDialogTest` (1) y `DocumentImportSpecTest` (2).
  - `ClienteDAOTest`: 3
  - `EmpleadoDAOTest`: 3 (+3 Deuda 24)
  - `NominaDAOTest`: 3 (+3 Deuda 24)
  - `PagoMaterialDAOTest`: 3 (+3 Deuda 24)
  - `PedidoDAOTest`: 3 (+3 Deuda 24)
  - `PagoPedidoDAOTest`: 3 (+3 Deuda 24)
  - `PresupuestoDAOTest`: 3
  - `MaterialDAOTest`: 3
  - `FacturaDAOTest`: 3
  - `AlbaranDAOTest`: 4
  - `TxAnidadaTest`: 3
  - `ImportBackupServiceTest`: 12
  - `EntityImportServiceAlbaranTest`: 11
  - `EntityImportServiceFacturaTest`: 9
  - `EntityImportServiceNominaTest`: 6 (+1 Sprint C)
  - `EntityImportServicePedidoTest`: 10
  - `EntityImportServicePresupuestoTest`: 7
  - `TypedValueFormatterTest`: 5
  - `ImportServiceParsingTest`: 6

### Estado de los archivos clave

- `PresupuestoDAO.set` — preserva DEFAULT en `estado` ('borrador') y `condiciones` (vía constante `DEFAULT_CONDICIONES`).
- `FacturaDAO.set` — preserva DEFAULT en `estado` ('pendiente') y `forma_pago` ('Transferencia bancaria').
- `AlbaranDAO.set` — preserva DEFAULT en `estado` ('pendiente').
- `EmpleadoDAO.set` — preserva DEFAULT en `categoria` ('Operario'). *(Sprint D-ter 1a)*
- `MaterialDAO.set` — preserva DEFAULT en `categoria` ('consumibles') y `unidad` ('ud'). *(Sprint D-ter 1b)*
- `PagoMaterialDAO.bind` — preserva DEFAULT en `forma_pago` ('Contado'). *(Sprint D-ter 1c)*
- `ClienteDAO.setBase` — preserva DEFAULT en `tipo` ('empresa') y `ciudad` ('Almería'). *(Sprint D-ter 1d)*
- `AuthService.java` — `changePassword` verifica contraseña actual; `resetPasswordAdmin` separado. *(Sprint SEC)*
- `UserDAO.java` — no cierra singleton Connection; rol desconocido lanza excepción. *(Sprint SEC)*
- `DatabaseManager.java` — `runMigrations` solo silencia "column already exists". *(Sprint SEC)*
- `LoginView.java` + `AdminSetupView.java` — mínimo 8 chars ejecutable. *(Sprint SEC)*
- `styles.css` — variables `-c-status-*`, `.btn-toolbar-active`, `.input-success`, DatePicker, ProgressBar, RadioButton, ContextMenu, IA view, `.skeleton-row`. *(Sprint UI-A + UI-C + UI-D)*
- `theme-mulberry.css` — solo variables `.root`, sin sobreescrituras de componentes. *(Sprint UI-A UA-9)*
- `IAView.java` — `command-bar`, `btn-toolbar`, clases CSS IA, sin `setStyle()` hardcodeados. *(Sprint UI-C)*
- `ClientesView.java` / `FacturasView.java` / `PedidosView.java` — `ProgressIndicator` overlay sobre `StackPane` wrapping `TableView`; visible durante carga JDBC síncrona. *(Sprint UI-D)*
- `EntityImportService.java` — `resolverEmpleadoId` sin filtro `activo=1`; nóminas históricas de empleados inactivos importables. *(Sprint C)*
- `ImportService.java` — `parseFile()` soporta CSV/Excel/JSON. XLSB se lee con `XSSFBEventBasedExcelExtractor` y se pasa por el mismo parser tabulado. CSV vacío devuelve resultado vacío, no excepción. Parser común por grid: detecta cabecera real, conserva tablas laterales, hace cabeceras únicas, infiere cabeceras vacías (`UNIDADES`, `DESCRIPCIÓN`, `PRECIO`) y filtra separadores/cabeceras repetidas. Añadidos `validateImportData()` (Ollama + fallback local), `corregirValor()`, `ImportRepairPlan` y fallback local permanente en `mapearCampos()`. *(Sprint IMPORT-UPGRADE + WIZARD-VALIDATION + IMPORT-REPAIR + IMPORT-PARSER)*
- `TypedValueFormatter.java` — helper común para normalizar/formatear `PRECIO`, `NUMERICO` y `FECHA`; acepta separadores españoles, símbolo `€` y fechas `dd/MM/yyyy`/ISO. Incluye modo estricto (`tryNormalizeForStorage`) para rechazar valores no convertibles. *(Sprint COLUMN-FORMAT + IMPORT-REPAIR)*
- `ValidationIssue.java` — record nuevo: `rowIndex`, `columnName`, `issue`, `suggestedFix (Optional<String>)`, `severity (ERROR/WARNING)`. *(Sprint WIZARD-VALIDATION)*
- `DatabaseManager.java` — añadido `dropColumn(tabla, columna)` con `requireSqlIdentifier` + `quoteIdentifier`. Requiere SQLite 3.35+. *(Sprint COLUMN-TYPES)*
- `ColumnConfigDAO.java` — record `ColumnConfig` añade campo `dataType`. Nuevo `updateDataType()`, `deleteDynamic()`, `setColumnVisible()` genérico (sin filtro base_column). Migración automática de `data_type` en `ensureConfigTable()`. *(Sprint COLUMN-TYPES)*
- `ColumnConfiguratorDialog.java` — botón "+ Añadir" muestra ComboBox de tipo; botón "Tipo…" para editar tipo en columnas existentes; botón "Eliminar" (solo columnas usuario con doble confirmación); ocultar/mostrar funciona para base y usuario. *(Sprint COLUMN-TYPES)*
- `DynamicColumnRuntime.java` — `addFormFields()` renderiza DatePicker (FECHA), TextField numérico (NUMÉRICO), TextField decimal (PRECIO) o TextField libre (TEXTO). Ahora las tablas muestran valores formateados por tipo y guardan valores normalizados. *(Sprint COLUMN-TYPES + COLUMN-FORMAT)*
- `DynamicColumnValueDAO.java` — añadido `findUnconvertibleValues()` y `normalizeColumnValues()` transaccional para adaptar valores existentes de columnas dinámicas al cambiar tipo. Usa `requireSqlIdentifier` + `quoteIdentifier`. *(Sprint COLUMN-FORMAT)*
- `ColumnConfiguratorDialog.java` — al cambiar tipo de columna de usuario pregunta si adaptar valores existentes; reporta valores no convertibles y solo normaliza con confirmación. *(Sprint COLUMN-FORMAT)*
- `Material.java` / `Tarifa.java` — sinónimos de importación ampliados. Tarifas reconoce `UNIDADES`/`CANTIDAD` como `minimo_unidades`, `CONCEPTO`/`DESCRIPCIÓN` como `nombre`; Materiales reconoce `tipo_papel`, `tipo de papel`, `modelo`, `producto`, `familia`, `concepto` como `nombre` y precios de resma/pliego/euro como `precio_unidad`. *(Sprint IMPORT-PARSER + MAPPING-GUARD)*
- `ImportService.java` — añadido `ImportRepairPlan`, `DynamicFieldSuggestion`, `RowValueFix` y `proponerReparacionImportacion()`; parsea JSON IA filtrando columnas/campos permitidos. Validación local de precios usa `TypedValueFormatter`. `mapearCampos()` ya no confía ciegamente en Ollama: ejecuta fallback local siempre para evitar `0/20 columnas mapeadas`. *(Sprint IMPORT-REPAIR + MAPPING-GUARD)*
- `EntityImportService.java` — validación y conversión numérica usan `TypedValueFormatter`, evitando fallos con `1.234,56 €`. *(Sprint IMPORT-REPAIR)*
- `ImportView.java` — paso 3.5 entre mapeo e importación; añade "🤖 Reparar importación" para aplicar plan IA con confirmación, crear campos dinámicos tipados, valores fijos y correcciones de celda. Normaliza valores mapeados antes de validar/importar. Activa técnica/categoría fija por defecto cuando falta el campo de agrupación, usando el nombre del archivo. Bloquea `Siguiente` si faltan campos obligatorios sin mapear, sin valor fijo y sin modo expandido que los cubra. *(Sprint WIZARD-VALIDATION + IMPORT-REPAIR + MAPPING-GUARD)*
- `TableColumnSizing.java` — nuevo helper: `enableHorizontalScroll()` + `autoSizeLater()` para scroll horizontal real en TableViews con muchas columnas. *(Sprint WIZARD-VALIDATION)*
- `EntityImportSpec.java` — `tableName()` computed method: mapea nombre del spec a tabla SQLite; devuelve `null` para entidades parent-child (Presupuestos, Facturas, Albaranes). *(Sprint IMPORT-UPGRADE)*
- `EntityImportService.java` — `procesarFila()` devuelve `int[3]{insertadas, actualizadas, entityId}`; `insertarFilas()` detecta columnas extra y las escribe via `DynamicColumnValueDAO`. *(Sprint IMPORT-UPGRADE)*
- `ColumnMappingDialog.java` — `opciones` es `ObservableList<String>` compartida entre todas las `CampoCelda`; botón "➕ Nuevo campo…" crea columna dinámica via `ColumnConfigDAO.addDynamicColumn()` y actualiza todos los ComboBoxes al instante. *(Sprint IMPORT-UPGRADE)*
- FileChooser en 10 vistas — añadidos `.xlsb`, `.xlsm` a los filtros. *(Sprint IMPORT-UPGRADE)*
- Tx-awareness de Sprint B intacta en todos los DAOs modificados.
- `DatabaseManager.getConnection()`, `buildDbUrl()`, `createTables()` — sin cambios.

---

## DEUDAS TÉCNICAS — ESTADO

| ID | Descripción | Estado |
|---|---|---|
| 1 | Validación de fechas en Fase 2 | CERRADA en 3C-paso-2a. |
| 2 | Filtro `activo=1` en `resolverEmpleadoId` podía romper nóminas históricas | CERRADA en `1851216`. |
| 3 | `lower()` en SQLite no normaliza tildes | ABIERTA. |
| 4 | UPDATE path Pedido sin guard de errores | CERRADA en `9798225`. |
| 5 | Contrato `filtroExtraSql` admite `null` pero rechaza blank | ABIERTA. Riesgo bajo. |
| 6 | `UPDATE_EXISTING` para Nómina y Pedido sin test directo | PARCIAL. Nómina sin test. |
| 7 | Asimetría: `aplicarValoresNomina` no recibe `errores` | ABIERTA. |
| 8 | `mostrarResultadoImportacion` duplicado en 8 vistas | ABIERTA. Refactor UI fuera de scope. |
| 9 | `*.save()` sin transacción explícita BEGIN/COMMIT | CERRADA en Sprint B. |
| 10 | `saveLineas` DELETE+INSERT total | CERRADA en 3C-paso-2b. |
| 11 | `fecha_alta` de Empleado sin validación ISO | ABIERTA, riesgo bajo. |
| 12 | `@SuppressWarnings("unused")` en `procesarGrupo` | CERRADA. |
| 13 | Detección de inconsistencia parent-child sin test | CERRADA. |
| 14 | `CREATE_NEW` para parent-child sin semántica clara | CERRADA. |
| 15 | Tests parent-child no cubren campos cabecera opcionales | ABIERTA, riesgo bajo. |
| 16 | `FacturaDAO.set()` escribía `presupuesto_id=0` literal | CERRADA en 4a. |
| 17 | Estilo de imports en `Factura.java` | ABIERTA, riesgo nulo. |
| 18 | `cliente_id` en `FacturaDAO.set()` no aplica `setNull` | ABIERTA, riesgo bajo. |
| 18-bis | `cliente_id` en `AlbaranDAO.set()` no aplica `setNull` | ABIERTA, riesgo nulo. |
| 19 | Validación numérica de `cantidad` ausente | ABIERTA. Aceptada consciente. |
| **20** | Defaults DDL ignorados con NULL explícito (columnas TEXT en Presupuesto/Factura/Albarán) | **CERRADA en Sprint D.** Patrón "literal Java-side con ternario" aplicado a los 5 sitios afectados. |
| **20-bis** | Defaults DDL numéricos primitivos pisados con `double=0.0` (ej. `iva_porcentaje DEFAULT 21.0`) | **NUEVA, ABIERTA.** Requiere cambio modelo `double`→`Double` con blast radius mayor. En la práctica todos los flujos setean IVA explícitamente; bug teórico. |
| **20-ter** | Defaults DDL TEXT en DAOs fuera Sprint D | **CERRADA en Sprint D-ter.** EmpleadoDAO, MaterialDAO, PagoMaterialDAO, ClienteDAO corregidos. PedidoDAO y PagoPedidoDAO ya estaban limpios. NominaDAO fuera scope (no tiene DEFAULT TEXT no-trivial). |
| **24** | Ausencia de tests JDBC en EmpleadoDAO, PagoMaterialDAO, NominaDAO, PedidoDAO, PagoPedidoDAO | **CERRADA en `acc81a3`.** 15 tests JDBC añadidos. |
| **25** | Asimetría `forma_pago` vs `estado` en `PagoMaterialDAO.bind` | **NACIÓ Y MURIÓ en Sprint D-ter 1c.** Cerrada en el mismo commit. |
| **26** | Capa completa de ayuda integrada dentro de la aplicación | **PARCIALMENTE CERRADA.** HELP-0 (spec) y HELP-1 (81 artículos HTML) completados. Pendiente HELP-2 (HelpView JavaFX) y HELP-3 (F1 contextual). |
| **27** | Migración de tablas complejas desde archivos humanos (Excel con celdas combinadas, varias tablas por hoja, fórmulas, PDF/Word) | **ABIERTA, PRIORITARIA.** Ver `MIGRACION_HISTORICO.md`. No confundir con importación CSV/Excel limpio ya cerrada. |
| 21 | Mapeo automático no reconoce `numero` ni `nif` en spec Albarán | ABIERTA. Riesgo bajo, UX. |
| 22 | Mensaje de error `cliente_nif` dice "para el pedido" en albarán/factura/presupuesto | ABIERTA. Trivial. |
| 23 | Diálogo de mapeo no lista campos de línea en desplegables | ABIERTA. Informativa. |

---

## SPRINT D-ter — DEFAULTS DDL EN DAOs RESTANTES — CERRADO

### Objetivo
Aplicar el patrón `ternario + comentario DDL` a los DAOs que quedaron fuera del Sprint D. Scope efectivo: 4 DAOs (EmpleadoDAO, MaterialDAO, PagoMaterialDAO, ClienteDAO), 6 columnas TEXT en total.

### Hallazgos de auditoría — confirmados leyendo el código

- **PedidoDAO y PagoPedidoDAO ya estaban limpios.** Aplicaban el patrón antes de Sprint D-ter. Quedan fuera de scope sin parche.
- **NominaDAO queda fuera del scope D-ter:** la tabla `nominas` no tiene columnas TEXT con DEFAULT no-trivial. Todos sus DEFAULTs son numéricos (REAL/INTEGER) → Deuda 20-bis.
- **PedidoDAO tiene mitigación pragmática para `iva_porcentaje` primitivo:** `p.getIvaPorcentaje() > 0 ? p.getIvaPorcentaje() : 21.0`. Reduce urgencia de Deuda 20-bis pero no es solución general.

### Sub-bloques ejecutados

| Sub-bloque | Commit | Cambios | Tests | Notas |
|---|---|---|---|---|
| **1a** | `5f50954` | `EmpleadoDAO.set` preserva DEFAULT 'Operario' en categoria | Sin test (no existe EmpleadoDAOTest — Deuda 24) | Patrón validado |
| **1b** | `8c2c0d1` | `MaterialDAO.set` preserva DEFAULT 'consumibles' (categoria) y 'ud' (unidad) | +1 (`MaterialDAOTest`: 2→3) con red-green explícito confirmado | Test falló con `expected: <consumibles> but was: <null>` antes del fix |
| **1c** | `cfb83fa` | `PagoMaterialDAO.bind` preserva DEFAULT 'Contado' en forma_pago | Sin test (no existe PagoMaterialDAOTest — Deuda 24) | Cerró asimetría con `estado` que ya usaba ternario |
| **1d** | `d19a342` | `ClienteDAO.setBase` preserva DEFAULT 'empresa' (tipo) y 'Almería' (ciudad) | +1 (`ClienteDAOTest`: 2→3) | Deuda 20-ter completamente cerrada en scope TEXT |

### Archivos leídos en sesión Sprint D-ter

- `DatabaseManager.java` (completo, releído)
- `PedidoDAO.java`, `ClienteDAO.java`, `EmpleadoDAO.java`, `PagoPedidoDAO.java`, `MaterialDAO.java`, `PagoMaterialDAO.java` (todos completos)
- `MaterialDAOTest.java`, `ClienteDAOTest.java`, `Cliente.java` (completos)

---

## SPRINT IMPORT-UPGRADE — XLSB/XLSM + CAMPO NUEVO EN IMPORTACIÓN — CERRADO

### Objetivo
Ampliar el asistente de importación para: (a) soportar archivos XLSB/XLSM de forma nativa, (b) escribir columnas dinámicas durante el import y (c) permitir crear campos nuevos directamente desde `ColumnMappingDialog` sin salir del flujo.

### Cambios ejecutados

| Archivo | Cambio |
|---|---|
| `ImportService.java` | `parseFile()` enruta XLSB/XLSM/XLTX/XLTM; `parseExcel()` usa `WorkbookFactory.create(file, null, true)` |
| `EntityImportSpec.java` | `tableName()` computed: mapea spec → tabla SQLite; `null` para parent-child |
| `EntityImportService.java` | `procesarFila()` → `int[3]{insertadas,actualizadas,entityId}`; `insertarFilas()` detecta columnas extra y escribe via `DynamicColumnValueDAO` |
| `ColumnMappingDialog.java` | `opciones` como `ObservableList<String>` compartida; botón "➕ Nuevo campo…" con `ColumnConfigDAO.addDynamicColumn()` |
| 10 vistas | Filtros FileChooser añaden `.xlsb` y `.xlsm` |

### Sub-bloques ejecutados

| Sub-bloque | Commit | Cambios |
|---|---|---|
| **Sprint completo** | `4bc6c9c` | Todo el sprint en un único commit atómico. 89/89 tests verdes. `clean compile` OK. |

### Archivos leídos en esta sesión
- `ImportService.java`, `EntityImportService.java`, `EntityImportSpec.java`, `FieldSpec.java`, `ColumnMatcher.java`
- `MappingResult.java`, `DynamicColumnValueDAO.java`, `ColumnConfigDAO.java`, `DynamicColumnRuntime.java`
- `ColumnMappingDialog.java` (completo)
- `ClientesView.java`, `EmpleadosView.java`, `FacturasView.java`, `AlbaranesView.java`, `MaterialesView.java`, `NominasView.java`, `PedidosView.java`, `PresupuestosView.java`, `TarifasView.java`, `ImportView.java` (secciones de FileChooser)
- `ImportarClientesService.java`, `ImportBackupService.java`

---

## SPRINT IMPORT-WIZARD/PIVOT UI — EN CURSO (2026-06-11)

### Objetivo
Hacer que la importación nueva sea visible desde los módulos reales y que el modo expandido sirva para importar matrices de precios tipo `01_TARJETAS_DE_VISITA.xlsx` sin generar una macro tabla plana inútil.

### Implementado hasta ahora

| Área | Estado |
|---|---|
| Entrada al wizard | `ClientesView`, `MaterialesView`, `EmpleadosView` y `TarifasView` abren `ImportView` con el tipo de entidad fijado desde el módulo. |
| Sidebar | Eliminado el grupo `DATOS` y su entrada de importación global; la importación vive en cada módulo. |
| Servicio | `ImportView` importa mediante `EntityImportService` + `EntityImportSpec`; el flujo antiguo queda desplazado para estas entidades planas. |
| Modo expandido | Añadido en paso 3: columnas de valor seleccionables, campo destino para nombre de columna y campo destino para valor de celda. |
| Tarifas tipo matriz | Soporta convertir 39 filas x 8 columnas de precio en aprox. 312 registros, uno por combinación cantidad/técnica. |
| Agrupación básica | Materiales usa `categoria`; Tarifas usa `tecnica`. Para Tarifas con modo expandido NO activar grupo fijo si `tecnica` ya sale del nombre de columna. |
| Scroll/tablas | Añadido `TableColumnSizing` y `UNCONSTRAINED_RESIZE_POLICY` para que columnas anchas generen scroll horizontal real. |
| Legibilidad | Reforzado color de labels/checkboxes del wizard y CSS de `.check-box .text` con `-fx-fill: -c-text`. |
| Seguridad | No se aceptan nombres de columnas dinámicas sin pasar por `ColumnConfigDAO`/validaciones existentes; valores importados siguen entrando por `PreparedStatement`. |

### Cómo rellenar el modo expandido para TARIFAS

Abrir desde `Tarifas > Importar`, no desde Materiales.

En la tabla principal de mapeo:
- `UNIDADES` -> `minimo_unidades`
- `DESCRIPCIÓN` -> `nombre`
- Las columnas de precio -> `(ignorar)`

En `Modo expandido`:
- Marcar solo las columnas de precio/valor.
- `El nombre de columna va al campo` -> `tecnica`
- `El valor de la celda va al campo` -> `precio_unit`
- No activar `Aplicar técnica fija a toda la importación` en este caso.

Resultado esperado: por cada fila de cantidad se generan tantos registros como columnas de precio marcadas.

### Pendiente inmediato

1. Smoke test manual con el Excel real `01_TARJETAS_DE_VISITA.xlsx`.
2. Mejorar UX del paso 3: deshabilitar o advertir visualmente las columnas ya mapeadas para que no se marquen como pivot por error.
3. Confirmar en ventana maximizada que las tablas mantienen scroll horizontal y no comprimen columnas.
4. Decidir si migrar otros módulos planos después: Proveedores si existe vista, Nóminas solo si el flujo actual lo permite. Parent-child (`Pedidos`, `Presupuestos`, `Facturas`, `Albaranes`) queda para fase posterior por mayor riesgo.

### Validación última

- `.\mvnw.cmd clean compile` — BUILD SUCCESS (104 fuentes).
- `.\mvnw.cmd test` — BUILD SUCCESS, 99/99 tests verdes.

---

---

## SPRINT COLUMN-TYPES — COLUMNAS TIPADAS + HARD DELETE — CERRADO (2026-06-11)

### Objetivo
Permitir especificar y editar el tipo de dato (TEXTO/NUMÉRICO/PRECIO/FECHA) de cualquier columna en todos los módulos, rendir controles de entrada adecuados en formularios, permitir eliminación definitiva de columnas de usuario y ocultar/mostrar columnas base.

### Commits

| Commit | Cambio |
|---|---|
| `e12a687` | `DatabaseManager.dropColumn()` con validación de identificadores SQL |
| `701fc5e` | `ColumnConfigDAO`: campo `dataType`, `deleteDynamic()`, `setColumnVisible()` genérico, migración DDL |
| `9979bdf` | `ColumnConfiguratorDialog`: ComboBox tipo al añadir, botón Eliminar, ocultar/mostrar en base |
| `43ef10d` | `DynamicColumnRuntime`: DatePicker (FECHA), TextField numérico/precio, `extraControls` |
| `bc90305` | `ColumnConfigDAO.updateDataType()` |
| `d4db11b` | `ColumnConfiguratorDialog`: botón "Tipo…" para editar tipo en columnas existentes |

### Decisiones de diseño

- El tipo se almacena en `column_configs.data_type` (TEXT), nunca en el esquema SQLite de la tabla de datos. Los valores siguen siendo TEXT libre en SQLite; el tipo solo controla el control de entrada.
- Las columnas base NO se pueden eliminar (romperían el código Java). Solo ocultar/mostrar.
- `setColumnVisible()` es genérico sin filtro `base_column=0`, lo que permite ocultar/mostrar base Y usuario.
- `deleteDynamic()` tiene doble protección: `AND base_column=0` en SQL + alerta informativa en la UI.
- Migración automática de `data_type` en `ensureConfigTable()` para bases de datos existentes.

---

## SPRINT WIZARD-VALIDATION — PASO 3.5 IA EN IMPORTACIÓN — CERRADO (2026-06-11)

### Objetivo
Añadir un paso de validación con IA entre el mapeo de campos (paso 3) y la vista previa/importación (paso 4). La IA analiza las primeras 20 filas, detecta problemas y permite corrección individual antes de importar.

### Commits

| Commit | Cambio |
|---|---|
| `988a8fb` | `TableColumnSizing` — helper scroll horizontal real |
| `0563405` | `ValidationIssue` record — rowIndex, columnName, issue, suggestedFix, severity |
| `b33d095` | `ImportService`: `validateImportData()`, `corregirValor()`, validación local, helpers privados |
| `94b449d` | `ImportView`: paso 3.5 completo (cargando → tabla issues → botones → continuar bloqueado) |
| `979cd06` | Fix: `issueList.remove()` solo cuando `fix.isPresent()` — no ocultar ERRORs sin corregir |

### Comportamiento esperado

- **Con Ollama disponible:** analiza las primeras 20 filas con el modelo activo; parsea JSON array de issues.
- **Sin Ollama:** validación local de NIF (regex), email (regex), precios (parseDouble), enteros, fechas (4 formatos), duplicados de NIF/email.
- **Corrección IA:** llama a `corregirValor()` que pide a Ollama `{"correctedValue": "..."}`. Si devuelve valor, muta la fila en memoria y descarta el issue. Si Ollama falla o devuelve null, el issue permanece y el botón muestra "Sin corrección — reintentar".
- **"Continuar →"** queda deshabilitado mientras haya issues `ERROR` sin resolver. Los `WARNING` no bloquean.
- **Seguridad:** bounds check en `rowIndex` para evitar `IndexOutOfBoundsException` si Ollama devuelve índice fuera de rango.

### Validación local por campo

| Campo | Tipo de validación |
|---|---|
| `nif` | Regex NIF/CIF + deduplicación |
| `email` | Regex básico `\S+@\S+\.\S+` + deduplicación |
| `precio_unidad`, `precio_unit`, `precio_setup`, `salario_base` | parseDouble, no negativo |
| `stock_actual`, `stock_minimo`, `minimo_unidades` | parseInt |
| `fecha_alta` | 4 formatos: `dd/MM/yyyy`, `yyyy-MM-dd`, `dd-MM-yyyy`, `d/M/yyyy` |
| Campos requeridos | `nombre` (Clientes/Materiales); `nombre`+`nif` (Empleados); `tecnica`+`nombre` (Tarifas) |

---

## SPRINT COLUMN-FORMAT + IMPORT-REPAIR — CERRADO Y COMMITEADO (2026-06-11)

### Objetivo
Hacer que los tipos de columna tengan efecto real sobre valores existentes y edición diaria, y ampliar el wizard para que la IA proponga una reparación estructurada de importación antes de descartar filas.

### Cambios principales

- `PRECIO` se guarda normalizado (`12.50`) y se muestra como importe con `€`.
- `FECHA` se guarda en ISO (`yyyy-MM-dd`) y se edita con `DatePicker`; acepta formatos habituales (`dd/MM/yyyy`, `dd-MM-yyyy`, ISO).
- `NUMERICO` se limpia con parseo robusto de separadores españoles.
- El botón "Tipo…" puede adaptar valores existentes de columnas de usuario al nuevo tipo; antes reporta valores no convertibles y la actualización se ejecuta dentro de transacción.
- La edición directa de celdas tipadas rechaza valores no convertibles y mantiene el valor anterior.
- El wizard añade "🤖 Reparar importación": Ollama devuelve un plan JSON con mapeo, campos dinámicos tipados, valores fijos seguros y correcciones de celda.
- El plan IA se aplica solo tras confirmación del usuario y se filtra contra columnas/campos permitidos.
- Antes de validar/importar se normalizan precios, números y fechas de campos mapeados.

### Seguridad y límites

- SQL dinámico limitado a identificadores validados con `requireSqlIdentifier` y `quoteIdentifier`.
- La IA no puede crear nombres de columna físicos directamente; solo propone etiquetas, y `ColumnConfigDAO` genera el identificador seguro.
- La IA no debe inventar NIF, emails, clientes, proveedores ni relaciones; esos casos quedan para decisión del usuario.
- `rowFixes` se ignora si la fila o columna no existe.
- Validado en ese sprint con `.\mvnw.cmd test`: 104/104 verdes. Estado vigente tras sprint posterior: 121/121 verdes.

---

## SPRINT IMPORT-PARSER + MAPPING-GUARD — CERRADO Y COMMITEADO (2026-06-12)

### Objetivo
Corregir el fallo real reportado por el usuario en captura: el wizard de Tarifas permitía importar con `IA mapeó 0/20 columnas`, llegaba al paso 4 y descartaba todas las filas porque `técnica` y `nombre` estaban vacíos.

### Causa raíz
- El parser ya abría muchos archivos, pero algunas cabeceras reales estaban en filas posteriores o en tablas laterales.
- Ollama podía devolver JSON válido con todos los campos a `null`; `ImportService.mapearCampos()` lo aceptaba como éxito y no ejecutaba fallback local.
- `ImportView` no bloqueaba el avance al paso 4 si faltaban campos obligatorios.
- En tarifas, `tecnica` suele venir del nombre del archivo o del modo expandido, no de una columna explícita.

### Cambios ejecutados

| Archivo | Cambio |
|---|---|
| `ImportService.java` | Parser común por grid para CSV/XLSX/XLSB; CSV vacío no revienta; XLSB se lee por `XSSFBEventBasedExcelExtractor`; detección de cabecera real; cabeceras únicas; inferencia de cabeceras vacías; filtrado de separadores/cabeceras repetidas. |
| `ImportService.java` | `mapearCampos()` ejecuta `fallbackMapping()` siempre, aunque Ollama responda sin error. El fallback usa el `IMPORT_SPEC` real de cada entidad y evita duplicar destinos. |
| `Tarifa.java` | Sinónimos: `UNIDADES`/`CANTIDAD` → `minimo_unidades`; `CONCEPTO`/`DESCRIPCIÓN` → `nombre`. |
| `Material.java` | Sinónimos: `tipo_papel`, `tipo de papel`, `modelo`, `producto`, `familia`, `concepto` → `nombre`; precios de resma/pliego/euro → `precio_unidad`. |
| `ImportView.java` | Preselecciona técnica/categoría fija cuando falta el campo de agrupación y rellena el valor desde el nombre del archivo. Bloquea `Siguiente` si quedan obligatorios sin cubrir. |
| `ImportServiceParsingTest.java` | 6 tests: CSV vacío, cabecera tras filas título, tablas laterales con duplicados, cabeceras repetidas internas, inferencia de cabeceras vacías tipo OVALOS, sinónimos locales. |

### Validación con archivos reales del usuario

Rutas probadas:
- `C:\Users\Gipsy Dávy\Desktop\CSV`
- `C:\Users\Gipsy Dávy\Desktop\excel`
- `C:\Users\Gipsy Dávy\Desktop\EXCEL_SEPARADO`
- `C:\Users\Gipsy Dávy\Desktop\files`
- `C:\Users\Gipsy Dávy\Desktop\TARIFAS_SEPARADAS`
- `C:\Users\Gipsy Dávy\Desktop\TARIFAS_SEPARADAS 1`
- `C:\Users\Gipsy Dávy\Desktop\todas_las_tarifas`

Resultado del probe con `ImportService.parseFile()`:
- **288/288 archivos abren.**
- **110 CSV**, **177 XLSX**, **1 XLSB**.
- **34 archivos** abren pero están vacíos o sin columnas reales; no son error de parser.

Dry-run contra SQLite temporal (`jdbc:sqlite:C:\tmp\gm-import-dryrun.db`) sin tocar la BD real:
- `17_DISEÑOS.xlsx`: 18/18 importables.
- `19_OVALOS.xlsx`: 12/12 importables.
- `40_IMANES.xlsx`: 31/31 importables.
- CSV de `Desktop\files`: muestras probadas importan limpias.
- `NUEVAS TARIFAS (2) (version 1).xlsb`: abre y recupera 1020 filas; 938 importables en dry-run simple, con descartes por secciones internas del libro.
- `PRECIOS PAPEL PROVEEDORES Formulas.xlsx`: 39/76 importables en dry-run simple; el resto son separadores/bloques humanos.

### Prueba directa del fallo de captura

Archivo usado: `C:\Users\Gipsy Dávy\Desktop\CSV\01_TARJETAS_DE_VISITA.csv`.

Resultado esperado vigente de `ImportService.mapearCampos(TARIFAS, headers)` aunque Ollama esté caído:
```text
UNIDADES=minimo_unidades
DESCRIPCIÓN=nombre
PRECIO S/ PLASTIFICAR UNA CARA=precio_unit
```

En el wizard:
- Si no se usa modo expandido, la técnica fija debe salir activada y prellenada con el nombre del archivo.
- Si se usa modo expandido, desactivar técnica fija y usar `nombre de columna -> tecnica`, `valor de celda -> precio_unit`.
- `Siguiente` no debe permitir llegar al paso 4 si faltan `tecnica` o `nombre`.

### Seguridad
- No se ejecuta contenido de CSV/Excel/XLSB.
- XLSB se transforma a texto tabulado mediante POI y pasa por parser propio.
- No hay SQL dinámico nuevo.
- Los valores dudosos se descartan o bloquean, no se inventan.
- La IA no es autoridad final: fallback local y bloqueo de obligatorios protegen el flujo.

### Validación
- `.\mvnw.cmd test` — **121/121 verdes** como estado vigente tras IMPORT-ADAPTIVE.
- `git diff --check` — sin errores en esa validación; avisos CRLF habituales en Windows.

---

## PRÓXIMOS SPRINTS CANDIDATOS

### 1. Sprint MIGRACION-COMPLEJA — Tablas complejas históricas (Deuda 27)

Retomar el problema original de migración de archivos reales del cliente: Excel con estructura humana,
celdas combinadas, bloques laterales, fórmulas, varias mini-tablas por hoja, PDFs y Word.

Objetivo inmediato:
- inventariar archivos reales;
- clasificar cada archivo por vía A1/A2/B/C;
- convertir al menos un archivo complejo a CSV limpio;
- probar importación en entorno de prueba;
- documentar plantilla y procedimiento en `MIGRACION_HISTORICO.md`.

No tocar código Java en este sprint salvo autorización explícita posterior. Primero cerrar diagnóstico,
plantilla y procedimiento.

### 2. Sprint DOC-SYNC — COMPLETO (`4bc6c9c`)

Documentación sincronizada con HEAD real tras Sprint IMPORT-UPGRADE. `continuar.md`, `Resumen.md` y `MIGRACION_HISTORICO.md` actualizados.

### 3. HELP-0 — Especificación de ayuda completa (más adelante)

Definir arquitectura de ayuda, taxonomía de artículos, mapa módulo-artículo y criterios de aceptación.
Debe cubrir todas las posibilidades y opciones integradas en la ayuda: centro de ayuda, manual,
guías, FAQ, glosario, ayuda contextual, onboarding, buscador, errores con solución y modo
principiante/avanzado. Sin tocar código Java en esta fase.

### 4. HELP-1 — Documentación offline versionada (más adelante)

Crear la estructura de documentación local empaquetable con la aplicación: artículos, primeros pasos,
manual básico, FAQ, glosario, guías por módulo y advertencias operativas.

### 5. HELP-2 — Centro de ayuda JavaFX

Completado en `47e46dc`: sistema de ayuda integrado offline con 81 artículos.

### 6. Refactor B2 — Inyección de Connection en DAOs (después de HELP-2)

Eliminar dependencia del singleton estático inyectando `Connection` por constructor o parámetro.
Desbloquea tests paralelos y abre la puerta a desconectar el singleton. Amplio y de mayor riesgo:
dejarlo para después de DOC-SYNC, HELP-0, HELP-1 y HELP-2.

### 7. Sprint D-bis — Defaults DDL numéricos primitivos (Deuda 20-bis)
`iva_porcentaje DEFAULT 21.0` pisado por `double = 0.0`. Requiere cambio modelo `double`→`Double`.
Blast radius mayor. Bajo urgencia porque los flujos actuales setean IVA explícitamente.

### 8. Sprint DOC/HELP — Capa completa de ayuda integrada (roadmap global)
Incorporar una capa de ayuda dentro de la aplicación, usable sin soporte externo:
centro de ayuda, ayuda contextual por pantalla, manual integrado, guías paso a paso,
primer arranque/onboarding, tooltips avanzados, ejemplos de uso, FAQ, glosario de formatos,
explicación de riesgos y advertencias, documentación offline, buscador de ayuda,
enlaces desde errores a soluciones y modo principiante/avanzado.
Debe ejecutarse de forma incremental: cada sprint funcional futuro debe incluir su parte
de ayuda, documentación y asistencia al usuario.

---

## ARCHIVOS YA INSPECCIONADOS — NO PEDIRLOS DE NUEVO

**De v3.6/v3.7/v3.8 ya inspeccionados:**
- `CLAUDE.md`, `MIGRACION_HISTORICO.md`.
- `DuplicatePolicy.java`, `ColumnMatcher.java`.
- `Tarifa.java`, `Pedido.java`, `LineaPresupuesto.java`, `LineaFactura.java`, `LineaAlbaran.java`.
- `PedidoDAO.java`.
- `PresupuestosView.java`, `FacturasView.java`, `AlbaranesView.java`, `TarifasView.java`, `NominasView.java`, `PedidosView.java`.
- `EntityImportSpec.java`, `FieldSpec.java`.
- `EntityImportServicePedidoTest.java`, `EntityImportServicePresupuestoTest.java`, `EntityImportServiceFacturaTest.java`, `EntityImportServiceAlbaranTest.java`.
- `MaterialDAO.java`, `ClienteDAOTest.java`.

**Releídos completos en Sprint D:**
- `PresupuestoDAO.java` — completo (no solo zona de tx).
- `FacturaDAO.java` — completo.
- `AlbaranDAO.java` — completo.
- `DatabaseManager.java` — completo (incluido `createTables` y `runMigrations` con DDLs de las 18+ tablas).
- `Presupuesto.java`, `Factura.java`, `Albaran.java` — modelos completos. Confirmado: campos `String` sin inicializar devuelven `null` por defecto.
- `PresupuestoDAOTest.java` — completo (harness + helpers).

**Leídos completos en Sprint D-ter:**
- `DatabaseManager.java` — releído completo.
- `PedidoDAO.java`, `ClienteDAO.java`, `EmpleadoDAO.java`, `PagoPedidoDAO.java`, `MaterialDAO.java`, `PagoMaterialDAO.java` — todos completos.
- `MaterialDAOTest.java` — completo.
- `Cliente.java` — completo. Confirmado: doble constructor (0 args / 9 args). Con 0 args, tipo y ciudad son null.

**Leídos completos en Sprint D-ter (completado):**
- `ClienteDAOTest.java` — leído antes de 1d. Confirmado: patrón de harness idéntico al resto.

**Tests JDBC del Sprint B, Sprint D y Sprint D-ter:**
- `PresupuestoDAOTest.java`, `MaterialDAOTest.java`, `FacturaDAOTest.java`, `AlbaranDAOTest.java`, `TxAnidadaTest.java`.

---

## RESPUESTA TÍPICA EN PRÓXIMAS SESIONES

- Sin emojis salvo cita literal de código.
- Sin "vamos a", "podríamos", "voy a intentar". Imperativo claro.
- Listas y tablas cuando estructuran. Prosa cuando fluye.
- Bloques de código con triple backtick y lenguaje declarado.
- Bloques para agentes en triple backtick anidado con cinco: ` ````` ` para copia limpia.
- Al final de cada respuesta técnica, una pregunta concreta o un siguiente paso explícito.

---

## PRIMER MENSAJE QUE VOY A RECIBIR

El usuario abrirá un chat nuevo con "continúa" o "¿qué toca?". Mi primer mensaje debe ser:

1. **Leer:** `CLAUDE.md`, este `Resumen.md`, `continuar.md` — en ese orden, antes de declarar nada.

2. **Verificar estado git:**
   - `git log --oneline -5` — confirmar commit de documentación de cierre posterior a `63c6592` o documentar divergencia.
   - `git status --short` — esperado: solo `.claude/settings.local.json` modificado, salvo cambios del usuario.

3. **Verificar tests:** `.\mvnw.cmd test` — confirmar 121/121 verdes o documentar divergencia.

4. **Declarar situación:**
   > Tests esperados 121/121 verdes. IMPORT-ADAPTIVE cerrado y commiteado hasta `63c6592` más documentación de cierre posterior. Abiertos de auditoría: SEC-NEW-4, SEC-NEW-5, COD-NEW-2. Cola activa: validación manual final de importación en app y Sprint MIGRACION-COMPLEJA.

5. **Si la verificación revela divergencia** inesperada, diagnosticar con `git log --oneline -10` antes de avanzar.

FIN DEL HANDOFF.
