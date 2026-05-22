# HANDOFF — Graficas Mulberry · Sprint Importación CSV
# Versión: 3.6 · Fecha cierre: 22/05/2026 · Checkpoint: Sprint Importación CSV CERRADO. Bloque 6 commiteado (74f174c). Las 9 entidades importan CSV. MIGRACION_HISTORICO.md actualizado. Próximo sprint a decidir por el usuario.

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
- **Si un archivo aparece como modificado al arrancar sesión sin commit previo identificable, declararlo explícitamente.** Lección 3C-paso-3b: `Resumen.md` arrastró cambios v3.2 entre sesiones porque nunca se commiteó y se acumuló como "ruido" durante 3a y 3b; al revertir un fallo de `str_replace` se perdió ese trabajo intermedio. En el primer pase de cada sesión, verificar `git diff --stat` y declarar scope del archivo no-commiteado antes de empezar a editar nuevos archivos. PowerShell con encoding CP-850 muestra UTF-8 como salsa rara (`Versi├│n`, `ÔÇö`) y oculta cambios reales; usar `Format-Hex` cuando el diff se vea sospechoso.
- **Si Codex declara un cambio funcional no pedido, parar y pedir el diff antes de aprobar.** Lección 3C-paso-3a: Codex añadió default `fecha = LocalDate.now().toString()` para que sus tests pasaran. La decisión era defendible (análoga a D-IVA y D-EST) pero no estaba autorizada. Aunque los tests estén verdes, revisar diff antes del commit y, si el cambio se acepta, declararlo como decisión explícita en el mensaje.
- **Codex prefiere inserción aditiva sobre reemplazo cuando puede.** Lección 4a.1: dicté "reemplaza dos líneas de imports por estas siete" y Codex añadió las nuevas sin borrar las viejas. Funcionalmente equivalente, diff distinto del esperado (0 supresiones en lugar de 2). Cuando un `str_replace` afecta a líneas que también deben preservarse, conviene dictarlo como inserción pura ("inserta después de X estas N líneas") sin pedir reemplazo de líneas que pueden quedarse. **Truco aplicado en 4b/5b/6:** incluir las líneas a preservar tanto en `old_str` como en `new_str` blinda el resultado contra ambos comportamientos.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Lección 4a.2: el patrón terminó vacío y `findstr` devolvió todo el archivo. Alternativas robustas:
  - `findstr /N /C:"X \"Y\"" archivo` (con `/C:` literal)
  - `Select-String -Path 'archivo' -Pattern 'X .Y.'` (regex, el `.` cubre la comilla)
- **Si una "deuda latente" toca escritura de FK y los tests sintéticos arrancan con `PRAGMA foreign_keys=ON`, NO es latente: es bug seguro.** Lección 4a.2 final: etiqueté el bug de `Factura.presupuestoId=0` como deuda preexistente sin tocar, y los 3 tests sintéticos que no informaban presupuesto lo dispararon de inmediato. Si la deuda implica escribir un valor que infringe una constraint con FK activas, los tests la cazarán. Tratarla como bug a fix, no como deuda diferible.
- **`Nothing to compile - all classes are up to date` NO es prueba de que compila.** Lección 4a.1: Maven se saltó la recompilación porque los `.class` ya estaban frescos respecto al `.java` antiguo. Tras editar un archivo, usar `.\mvnw.cmd clean compile` para forzar recompilación real desde cero. Solo entonces `BUILD SUCCESS` cuenta como evidencia.
- **Si un `clean compile` falla con `Failed to delete` en `target/classes`, es bloqueo de archivo Windows.** Lección 5a.1: la app JavaFX del smoke test anterior seguía con un .jpg abierto; Maven no podía borrar el `target/`. Solución antes de Maven: `Get-Process java | Stop-Process -Force` para cerrar instancias colgantes. IntelliJ también puede bloquear recursos.
- **No instanciar APIs sin haberlas leído.** Lección 5a.1: dicté `new ColumnMatcher(Map.ofEntries(...))` sin haber leído `ColumnMatcher.java`. Es una `@FunctionalInterface`, no una clase concreta: se crea con lambda `h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn)`. El bloque falló en `javac` con "is abstract; cannot be instantiated". Antes de dictar `new X(...)`, leer X.java o leer un archivo que ya lo use bien (en este caso `Factura.java` tenía el patrón canónico).
- **SQLite no aplica DEFAULT cuando se pasa NULL explícito vía `setString`/`setInt`.** Lección 5a.3: el test `importaAlbaranConDosLineas` falló con `expected: <pendiente> but was: <null>` porque `AlbaranDAO.set()` hacía `ps.setString(6, null)` y eso sobrescribe el DEFAULT del DDL. Los DEFAULT del DDL solo aplican cuando la columna se omite del INSERT, no cuando se le pasa NULL explícito. Solución: aplicar el default en Java en `ensamblarX()` antes del save. Aplica a cualquier columna con DEFAULT cuyo setter pueda recibir null.

---

## CONTEXTO DEL PROYECTO

