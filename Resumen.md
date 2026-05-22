# HANDOFF — Graficas Mulberry · Sprint Transacciones DAOs
# Versión: 3.7 · Fecha cierre: 22/05/2026 · Checkpoint: Sprint A (smoke Albaranes) CERRADO sin commit. Sprint B (transacciones DAOs) ARRANCADO en análisis. 6 decisiones de diseño cerradas, D-B-HELPER pendiente. Bloque 1 (plan partido en 7 sub-bloques) pendiente de redactar tras decidir D-B-HELPER.

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
- **Pide el archivo antes de redactar bloques sobre métodos concretos.** Lección 3C-paso-2c, 3C-paso-3a, Bloque 4a y reforzada en Bloque 5a.1: no fiarse del recuerdo del handoff sobre la forma exacta de un método o API si la sesión actual no lo ha leído. Pedir y leer es barato; redactar a ciegas un bloque carácter-por-carácter es caro.
- **Regla de inicio "_Gipsybuho_, procedo a ejecutar..."** del `CLAUDE.md`: aplícala solo en mensajes conversacionales claros. En respuestas técnicas, bloques, parches, listas de pasos, no aplica.
- **Releer el bloque redactado antes de pegarlo.** Lección 3C-paso-3a: detecté dos basuras en mi propia redacción al releer (ternario inútil `apellidos.isBlank() ? nombre : nombre` y línea sentinela rara con comparación absurda). Releer una vez antes de dar por bueno el bloque cazó ambos. Releer es barato.
- **Si un archivo aparece como modificado al arrancar sesión sin commit previo identificable, declararlo explícitamente.** Lección 3C-paso-3b: `Resumen.md` arrastró cambios v3.2 entre sesiones porque nunca se commiteó y se acumuló como "ruido" durante 3a y 3b. En el primer pase de cada sesión, verificar `git diff --stat` y declarar scope del archivo no-commiteado antes de empezar a editar nuevos archivos.
- **Si Codex declara un cambio funcional no pedido, parar y pedir el diff antes de aprobar.** Lección 3C-paso-3a: Codex añadió default `fecha = LocalDate.now().toString()` para que sus tests pasaran. Aunque los tests estén verdes, revisar diff antes del commit y, si el cambio se acepta, declararlo como decisión explícita en el mensaje.
- **Codex prefiere inserción aditiva sobre reemplazo cuando puede.** Lección 4a.1: dicté "reemplaza dos líneas de imports por estas siete" y Codex añadió las nuevas sin borrar las viejas. **Truco aplicado en 4b/5b/6:** incluir las líneas a preservar tanto en `old_str` como en `new_str` blinda el resultado contra ambos comportamientos.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Lección 4a.2. Alternativas robustas:
  - `findstr /N /C:"X \"Y\"" archivo` (con `/C:` literal)
  - `Select-String -Path 'archivo' -Pattern 'X .Y.'` (regex, el `.` cubre la comilla)
