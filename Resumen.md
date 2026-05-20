# HANDOFF — Graficas Mulberry · Sprint Importación CSV
# Versión: 3.4 · Fecha cierre: 20/05/2026 · Checkpoint: Bloque 4a cerrado. Siguiente: Bloque 4b (cablear FacturasView.importar()).

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
- **Pide el archivo antes de redactar bloques sobre métodos concretos.** Lección 3C-paso-2c y reforzada en 3C-paso-3a y Bloque 4a: no fiarse del recuerdo del handoff sobre la forma exacta de un método si la sesión actual no lo ha leído. Pedir y leer es barato; redactar a ciegas un reemplazo carácter-por-carácter es caro.
- **Regla de inicio "_Gipsybuho_, procedo a ejecutar..."** del `CLAUDE.md`: aplícala solo en mensajes conversacionales claros. En respuestas técnicas, bloques, parches, listas de pasos, no aplica.
- **Releer el bloque redactado antes de pegarlo.** Lección 3C-paso-3a: detecté dos basuras en mi propia redacción al releer (ternario inútil `apellidos.isBlank() ? nombre : nombre` y línea sentinela rara con comparación absurda). Releer una vez antes de dar por bueno el bloque cazó ambos. Releer es barato.
- **Si un archivo aparece como modificado al arrancar sesión sin commit previo identificable, declararlo explícitamente.** Lección 3C-paso-3b: `Resumen.md` arrastró cambios v3.2 entre sesiones porque nunca se commiteó y se acumuló como "ruido" durante 3a y 3b; al revertir un fallo de `str_replace` se perdió ese trabajo intermedio. En el primer pase de cada sesión, verificar `git diff --stat` y declarar scope del archivo no-commiteado antes de empezar a editar nuevos archivos. PowerShell con encoding CP-850 muestra UTF-8 como salsa rara (`Versi├│n`, `ÔÇö`) y oculta cambios reales; usar `Format-Hex` cuando el diff se vea sospechoso.
- **Si Codex declara un cambio funcional no pedido, parar y pedir el diff antes de aprobar.** Lección 3C-paso-3a: Codex añadió default `fecha = LocalDate.now().toString()` para que sus tests pasaran. La decisión era defendible (análoga a D-IVA y D-EST) pero no estaba autorizada. Aunque los tests estén verdes, revisar diff antes del commit y, si el cambio se acepta, declararlo como decisión explícita en el mensaje.
- **Codex prefiere inserción aditiva sobre reemplazo cuando puede.** Lección 4a.1: dicté "reemplaza dos líneas de imports por estas siete" y Codex añadió las nuevas sin borrar las viejas. Funcionalmente equivalente, diff distinto del esperado (0 supresiones en lugar de 2). Cuando un `str_replace` afecta a líneas que también deben preservarse, conviene dictarlo como inserción pura ("inserta después de X estas N líneas") sin pedir reemplazo de líneas que pueden quedarse.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Lección 4a.2: el patrón terminó vacío y `findstr` devolvió todo el archivo. Alternativas robustas:
  - `findstr /N /C:"X \"Y\"" archivo` (con `/C:` literal)
  - `Select-String -Path 'archivo' -Pattern 'X .Y.'` (regex, el `.` cubre la comilla)
- **Si una "deuda latente" toca escritura de FK y los tests sintéticos arrancan con `PRAGMA foreign_keys=ON`, NO es latente: es bug seguro.** Lección 4a.2 final: etiqueté el bug de `Factura.presupuestoId=0` como deuda preexistente sin tocar, y los 3 tests sintéticos que no informaban presupuesto lo dispararon de inmediato. Si la deuda implica escribir un valor que infringe una constraint con FK activas, los tests la cazarán. Tratarla como bug a fix, no como deuda diferible.
- **`Nothing to compile - all classes are up to date` NO es prueba de que compila.** Lección 4a.1: Maven se saltó la recompilación porque los `.class` ya estaban frescos respecto al `.java` antiguo. Tras editar un archivo, usar `.\mvnw.cmd clean compile` para forzar recompilación real desde cero. Solo entonces `BUILD SUCCESS` cuenta como evidencia.

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
- `src/main/java/org/gipsybuho/service/` — lógica de servicios. **`EntityImportService` está aquí** con package `org.gipsybuho.service`, no en `.importer`. Verificado leyendo el archivo.
- `src/main/java/org/gipsybuho/service/importer/` — tipos auxiliares del importador: `EntityImportSpec`, `FieldSpec`, `ColumnMatcher`, `DuplicatePolicy`, `ImportResult`, `RowError`, `ErrorTipo`.
- `src/main/java/org/gipsybuho/ui/` — vistas JavaFX.
- `src/test/java/org/gipsybuho/service/importer/` — tests del importador. **Convención establecida:** todos los tests nuevos del importador van aquí.
- `src/main/java/org/gipsybuho/db/DatabaseManager.java` — DDL inline en Java (no hay `schema.sql`). Activa `PRAGMA foreign_keys = ON`; las FK se evalúan en todos los entornos, incluidos los tests con `@TempDir`.

### Documentos del proyecto que NO debes ignorar
- `CLAUDE.md` — reglas operativas, Multi-IA, cambios quirúrgicos.
- `MIGRACION_HISTORICO.md` — procedimiento Vía A para procesar archivos históricos del cliente. Clave: el `ImportService` ya normaliza fechas Excel a ISO en parseo, antes de llegar a `EntityImportService`.

### Valores conocidos del enum `DuplicatePolicy`
`SKIP_IF_EXISTS` (default), `UPDATE_EXISTING`, `CREATE_NEW`. **NO existe `UPDATE_IF_EXISTS`** — error frecuente.