**Nombre:** Graficas Mulberry
**Ruta local:** `C:\Users\GipsyDavy\MAVEN\Graficas Mulberry`
**Stack:** Java 21 · JavaFX 21 · SQLite (vía JDBC) · Maven · JUnit 5
**Restricciones duras:** sin Lombok, sin Spring, sin servidores HTTP, sin nuevas dependencias salvo justificación.
**Build commands:** `.\mvnw.cmd compile`, `.\mvnw.cmd clean compile`, `.\mvnw.cmd test`, `.\mvnw.cmd package`, `.\mvnw.cmd javafx:run`. `mvn` directo NO está en el PATH; usar siempre el wrapper.
**Versión Maven:** 3.9.11 vía wrapper.
**Reglas operativas:** ver `CLAUDE.md` en la raíz del proyecto. Karpathy-style: cambios quirúrgicos, mínima modificación, YAGNI, no refactorizar lo que no está roto, código auto-documentado.

### Estructura relevante
- `src/main/java/org/gipsybuho/model/` — entidades de dominio.
- `src/main/java/org/gipsybuho/dao/` — acceso a datos JDBC.
- `src/main/java/org/gipsybuho/service/` — lógica de servicios. `EntityImportService` está aquí.
- `src/main/java/org/gipsybuho/service/importer/` — tipos auxiliares del importador: `EntityImportSpec`, `FieldSpec`, `ColumnMatcher` (FunctionalInterface), `DuplicatePolicy`, `ImportResult`, `RowError`, `ErrorTipo`.
- `src/main/java/org/gipsybuho/ui/` — vistas JavaFX.
- `src/test/java/org/gipsybuho/service/importer/` — tests del importador. **Convención:** todos los tests nuevos del importador van aquí.
- `src/main/java/org/gipsybuho/db/DatabaseManager.java` — DDL inline en Java (no hay `schema.sql`). Activa `PRAGMA foreign_keys = ON`; las FK se evalúan en todos los entornos, incluidos los tests con `@TempDir`.

### Documentos del proyecto que NO debes ignorar
- `CLAUDE.md` — reglas operativas, Multi-IA, cambios quirúrgicos.
- `MIGRACION_HISTORICO.md` — procedimiento Vía A para procesar archivos históricos del cliente. **Actualizado en Bloque 6 (`74f174c`):** ahora documenta las 9 entidades importables, modelo CSV ancho con cabecera repetida, FK opcionales (D5), defaults Java por entidad, limitaciones conocidas (Deudas 2, 3, 19, 20).

### Valores conocidos del enum `DuplicatePolicy`
`SKIP_IF_EXISTS` (default), `UPDATE_EXISTING`, `CREATE_NEW`. **NO existe `UPDATE_IF_EXISTS`** — error frecuente.

### Forma actual de `EntityImportSpec`
```java
public record EntityImportSpec(
    String nombre,
    List<FieldSpec> campos,
    ColumnMatcher matcher,
    DuplicatePolicy politicaDefecto,
    String claveAgrupacion,   // null en planos
    String campoLineas,       // null en planos
    EntityImportSpec specLinea // null en planos
)
```
Constructor canónico valida coherencia parent-child (los 3 últimos a null o los 3 no-null). Constructor secundario de 4 argumentos para retrocompatibilidad con specs planos. Helper `esParentChild()` devuelve `specLinea != null`.

### Forma de `ColumnMatcher` (clave para no romper buildSpec)
```java
@FunctionalInterface
public interface ColumnMatcher {
    String sugerirCampo(String headerArchivo);
    static String normalize(String s) { /* lowercase + sin tildes + alfanumérico */ }
    static String matchLongest(String norm, Map<String, List<String>> synonyms) { /* substring + el más largo gana */ }
}
```
Patrón de uso en `buildSpec()`:
```java
var syn = Map.ofEntries(Map.entry("campo", List.of("sin1", "sin2")), ...);
ColumnMatcher matcher = h -> ColumnMatcher.matchLongest(ColumnMatcher.normalize(h), syn);
```

---

## WORKFLOW MULTI-IA

**No hay invocación directa por CLI desde mí (Claude consumer).** El flujo es:

1. **Yo (Claude consumer en el chat)** coordino el sprint, redacto bloques autocontenidos.
2. **Tú** pegas el bloque en el chat del agente correspondiente dentro de IntelliJ IDEA.
3. **El agente del IDE** (Claude Code / Codex / Gemini) ejecuta.
4. **Tú** me pegas la respuesta del agente y yo evalúo si seguir, corregir o pedir fix-up.

**Roles según `CLAUDE.md`:**
- **Claude Code (en IDE):** preferente para planificación, revisión final, calidad, seguridad, tests, cumplimiento de reglas. No usado en Bloques 4b/5/5b/6.
- **Codex (en IDE):** edición local, ejecución de comandos, parches quirúrgicos, inspección. Ejecutor de los bloques blindados. Confirmado en todos los Bloques 4a, 4b, 5a.1, 5a.2, 5a.3, 5a.3-fix, 5b, 6: obedece reemplazo carácter por carácter (con `apply_patch` como herramienta nativa equivalente a `str_replace`), respeta el "no commitear" cuando se le dice, y sigue las reglas de "fallar y revertir" sin improvisar. Tendencia confirmada a inserción aditiva sobre reemplazo (ver lecciones).
- **Gemini (en IDE):** contexto amplio, arquitectura, segunda opinión, investigación. **Usado UNA VEZ en Bloque 5a** para dictamen sobre transacción explícita en DAOs (resultado: D-A-TX, diferir a sprint dedicado, `BEGIN DEFERRED`).