- **Si una "deuda latente" toca escritura de FK y los tests sintéticos arrancan con `PRAGMA foreign_keys=ON`, NO es latente: es bug seguro.** Lección 4a.2 final. Tratarla como bug a fix, no como deuda diferible.
- **`Nothing to compile - all classes are up to date` NO es prueba de que compila.** Lección 4a.1: Maven se saltó la recompilación porque los `.class` ya estaban frescos respecto al `.java` antiguo. Tras editar un archivo, usar `.\mvnw.cmd clean compile` para forzar recompilación real desde cero.
- **Si un `clean compile` falla con `Failed to delete` en `target/classes`, es bloqueo de archivo Windows.** Lección 5a.1: app JavaFX colgada bloqueando recursos. Solución antes de Maven: `Get-Process java | Stop-Process -Force`.
- **No instanciar APIs sin haberlas leído.** Lección 5a.1: dicté `new ColumnMatcher(Map.ofEntries(...))` sin haber leído `ColumnMatcher.java`. Es una `@FunctionalInterface`, no una clase concreta. Antes de dictar `new X(...)`, leer X.java o un archivo que ya lo use bien.
- **SQLite no aplica DEFAULT cuando se pasa NULL explícito vía `setString`/`setInt`.** Lección 5a.3: aplicar el default en Java en `ensamblarX()` antes del save. Aplica a cualquier columna con DEFAULT cuyo setter pueda recibir null.
- **No diagnosticar deudas técnicas sin leer el código que las realizaría.** Lección Sprint B análisis: etiqueté la Deuda 9 ("`*.save()` sin transacción explícita") como abierta basándome en el nombre, sin haber leído `EntityImportService.insertarFilas`/`insertarGrupos`. **Sí hay tx explícita** en el servicio (setAutoCommit(false) + Savepoint por grupo + commit). La deuda real es distinta: los DAOs no la tienen cuando se invocan desde UI o desde flujos `crearDesde*`. Antes de afirmar "X está roto", leer el código que haría X.
- **Cuando el usuario aporta información casual ("no hay nif, usa este") interpretarla con cuidado.** Sesión Sprint A: lo interpreté como "usa el placeholder de todos modos" cuando podía significar literalmente "no tengo NIFs en mi BD". El smoke detectó el malentendido (ERROR `cliente no encontrado`). No es bug pero gastó un ciclo. Si la frase del usuario es ambigua, preguntar antes de actuar.
- **Las capturas de UI contienen información que reemplaza preguntas.** Sesión Sprint A: pedí al usuario que listara las opciones del desplegable cuando una captura del desplegable abierto resuelve la pregunta directa. Si una captura puede responder, pedirla en vez de pedir descripción manual.

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
- `src/test/java/org/gipsybuho/service/importer/` — tests del importador.
- `src/main/java/org/gipsybuho/db/DatabaseManager.java` — DDL inline en Java. Activa `PRAGMA foreign_keys = ON`. **Connection es singleton estático.**

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
**Implicación crítica para sprint B:** todos los DAOs y el servicio comparten el mismo objeto Connection. La atomicidad del importador funciona porque `setAutoCommit(false)` aplica al singleton y los DAOs heredan el estado sin saberlo. **Acoplamiento implícito.**

### Patrón transaccional canónico en este repo
Ya implementado en `MaterialDAO.ajustarStock` (~línea 65) y en `EntityImportService.insertarFilas`/`insertarGrupos` (~líneas 300-365):
```java
boolean prevAC = conn.getAutoCommit();
conn.setAutoCommit(false);
try {
    // trabajo SQL
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(prevAC);
}
```
**Pero `ajustarStock` no detecta tx externa.** Si un caller ya tiene tx abierta, `commit()` la cierra prematuramente. Sprint B corrige este idiom a la variante "tx solo si no hay externa".

---

## WORKFLOW MULTI-IA

**No hay invocación directa por CLI desde mí (Claude consumer).** El flujo es:

1. **Yo (Claude consumer en el chat)** coordino el sprint, redacto bloques autocontenidos.
2. **Tú** pegas el bloque en el chat del agente correspondiente dentro de IntelliJ IDEA.
3. **El agente del IDE** (Claude Code / Codex / Gemini) ejecuta.
4. **Tú** me pegas la respuesta del agente y yo evalúo si seguir, corregir o pedir fix-up.

**Roles según `CLAUDE.md`:**
- **Claude Code (en IDE):** preferente para planificación, revisión final, calidad, seguridad, tests, cumplimiento de reglas.
- **Codex (en IDE):** edición local, ejecución de comandos, parches quirúrgicos. Ejecutor de bloques blindados. Tendencia confirmada a inserción aditiva sobre reemplazo.
- **Gemini (en IDE):** contexto amplio, arquitectura, segunda opinión. Usado UNA VEZ en Bloque 5a para dictamen `BEGIN DEFERRED`.

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

**Convención de commits:** un bloque = un commit + push. Mensaje con título imperativo (`feat:`, `fix:`, `docs:`) ≤72 chars, línea en blanco, cuerpo con párrafos. Editor configurado: `git config --global core.editor "notepad"`. Multilínea complejo: archivo temporal + `git commit -F archivo.txt`.