### Forma actual de `EntityImportSpec` (post 3C-paso-1)
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

---

## WORKFLOW MULTI-IA

**No hay invocación directa por CLI desde mí (Claude consumer).** El flujo es:

1. **Yo (Claude consumer en el chat)** coordino el sprint, redacto bloques autocontenidos.
2. **Tú** pegas el bloque en el chat del agente correspondiente dentro de IntelliJ IDEA.
3. **El agente del IDE** (Claude Code / Codex / Gemini) ejecuta.
4. **Tú** me pegas la respuesta del agente y yo evalúo si seguir, corregir o pedir fix-up.

**Roles según `CLAUDE.md`:**
- **Claude Code (en IDE):** preferente para planificación, revisión final, calidad, seguridad, tests, cumplimiento de reglas. Usado en este sprint para análisis 3A (parent-child), revisión 2B (detectó deudas 4 y 5), y consulta arquitectónica previa a 3C-paso-2b (P1–P4 + Riesgos 1, 2, 3).
- **Codex (en IDE):** edición local, ejecución de comandos, parches quirúrgicos, inspección. Ejecutor de los bloques blindados. Confirmado en todo el Bloque 4a: obedece reemplazo carácter por carácter, respeta el "no commitear" cuando se le dice, y sigue las reglas de "fallar y revertir" sin improvisar. Detectado en 4a.1 que prefiere inserción aditiva sobre reemplazo (ver lecciones).
- **Gemini (en IDE):** contexto amplio, arquitectura, segunda opinión, investigación. **No usado todavía en este sprint.** Reservado para Bloque 5 (Albarán + transacción explícita + FKs opcionales) o segunda opinión si Claude Code y yo discrepamos.

**Cómo redactar bloques para los agentes — lecciones consolidadas:**
- Instrucciones cerradas, sin espacio interpretativo libre.
- Restricciones negativas explícitas (qué NO modificar, qué NO ejecutar, qué NO invocar).
- Criterio de éxito verificable sin reejecutar (lista de archivos, salida de tests, decisiones declaradas).
- Una sola tarea por bloque (o sub-paso). Lección 3C-paso-3: 3a y 3b se redactan por separado aunque sean del mismo paso conceptual. Lección 4a: dividir en 4 sub-pasos (CAMPOS_FECHA, dispatcher, métodos, test) facilitó la verificación incremental.
- Trazabilidad obligatoria al final.
- **Declarar nombres literales** de enums, métodos, paquetes. Si no los conoces con certeza, pídelos al usuario antes de redactar el bloque.
- **Verificar que el snippet de Java sería aceptado por `javac` aislado** antes de dictarlo: genéricos parametrizados, switch expressions exhaustivas con valor en al menos un brazo, imports presentes.
- **Añadir `git diff --stat` al criterio de éxito** como segunda verificación barata de scope.
- **Pedir el archivo objetivo antes de redactar bloques no triviales** sobre métodos que no se han leído en la sesión actual.
- **Releer el bloque redactado antes de darlo por bueno.** Lección 3C-paso-3a.
- **Para edits de archivos grandes con cambios extensos, preferir reescritura completa sobre N `str_replace`** si los cambios afectan a la mayoría de secciones. Lección 3C-paso-3 cierre: 12 `str_replace` para un handoff a medio actualizar fallaron por mismatch de cadena; reescritura íntegra evita ese fallo y produce diff más limpio. Aplicada a este v3.4.
- **Preferir inserciones puras a reemplazos cuando las líneas afectadas pueden conservarse.** Lección 4a.1.
- **Si un test va a ejercitar escritura de FK opcional, verificar que el DAO escribe `NULL` en lugar de `0` literal** antes de redactar el test.

**Aviso explícito del usuario:** "En acciones anteriores detectamos incoherencias, desobediencias y falta de fiabilidad en la ejecución de las instrucciones dadas por ti o por mi a los agentes IA del IDE". Por eso los bloques deben ser blindados y verificables. Validar siempre el entregable antes de aceptar.

**Convención de commits establecida:** un bloque = un commit + push. Mensaje con título imperativo (`feat:`, `fix:`, `docs:`) ≤72 chars, línea en blanco, cuerpo con párrafos separados por líneas en blanco. Editor configurado: `git config --global core.editor "notepad"`. Para mensajes multilínea complejos, escribir a archivo temporal y `git commit -F archivo.txt`.

---

## SPRINT ACTUAL — IMPORTACIÓN CSV

### Pedido original del usuario (literal)
> "necesito que las clases presupuesto, factura, clientes, albaranes, materiales, empleados y nominas puedan importar con la misma funcionalidad que le hemos puesto a tarifa"

### Reinterpretación tras inspección de código

El importador ya soportaba 4 entidades planas antes del sprint (Tarifa, Cliente, Material, Empleado). Lo que faltaba:

1. Nómina (plana). **HECHO** (`c43118a`).
2. Pedido (plano). **HECHO** (`50d5902` + fix UPDATE path en `9798225`).
3. Presupuesto (parent-child). **HECHO** (`2a4ead5` motor+spec+tests + `2513a71` cableado UI).
4. Factura (parent-child). **HECHO motor+spec+tests+fix FK** (`e6d7a9a`). **Falta cableado UI (4b).**
5. Albarán (parent-child). PENDIENTE.

Vistas con Alert "Funcionalidad próximamente" todavía pendientes: `FacturasView` (será 4b), `AlbaranesView`. (`NominasView`, `PedidosView` y `PresupuestosView` ya cableadas.)

### Decisiones funcionales tomadas (críticas, NO se rediscuten)

