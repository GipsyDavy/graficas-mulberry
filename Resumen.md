# HANDOFF — Graficas Mulberry · Sprint Transacciones DAOs CERRADO
# Versión: 3.8 · Fecha cierre: 22/05/2026 · Checkpoint: Sprint B (transacciones DAOs) CERRADO COMPLETO. 5 commits del sprint en master, 68/68 tests verdes. Deuda 9 cerrada definitivamente.

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
- **Codex prefiere inserción aditiva sobre reemplazo cuando puede.** Lección 4a.1. **Truco aplicado en 4b/5b/6 y validado en todo Sprint B:** incluir las líneas a preservar tanto en `old_str` como en `new_str` blinda el resultado.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Lección 4a.2. Alternativas robustas:
  - `findstr /N /C:"X \"Y\"" archivo` (con `/C:` literal)
  - `Select-String -Path 'archivo' -Pattern 'X .Y.'` (regex, el `.` cubre la comilla)
- **Si una "deuda latente" toca escritura de FK y los tests sintéticos arrancan con `PRAGMA foreign_keys=ON`, NO es latente: es bug seguro.** Lección 4a.2 final.
- **`Nothing to compile - all classes are up to date` NO es prueba de que compila.** Lección 4a.1. Tras editar un archivo, usar `.\mvnw.cmd clean compile` para forzar recompilación real desde cero.
- **Si un `clean compile` falla con `Failed to delete` en `target/classes`, es bloqueo de archivo Windows.** Lección 5a.1. Solución antes de Maven: `Get-Process java | Stop-Process -Force`.
- **No instanciar APIs sin haberlas leído.** Lección 5a.1. Antes de dictar `new X(...)`, leer X.java o un archivo que ya lo use bien.
- **SQLite no aplica DEFAULT cuando se pasa NULL explícito vía `setString`/`setInt`.** Lección 5a.3.
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
**Implicación crítica:** todos los DAOs y el servicio comparten el mismo objeto Connection. La atomicidad cross-DAO funciona porque `setAutoCommit(false)` aplica al singleton y los DAOs heredan el estado. **Tras Sprint B, los DAOs ahora detectan tx externa explícitamente y respetan el contrato.**

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

### Override de URL para tests JDBC reales
`DatabaseManager.buildDbUrl()` lee `System.getProperty("graficas.mulberry.db.url")` antes de calcular la ruta default en `%LOCALAPPDATA%`. **Esto desbloquea harness JDBC con BD efímera por `@TempDir` sin tocar DatabaseManager.**

Patrón canónico de tests JDBC (replicado en 5 tests del Sprint B y existente en los `EntityImportService*Test`):
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
- **Codex (en IDE):** edición local, ejecución de comandos, parches quirúrgicos. Ejecutor de bloques blindados. Tendencia confirmada a inserción aditiva sobre reemplazo. **Validado en todo Sprint B**: el truco "líneas idénticas en old_str y new_str" funciona sistemáticamente.
- **Gemini (en IDE):** contexto amplio, arquitectura, segunda opinión. Usado UNA VEZ en Bloque 5a para dictamen `BEGIN DEFERRED`. **No usado en Sprint B.**

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
- **NUEVA Sprint B:** Bloque al menos debe incluir pre-requisito de lectura explícito ("LEE Y VERIFICA") antes de las ediciones. Codex puede entonces parar y reportar si encuentra discrepancia en setters o constraints.

**Convención de commits:** un bloque = un commit + push. Mensaje con título imperativo (`feat:`, `fix:`, `docs:`, `test:`) ≤72 chars, línea en blanco, cuerpo con párrafos. Editor configurado: `git config --global core.editor "notepad"`. Multilínea complejo: archivo temporal + `git commit -F archivo.txt`.

---

## SPRINTS PREVIOS — RESUMEN