---

## SPRINT ANTERIOR — IMPORTACIÓN CSV (CERRADO en v3.6)

Las 9 entidades importan CSV. HEAD anterior `74f174c`. 56/56 tests verdes (2 + 12 + 11 + 9 + 5 + 10 + 7). MIGRACION_HISTORICO.md actualizado. Detalles completos en handoff v3.6 (no se replican aquí).

---

## SPRINT A — SMOKE TEST MANUAL DE ALBARANES (CERRADO sin commit)

### Resultado
**PASA.** El cableado completo de la importación CSV de Albaranes funciona en runtime.

### Pasos ejecutados
1. `Get-Process java | Stop-Process -Force` (limpieza previa).
2. `.\mvnw.cmd javafx:run` — app arrancó con `BUILD SUCCESS` en 1:05.
3. Generado `smoke_albaran.csv` en Escritorio con 1 cabecera + 2 líneas, NIF `B12345678`:
   ```csv
   numero,fecha,nif,observaciones,descripcion,cantidad,unidad
   ALB-SMOKE-001,2026-05-22,B12345678,Smoke test sprint A,Producto de prueba A,10,uds
   ALB-SMOKE-001,2026-05-22,B12345678,Smoke test sprint A,Producto de prueba B,5,cajas
   ```
4. Albaranes → `📥 Importar` → **FileChooser abierto** (no Alert). Cableado 5b confirmado en runtime.
5. Diálogo "Configurar importación de Albaranes" → 7 columnas detectadas. **Mapeo automático falló para `numero` y `nif`** (quedaron en `(ignorar)`). Resto OK.
6. Primer intento: NIF no existía en BD → "0 importadas, 2 descartadas". Error: *"Cliente con nif 'B12345678' no encontrado para el pedido"*. Decisión D5 funcional confirmada en runtime.
7. Cliente creado manualmente con NIF `B12345678` (nombre "Cliente Smoke Test") vía UI.
8. Re-importación: **"1 importada, 0 actualizadas, 0 descartadas"**.
9. Albarán `ALB-SMOKE-001` apareció en tabla con cliente correcto, fecha `2026-05-22`, estado **PENDIENTE** (default Java D-A-EST aplicado correctamente en runtime — lección 5a.3 confirmada).
10. Limpieza manual: borrado albarán + cliente desde UI.

### Hallazgos del smoke (Deudas nuevas registradas)

| ID | Descripción | Severidad |
|---|---|---|
| 21 | Mapeo automático no reconoce `numero` ni `nif` en spec de Albarán; usuario debe mapearlos a mano cada vez. Probablemente también afecta a Factura y Presupuesto. | Riesgo bajo, UX. |
| 22 | Mensaje de error `cliente_nif` dice *"no encontrado para el pedido"* cuando es albarán/factura/presupuesto. Copia-pega no parametrizado en `EntityImportService.resolverClienteId`. | Trivial, cosmético. |
| 23 | Diálogo de mapeo no lista campos de línea (`descripcion`, `cantidad`, `unidad`) en los desplegables pero acepta el mapeo igual. Probablemente diseño consciente; documentar. | Informativa. |

### Cierre Sprint A
- **No hay commit de código** (smoke pasó, no se modificó nada).
- Datos de prueba borrados de BD.
- 3 deudas nuevas registradas.

---

## SPRINT ACTUAL — TRANSACCIONES EXPLÍCITAS EN DAOS (Sprint B)

### Estado: análisis en curso, parado en D-B-HELPER pendiente

### Contexto del sprint

**Lectura crítica realizada en esta sesión:** `PresupuestoDAO.java`, `FacturaDAO.java`, `AlbaranDAO.java`, `MaterialDAO.java`, `EntityImportService.java`, `DatabaseManager.java`.

### Hallazgo principal — redefine la Deuda 9

La Deuda 9 (`*.save() sin transacción explícita BEGIN/COMMIT`) estaba **mal descrita**. Realidad:

1. **`EntityImportService.insertarFilas` e `insertarGrupos` SÍ tienen tx explícita.** `setAutoCommit(false)` + Savepoint por grupo + `commit()` + finally con restore. Aplica al importador desde el día 1.
2. **Los DAOs NO tienen tx envolvente en `save()`.** `PresupuestoDAO.save(p)` hace `insert(p)` + `saveLineas(p)` en autocommit-on → **dos transacciones separadas**. Si crashea entre medias, cabecera queda con líneas viejas (saveLineas hace DELETE+INSERT). Mismo bug en `FacturaDAO.save` y `AlbaranDAO.save`.
3. **Funciona en el importador por accidente del singleton.** `DatabaseManager.getConnection()` devuelve el mismo objeto a servicio y DAOs. El `setAutoCommit(false)` del servicio aplica al DAO sin que el DAO lo sepa. Si alguien cambia el singleton, la atomicidad del importador se rompe sin error visible.
4. **El bug real ocurre en UI directa.** Botón Guardar de `PresupuestosView.editar` → llama `PresupuestoDAO.save(p)` con autocommit-on → 2 tx separadas → riesgo de inconsistencia si crash entre cabecera y líneas.
5. **`crearDesde*` agravan el problema.** `FacturaDAO.crearDesdePresupuesto` encadena `save(f)` + N llamadas a `descontarMateriales` (que a su vez llama a `ajustarStock`) + `pDao.updateEstado`. Cada uno es tx separada. Si crashea a mitad, factura creada + stock parcialmente descontado + presupuesto sin actualizar.

### Decisiones de diseño cerradas en esta sesión

| ID | Decisión | Valor cerrado | Justificación |
|---|---|---|---|
| D-B-SCOPE | ¿Solo `save()` o también `crearDesde*`? | **B (ambos)** | `crearDesde*` es donde la cadena larga puede dejar BD inconsistente. Esfuerzo extra bajo. |
| D-B-CONN | ¿De dónde sale la Connection en DAOs? | **A (singleton sin más)** | YAGNI. El refactor honesto (inyectar Connection en DAOs) es sprint propio, fuera de scope. |
| D-B-CONFLICTO | ¿Cómo evitar que `save()` del DAO commitee la tx del importador? | **A (patrón "tx interno solo si no hay externo")** | Estándar JDBC. Una línea: `boolean externalTx = !conn.getAutoCommit()`. Mantiene compatibilidad con importador sin tocarlo. |
| D-B-CREARDESDE-ALCANCE | ¿`descontarMateriales` también dentro de tx de `crearDesdePresupuesto`? | **A (sí)** | Atomicidad real: ni factura ni movimientos de stock si crash. |
| D-B-AJUSTARSTOCK | ¿Refactorizar `MaterialDAO.ajustarStock` con el mismo patrón? | **A (sí)** | Obligatorio para que CREARDESDE-ALCANCE=A funcione. Hoy `ajustarStock` no detecta tx externa y commitea prematuramente. Cambio: una línea. |
| D-B-TESTS | ¿Tests sintéticos de rollback? | **Sí, 4 tests mínimo** | Lección 5a.2: deuda con tx requiere prueba de rollback. 1 por DAO parent-child + 1 de tx anidada. |
| D-B-ENTIDADES | ¿Solo los 3 parent-child o también los planos? | **Solo 3 parent-child + `MaterialDAO.ajustarStock`** | Los DAOs planos (`PedidoDAO`, `NominaDAO`, `ClienteDAO`, `EmpleadoDAO`, `TarifaDAO`) tienen `save()` mono-operación, ya atómico por JDBC. |

### Decisión pendiente

| ID | Decisión | Opciones | Mi opinión |
|---|---|---|---|
| **D-B-HELPER** | ¿Patrón inline en cada método o helper centralizado en `DatabaseManager`? | **A:** Inline. 5 copias del idiom try/finally. Karpathy-style puro, código auto-documentado.<br>**B:** Helper `DatabaseManager.withTx(SqlAction action)` + `@FunctionalInterface SqlAction`. 1 sitio donde vive el patrón. | **A.** El parámetro `Connection conn` del callback B sería decorativo porque los privados siguen yendo al singleton (mentirilla arquitectónica). Cinco copias de 7 líneas no es deuda urgente y la repetición hace el código más legible. |