| Decisión | Valor | Justificación |
|---|---|---|
| Modelo CSV para parent-child | **CSV ancho con cabecera repetida en cada línea, agrupar por `numero`** | Encaja con scripts Python de `MIGRACION_HISTORICO.md`. |
| FK cliente desde CSV | Si viene `nif` y no existe → ERROR sin fallback. Si `nif` vacío → buscar por nombre+apellidos. | Nif explícito es específico; fallback enmascara errores. |
| FK empleado desde CSV (Nómina) | nombre+apellidos. Match único → ok, múltiple → error. | Empleado no tiene clave única tan fiable. |
| Filtro `activo=1` en empleados | Aplicado. | Deuda 2 abierta (puede romper para nóminas históricas). |
| Política duplicados default | `SKIP_IF_EXISTS` | Patrón establecido. |
| Clave duplicado Nómina | `(empleado_id, mes, anio)` | Natural. |
| Clave duplicado Pedido | `numero` | Identificador natural. |
| Clave duplicado Presupuesto | `numero` | Identificador natural. |
| Clave duplicado Factura | `numero` | Identificador natural. |
| Validación fechas Pedido | Detectar en `ensamblarPedido` comparando `errores.size()`. Cerrada por D6 vía `CAMPOS_FECHA`. | — |

### Decisiones del Bloque 3 (D1–D6, todas tomadas y aplicadas)

| ID | Decisión | Valor | Estado |
|---|---|---|---|
| D1 | Política inconsistencia entre filas del mismo grupo | Estricta + Opción A (todos los campos de `spec.campos()` cuentan). | CERRADA en 3C-paso-2c (`2310588`). |
| D2 | Modelado parent-child en `EntityImportSpec` | Ampliar con 3 campos opcionales. | HECHO en 3C-paso-1 (`0bb82c7`). |
| D3 | `UPDATE_EXISTING` para parent-child | Bloquear con `IllegalArgumentException` al inicio de `importar()`. | HECHO en 3C-paso-2b. |
| D4 | Deuda 4 (UPDATE path Pedido) | Cerrada. | HECHO (`9798225`). |
| D5 | FKs opcionales (Albarán→Factura/Pedido, Factura→Presupuesto) | Resolver Factura en Bloque 4 vía `presupuesto_numero` del CSV. Albarán pendiente para Bloque 5. | CERRADA PARA FACTURA en Bloque 4a (`e6d7a9a`). Albarán pendiente. |
| D6 | Validación fechas Fase 2 | `CAMPOS_FECHA` análogo a `CAMPOS_NUMERICOS`. | HECHO en 3C-paso-2a (`fc63a50`). |

### Decisiones del Bloque 3C-paso-3 (todas cerradas y aplicadas en `2a4ead5` y `2513a71`)

| ID | Decisión | Valor | Razón |
|---|---|---|---|
| D-CN | Semántica `CREATE_NEW` en parent-child | **Bloquear** al inicio de `importar()` análogo a `UPDATE_EXISTING`. Solo `SKIP_IF_EXISTS` permitido. | La clave de agrupación ES el identificador natural; duplicarla no tiene semántica clara. |
| D-IVA | Default `ivaPorcentaje` si CSV vacío | **21.0** | Coincide con `PresupuestosView.nuevo()` (IVA estándar España). |
| D-EST | Validación de `estado` | Aceptar tal cual, default `borrador` si vacío. Sin lista cerrada. | Coherente con cómo Pedido trata `estado` (no valida). Sin sobreingeniería. |
| D-FV | Añadir `fecha_validez` a `CAMPOS_FECHA` | **Sí** | Cierra hueco de validación ISO para Presupuesto. |
| D-CON | Añadir `condiciones` a `CAMPOS_LIBRES` | **Sí** | Texto largo, merece `MAX_LEN_LIBRE=1_000` en vez de `MAX_LEN=255`. |
| D-DC | `descripcion` como campo virtual de cabecera Presupuesto | **No** | Presupuesto no tiene `descripcion` en cabecera del modelo. Solo en `LineaPresupuesto`. |
| D-TOT | Totales del CSV vs `calcularTotales()` | **Recalcular siempre con `calcularTotales()`** | El CSV humano puede traer redondeos inconsistentes; `calcularTotales()` es la verdad. `baseImponible`/`ivaImporte`/`total` NO van al `IMPORT_SPEC`. |
| D-SCO | Scope de 3C-paso-3 | **Partir en 3a (motor+spec+tests) y 3b (cableado UI)** | Karpathy: cuando hay duda, partir bloques. 3a testeable sin UI; 3b copia literal del patrón Pedido. |
| D-FECHA-DEFAULT | Default `fecha` cabecera Presupuesto si CSV vacío | **`LocalDate.now().toString()`** | Coherente con `PresupuestosView.nuevo()`. La BD impone NOT NULL en `presupuestos.fecha`. Análoga a D-IVA y D-EST. Declarada en ejecución de 3a tras revisión del diff de Codex; no estaba en el bloque blindado original. |

### Decisiones del Bloque 4a (todas cerradas y aplicadas en `e6d7a9a`)