- **Sprint Importación CSV** (handoff v3.6, HEAD `74f174c`): 9 entidades importables, 56 tests verdes (2+12+11+9+5+10+7).
- **Sprint A — Smoke Test manual de Albaranes** (sin commit de código): PASA. 3 deudas nuevas registradas (21, 22, 23).

---

## SPRINT ACTUAL — TRANSACCIONES EXPLÍCITAS EN DAOS (Sprint B) — CERRADO

### Resultado
**ÉXITO COMPLETO.** 5 commits, 5 métodos de producción refactorizados, 9 tests nuevos (12 sumando los 3 cross-DAO de `TxAnidadaTest`), 68/68 verdes al cierre. Cero regresiones.

### Hallazgo principal del análisis (mantiene de v3.7)

La Deuda 9 original estaba mal descrita. Realidad:
1. **`EntityImportService.insertarFilas` e `insertarGrupos` SÍ tienen tx explícita.** No aplica el bug ahí.
2. **Los DAOs NO tenían tx envolvente en `save()`/`crearDesde*`.** Sí estaba el bug.
3. **Funcionaba en el importador por accidente del singleton** (DAOs heredaban autocommit-off de la tx del servicio sin saberlo).
4. **El bug real ocurría en UI directa.** `PresupuestosView.editar` → `PresupuestoDAO.save(p)` con autocommit-on → 2 tx separadas → riesgo si crash entre cabecera y líneas.
5. **`crearDesde*` agravaban el problema.** Cadenas largas con N tx separadas (save + N ajustarStock + updateEstado).

**Tras Sprint B**: todos los DAOs detectan tx externa explícitamente y respetan el contrato. La atomicidad funciona ahora por diseño, no por accidente.

### Decisiones de diseño cerradas

| ID | Decisión | Valor cerrado | Notas finales |
|---|---|---|---|
| D-B-SCOPE | ¿Solo `save()` o también `crearDesde*`? | **B (ambos)** | Cerrada en análisis. Aplicada en 1c, 1d. |
| D-B-CONN | ¿De dónde sale la Connection en DAOs? | **A (singleton sin más)** | YAGNI. Refactor B2 sigue siendo candidato post-B. |
| D-B-CONFLICTO | ¿Cómo evitar que `save()` del DAO commitee la tx del importador? | **A (patrón "tx interno solo si no hay externo")** | Implementado y validado runtime en TxAnidadaTest. |
| D-B-CREARDESDE-ALCANCE | ¿`descontarMateriales` también dentro de tx de `crearDesdePresupuesto`? | **A (sí)** | Aplicado a `FacturaDAO.crearDesdePresupuesto`. En `AlbaranDAO.crearDesde*` la cadena era de 1 sola escritura, igual envueltos por simetría. |
| D-B-AJUSTARSTOCK | ¿Refactorizar `MaterialDAO.ajustarStock` con el mismo patrón? | **A (sí)** | Hecho en sub-bloque 1b. **Pre-requisito de 1c.** |
| D-B-TESTS | ¿Tests sintéticos de rollback? | **Sí, 4 tests mínimo** | Cerrados 9 tests reales (2+2+2+3) + 3 cross-DAO en TxAnidadaTest. **Superado mínimo.** |
| D-B-ENTIDADES | ¿Solo los 3 parent-child o también los planos? | **Solo 3 parent-child + `MaterialDAO.ajustarStock`** | Aplicado. |
| D-B-HELPER | ¿Patrón inline o helper centralizado? | **A (inline)** | Decisión arrancada en esta sesión. 7 copias del idiom de 11-13 líneas. Karpathy puro. |
| D-B-TESTS-ENFOQUE | ¿Cómo verificar el rollback en tests? | **α puro (BD real efímera vía override)** | Decisión arrancada en esta sesión tras descubrir el override `graficas.mulberry.db.url`. Sin proxy de Connection (β descartado por innecesario). |

### Sub-bloques ejecutados (orden FINAL, no el del handoff v3.7)