**Cómo redactar bloques para los agentes — lecciones consolidadas:**
- Instrucciones cerradas, sin espacio interpretativo libre.
- Restricciones negativas explícitas (qué NO modificar, qué NO ejecutar, qué NO invocar).
- Criterio de éxito verificable sin reejecutar (lista de archivos, salida de tests, decisiones declaradas).
- Una sola tarea por bloque (o sub-paso). Bloque 4a partido en 4 sub-pasos. Bloque 5a partido en 5 sub-pasos (5a.1, 5a.2, 5a.3, 5a.3-fix, commit conjunto). Bloque 6 monolítico (docs, scope limitado).
- Trazabilidad obligatoria al final.
- **Declarar nombres literales** de enums, métodos, paquetes. Si no los conoces con certeza, pídelos al usuario antes de redactar el bloque.
- **Verificar que el snippet de Java sería aceptado por `javac` aislado** antes de dictarlo: genéricos parametrizados, switch expressions exhaustivas con valor en al menos un brazo, imports presentes, **APIs realmente instanciables (cuidado con FunctionalInterface)**.
- **Añadir `git diff --stat` al criterio de éxito** como segunda verificación barata de scope.
- **Pedir el archivo objetivo antes de redactar bloques no triviales** sobre métodos que no se han leído en la sesión actual.
- **Releer el bloque redactado antes de darlo por bueno.** Lección 3C-paso-3a.
- **Para edits de archivos grandes con cambios extensos, preferir reescritura completa sobre N `str_replace`** si los cambios afectan a la mayoría de secciones. Aplicada a v3.4, v3.5, v3.6 y Bloque 6 (`MIGRACION_HISTORICO.md`).
- **Preferir inserciones puras a reemplazos cuando las líneas afectadas pueden conservarse.** Lección 4a.1. Aplicada en 4b/5b con truco "líneas idénticas en old_str y new_str".
- **Si un test va a ejercitar escritura de FK opcional, verificar que el DAO escribe `NULL` en lugar de `0` literal** antes de redactar el test.
- **Si un test va a ejercitar columna con DEFAULT del DDL, verificar que el DAO no pasa NULL explícito** o aplicar el default en Java en `ensamblarX()`. Lección 5a.3.

**Aviso explícito del usuario:** "En acciones anteriores detectamos incoherencias, desobediencias y falta de fiabilidad en la ejecución de las instrucciones dadas por ti o por mi a los agentes IA del IDE". Por eso los bloques deben ser blindados y verificables. Validar siempre el entregable antes de aceptar.

**Convención de commits establecida:** un bloque = un commit + push. Mensaje con título imperativo (`feat:`, `fix:`, `docs:`) ≤72 chars, línea en blanco, cuerpo con párrafos separados por líneas en blanco. Editor configurado: `git config --global core.editor "notepad"`. Para mensajes multilínea complejos, escribir a archivo temporal y `git commit -F archivo.txt`.

---

## SPRINT CERRADO — IMPORTACIÓN CSV

### Pedido original del usuario (literal)
> "necesito que las clases presupuesto, factura, clientes, albaranes, materiales, empleados y nominas puedan importar con la misma funcionalidad que le hemos puesto a tarifa"

### Estado: SPRINT CERRADO

Las 9 entidades importan CSV. Documentación del procedimiento Vía A actualizada para reflejarlo.

1. Tarifa, Cliente, Material, Empleado (ya estaban antes del sprint).
2. Nómina (plana). HECHO (`c43118a`).
3. Pedido (plano). HECHO (`50d5902` + fix UPDATE path en `9798225`).
4. Presupuesto (parent-child). HECHO (`2a4ead5` motor+spec+tests + `2513a71` cableado UI).
5. Factura (parent-child). HECHO (`e6d7a9a` motor+spec+tests+fix FK + `55de9ef` cableado UI).
6. Albarán (parent-child). HECHO (`bfc0c1a` motor+spec+tests + `60138f2` cableado UI).
7. Documentación Vía A. HECHO (`74f174c` actualización `MIGRACION_HISTORICO.md`).

**Vistas con Alert "Funcionalidad próximamente":** ninguna. Las 9 entidades tienen `📥 Importar` funcional.

### Decisiones funcionales tomadas (críticas, NO se rediscuten)

| Decisión | Valor | Justificación |
|---|---|---|
| Modelo CSV para parent-child | **CSV ancho con cabecera repetida en cada línea, agrupar por `numero`** | Encaja con scripts Python de `MIGRACION_HISTORICO.md`. |
| FK cliente desde CSV | Si viene `nif` y no existe → ERROR sin fallback. Si `nif` vacío → buscar por nombre+apellidos. | Nif explícito es específico; fallback enmascara errores. |
| FK empleado desde CSV (Nómina) | nombre+apellidos. Match único → ok, múltiple → error. | Empleado no tiene clave única tan fiable. |
| Filtro `activo=1` en empleados | Aplicado. | Deuda 2 abierta (puede romper para nóminas históricas). |
| Política duplicados default | `SKIP_IF_EXISTS` | Patrón establecido. |
| Clave duplicado Nómina | `(empleado_id, mes, anio)` | Natural. |
| Clave duplicado Pedido/Presupuesto/Factura/Albarán | `numero` | Identificador natural. |

### Decisiones del Bloque 3 (D1–D6, todas aplicadas)

