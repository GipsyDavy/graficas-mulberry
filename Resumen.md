# HANDOFF — Graficas Mulberry · Sprint Defaults DDL TEXT CERRADO
# Versión: 3.9 · Fecha cierre: 23/05/2026 · Checkpoint: Sprint D (defaults DDL TEXT en 3 DAOs) CERRADO. 3 commits del sprint en master, 71/71 tests verdes. Deuda 20 cerrada parcialmente (solo columnas TEXT).

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
- **Regla de inicio "_Gipsybuho_, procedo a ejecutar..."** del `CLAUDE.md`: aplícala solo en mensajes conversacionales claros. En respuestas técnicas, bloques, parches, listas de pasos, no aplica.
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

## SPRINTS PREVIOS — RESUMEN

- **Sprint Importación CSV** (handoff v3.6, HEAD `74f174c`): 9 entidades importables, 56 tests verdes (2+12+11+9+5+10+7).
- **Sprint A — Smoke Test manual de Albaranes** (sin commit de código): PASA. 3 deudas nuevas registradas (21, 22, 23).
- **Sprint B — Transacciones explícitas en DAOs** (handoff v3.8, HEAD `fbc6fc8`): 5 commits, 12 tests nuevos (9 unit + 3 cross-DAO), 68/68 verdes. Deuda 9 cerrada.

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
- **HEAD esperado tras commit del v3.9:** `docs:` v3.9 encima de `8ef936a`.
- **HEAD actual (antes del commit del handoff):** `8ef936a` — `feat: preservar default DDL en AlbaranDAO.set (estado)`.
- **Sincronizado con `origin/master`.**
- **Commits del Sprint D (3):** `0041fa2`, `e434d31`, `8ef936a`.
- **Working tree:** sólo `Resumen.md` modificado (v3.9 a commitear).

### Tests
- **71/71 verdes.** Reparto:
  - `ClienteDAOTest`: 2
  - `PresupuestoDAOTest`: 3 (+1 en Sprint D)
  - `MaterialDAOTest`: 2
  - `FacturaDAOTest`: 3 (+1 en Sprint D)
  - `AlbaranDAOTest`: 4 (+1 en Sprint D)
  - `TxAnidadaTest`: 3
  - `ImportBackupServiceTest`: 12
  - `EntityImportServiceAlbaranTest`: 11
  - `EntityImportServiceFacturaTest`: 9
  - `EntityImportServiceNominaTest`: 5
  - `EntityImportServicePedidoTest`: 10
  - `EntityImportServicePresupuestoTest`: 7

### Estado de los archivos clave tras Sprint D

- `PresupuestoDAO.set` — preserva DEFAULT en `estado` ('borrador') y `condiciones` (vía constante `DEFAULT_CONDICIONES`).
- `FacturaDAO.set` — preserva DEFAULT en `estado` ('pendiente') y `forma_pago` ('Transferencia bancaria').
- `AlbaranDAO.set` — preserva DEFAULT en `estado` ('pendiente').
- Tx-awareness de Sprint B intacta. Cero modificaciones a la lógica transaccional.
- `DatabaseManager.getConnection()`, `buildDbUrl()`, `createTables()` — sin cambios.

---

## DEUDAS TÉCNICAS — ESTADO

| ID | Descripción | Estado |
|---|---|---|
| 1 | Validación de fechas en Fase 2 | CERRADA en 3C-paso-2a. |
| 2 | Filtro `activo=1` en `resolverEmpleadoId` puede romper nóminas históricas | ABIERTA. Candidata sprint C. |
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
| **20-ter** | Defaults DDL no auditados en DAOs fuera del Sprint D (Pedido, Cliente, Empleado, Nomina, PagoPedido, PagoMaterial, Material) | **NUEVA, ABIERTA.** Auditoría pendiente. Riesgo desconocido. |
| 21 | Mapeo automático no reconoce `numero` ni `nif` en spec Albarán | ABIERTA. Riesgo bajo, UX. |
| 22 | Mensaje de error `cliente_nif` dice "para el pedido" en albarán/factura/presupuesto | ABIERTA. Trivial. |
| 23 | Diálogo de mapeo no lista campos de línea en desplegables | ABIERTA. Informativa. |

---

## PRÓXIMOS SPRINTS CANDIDATOS

### C. Sprint de empleados inactivos (Deuda 2)
Filtro `activo=1` en `resolverEmpleadoId` rompe nóminas históricas. Coste bajo, riesgo bajo. Tres opciones: quitar filtro, parámetro `incluirInactivos`, o solo documentar workaround.

### D-ter. Auditoría de defaults DDL en DAOs restantes (Deuda 20-ter)
Auditar PedidoDAO, ClienteDAO, EmpleadoDAO, NominaDAO, PagoPedidoDAO, PagoMaterialDAO, escritura inicial de MaterialDAO. Aplicar patrón ya validado en Sprint D donde corresponda. Coste medio (más DAOs), riesgo bajo (patrón conocido).

### D-bis. Defaults DDL numéricos primitivos (Deuda 20-bis)
Cambio de modelo `double`→`Double` boxed para campos con DEFAULT DDL no-trivial (`iva_porcentaje`, `salario_base`, etc.). Blast radius medio-alto: toca modelos, maps de ResultSet, posiblemente UI. **Coste-beneficio dudoso porque el bug es teórico** (todos los flujos setean explícitamente). Candidata baja prioridad.

### Refactor B2. Inyectar Connection en DAOs
Eliminar dependencia del singleton estático. DAOs reciben Connection por constructor o por método. Refactor amplio, requiere tocar todos los DAOs y servicios. **Después de C, D-ter o D-bis.**

### Bundle pequeño
- Deudas 21, 22, 23 (smoke Sprint A). Coste muy bajo.
- Deudas 8, 15, 17, 18, 18-bis.

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

**Tests JDBC del Sprint B y Sprint D:**
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

El usuario abrirá un chat nuevo y pegará este documento entero. Mi primer mensaje debe ser:

1. **Confirmar contexto cargado:** HEAD `8ef936a` o `docs:` v3.9 inmediatamente encima, 71/71 verdes, Sprint D CERRADO, Deudas 20 cerrada, 20-bis y 20-ter abiertas.

2. **Pedir verificación de estado:**
  - `git log --oneline -8` — confirmar HEAD y los 3 commits del Sprint D + handoff v3.9 + 5 commits del Sprint B + handoff v3.8.
  - `git status` — working tree limpio.
  - `.\mvnw.cmd test` — 71/71 verdes.

3. **Preguntar qué sprint arrancar:** C (empleados inactivos), D-ter (auditoría defaults DDL restantes), D-bis (defaults numéricos primitivos), Refactor B2, bundle pequeño, u otro.

4. **Si la verificación de estado revela divergencia** respecto a `8ef936a` o `docs:` v3.9 encima, diagnosticar antes de avanzar (`git log --oneline -10`).

FIN DEL HANDOFF.