**El orden del handoff v3.7 se reordenó porque MaterialDAO debía ir antes que los `crearDesde*` para evitar bug latente entre commits.** Plan ejecutado:

| Sub-bloque | Commit | Cambios | Tests | Notas |
|---|---|---|---|---|
| **1a** | `af412a8` | `PresupuestoDAO.save` tx-aware | +2 (`PresupuestoDAOTest`) | Primer sub-bloque. Sienta el precedente del idiom. |
| **1b** | `f6efb9a` | `MaterialDAO.ajustarStock` con detección de tx externa | +2 (`MaterialDAOTest`) | Reordenado antes de 1c. Pre-requisito para `crearDesde*`. Validó "read your own writes" dentro de tx. |
| **1c** | `c1a604d` | `FacturaDAO.save` + `crearDesdePresupuesto` tx-aware (cadena completa incluida) | +2 (`FacturaDAOTest`) | **Falló en primer intento.** El truco "UPDATE descripcion=NULL" no funcionó porque `lineas_presupuesto.descripcion` es `NOT NULL`. Patch aplicado: forzar colisión UNIQUE en `facturas.numero` reservando + reseteando `siguiente_factura`. |
| **1d** | `b017b7d` | `AlbaranDAO.save` + `crearDesdeFactura` + `crearDesdePresupuesto` tx-aware | +3 (`AlbaranDAOTest`) | Tres tests por simetría con los tres métodos. Usa colisión UNIQUE en `albaranes.numero`. |
| **1e** | `d60359e` | (sin código de producción) | +3 (`TxAnidadaTest`) | Verificación cross-DAO end-to-end del patrón. Simula flujo del importador sin tocar el servicio. |
| **1f** | (este handoff) | `Resumen.md` v3.8 | 0 | Cierre documental. |

### Aplicación final del patrón canónico

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

Aplicado uniformemente en los 7 sitios:
1. `PresupuestoDAO.save`
2. `FacturaDAO.save`
3. `FacturaDAO.crearDesdePresupuesto` (envuelve `generarNumeroFactura` + `save(f)` + `descontarMateriales` + `pDao.updateEstado`)
4. `AlbaranDAO.save`
5. `AlbaranDAO.crearDesdeFactura` (envuelve `generarNumeroAlbaran` + `save(a)`)
6. `AlbaranDAO.crearDesdePresupuesto` (envuelve `generarNumeroAlbaran` + `save(a)`)
7. `MaterialDAO.ajustarStock`

**Decisión sub-fina (no reabrir):** se eligió `setAutoCommit(true)` incondicional en finally, no `setAutoCommit(prevAC)`. Verificado en `PresupuestosView` que ningún caller de UI directa entra con autocommit-off por motivo distinto a tx externa. Si en el futuro aparece un caller que sí lo haga, refactorizar los 7 sitios a la vez (cambio mecánico).

### Errores cometidos en esta sesión (para no repetirlos)

1. **Asumí que UPDATE descripcion=NULL funcionaría en test de FacturaDAO.** No funcionó porque la BD aplica NOT NULL en UPDATE igual que en INSERT. Aprendí: para tests de rollback de cadenas DAO, la técnica robusta es **colisión UNIQUE en columnas de número** (reservar número + pre-crear + resetear `siguiente_X` + invocar `crearDesde*`).
2. **Dicté un bloque de test con try-with-resources duplicado y roto.** El primer bloque tenía un `try` que no hacía nada y el segundo era el real. Codex no llegó a ejecutar (el usuario anuló a mitad). Lección de la línea "Releer el bloque redactado antes de pegarlo" volvió a aplicar. Redicté limpio.
3. **Olvidé pedir `MaterialDAO.java` al inicio del análisis y casi recomiendo sprint B sin haber leído el código.** Reculé tras leer. (Es la misma lección de v3.7, archivada en sesión anterior.)