| ID | Decisión | Valor | Estado |
|---|---|---|---|
| D1 | Política inconsistencia entre filas del mismo grupo | Estricta + Opción A (todos los campos de `spec.campos()` cuentan). | CERRADA. |
| D2 | Modelado parent-child en `EntityImportSpec` | Ampliar con 3 campos opcionales. | CERRADA. |
| D3 | `UPDATE_EXISTING` para parent-child | Bloquear con `IllegalArgumentException` al inicio de `importar()`. | CERRADA. |
| D4 | Deuda 4 (UPDATE path Pedido) | Cerrada. | CERRADA. |
| D5 | FKs opcionales | Resolver vía número del CSV. | **CERRADA COMPLETA** (Factura en 4a vía `presupuesto_numero`; Albarán en 5a vía `factura_numero` y `pedido_numero`). |
| D6 | Validación fechas Fase 2 | `CAMPOS_FECHA` análogo a `CAMPOS_NUMERICOS`. | CERRADA. |

### Decisiones del Bloque 3C-paso-3 (Presupuesto, todas aplicadas)

| ID | Decisión | Valor |
|---|---|---|
| D-CN | Semántica `CREATE_NEW` en parent-child | Bloquear. Solo `SKIP_IF_EXISTS` permitido. |
| D-IVA | Default `ivaPorcentaje` si CSV vacío | 21.0 |
| D-EST | Validación de `estado` | Aceptar tal cual, default `borrador` si vacío. |
| D-FV | `fecha_validez` a `CAMPOS_FECHA` | Sí. |
| D-CON | `condiciones` a `CAMPOS_LIBRES` | Sí, `MAX_LEN_LIBRE=1000`. |
| D-DC | `descripcion` virtual de cabecera Presupuesto | No. |
| D-TOT | Totales del CSV vs `calcularTotales()` | Recalcular siempre. |
| D-SCO | Scope de 3C-paso-3 | Partir en 3a + 3b. |
| D-FECHA-DEFAULT | Default `fecha` cabecera Presupuesto | `LocalDate.now().toString()`. |

### Decisiones del Bloque 4a (Factura, todas aplicadas)

| ID | Decisión | Valor |
|---|---|---|
| D-F-FECHA | Default `fecha` cabecera Factura | `LocalDate.now().toString()`. |
| D-F-IVA | Default `iva_porcentaje` | 21.0. |
| D-F-EST | Default `estado` | `'pendiente'`. |
| D-F-FORMAPAGO | `forma_pago` | Opcional, sin default Java; BD aplica `'Transferencia bancaria'`. |
| D-F-FV | `fecha_vencimiento` a `CAMPOS_FECHA` | Sí. |
| D-F-TOT | Totales | Recalcular siempre. |
| D-F-DC | `descripcion` virtual de cabecera | No. |
| D-F-CN | Política duplicados parent-child | Bloqueo heredado. |
| D-F-MATERIALES | Descuento de materiales | No descontar (flujo UI separado). |
| D-F-SINONIMOS | Sinónimos sin solape `numero`/`presupuesto_numero` | Aplicado. |
| D5 (Factura) | FK opcional `presupuesto_id` | Resolver vía `presupuesto_numero` del CSV. |
| D-F-FK-NULL | `FacturaDAO.set()` para `presupuesto_id == 0` | `setNull(2, Types.INTEGER)`. Cierra Deuda 16. |

### Decisiones del Bloque 5a (Albarán, todas aplicadas)

| ID | Decisión | Valor |
|---|---|---|
| D-A-FECHA | Default `fecha` cabecera Albarán | `LocalDate.now().toString()`. Simetría con D-FECHA-DEFAULT y D-F-FECHA. |
| D-A-EST | Default `estado` | **`'pendiente'` en Java** (redefinida en 5a.3-fix: SQLite no aplica DEFAULT cuando se pasa NULL explícito). |
| D-A-OBS | `observaciones` a `CAMPOS_LIBRES` | Sí, `MAX_LEN_LIBRE=1000`. Simetría con `notas` y `condiciones`. |
| D-A-TOT | Totales | **No aplica.** Albarán no tiene totales (sin IVA, sin precio, sin descuento). |
| D-A-DC | `descripcion` virtual de cabecera | No. |
| D-A-CN | Política duplicados parent-child | Bloqueo heredado. |
| D-A-SINONIMOS | Sinónimos sin solape `numero`/`factura_numero`/`pedido_numero` | Aplicado. |
| D5 (Albarán) | FKs opcionales `factura_id` y `pedido_id` | Resolver vía `factura_numero` y `pedido_numero` del CSV. Cierra D5 completa. |
| D-A-LINEA | Campos línea | `descripcion` (obligatorio), `cantidad` (obligatorio), `unidad` (opcional). 3 campos. |
| D-A-ORDEN | Campo `orden` en spec línea | No. `AlbaranDAO.saveLineas()` lo reasigna. |
| D-A-TX | Transacción explícita en DAO | **Diferir a sprint dedicado.** Dictamen Gemini: deuda real, abordar sistemáticamente con `BEGIN DEFERRED` en sprint propio. |

### Decisiones del Bloque 6 (docs)

| ID | Decisión | Valor |
|---|---|---|
| D-6-SCOPE | Detalle de la sección "Estado actual del importador" | Medio (tabla 9 entidades + claves duplicado + FK opcionales + defaults Java + limitaciones). Ni mínimo ni exhaustivo. |
| D-6-ESTRUCTURA | Edición del `.md` | Reescritura completa vía única sustitución (lección 14). Codex usó `apply_patch` como equivalente nativo de `str_replace`. |
| D-6-CONSERVAR | Secciones existentes | Conservar literal: Procedimiento general, Convenciones CSV destino, Ejemplo xlsx Python, Otros formatos, Cuándo reevaluar Vía B. Reescribir: encabezado, Contexto, Referencias del código. Insertar nueva: Estado actual del importador. |