**Decidir D-B-HELPER es lo primero al arrancar la próxima sesión.** El usuario quedó conforme con la propuesta global "tu opinión" pero la decisión D-B-HELPER se planteó después de esa confirmación y quedó sin cerrar por falta de cuota.

### Plan de sub-bloques (pendiente de redactar tras D-B-HELPER)

Estructura prevista (7 sub-bloques):

- **1a** — Si D-B-HELPER=B: implementar helper `DatabaseManager.withTx` + interfaz `SqlAction`. Si D-B-HELPER=A: este sub-bloque desaparece.
- **1b** — `PresupuestoDAO.save()` envuelto en tx con patrón "tx solo si no hay externa" + test sintético de rollback (`PresupuestoDAOTest.saveRollbackOnLineaFailure` o equivalente).
- **1c** — `FacturaDAO.save()` envuelto en tx + `FacturaDAO.crearDesdePresupuesto` envuelto en tx (incluyendo `descontarMateriales` + `updateEstado`) + tests rollback.
- **1d** — `AlbaranDAO.save()` envuelto en tx + `AlbaranDAO.crearDesdeFactura` envuelto en tx + `AlbaranDAO.crearDesdePresupuesto` envuelto en tx + tests rollback.
- **1e** — `MaterialDAO.ajustarStock` refactor con patrón "tx solo si no hay externa". Cambio mínimo: una línea para detectar tx externa.
- **1f** — Test de tx anidada: simular flujo del importador (servicio abre tx, llama a `save`, `save` detecta tx externa, no commitea). Verificar que rollback del savepoint del servicio borra todo del grupo.
- **1g** — Handoff v3.8 con sprint B cerrado + actualización de Deuda 9 (cerrada o redescrita) + Deudas residuales.

**Convención commits sprint B:** un sub-bloque = un commit. Mensaje `feat: tx explicita en X.save()` o `feat: tx explicita en X.crearDesde*` o `test: rollback en X.save`.

### Archivos leídos en esta sesión que NO estaban en handoff v3.6

- `PresupuestoDAO.java` (estado actual al cierre Sprint A).
- `FacturaDAO.java` (estado actual al cierre Sprint A).
- `AlbaranDAO.java` (estado actual al cierre Sprint A).
- `MaterialDAO.java` (con `ajustarStock` transaccional pero sin detección de tx externa).

### Patrón canónico del sprint (referencia para redactar bloques)

```java
public void save(Presupuesto p) throws SQLException {
    Connection conn = DatabaseManager.getConnection();
    boolean externalTx = !conn.getAutoCommit();
    if (!externalTx) conn.setAutoCommit(false);
    try {
        if (p.getId() == 0) insert(p); else update(p);
        saveLineas(p);
        if (!externalTx) conn.commit();
    } catch (SQLException e) {
        if (!externalTx) conn.rollback();
        throw e;
    } finally {
        if (!externalTx) conn.setAutoCommit(true);
    }
}
```

Nota: `setAutoCommit(true)` en finally restaura a true incondicionalmente porque el patrón asume que callers sin tx externa esperan autocommit-on tras la llamada. **Si esto resulta ser falso al revisar callers reales, ajustar a `setAutoCommit(prevAC)`.** Verificar antes de redactar el bloque 1b.

---

## ESTADO TÉCNICO AL CIERRE DE ESTA SESIÓN

### Git
- **Rama:** `master`
- **HEAD:** `74f174c` — `docs: actualizar MIGRACION_HISTORICO.md con las 9 entidades importables`. Sincronizado con `origin/master`. **Sin cambios desde v3.6.**
- **Working tree:** sólo `Resumen.md` modificado (v3.7 en curso). Tras commit del v3.7 quedará limpio.
- **Commits relevantes del sprint anterior:** ver handoff v3.6 sección "Git" (no se replican aquí).
- **Sprint A no genera commits de código** (smoke pasó sin cambios).
- **Sprint B no ha generado commits** (solo análisis; sin Bloque 1 redactado todavía).