| ID | Decisión | Valor | Razón |
|---|---|---|---|
| D-F-FECHA | Default `fecha` cabecera Factura si CSV vacío | **`LocalDate.now().toString()`** | `facturas.fecha NOT NULL` en BD. Simetría con D-FECHA-DEFAULT. **Declarada explícita en el bloque blindado original**, no como cambio emergente de Codex (lección 12 aplicada). |
| D-F-IVA | Default `iva_porcentaje` si CSV vacío | **21.0** | Coincide con `facturas.iva_porcentaje DEFAULT 21.0` de la BD. |
| D-F-EST | Default `estado` si CSV vacío | **`'pendiente'`** | Coincide con `facturas.estado DEFAULT 'pendiente'` de la BD. Aceptar valor del CSV tal cual sin lista cerrada (D-EST simétrico). |
| D-F-FORMAPAGO | Tratamiento `forma_pago` | Opcional en spec. **Sin default desde Java** si CSV vacío; deja que la BD aplique su DEFAULT `'Transferencia bancaria'`. | Lo mínimo que funcione. La BD ya lo cubre. |
| D-F-FV | `fecha_vencimiento` | **Añadir a `CAMPOS_FECHA`**. Opcional en spec (nullable en BD). | Análogo a `fecha_validez` de Presupuesto. |
| D-F-TOT | Totales del CSV vs `calcularTotales()` | **Recalcular siempre.** `baseImponible`/`ivaImporte`/`total` NO van al spec. | Simetría con D-TOT de Presupuesto. |
| D-F-DC | `descripcion` virtual de cabecera Factura | **No.** Factura no tiene `descripcion` en cabecera. | Simetría con D-DC. |
| D-F-CN | Política duplicados parent-child en Factura | **Bloqueo `UPDATE_EXISTING` y `CREATE_NEW`** heredado del bloqueo genérico de `importar()`. Solo `SKIP_IF_EXISTS`. | D-CN aplica genéricamente. |
| D-F-MATERIALES | Descuento de materiales del stock en importación | **No descontar.** `descontarMateriales` es del flujo UI `crearDesdePresupuesto`, no del importador. | Importación es Vía A (datos históricos). Mismo criterio que Presupuesto. |
| D-F-SINONIMOS | Sinónimos de `numero` y `presupuesto_numero` | **Sin solape.** `numero` incluye "factura", "numero factura", "referencia factura". `presupuesto_numero` incluye "presupuesto", "numero presupuesto", "ref presupuesto". | Cero ambigüedad: "presupuesto" en CSV de Factura siempre apunta a la FK. |
| D5 (Factura) | FK opcional `presupuesto_id` | **Resolver vía `presupuesto_numero` del CSV.** Si vacío → NULL en BD. Si informado y no existe → ERROR + descarte del grupo. | Patrón idéntico a `cliente_nif`. Sin sobreingeniería. Cierra D5 para Factura; Albarán queda para Bloque 5. |
| D-F-FK-NULL | `FacturaDAO.set()` para `presupuesto_id == 0` | **`setNull(2, Types.INTEGER)`** en lugar de `setInt(2, 0)`. | Cierra Deuda 16. El DDL admite NULL; escribir `0` literal infringía la FK contra `presupuestos.id` con `PRAGMA foreign_keys=ON`. Declarada como fix emergente tras 3 tests rojos en 4a.2-paso-4; no estaba en el plan original. Limitado a `presupuesto_id`; `cliente_id` queda como está porque ningún flujo lo dispara sin id válido. |

---

## ESTADO TÉCNICO AL CIERRE DE ESTA SESIÓN

### Git
- **Rama:** `master`
- **HEAD:** `e6d7a9a` — `feat: importar Facturas parent-child (motor + spec + tests + fix FK)`. Sincronizado con `origin/master`.
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
- **Working tree:** sólo `Resumen.md` modificado (este mismo archivo, en curso de actualización a v3.4). Tras commit del v3.4 quedará limpio.

### Tests
- **45/45 verdes** al cierre. Reparto:
  - `ClienteDAOTest` — 2
  - `ImportBackupServiceTest` — 12
  - `EntityImportServiceNominaTest` — 5
  - `EntityImportServicePedidoTest` — 10
  - `EntityImportServicePresupuestoTest` — 7
  - `EntityImportServiceFacturaTest` — 9

### Estado del archivo `EntityImportService.java` (post-4a)
- Package: `org.gipsybuho.service`.
- Imports con wildcards: `org.gipsybuho.dao.*`, `org.gipsybuho.model.*`, `org.gipsybuho.service.importer.*`. **No requiere imports nuevos al añadir Factura.**
- Constantes: `MAX_FILAS=10_000`, `MAX_LEN=255`, `MAX_LEN_LIBRE=1_000`, `CAMPOS_NUMERICOS`, `CAMPOS_FECHA` (5 elementos: `"fecha"`, `"fecha_validez"`, `"fecha_vencimiento"`, `"fecha_entrega_prevista"`, `"fecha_entrega_real"`), `CAMPOS_LIBRES` (3 elementos: `"notas"`, `"descripcion"`, `"condiciones"`).
- Records privados: `ValidRow(int numero, Map<String,String> vals)`, `ValidGroup(String clave, List<ValidRow> filas)`.
- Fase 2.5 `agruparEnFase2_5` con `detectarInconsistenciaGrupo` invocado por grupo.
- Fase 3 bifurcada: `insertarFilas` + `insertarGrupos`.
- Dispatchers: `procesarFila` (planos, 6 casos) y `procesarGrupo` (parent-child, 2 casos: `"Presupuestos"`, `"Facturas"`).
- Bloqueos al inicio de `importar()` para parent-child: `UPDATE_EXISTING` y `CREATE_NEW`. Solo `SKIP_IF_EXISTS` permitido.
- Métodos privados de Presupuesto: `procesarPresupuesto`, `ensamblarPresupuesto`, `aplicarValoresPresupuestoCabecera`, `ensamblarLineaPresupuesto`.
- Métodos privados de Factura (Bloque 4a): `procesarFactura`, `ensamblarFactura`, `aplicarValoresFacturaCabecera`, `ensamblarLineaFactura`, `resolverPresupuestoIdPorNumero`.