---

## ESTADO TÉCNICO AL CIERRE DE ESTA SESIÓN

### Git
- **Rama:** `master`
- **HEAD:** `74f174c` — `docs: actualizar MIGRACION_HISTORICO.md con las 9 entidades importables`. Sincronizado con `origin/master`.
- **Commits del sprint en orden cronológico:**
  - `c43118a` — Bloque 2A: importación CSV de Nóminas.
  - `50d5902` — Bloque 2B: importación CSV de Pedidos.
  - `9798225` — Bloque 2C: fix UPDATE path Pedido.
  - `0bb82c7` — Bloque 3C-paso-1: ampliar `EntityImportSpec` parent-child.
  - `fc63a50` — Bloque 3C-paso-2a: `CAMPOS_FECHA` + validación ISO Fase 2.
  - `a8b596f` — Bloque 3C-paso-2b: infraestructura parent-child.
  - `2310588` — Bloque 3C-paso-2c: `detectarInconsistenciaGrupo` (cierra D1).
  - `bf9f34c` — docs: handoff v3.1.
  - `2a4ead5` — Bloque 3C-paso-3a: Presupuestos parent-child (motor + spec + tests).
  - `2513a71` — Bloque 3C-paso-3b: cablear importación CSV en `PresupuestosView`.
  - `e8036f8` — docs: handoff v3.3 (cierre Bloque 3 completo).
  - `e6d7a9a` — Bloque 4a: Facturas parent-child (motor + spec + tests + fix FK).
  - `7126b15` — docs: handoff v3.4 (Bloque 4a cerrado).
  - `55de9ef` — Bloque 4b: cablear importación CSV en `FacturasView`.
  - `bfc0c1a` — Bloque 5a: Albaranes parent-child (motor + spec + tests).
  - `60138f2` — Bloque 5b: cablear importación CSV en `AlbaranesView`.
  - `74f174c` — Bloque 6: actualizar `MIGRACION_HISTORICO.md`.
- **Working tree:** sólo `Resumen.md` modificado (este mismo archivo, v3.6 en curso). Tras commit del v3.6 quedará limpio.
- **Nota:** el commit `docs: handoff v3.5` previsto al cierre de 5b no llegó a hacerse. El v3.6 absorbe el contenido del v3.5 fantasma.

### Tests
- **56/56 verdes** al cierre. Reparto:
  - `ClienteDAOTest` — 2
  - `ImportBackupServiceTest` — 12
  - `EntityImportServiceAlbaranTest` — 11
  - `EntityImportServiceFacturaTest` — 9
  - `EntityImportServiceNominaTest` — 5
  - `EntityImportServicePedidoTest` — 10
  - `EntityImportServicePresupuestoTest` — 7

### Estado del archivo `EntityImportService.java`
- Package: `org.gipsybuho.service`.
- Imports con wildcards: `org.gipsybuho.dao.*`, `org.gipsybuho.model.*`, `org.gipsybuho.service.importer.*`.
- Constantes: `MAX_FILAS=10_000`, `MAX_LEN=255`, `MAX_LEN_LIBRE=1_000`, `CAMPOS_NUMERICOS`, `CAMPOS_FECHA` (5 elementos), `CAMPOS_LIBRES` (4 elementos: `"notas"`, `"descripcion"`, `"condiciones"`, `"observaciones"`).
- Records privados: `ValidRow(int numero, Map<String,String> vals)`, `ValidGroup(String clave, List<ValidRow> filas)`.
- Fase 2.5 `agruparEnFase2_5` con `detectarInconsistenciaGrupo` invocado por grupo.
- Fase 3 bifurcada: `insertarFilas` + `insertarGrupos`.
- Dispatchers: `procesarFila` (planos, 6 casos) y `procesarGrupo` (parent-child, 3 casos: `"Presupuestos"`, `"Facturas"`, `"Albaranes"`).
- Bloqueos al inicio de `importar()` para parent-child: `UPDATE_EXISTING` y `CREATE_NEW`. Solo `SKIP_IF_EXISTS` permitido.
- Métodos privados de Presupuesto: `procesarPresupuesto`, `ensamblarPresupuesto`, `aplicarValoresPresupuestoCabecera`, `ensamblarLineaPresupuesto`.
- Métodos privados de Factura: `procesarFactura`, `ensamblarFactura`, `aplicarValoresFacturaCabecera`, `ensamblarLineaFactura`, `resolverPresupuestoIdPorNumero`.
- Métodos privados de Albarán: `procesarAlbaran`, `ensamblarAlbaran`, `aplicarValoresAlbaranCabecera`, `ensamblarLineaAlbaran`, `resolverFacturaIdPorNumero`, `resolverPedidoIdPorNumero`.

### Estado del archivo `MIGRACION_HISTORICO.md` (post-Bloque 6)
- Última revisión: 22/05/2026. Estado: Vía A activa, Sprint 2 cerrado.
- Sección "Estado actual del importador" nueva: tabla de 9 entidades con tipo + clave duplicado + política default, descripción del modelo CSV ancho con cabecera repetida, FK opcionales por número, defaults Java por entidad, limitaciones conocidas (Deudas 2, 3, 19, 20).
- Secciones conservadas literales: Procedimiento general, Convenciones CSV destino, Ejemplo xlsx Python (`PRECIOS_PAPEL_PROVEEDORES_Formulas.xlsx`), Otros formatos (xlsb/pdf/docx), Cuándo reevaluar Vía B.
- Secciones reescritas: encabezado, Contexto, Referencias del código (ahora lista 9 `IMPORT_SPEC` + nota sobre `EntityImportSpec` parent-child).