### Archivos leídos esta sesión (acumulados sobre v3.7)

**De v3.6/v3.7 ya inspeccionados (no pedir de nuevo):**
- `CLAUDE.md`, `MIGRACION_HISTORICO.md`.
- `DuplicatePolicy.java`, `ColumnMatcher.java`.
- `Tarifa.java`, `Pedido.java`, `Presupuesto.java`, `Factura.java`, `Albaran.java` con IMPORT_SPEC.
- `LineaPresupuesto.java`, `LineaFactura.java`, `LineaAlbaran.java`.
- `PedidoDAO.java`.
- `PresupuestosView.java`, `FacturasView.java`, `AlbaranesView.java`.
- `TarifasView.java`, `NominasView.java`, `PedidosView.java`.
- `EntityImportSpec.java`, `FieldSpec.java`.
- `EntityImportServicePedidoTest.java`, `EntityImportServicePresupuestoTest.java`, `EntityImportServiceFacturaTest.java`, `EntityImportServiceAlbaranTest.java`.

**Nuevos en esta sesión (Sprint B 1a-1f):**
- `PresupuestoDAO.java` — modificado (1a).
- `MaterialDAO.java` — modificado (1b).
- `FacturaDAO.java` — modificado (1c).
- `AlbaranDAO.java` — modificado (1d).
- `DatabaseManager.java` — leído parcialmente. Override de URL confirmado (línea 17-20). DDL de `facturas` confirmado UNIQUE en numero (línea 263).
- `ClienteDAOTest.java` — leído como referencia (patrón Proxy, no JDBC real).
- `EntityImportServicePresupuestoTest.java` — leído. Reveló el harness JDBC con `@TempDir`.

**Nuevos archivos creados en Sprint B:**
- `PresupuestoDAOTest.java`
- `MaterialDAOTest.java`
- `FacturaDAOTest.java`
- `AlbaranDAOTest.java`
- `TxAnidadaTest.java`

---

## ESTADO TÉCNICO AL CIERRE

### Git
- **Rama:** `master`
- **HEAD esperado tras commit del v3.8:** `docs:` v3.8 encima de `d60359e`.
- **HEAD actual (antes del commit del handoff):** `d60359e` — `test: tx anidada cross-DAO simulando importador`.
- **Sincronizado con `origin/master`.**
- **Commits del Sprint B (5):** `af412a8`, `f6efb9a`, `c1a604d`, `b017b7d`, `d60359e`.
- **Working tree:** sólo `Resumen.md` modificado (v3.8 a commitear).

### Tests
- **68/68 verdes.** Reparto:
  - `ClienteDAOTest`: 2
  - `PresupuestoDAOTest`: 2 (nuevo)
  - `MaterialDAOTest`: 2 (nuevo)
  - `FacturaDAOTest`: 2 (nuevo)
  - `AlbaranDAOTest`: 3 (nuevo)
  - `TxAnidadaTest`: 3 (nuevo)
  - `ImportBackupServiceTest`: 12
  - `EntityImportServiceAlbaranTest`: 11
  - `EntityImportServiceFacturaTest`: 9
  - `EntityImportServiceNominaTest`: 5
  - `EntityImportServicePedidoTest`: 10
  - `EntityImportServicePresupuestoTest`: 7

### Estado de los archivos clave tras Sprint B