### Estado del archivo `Factura.java` (post-4a)
- `IMPORT_SPEC` público estático añadido al final del bloque de campos, antes del constructor.
- `buildSpec()` privado estático con 10 campos cabecera y 5 campos línea.
- 10 campos cabecera: `cliente_nif`, `cliente_nombre`, `cliente_apellidos`, `numero` (obligatorio), `presupuesto_numero`, `fecha`, `fecha_vencimiento`, `estado`, `forma_pago`, `iva_porcentaje`, `notas`. Solo `numero` es obligatorio.
- 5 campos línea: idénticos a `LineaPresupuesto`.
- Sinónimos blindados sin solape `numero`/`presupuesto_numero` (D-F-SINONIMOS).
- Asimetría menor de estilo de imports respecto a `Presupuesto.java` (Deuda 17). Riesgo nulo.

### Estado del archivo `FacturaDAO.java` (post-4a)
- Método `set(PreparedStatement, Factura)` modificado: si `f.getPresupuestoId() > 0`, `ps.setInt(2, ...)`; si no, `ps.setNull(2, Types.INTEGER)`. Una sola edición quirúrgica, resto intacto.
- `Types` resuelto vía `import java.sql.*;` preexistente.
- `crearDesdePresupuesto` y `descontarMateriales` no tocados; siguen siendo flujos UI separados del importador.

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
| **4b** | **Cablear `FacturasView.importar()` (sustituir Alert por flujo idéntico a `PresupuestosView`)** | **🔄 SIGUIENTE.** |
| 5 | Implementar Albarán. Decidir transacción explícita en `AlbaranDAO.save()`. Resolución FK opcionales (D5 para Albarán). Considerar Gemini. | PENDIENTE. |
| 6 | Actualizar `MIGRACION_HISTORICO.md`. | PENDIENTE. |

---

## PUNTO EXACTO DE CONTINUACIÓN

**Siguiente: Bloque 4b — Cablear `FacturasView.importar()`.**

Tarea: sustituir el método `importar()` actual (Alert "Funcionalidad próximamente") en `FacturasView.java` por el flujo estándar de importación CSV, copia literal del patrón usado en `PresupuestosView` y `PedidosView`. Añadir también el helper `mostrarResultadoImportacion(ImportResult)`.

### Lo que ya está leído y no requiere relectura

- `FacturasView.java` íntegro. Confirmado:
  - Método de recarga local: `cargar()` (no `cargarFacturas()`).
  - Método `importar()` actual: Alert "Funcionalidad próximamente". A reemplazar.
  - `mostrarResultadoImportacion` **no existe**, debe añadirse como copia literal del patrón Pedido.
  - Botón `📥 Importar` color `#27AE60` ya cableado a `this::importar` en `buildToolbar()`.
  - Hay infraestructura `dynamicColumns`. No interfiere con el cableado.
- `PedidosView.importar()` (líneas 606-653) + `mostrarResultadoImportacion` (líneas 655-673) es la plantilla canónica.
- `PresupuestosView.importar()` post-3b es plantilla igualmente válida.

### Lectura necesaria al arrancar 4b

- **`PresupuestosView.importar()` y `PresupuestosView.mostrarResultadoImportacion()`** completos. Es la copia más reciente del patrón y la más cercana en estructura. Pedir el archivo `src/main/java/org/gipsybuho/ui/PresupuestosView.java` al arrancar. NO redactar el bloque 4b sin tenerlo a la vista.

### Lo que tiene que pasar al arrancar la nueva sesión

1. **Verificar estado:** `git log --oneline -5`, `git status`, `.\mvnw.cmd test`. HEAD esperado: `e6d7a9a` o un commit `docs:` v3.4 inmediatamente encima. Working tree limpio. 45/45 verdes con el reparto del cierre.
2. **Pedir `PresupuestosView.java`** para extraer el patrón literal de `importar()` + `mostrarResultadoImportacion`.
3. **Redactar Bloque 4b** como un único `str_replace` que sustituya el método `importar()` Alert de `FacturasView` por el método estándar, más una inserción del helper `mostrarResultadoImportacion`. Releer antes de pegar.
4. **Commit del 4b:** `feat: cablear importación CSV en FacturasView (4b)`.
5. **Si la verificación de estado revela divergencia**, diagnosticar antes de avanzar.

### Avisos heredados de 4a que aplicarán a 4b

- Releer el bloque antes de pegarlo.
- `FacturasView` tiene `📥 Importar` con color `#27AE60`. No tocar el botón, solo el método `importar()`.
- El helper `mostrarResultadoImportacion(ImportResult)` debe ser copia carácter por carácter del de `PedidosView`/`PresupuestosView` para mantener la consistencia visual y reforzar la Deuda 8 como deuda real (factor 7+ vistas con el mismo método duplicado).
- El método `cargar()` ya existe en `FacturasView`. Reutilizarlo, no inventar `cargarFacturas()`.

---

## DEUDAS TÉCNICAS — ESTADO