---

## PLAN DE BLOQUES — ESTADO

| Bloque | Descripción | Estado |
|---|---|---|
| 1 | Análisis Nómina/Albarán | ✅ HECHO |
| 2A | Implementar Nómina + tests | ✅ HECHO. `c43118a`. |
| 2B | Implementar Pedido + extraer `resolverFkPorNombre` + tests | ✅ HECHO. `50d5902`. |
| 2C | Fix deuda 4: UPDATE path Pedido | ✅ HECHO. `9798225`. |
| 3A | Análisis parent-child | ✅ HECHO. |
| 3B | Diseño parent-child (D1–D6) | ✅ HECHO. |
| 3C-paso-1 | Ampliar `EntityImportSpec` | ✅ HECHO. `0bb82c7`. |
| 3C-paso-2a | `CAMPOS_FECHA` + validación ISO | ✅ HECHO. `fc63a50`. |
| 3C-paso-2b | Infraestructura parent-child | ✅ HECHO. `a8b596f`. |
| 3C-paso-2c | Detección inconsistencia | ✅ HECHO. `2310588`. |
| 3C-paso-3a | Presupuesto: spec parent-child + tests | ✅ HECHO. `2a4ead5`. |
| 3C-paso-3b | Cableado `PresupuestosView.importar()` | ✅ HECHO. `2513a71`. |
| 4a | Factura: spec + motor parent-child + tests + fix FK | ✅ HECHO. `e6d7a9a`. |
| 4b | Cablear `FacturasView.importar()` | ✅ HECHO. `55de9ef`. |
| 5a | Albarán: spec + motor parent-child + tests + fix DEFAULT | ✅ HECHO. `bfc0c1a`. |
| 5b | Cablear `AlbaranesView.importar()` | ✅ HECHO. `60138f2`. |
| 6 | Actualizar `MIGRACION_HISTORICO.md` | ✅ HECHO. `74f174c`. |

**Sprint Importación CSV CERRADO.**

---

## PRÓXIMOS SPRINTS CANDIDATOS

El sprint ha cerrado sin nuevo sprint asignado. Cuando arranque la próxima sesión, el usuario decidirá entre estas opciones (o propondrá otra). Listadas sin orden de prioridad; el usuario elige.

### A. Smoke test manual de Albaranes pendiente
Coste: minutos. Riesgo: cero (es validación, no cambia código). Acción:
- `.\mvnw.cmd javafx:run` → ir a Albaranes → pulsar `📥 Importar` → debe abrir FileChooser, no Alert.
- Cargar un CSV de ejemplo con 1 albarán + 2 líneas y verificar que persiste.
- Cerrar la app correctamente (`Get-Process java | Stop-Process -Force` si se queda colgada).
- Si todo OK, ningún commit. Si hay bug en UI, abrir fix-up.

Deja la última pieza del sprint validada manualmente. Aceptable como cierre simbólico antes de empezar otro sprint.

### B. Sprint de transacciones explícitas en DAOs (Deuda 9 + D-A-TX)
Coste: medio. Riesgo: medio. Aplica a `PresupuestoDAO`, `FacturaDAO`, `AlbaranDAO` (al menos). Dictamen Gemini en 5a: usar `BEGIN DEFERRED` sistemáticamente. Necesita decisiones:
- ¿Transacción al nivel del DAO (cada `save` parent + `saveLineas`) o al nivel del servicio importador (toda la importación en una sola tx)?
- ¿Cómo se comporta cuando una línea falla a mitad? ¿Rollback de la cabecera ya insertada o aceptar parcial?
- ¿Tests sintéticos que prueben rollback?

### C. Sprint de empleados inactivos (Deuda 2)
Coste: bajo. Riesgo: bajo. Hoy `resolverEmpleadoId` filtra `activo=1`. Para importar nóminas históricas de empleados ya inactivos esto rompe. Opciones:
- Quitar filtro y dejar resolver inactivos. Riesgo: confusión en otras pantallas que esperan solo activos.
- Añadir parámetro `incluirInactivos` a la resolución, true solo desde importador.
- Workaround manual ya documentado (reactivar → importar → desactivar). Tal vez baste con documentar y no tocar código.

### D. Sprint de defaults DDL ignorados con NULL explícito (Deuda 20)
Coste: bajo-medio. Riesgo: bajo. Detectada en 5a.3, mitigada solo en Albarán (D-A-EST). Aplica potencialmente a `forma_pago` en `FacturaDAO`, `condiciones` en `PresupuestoDAO` y cualquier otra columna con DEFAULT cuyo setter pase NULL. Acción:
- Auditar todos los DAOs y todos los DEFAULT del DDL.
- Por cada combinación setter+NULL+DEFAULT_DDL, decidir si aplicar default en Java (`ensamblarX`) o cambiar el DAO para omitir la columna del INSERT.
- Tests sintéticos que ejerciten cada caso con valor vacío en CSV.

### Otros candidatos menores (no requieren sprint completo)
- Deuda 8 (refactor `mostrarResultadoImportacion` duplicado en 8 vistas). Toca UI, fuera del scope habitual.
- Deuda 15 (tests parent-child no cubren campos cabecera opcionales mal formados). Fácil de añadir, riesgo bajo.
- Deuda 17 (estilo imports asimétrico en `Factura.java`). Trivial.
- Deuda 18 / 18-bis (`cliente_id` setNull en `FacturaDAO`/`AlbaranDAO`). Riesgo nulo, podría bundlearse con sprint B.