### Tests
- **56/56 verdes** al cierre Sprint anterior, sin cambios. Reparto: 2 + 12 + 11 + 9 + 5 + 10 + 7.
- Tras Sprint B: añadir ≥4 tests (1 por DAO parent-child + 1 tx anidada). Distribución exacta a determinar en redacción del Bloque 1.

### Estado de los archivos clave para Sprint B (post-lectura sesión actual)

- `PresupuestoDAO.save()` — 2 tx separadas (insert/update + saveLineas), autocommit-on.
- `FacturaDAO.save()` — 2 tx separadas.
- `FacturaDAO.crearDesdePresupuesto()` — ≥4 tx separadas (save + N ajustarStock + updateEstado).
- `AlbaranDAO.save()` — 2 tx separadas.
- `AlbaranDAO.crearDesdeFactura()` — 2 tx separadas (save).
- `AlbaranDAO.crearDesdePresupuesto()` — 2 tx separadas (save).
- `MaterialDAO.ajustarStock()` — **transaccional pero sin detección de tx externa.** Commit interno revienta tx del caller.
- `EntityImportService.insertarFilas`/`insertarGrupos` — transaccional con Savepoint por fila/grupo. **No tocar en sprint B.**
- `DatabaseManager.getConnection()` — singleton. **No tocar en sprint B.**

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
| 9 | `*.save()` sin transacción explícita BEGIN/COMMIT (DAOs cuando se invocan desde UI directa o desde flujos `crearDesde*`) | **REDESCRITA en sesión actual.** Sprint B activo. **NO aplica al importador** (ya tiene tx). Aplica a `PresupuestoDAO.save`, `FacturaDAO.save`, `FacturaDAO.crearDesdePresupuesto`, `AlbaranDAO.save`, `AlbaranDAO.crearDesdeFactura`, `AlbaranDAO.crearDesdePresupuesto`. |
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
| **21** | **Mapeo automático no reconoce `numero` ni `nif` en spec Albarán (probablemente Factura y Presupuesto también)** | **NUEVA en Sprint A.** Riesgo bajo, UX. |
| **22** | **Mensaje de error `cliente_nif` dice "para el pedido" en albarán/factura/presupuesto** | **NUEVA en Sprint A.** Trivial. |
| **23** | **Diálogo de mapeo no lista campos de línea en desplegables (comportamiento intencional, documentar)** | **NUEVA en Sprint A.** Informativa. |

---

## PRÓXIMOS SPRINTS CANDIDATOS (post Sprint B)

### C. Sprint de empleados inactivos (Deuda 2)
Filtro `activo=1` en `resolverEmpleadoId` rompe nóminas históricas. Coste bajo, riesgo bajo. Tres opciones: quitar filtro, parámetro `incluirInactivos`, o solo documentar workaround.

### D. Sprint de defaults DDL ignorados con NULL (Deuda 20)
Auditar todos los DAOs y DEFAULTs del DDL. Candidatos identificados: `forma_pago` en `FacturaDAO`, `condiciones` en `PresupuestoDAO`. Coste bajo-medio.

### Otros candidatos menores (no requieren sprint completo)
- Deudas 21, 22, 23 (smoke Sprint A). Bundle pequeño.
- Deudas 8, 15, 17, 18, 18-bis.
- Refactor B2 (inyectar Connection en DAOs) — sprint propio, complejo. Después de B.

---

## ARCHIVOS YA INSPECCIONADOS — NO PEDIRLOS DE NUEVO

Estos archivos están analizados y leídos. NO pedirlos de nuevo salvo cambio explícito:

**Del sprint anterior (v3.6):**
- `CLAUDE.md`, `MIGRACION_HISTORICO.md`.
- `DuplicatePolicy.java`, `ColumnMatcher.java`.
- `Tarifa.java`, `Pedido.java`, `Presupuesto.java`, `Factura.java`, `Albaran.java` con IMPORT_SPEC.
- `LineaPresupuesto.java`, `LineaFactura.java`, `LineaAlbaran.java`.
- `PedidoDAO.java`.
- `PresupuestosView.java`, `FacturasView.java`, `AlbaranesView.java`.
- `TarifasView.java`, `NominasView.java`, `PedidosView.java`.
- `EntityImportSpec.java`, `FieldSpec.java`.
- `EntityImportServicePedidoTest.java`, `EntityImportServicePresupuestoTest.java`, `EntityImportServiceFacturaTest.java`, `EntityImportServiceAlbaranTest.java`.

**Leídos en sesión actual (Sprint A + B análisis):**
- `PresupuestoDAO.java` — estado actual al cierre Sprint A.
- `FacturaDAO.java` — estado actual al cierre Sprint A.
- `AlbaranDAO.java` — estado actual al cierre Sprint A.
- `MaterialDAO.java` — estado actual al cierre Sprint A.
- `EntityImportService.java` — estado actual al cierre Sprint A.
- `DatabaseManager.java` — estado actual al cierre Sprint A.

---

## ERRORES COMETIDOS EN ESTA SESIÓN (para no repetirlos)

1. **Etiqueté Deuda 9 como "abierta y aplicable a importador" sin leer `insertarFilas`/`insertarGrupos`.** El importador ya tenía tx explícita. Lección: no diagnosticar deudas técnicas sin leer el código que las realizaría. Aplicada al redescribir Deuda 9 al final del análisis.

2. **Interpreté "no hay nif, usa este" del usuario como "usa el placeholder de todos modos".** Significaba literalmente "no tengo NIFs en mi BD". El smoke detectó el error vía ERROR `cliente no encontrado`. Lección: si frase del usuario es ambigua y la acción es costosa de revertir, preguntar antes.

3. **Pedí al usuario listar opciones de desplegable cuando una captura del desplegable abierto las habría dado.** Gastó un ciclo. Lección: si captura puede responder, pedir captura, no descripción manual.

4. **Recomendé sprint B sin haber leído el código del importador.** Tuve que recular públicamente cuando vi `insertarFilas` ya transaccional. Lección reforzada de Sprint anterior: pedir archivos antes de recomendar.

5. **Casi olvido `MaterialDAO.ajustarStock` en el scope.** El usuario subió `MaterialDAO.java` por iniciativa propia y solo entonces vi que `ajustarStock` también necesita el patrón refactorizado para que CREARDESDE-ALCANCE=A funcione. Lección: si un sprint envuelve un método que llama a otros DAOs, leer también esos DAOs antes de cerrar el scope.

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

1. **Confirmar contexto cargado:** HEAD `74f174c` o `docs:` v3.7 inmediatamente encima, 56/56 verdes, Sprint A cerrado sin commit, Sprint B en análisis parado en D-B-HELPER.

2. **Pedir verificación de estado:**
   - `git log --oneline -5` — confirmar HEAD en `74f174c` o `docs:` v3.7 encima.
   - `git status` — working tree limpio.
   - `.\mvnw.cmd test` — 56/56 verdes.

3. **Resolver D-B-HELPER inmediatamente.** Es lo único que bloquea redactar el Bloque 1. Recordar mi opinión cerrada: **A (inline)**. Justificación principal: el parámetro `Connection conn` del callback de un helper sería decorativo porque los métodos privados siguen llamando al singleton. Si el usuario confirma A, arrancar Bloque 1 con 6 sub-bloques (1a desaparece). Si confirma B, arrancar con 7 sub-bloques.

4. **Redactar Bloque 1** = plan consolidado del sprint con sub-bloques en orden, cada uno con archivo objetivo, decisión que aplica y criterio de éxito.

5. **Si la verificación de estado revela divergencia** respecto a `74f174c` o `docs:` v3.7 encima, diagnosticar antes de avanzar (`git log --oneline -10`).

FIN DEL HANDOFF.