| ID | Descripción | Estado |
|---|---|---|
| 1 | Validación de fechas en Fase 2 | CERRADA en 3C-paso-2a. |
| 2 | Filtro `activo=1` en `resolverEmpleadoId` puede romper nóminas históricas | ABIERTA. |
| 3 | `lower()` en SQLite no normaliza tildes | ABIERTA. |
| 4 | UPDATE path Pedido sin guard de errores post-`aplicarValoresPedido` | CERRADA en `9798225`. |
| 5 | Contrato `filtroExtraSql` admite `null` pero rechaza blank con excepción | ABIERTA. Riesgo bajo. |
| 6 | `UPDATE_EXISTING` para Nómina y Pedido sin test directo | PARCIALMENTE CUBIERTA en 2C (Pedido ejercitado). Nómina sin test directo. |
| 7 | Asimetría: `aplicarValoresNomina` no recibe `errores` | ABIERTA. Sin bug observable hoy. |
| 8 | `mostrarResultadoImportacion` duplicado en N vistas | ABIERTA, refuerzo previsto en 4b (alcanzará 7+ vistas). Refactor UI fuera del sprint. Revisar tras Bloque 5. |
| 9 | `AlbaranDAO.save()` sin transacción explícita BEGIN/COMMIT | ABIERTA. Mitigada por transacción del importador. Decidir en Bloque 5. Aplica igual a `PresupuestoDAO.save()` y `FacturaDAO.save()`. |
| 10 | `saveLineas` hace DELETE+INSERT total → riesgo destructivo con `UPDATE_EXISTING` en parent-child | CERRADA en 3C-paso-2b (bloqueo). |
| 11 | `fecha_alta` de Empleado sin validación ISO | ABIERTA, riesgo bajo. |
| 12 | `@SuppressWarnings("unused")` en `procesarGrupo` mientras no haya casos | CERRADA en 3C-paso-3a (`2a4ead5`). |
| 13 | Detección de inconsistencia parent-child sin test ejecutable | CERRADA en 3C-paso-3a (`2a4ead5`, test `descartaGrupoConCabeceraInconsistente`). Reforzada en Bloque 4a con test análogo en Factura. |
| 14 | `CREATE_NEW` para parent-child sin semántica clara | CERRADA en 3C-paso-3a (D-CN: bloqueo análogo a UPDATE_EXISTING). |
| 15 | Tests de Presupuesto no cubren campos cabecera opcionales (fecha válida explícita aplicada vs default, `iva_porcentaje` explícito, `fecha_validez` ISO mal formada, `condiciones` largas dentro de `MAX_LEN_LIBRE`) | ABIERTA, riesgo bajo. Cobertura mínima viable verde; deuda menor para sprints futuros. Aplicará también a Factura (mismo gap en `EntityImportServiceFacturaTest`) y Albarán por simetría. |
| 16 | `FacturaDAO.set()` escribía `presupuesto_id=0` literal infringiendo FK | CERRADA en Bloque 4a (D-F-FK-NULL). |
| 17 | Estilo de imports en `Factura.java` tras 4a.1 (sin línea en blanco entre `package` e imports, dos bloques de imports separados en vez de uno) | ABIERTA, riesgo nulo. Candidato a Ctrl+Alt+L manual cuando se abra el archivo. No vale commit dedicado. |
| 18 | `cliente_id` en `FacturaDAO.set()` también es FK opcional pero NO aplica `setNull` (asimetría con `presupuesto_id`) | ABIERTA, riesgo bajo. Ningún flujo actual lo dispara sin id válido (resolverClienteId garantiza match o ERROR). Revisar si surge bug observable. |

---

## ERRORES COMETIDOS EN ESTE SPRINT (para no repetirlos)

1. **Asumí el tipo de `Pedido.fecha` sin verificarlo** (2B). Era `LocalDate`, no `String`. Lección: antes de juzgar, leer el modelo.

2. **Fui demasiado cómplice al inicio con decisiones del usuario** (2B). Aprobé "fallback nif→nombre" y "filtro activo=1" sin discutir. Lección: si veo un problema, decirlo en el momento.

3. **Redacté el Bloque 2B juntando dos cambios.** Defendible pero discutible. Lección: cuando hay duda, partir bloques. **Aplicada en 3C-paso-3** (3a + 3b separados) y en Bloque 4a (4 sub-pasos).

4. **Confundí `UPDATE_IF_EXISTS` con `UPDATE_EXISTING`** (2C). Lección: declarar nombres literales solo cuando se han verificado leyendo el archivo.

5. **Perdí genéricos al reescribir `EntityImportSpec`** (3C-paso-1). `List<FieldSpec>` quedó como `List`. Lección: cuando dicto "reemplazo completo de archivo", comparar carácter a carácter con el original.

6. **Dicté switch expression sin brazos con valor** (3C-paso-2b). Lección: antes de dictar Java nuevo, simular `javac` mentalmente.

7. **Entré en pánico falso por `git status` "limpio"** (3C-paso-1). Lección: `git log --oneline` antes de declarar emergencia.

8. **El usuario pegó por error respuesta idéntica del turno anterior** (3C-paso-2b fix). Lección: si dos salidas son idénticas, pedir verificación antes de actuar.

9. **Casi redacto 3C-paso-2c sin pedir el archivo objetivo.** Lección: pedir el archivo antes de redactar bloques que editen métodos concretos.

10. **Subida ambigua en sesión 3C-paso-3** (usuario subió `PresupuestosView.java` sin mensaje). Lección: ante subida sin texto, preguntar antes de actuar; no asumir reproceso.

11. **Dos basuras en mi propia redacción del bloque 3a** detectadas al releer: ternario inútil (`apellidos.isBlank() ? nombre : nombre`) y línea sentinela rara en el test. Lección: releer el bloque completo antes de darlo por bueno. Releer es barato y caza errores que `javac` no caza (los míos compilaban, pero eran código basura).

12. **Codex añadió un default no declarado en 3a** (`fecha = LocalDate.now().toString()` cuando el CSV no la trae) para que sus tests pasaran. La decisión era defendible (análoga a D-IVA y D-EST) pero no estaba autorizada. Lección: si Codex declara un cambio funcional no pedido, parar y pedir el diff antes de aprobar el commit, aunque los tests estén verdes. Tras revisar diff y tests se aceptó como D-FECHA-DEFAULT. **En Bloque 4a se aplicó la lección**: D-F-FECHA se declaró explícita en el bloque blindado original, no como cambio emergente.