---

## DEUDAS TÉCNICAS — ESTADO

| ID | Descripción | Estado |
|---|---|---|
| 1 | Validación de fechas en Fase 2 | CERRADA en 3C-paso-2a. |
| 2 | Filtro `activo=1` en `resolverEmpleadoId` puede romper nóminas históricas | ABIERTA. Candidata sprint C. |
| 3 | `lower()` en SQLite no normaliza tildes | ABIERTA. |
| 4 | UPDATE path Pedido sin guard de errores post-`aplicarValoresPedido` | CERRADA en `9798225`. |
| 5 | Contrato `filtroExtraSql` admite `null` pero rechaza blank con excepción | ABIERTA. Riesgo bajo. |
| 6 | `UPDATE_EXISTING` para Nómina y Pedido sin test directo | PARCIAL. Nómina sin test directo. |
| 7 | Asimetría: `aplicarValoresNomina` no recibe `errores` | ABIERTA. Sin bug observable. |
| 8 | `mostrarResultadoImportacion` duplicado en 8 vistas | ABIERTA. Refactor UI fuera del sprint. Reforzada en 4b/5b. |
| 9 | `*.save()` sin transacción explícita BEGIN/COMMIT | ABIERTA. **Reforzada con dictamen Gemini en 5a (D-A-TX).** Candidata sprint B. Aplica a PresupuestoDAO, FacturaDAO, AlbaranDAO. |
| 10 | `saveLineas` DELETE+INSERT total | CERRADA en 3C-paso-2b (bloqueo UPDATE_EXISTING parent-child). |
| 11 | `fecha_alta` de Empleado sin validación ISO | ABIERTA, riesgo bajo. |
| 12 | `@SuppressWarnings("unused")` en `procesarGrupo` | CERRADA en 3C-paso-3a. |
| 13 | Detección de inconsistencia parent-child sin test | CERRADA. Tests en 3 entidades (Presupuesto, Factura, Albarán). |
| 14 | `CREATE_NEW` para parent-child sin semántica clara | CERRADA en 3C-paso-3a (D-CN). |
| 15 | Tests parent-child no cubren campos cabecera opcionales (fecha_validez ISO mal formada, condiciones largas, etc.) | ABIERTA, riesgo bajo. Aplica a 3 entidades. |
| 16 | `FacturaDAO.set()` escribía `presupuesto_id=0` literal infringiendo FK | CERRADA en 4a (D-F-FK-NULL). |
| 17 | Estilo de imports en `Factura.java` (asimetría tras 4a.1) | ABIERTA, riesgo nulo. |
| 18 | `cliente_id` en `FacturaDAO.set()` también es FK opcional pero NO aplica `setNull` | ABIERTA, riesgo bajo. |
| 18-bis | `cliente_id` en `AlbaranDAO.set()` también es FK opcional pero NO aplica `setNull` | ABIERTA, riesgo nulo. Análoga a Deuda 18. `resolverClienteId` garantiza match o ERROR. |
| 19 | Validación numérica de `cantidad` ausente; valores no-numéricos persisten como 0 en BD | ABIERTA. Aplica a Pedido/Presupuesto/Factura/Albarán. Aceptada como consciente en 5a. Documentada en MIGRACION_HISTORICO.md. |
| 20 | Defaults DDL ignorados cuando setter pasa NULL explícito | DETECTADA en 5a.3, mitigada en Albarán (D-A-EST). Candidata sprint D. **Potencialmente aplica a otras columnas con DEFAULT** (revisar `forma_pago` en `FacturaDAO`, `condiciones` en `PresupuestoDAO`, etc.). |

---

## ERRORES COMETIDOS EN ESTE SPRINT (para no repetirlos)

1. **Asumí el tipo de `Pedido.fecha` sin verificarlo** (2B). Era `LocalDate`, no `String`.

2. **Fui demasiado cómplice al inicio con decisiones del usuario** (2B).

3. **Redacté el Bloque 2B juntando dos cambios.** Aplicado: bloques partidos en 3C-paso-3, 4a, 5a.

4. **Confundí `UPDATE_IF_EXISTS` con `UPDATE_EXISTING`** (2C).

5. **Perdí genéricos al reescribir `EntityImportSpec`** (3C-paso-1).

6. **Dicté switch expression sin brazos con valor** (3C-paso-2b).

7. **Entré en pánico falso por `git status` "limpio"** (3C-paso-1).

8. **El usuario pegó por error respuesta idéntica del turno anterior** (3C-paso-2b fix).

9. **Casi redacto 3C-paso-2c sin pedir el archivo objetivo.**

10. **Subida ambigua en sesión 3C-paso-3** (usuario subió `PresupuestosView.java` sin mensaje).

11. **Dos basuras en mi propia redacción del bloque 3a** detectadas al releer.

12. **Codex añadió un default no declarado en 3a.** Aplicado: D-F-FECHA declarada explícita en 4a; D-A-FECHA en 5a.

13. **`Resumen.md` arrastró sin commit entre sesiones** (3b). Aplicado en 4a y 5a: handoff commiteado al cierre. **Recaída en 5b: el commit `docs: v3.5` previsto no se hizo.** El v3.6 absorbe el v3.5 fantasma.