- `PresupuestoDAO.save()` — tx-aware. Detecta tx externa.
- `FacturaDAO.save()` — tx-aware.
- `FacturaDAO.crearDesdePresupuesto()` — tx atómica completa (generarNumero + save + descontarMateriales + updateEstado).
- `AlbaranDAO.save()` — tx-aware.
- `AlbaranDAO.crearDesdeFactura()` — tx atómica (generarNumero + save).
- `AlbaranDAO.crearDesdePresupuesto()` — tx atómica (generarNumero + save).
- `MaterialDAO.ajustarStock()` — tx-aware. Respeta tx externa de `descontarMateriales`.
- `EntityImportService.insertarFilas`/`insertarGrupos` — sin cambios. Funciona idéntico, ahora con garantías arquitectónicas explícitas (los DAOs respetan su tx en vez de heredarla por accidente).
- `DatabaseManager.getConnection()` — sin cambios.

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
| **9** | `*.save()` sin transacción explícita BEGIN/COMMIT | **CERRADA en Sprint B.** Patrón "tx solo si no hay externa" aplicado a los 7 sitios identificados. Verificado runtime con 12 tests (9 unit + 3 cross-DAO). |
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
| 20 | Defaults DDL ignorados con NULL explícito | DETECTADA en 5a.3, mitigada solo en Albarán. Candidata sprint D. |
| 21 | Mapeo automático no reconoce `numero` ni `nif` en spec Albarán (probablemente Factura y Presupuesto también) | ABIERTA. Riesgo bajo, UX. |
| 22 | Mensaje de error `cliente_nif` dice "para el pedido" en albarán/factura/presupuesto | ABIERTA. Trivial. |
| 23 | Diálogo de mapeo no lista campos de línea en desplegables (comportamiento intencional) | ABIERTA. Informativa. |

**Deuda 9: cierre formal.** Los 7 sitios refactorizados están cubiertos por tests en `PresupuestoDAOTest`, `FacturaDAOTest`, `AlbaranDAOTest`, `MaterialDAOTest` y `TxAnidadaTest`. Lo único que queda como nota arquitectónica es el acoplamiento implícito del singleton Connection, que se aborda en el Refactor B2 (candidato).

---

## PRÓXIMOS SPRINTS CANDIDATOS

### C. Sprint de empleados inactivos (Deuda 2)
Filtro `activo=1` en `resolverEmpleadoId` rompe nóminas históricas. Coste bajo, riesgo bajo. Tres opciones: quitar filtro, parámetro `incluirInactivos`, o solo documentar workaround.

### D. Sprint de defaults DDL ignorados con NULL (Deuda 20)
Auditar todos los DAOs y DEFAULTs del DDL. Candidatos identificados: `forma_pago` en `FacturaDAO`, `condiciones` en `PresupuestoDAO`. Coste bajo-medio.

### Refactor B2 (Sprint propio, complejo). Inyectar Connection en DAOs
Eliminar la dependencia del singleton estático. DAOs reciben Connection por constructor o por método. Permite tests más limpios sin el harness del override de system property. **Después del sprint C o D.** Refactor amplio, requiere tocar todos los DAOs y servicios.

### Otros candidatos menores (no requieren sprint completo)
- Deudas 21, 22, 23 (smoke Sprint A). Bundle pequeño.
- Deudas 8, 15, 17, 18, 18-bis.

---

## ARCHIVOS YA INSPECCIONADOS — NO PEDIRLOS DE NUEVO

Ver sección "Archivos leídos esta sesión (acumulados sobre v3.7)" arriba.

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

1. **Confirmar contexto cargado:** HEAD `d60359e` o `docs:` v3.8 inmediatamente encima, 68/68 verdes, Sprint B CERRADO, Deuda 9 cerrada.

2. **Pedir verificación de estado:**
   - `git log --oneline -7` — confirmar HEAD y los 5 commits del Sprint B + handoff v3.8.
   - `git status` — working tree limpio.
   - `.\mvnw.cmd test` — 68/68 verdes.

3. **Preguntar qué sprint arrancar:** C (empleados inactivos), D (defaults DDL), Refactor B2 (Connection injection), bundle pequeño (21+22+23 o 8+15+17+18+18-bis), u otro.

4. **Si la verificación de estado revela divergencia** respecto a `d60359e` o `docs:` v3.8 encima, diagnosticar antes de avanzar (`git log --oneline -10`).

FIN DEL HANDOFF.