13. **`Resumen.md` arrastró sin commit entre sesiones** (detectado tras 3b). El archivo se actualizó a v3.2 al cierre de la sesión anterior pero nunca se commiteó. Apareció en `git status` durante toda la sesión como "modified" y casi se cuela acumulado en commits de código. Al revertir un fallo de `str_replace` posterior se perdió ese trabajo intermedio. Lección: verificar `git diff --stat` al arrancar y declarar scope del archivo no-commiteado en el primer mensaje técnico. **En Bloque 4a el handoff v3.3 SÍ se commiteó** (`e8036f8`), evitando el problema.

14. **Intenté actualizar el handoff con 12 `str_replace` quirúrgicos** sobre un archivo que estaba a medio actualizar (v3.2 acumulada sin commit). Una de las cadenas no coincidió, Codex revirtió (correctamente) y volvió a v3.1, perdiendo el trabajo intermedio. Lección: para edits extensos a un archivo de documentación (handoff, READMEs largos), preferir reescritura completa sobre N reemplazos puntuales. **v3.4 se entrega como reescritura completa.**

15. **Codex hizo inserción aditiva en 4a.1 cuando dicté reemplazo.** Mi bloque pedía "reemplaza estas dos líneas de imports por estas siete"; Codex añadió las nuevas sin borrar las viejas. Funcionalmente equivalente, diff con 0 supresiones en lugar de 2. Resultado: estilo subóptimo en `Factura.java` (Deuda 17). Lección: cuando un `str_replace` puede convertirse en inserción aditiva sin pérdida funcional, redactar directamente como inserción ("inserta después de X"), no como reemplazo. Aplicada en pasos 4a.2.

16. **`Nothing to compile - all classes are up to date` me dio una falsa señal de "verificado".** Tras 4a.1, lancé `.\mvnw.cmd compile` y devolvió `BUILD SUCCESS` sin recompilar nada. Maven detectó que los `.class` estaban frescos respecto al `.java` anterior. Lección: tras editar un archivo, usar `.\mvnw.cmd clean compile` para forzar recompilación real. Solo entonces `BUILD SUCCESS` cuenta como prueba.

17. **Etiqueté un bug seguro como "deuda latente".** Detecté al leer `FacturaDAO` que `setInt(2, 0)` para `presupuesto_id=0` infringiría la FK contra `presupuestos.id`. Lo declaré "deuda preexistente, no la toco en Bloque 4". Los 3 tests sintéticos que no informaban `presupuesto_numero` la dispararon de inmediato porque `PRAGMA foreign_keys=ON` en `DatabaseManager`. Lección: si una "deuda" implica escribir un valor que infringe una constraint con FK activas, los tests sintéticos la cazarán. Tratarla como bug a fix en el mismo bloque, no como deuda diferible. Resuelto vía D-F-FK-NULL como ampliación del scope del Bloque 4a.

18. **`findstr /N "case \"Facturas\""` no escapó bien la comilla en PowerShell** y devolvió todo el archivo. Lección: para buscar literales con comillas dobles en `findstr` desde PowerShell, usar `/C:"..."` o cambiar a `Select-String -Pattern '...'` con regex que evite la comilla.

---

## ARCHIVOS YA INSPECCIONADOS — NO PEDIRLOS DE NUEVO

Estos archivos están analizados y leídos. NO pedirlos de nuevo al arrancar Bloque 4b salvo cambio explícito:

- `CLAUDE.md` — reglas operativas Multi-IA.
- `MIGRACION_HISTORICO.md` — procedimiento Vía A.
- `DuplicatePolicy.java` — enum: `SKIP_IF_EXISTS`, `UPDATE_EXISTING`, `CREATE_NEW`.
- `Tarifa.java` — patrón `IMPORT_SPEC` para entidades planas, constructor 4-arg.
- `Pedido.java` — modelo plano con `LocalDate` en 3 fechas. `IMPORT_SPEC` 4-arg.
- `Presupuesto.java` — cabecera + `List<LineaPresupuesto> lineas` + `calcularTotales()`. `IMPORT_SPEC` parent-child de 7 argumentos vía `buildSpec()` privado tras 3a.
- `LineaPresupuesto.java` — `(descripcion, tecnica, cantidad, precioUnit, descuento, total, orden)`. Constructor `(String, String, int, double, double)` que llama `calcularTotal()`. Setters disponibles.
- `PresupuestoDAO.java` — `save(p)` hace `insert(p)` o `update(p)` según `id==0`, luego `saveLineas(p)` (DELETE+INSERT total). `findById(id)` rellena lineas. Sin transacción explícita. Lanza `SQLException`.
- `PresupuestosView.java` — `importar()` cableado en 3b al patrón estándar. Botón `📥 Importar` con `this::importar`. Método de recarga local: `cargar()`. Helper `mostrarResultadoImportacion(ImportResult)` añadido en 3b. **Pedir al arrancar 4b como plantilla literal.**
- `Factura.java` — modelo cabecera + `List<LineaFactura> lineas` + `calcularTotales()`. `IMPORT_SPEC` parent-child de 7 argumentos vía `buildSpec()` privado tras 4a. 10 campos cabecera, 5 línea.
- `LineaFactura.java` — `(descripcion, tecnica, cantidad, precioUnit, descuento, total, orden)`. Idéntico a `LineaPresupuesto` campo a campo. Constructor `(String, String, int, double, double)`.
- `FacturaDAO.java` — `save(f)` hace `insert(f)` o `update(f)` según `id==0`, luego `saveLineas(f)`. `findById(id)` rellena lineas. Sin transacción explícita. `crearDesdePresupuesto(int)` para flujo UI (no usado por importador). `descontarMateriales(Factura)` para flujo UI. `set(PreparedStatement, Factura)` con `setNull` para `presupuesto_id=0` tras 4a.
- `FacturasView.java` — método `importar()` con Alert "Funcionalidad próximamente" pendiente de cablear en 4b. Método de recarga local: `cargar()`. NO existe `mostrarResultadoImportacion` (añadir en 4b). Botón `📥 Importar` color `#27AE60` en `buildToolbar()` con `this::importar`. Hay infraestructura `dynamicColumns` que no interfiere.
- `EntityImportSpec.java` — record con 7 componentes. Constructor secundario 4-arg para retrocompat. `esParentChild()`.
- `FieldSpec.java` — record `(String clave, String etiqueta, boolean obligatorio)`.
- `EntityImportService.java` — estado al cierre 4a (ver sección "Estado del archivo" arriba). Dispatcher `procesarGrupo` con `case "Presupuestos"` y `case "Facturas"`; Bloque 5 añadirá `case "Albaranes"`.
- `EntityImportServicePedidoTest.java` — plantilla literal del estilo de test plano. 10 tests con `@TempDir`, helpers `fila`/`mapping`/`crearCliente`.
- `EntityImportServicePresupuestoTest.java` — 7 tests con helpers `filaLinea`/`mapping`/`crearCliente`. Plantilla parent-child para Factura. Plantilla parent-child para Albarán.
- `EntityImportServiceFacturaTest.java` — 9 tests (Bloque 4a). Helpers `filaLinea` con 9 parámetros incluyendo `presupuestoNumero`. Tests específicos `resuelvePresupuestoNumeroAFkCuandoExiste` y `informaErrorCuandoPresupuestoNumeroNoExiste` validan D5 cerrada para Factura.
- `TarifasView.java`, `NominasView.java`, `PedidosView.java`, `PresupuestosView.java` — patrón de cableado del botón Importar. `PedidosView.importar()` líneas 606-653 + `mostrarResultadoImportacion` líneas 655-673 es la plantilla canónica.
- `DatabaseManager.java` — activa `PRAGMA foreign_keys = ON`. DDL inline. Cabecera de `facturas` con FK opcionales a `presupuestos` y `clientes`; `lineas_factura` idéntico campo a campo a `lineas_presupuesto`.