14. **Intenté actualizar el handoff con 12 `str_replace` quirúrgicos** sobre archivo a medio actualizar. Aplicado: v3.4, v3.5, v3.6 entregadas como reescritura completa.

15. **Codex hizo inserción aditiva en 4a.1 cuando dicté reemplazo.** Aplicado: truco "líneas idénticas en old_str y new_str" en 4b/5b.

16. **`Nothing to compile - all classes are up to date` falsa señal de "verificado".** Aplicado en todos los bloques posteriores: `clean compile`.

17. **Etiqueté un bug seguro como "deuda latente"** (4a). Resuelto vía D-F-FK-NULL.

18. **`findstr /N "case \"Facturas\""` no escapó bien** (4a).

19. **Dicté `new ColumnMatcher(Map.ofEntries(...))` sin haber leído `ColumnMatcher.java`** (5a.1). Es FunctionalInterface, no clase concreta. Causó BUILD FAILURE. Aplicado: pedir el archivo de la API y/o un archivo que la use bien antes de dictarla.

20. **No anticipé que SQLite ignora DEFAULT con NULL explícito** (5a.3). Test `importaAlbaranConDosLineas` rojo. D-A-EST redefinida en Java. Aplicado: cuando el spec marca un campo opcional con default de BD, verificar que el DAO no pasa NULL explícito o aplicar default en Java.

---

## ARCHIVOS YA INSPECCIONADOS — NO PEDIRLOS DE NUEVO

Estos archivos están analizados y leídos. NO pedirlos de nuevo al arrancar un nuevo sprint salvo cambio explícito:

- `CLAUDE.md` — reglas operativas Multi-IA.
- `MIGRACION_HISTORICO.md` — actualizado en Bloque 6.
- `DuplicatePolicy.java` — enum: `SKIP_IF_EXISTS`, `UPDATE_EXISTING`, `CREATE_NEW`.
- `ColumnMatcher.java` — FunctionalInterface, patrón `h -> matchLongest(normalize(h), syn)`.
- `Tarifa.java`, `Pedido.java`, `Presupuesto.java`, `Factura.java`, `Albaran.java` — todos con `IMPORT_SPEC`.
- `LineaPresupuesto.java`, `LineaFactura.java`, `LineaAlbaran.java`.
- `PresupuestoDAO.java`, `FacturaDAO.java`, `AlbaranDAO.java`, `PedidoDAO.java`.
- `PresupuestosView.java`, `FacturasView.java`, `AlbaranesView.java` (post cableado).
- `TarifasView.java`, `NominasView.java`, `PedidosView.java` — plantillas de cableado.
- `EntityImportSpec.java`, `FieldSpec.java`.
- `EntityImportService.java` — estado al cierre 5a.
- `EntityImportServicePedidoTest.java`, `EntityImportServicePresupuestoTest.java`, `EntityImportServiceFacturaTest.java`, `EntityImportServiceAlbaranTest.java`.
- `DatabaseManager.java` — activa `PRAGMA foreign_keys = ON`. DDL inline.

---

## RESPUESTA TÍPICA EN PRÓXIMAS SESIONES

- Sin emojis salvo cita literal de código (los hay en la UI: `📥 Importar`, `📦 Material`).
- Sin "vamos a", "podríamos", "voy a intentar". Imperativo claro.
- Listas y tablas cuando estructuran. Prosa cuando fluye.
- Bloques de código con triple backtick y lenguaje declarado.
- Bloques para agentes en triple backtick anidado con cinco: ` ````` ` para que el usuario los copie limpios.
- Al final de cada respuesta técnica, una pregunta concreta o un siguiente paso explícito.

---

## PRIMER MENSAJE QUE VOY A RECIBIR

El usuario abrirá un chat nuevo y pegará este documento entero. Mi primer mensaje debe ser:

1. **Confirmar contexto cargado:** HEAD `74f174c` (Bloque 6 cerrado) o un commit `docs:` v3.6 inmediatamente encima, 56/56 verdes, Sprint Importación CSV CERRADO. Las 9 entidades importan CSV. Documentación Vía A actualizada.

2. **Pedir verificación de estado:**
   - `git log --oneline -5` — confirmar HEAD en un commit `docs:` v3.6 encima de `74f174c` o en el propio `74f174c`.
   - `git status` — confirmar working tree limpio.
   - `.\mvnw.cmd test` — confirmar 56/56 verdes con el reparto 2 + 12 + 11 + 9 + 5 + 10 + 7.

3. **Preguntar al usuario qué sprint arranca**, presentando las 4 opciones de la sección "Próximos sprints candidatos" (A: smoke test, B: transacciones, C: empleados inactivos, D: defaults DDL) sin recomendar una. El usuario decide. Si propone otro sprint distinto, registrar el alcance y arrancar análisis (Bloque 1 del nuevo sprint).

4. **Una vez decidido el sprint:**
   - Si es A (smoke test): dictar comandos, esperar resultado, cerrar.
   - Si es B/C/D: arrancar con un Bloque 1 de análisis. Pedir archivos relevantes según sprint elegido (no asumir conocimiento previo del estado de archivos no listados en "Archivos ya inspeccionados").
   - Si es otro: dejar al usuario describir alcance, formular preguntas de diseño antes de redactar Bloque 1.

5. **Si la verificación de estado revela divergencia** respecto a `74f174c` o al docs v3.6 encima, diagnosticar antes de avanzar (mirar `git log --oneline -10` y comparar con commits listados en sección "Git").

FIN DEL HANDOFF.