---

## RESPUESTA TÍPICA EN ESTE SPRINT

- Sin emojis salvo cita literal de código (los hay en la UI: `📥 Importar`, `📦 Material`).
- Sin "vamos a", "podríamos", "voy a intentar". Imperativo claro.
- Listas y tablas cuando estructuran. Prosa cuando fluye.
- Bloques de código con triple backtick y lenguaje declarado.
- Bloques para agentes en triple backtick anidado con cinco: ` ````` ` para que el usuario los copie limpios.
- Al final de cada respuesta técnica, una pregunta concreta o un siguiente paso explícito.

---

## PRIMER MENSAJE QUE VOY A RECIBIR

El usuario abrirá un chat nuevo y pegará este documento entero. Mi primer mensaje debe ser:

1. **Confirmar contexto cargado:** HEAD `e6d7a9a` (Bloque 4a cerrado) o el commit `docs:` v3.4 inmediatamente encima, 45/45 verdes, Bloque 4a cerrado completo. Decisiones D1–D6 + D-CN/D-IVA/D-EST/D-FV/D-CON/D-DC/D-TOT/D-SCO/D-FECHA-DEFAULT + D-F-FECHA/D-F-IVA/D-F-EST/D-F-FORMAPAGO/D-F-FV/D-F-TOT/D-F-DC/D-F-CN/D-F-MATERIALES/D-F-SINONIMOS/D-F-FK-NULL aplicadas en código. D5 cerrada para Factura, pendiente para Albarán. Siguiente: Bloque 4b (cablear `FacturasView.importar()`).

2. **Pedir verificación de estado:**
   - `git log --oneline -5` — confirmar HEAD en un commit `docs:` encima de `e6d7a9a` o en el propio `e6d7a9a`.
   - `git status` — confirmar working tree limpio.
   - `.\mvnw.cmd test` — confirmar 45/45 verdes con el reparto 2 + 12 + 5 + 10 + 7 + 9.

3. **Antes de redactar el bloque 4b, pedir al usuario** que pegue (o adjunte) el archivo:
   - `src/main/java/org/gipsybuho/ui/PresupuestosView.java`
   Es la plantilla literal para el `importar()` + `mostrarResultadoImportacion` que se va a clonar en `FacturasView`. NO redactar bloque 4b antes de tenerlo leído en sesión. Lección 3C-paso-2c y 4a aplicada.

4. **Con el archivo a la vista, redactar Bloque 4b** como un único bloque blindado para Codex:
   - `str_replace` del método `importar()` Alert en `FacturasView.java` por el patrón estándar adaptado a `FacturaDAO` y `Factura.IMPORT_SPEC`.
   - Inserción del helper `mostrarResultadoImportacion(ImportResult)` como copia literal del de `PresupuestosView`/`PedidosView`.
   - Releer antes de pegar.

5. **Verificar tras 4b:**
   - `.\mvnw.cmd clean compile` debe terminar BUILD SUCCESS (no fiarse de `Nothing to compile`).
   - `.\mvnw.cmd test` sigue en 45/45 (4b no añade tests; los existentes no deben verse afectados).
   - Smoke test manual sugerido (no obligatorio): `.\mvnw.cmd javafx:run` y verificar que el botón Importar de Facturas ya no muestra el Alert.

6. **Commit del 4b:** `feat: cablear importación CSV en FacturasView (4b)`.

7. **Si la verificación de estado revela divergencia**, diagnosticar antes de avanzar (mirar `git log --oneline -10` y comparar con commits listados en sección "Git" arriba).

FIN DEL HANDOFF.
