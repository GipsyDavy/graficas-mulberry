# HANDOFF — Graficas Mulberry · Sprint Importación CSV
# Versión: 3.1 · Fecha cierre: 19/05/2026 · Checkpoint: Bloque 3C-paso-2c cerrado y pusheado, listo para 3C-paso-3

---

## CÓMO TRATARME

- **Tuteo siempre.** No "usted", no "señor".
- **Crítica sin envolver.** No buscar agradar. Si una decisión mía es mala, dilo directo.
- **Defender la solución correcta sobre la fácil.** No es válido "es más rápido aceptar lo que ya hay" si lo que hay está mal.
- **No pedir permiso para cada paso.** Avanza. Si algo es ambiguo, decide con criterio y declara la asunción.
- **Si te equivocas, recula sin ceremonia.** Una disculpa breve y sigues. No hay que justificarse en exceso.
- **Verifica antes de juzgar.** Lección dura del sprint: di por hecho que `Pedido.fecha` era String sin mirar el modelo, y casi se revierte un parseo necesario por mal diagnóstico. **Antes de afirmar que una decisión de un agente es mala, lee el código que la motiva.**
- **Declara nombres exactos.** Lección de la sesión 2C: confundí `UPDATE_IF_EXISTS` con `UPDATE_EXISTING` por memoria. El nombre real del enum se verifica leyendo el archivo, no recordando. Aplica a enums, métodos, paquetes, constantes.
- **Verifica que el código compilaría antes de dictarlo.** Lección de la sesión 3C (dos veces): reescribí `EntityImportSpec` perdiendo los genéricos de `List<FieldSpec>` y luego dicté un `switch` expression sin brazos que devuelvan valor. Ambos fallos triviales en `javac` pero ambos los pasé al bloque sin filtrar. Antes de dictar Java nuevo, tirar mentalmente del compilador.
- **No copiar respuestas viejas del agente.** Lección del fix de 3C-paso-2b: el usuario pegó por error la salida del turno anterior idéntica al actual. Si dos salidas son carácter por carácter iguales, pedir verificación antes de actuar (un `findstr` o `type` del archivo afectado).
- **Antes de declarar emergencia, leer `git log`.** Sub-lección: si `git status` dice algo raro, `git log --oneline` lo confirma o lo desmiente antes que cualquier comando destructivo. No entrar en pánico por un `nothing added to commit` si no se ha mirado el log primero.
- **Pide el archivo antes de redactar bloques sobre métodos concretos.** Lección 3C-paso-2c: no fiarse del recuerdo del handoff sobre la forma exacta de un método si la sesión actual no lo ha leído. Pedir y leer es barato; redactar a ciegas un reemplazo carácter-por-carácter es caro.
- **Regla de inicio "_Gipsybuho_, procedo a ejecutar..."** del `CLAUDE.md`: aplícala solo en mensajes conversacionales claros. En respuestas técnicas, bloques, parches, listas de pasos, no aplica.

---

## CONTEXTO DEL PROYECTO

**Nombre:** Graficas Mulberry
**Ruta local:** `C:\Users\GipsyDavy\MAVEN\Graficas Mulberry`
**Stack:** Java 21 · JavaFX 21 · SQLite (vía JDBC) · Maven · JUnit 5
**Restricciones duras:** sin Lombok, sin Spring, sin servidores HTTP, sin nuevas dependencias salvo justificación.
**Build commands:** `.\mvnw.cmd compile`, `.\mvnw.cmd test`, `.\mvnw.cmd package`, `.\mvnw.cmd javafx:run`. `mvn` directo NO está en el PATH; usar siempre el wrapper.
**Versión Maven:** 3.9.11 vía wrapper.
**Reglas operativas:** ver `CLAUDE.md` en la raíz del proyecto. Karpathy-style: cambios quirúrgicos, mínima modificación, YAGNI, no refactorizar lo que no está roto, código auto-documentado.

### Estructura relevante
- `src/main/java/org/gipsybuho/model/` — entidades de dominio.
- `src/main/java/org/gipsybuho/dao/` — acceso a datos JDBC.
- `src/main/java/org/gipsybuho/service/` — lógica de servicios. **`EntityImportService` está aquí** con package `org.gipsybuho.service`, no en `.importer`. Verificado leyendo el archivo en 3C-paso-2c.
- `src/main/java/org/gipsybuho/service/importer/` — tipos auxiliares del importador: `EntityImportSpec`, `FieldSpec`, `ColumnMatcher`, `DuplicatePolicy`, `ImportResult`, `RowError`, `ErrorTipo`.
- `src/main/java/org/gipsybuho/ui/` — vistas JavaFX.
- `src/test/java/org/gipsybuho/service/importer/` — tests del importador. **Convención establecida:** todos los tests nuevos del importador van aquí.

### Documentos del proyecto que NO debes ignorar
- `CLAUDE.md` — reglas operativas, Multi-IA, cambios quirúrgicos.
- `MIGRACION_HISTORICO.md` — procedimiento Vía A para procesar archivos históricos del cliente. Clave: el `ImportService` ya normaliza fechas Excel a ISO en parseo, antes de llegar a `EntityImportService`.

### Valores conocidos del enum `DuplicatePolicy`
`SKIP_IF_EXISTS` (default), `UPDATE_EXISTING`, `CREATE_NEW`. **NO existe `UPDATE_IF_EXISTS`** — error frecuente, verificado en este sprint.

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
- **Codex (en IDE):** edición local, ejecución de comandos, parches quirúrgicos, inspección. Es el ejecutor de los bloques blindados. Confirmado en 3C-paso-2c: obedece reemplazo carácter por carácter y respeta el "no commitear" cuando se le dice.
- **Gemini (en IDE):** contexto amplio, arquitectura, segunda opinión, investigación. **No usado todavía en este sprint.** Reservado para Bloque 5 (Albarán + transacción explícita + FKs opcionales) o segunda opinión si Claude Code y yo discrepamos.

**Cómo redactar bloques para los agentes — lecciones consolidadas:**
- Instrucciones cerradas, sin espacio interpretativo libre.
- Restricciones negativas explícitas (qué NO modificar, qué NO ejecutar, qué NO invocar).
- Criterio de éxito verificable sin reejecutar (lista de archivos, salida de tests, decisiones declaradas).
- Una sola tarea por bloque (excepción justificada: refactor mecánico + entidad nueva si ambos comparten patrón).
- Trazabilidad obligatoria al final.
- **Declarar nombres literales** de enums, métodos, paquetes. Si no los conoces con certeza, pídelos al usuario antes de redactar el bloque.
- **Verificar que el snippet de Java sería aceptado por `javac` aislado** antes de dictarlo. Específicamente: genéricos parametrizados, switch expressions exhaustivas con valor en al menos un brazo, imports presentes.
- **Añadir `git diff --stat` al criterio de éxito** como segunda verificación barata de scope.
- **Pedir el archivo objetivo antes de redactar bloques no triviales sobre métodos que no se han leído en la sesión actual.** Lección 3C-paso-2c.

**Aviso explícito del usuario (importante):** "En acciones anteriores detectamos incoherencias, desobediencias y falta de fiabilidad en la ejecución de las instrucciones dadas por ti o por mi a los agentes IA del IDE". Por eso los bloques deben ser blindados y verificables. Validar siempre el entregable antes de aceptar.

**Convención de commits establecida:** un bloque = un commit + push. Mensaje con título imperativo (`feat:`, `fix:`) ≤72 chars, línea en blanco, cuerpo con párrafos separados por líneas en blanco. Editor configurado: `git config --global core.editor "notepad"`.

---

## SPRINT ACTUAL — IMPORTACIÓN CSV

### Pedido original del usuario (literal)
> "necesito que las clases presupuesto, factura, clientes, albaranes, materiales, empleados y nominas puedan importar con la misma funcionalidad que le hemos puesto a tarifa"

### Reinterpretación tras inspección de código

El importador ya soportaba 4 entidades planas antes del sprint (Tarifa, Cliente, Material, Empleado). Lo que faltaba:

1. Nómina (plana, FK a Empleado por nombre+apellidos). **HECHO** (commit `c43118a`).
2. Pedido (plano, FK a Cliente por nif sin fallback / nombre si nif vacío). **HECHO** (commit `50d5902` + fix UPDATE path en `9798225`).
3. Presupuesto (parent-child). **PENDIENTE** (infraestructura completa al cierre 2c, falta caso concreto).
4. Factura (parent-child). **PENDIENTE**.
5. Albarán (parent-child). **PENDIENTE**.

Vistas con Alert "Funcionalidad próximamente" todavía pendientes: `PresupuestosView`, `FacturasView`, `AlbaranesView`. (`NominasView` y `PedidosView` ya cableadas).

### Decisiones funcionales tomadas (críticas, NO se rediscuten)

| Decisión | Valor | Justificación |
|---|---|---|
| Modelo CSV para parent-child | **CSV ancho con cabecera repetida en cada línea, agrupar por `numero`** | Encaja con scripts Python descritos en `MIGRACION_HISTORICO.md`. Un solo archivo por entidad. Reusa motor actual. |
| FK cliente desde CSV (Pedido y futuros Presupuesto/Factura/Albarán) | **Si viene `nif` y no existe → ERROR sin fallback. Si `nif` vacío → buscar por nombre+apellidos.** | Si el usuario pone nif explícito, es específico. Hacer fallback enmascara errores tipográficos. |
| FK empleado desde CSV (Nómina) | Buscar por nombre+apellidos. Si apellidos vacío, solo por nombre. Match único → ok, múltiple → error ambigüedad. | Empleado no tiene clave única tan fiable como NIF para CSV humano. |
| Filtro `activo=1` en empleados al resolver FK | **Aplicado.** | Coherente con `EmpleadoDAO.findAll()`. **DEUDA 2**: puede romper para nóminas históricas de empleados de baja. |
| Política duplicados por defecto | `SKIP_IF_EXISTS` | Patrón ya establecido. |
| Clave duplicado Nómina | `(empleado_id, mes, anio)` | Natural. |
| Clave duplicado Pedido | `numero` | Identificador natural. |
| Validación de fechas en Pedido (cierre 2B) | Opción A: detectar en `ensamblarPedido` comparando `errores.size()`. | Mínimo cambio. **DEUDA 1 CERRADA** en 3C-paso-2a vía `CAMPOS_FECHA` en Fase 2. |

### Decisiones del Bloque 3 (D1–D6, todas tomadas y aplicadas)

| ID | Decisión | Valor | Estado |
|---|---|---|---|
| D1 | Política de inconsistencia entre filas con mismo `numero` | **Estricta + Opción A** (todos los campos de `spec.campos()` cuentan). Si dos filas del mismo grupo difieren en algún campo, el grupo entero se descarta y cada fila recibe un RowError con el primer campo discrepante. | **CERRADA** en 3C-paso-2c (commit `2310588`). Método `detectarInconsistenciaGrupo` invocado desde `agruparEnFase2_5`. |
| D2 | Modelado en `EntityImportSpec` para parent-child | Ampliar `EntityImportSpec` con campos opcionales (`claveAgrupacion`, `campoLineas`, `specLinea`). | **HECHO** en 3C-paso-1 (commit `0bb82c7`). |
| D3 | `UPDATE_EXISTING` para parent-child | Bloquear con error claro en `EntityImportService`. | **HECHO** en 3C-paso-2b: `IllegalArgumentException` al inicio de `importar()`. |
| D4 | Deuda 4 (UPDATE path de Pedido sin guard de errores) | Cerrada en commit `9798225` antes del Bloque 3. | HECHO. |
| D5 | FKs opcionales (Albarán→Factura/Pedido, Factura→Presupuesto) | Postergar a Bloque 4/5. En Bloque 3 esas FK quedan siempre vacías al importar. | PENDIENTE. |
| D6 | Validación de fechas en Fase 2 | Añadir `CAMPOS_FECHA` análogo a `CAMPOS_NUMERICOS`. | **HECHO** en 3C-paso-2a (commit `fc63a50`). Cierra deuda 1. |

### Sub-decisiones tomadas en 3C-paso-2b (no rediscutir)

| Sub-decisión | Valor | Razón |
|---|---|---|
| Punto de inserción de Fase 2.5 | Método privado nuevo entre Fase 2 y Fase 3, invocado desde `importar()` solo si `spec.esParentChild()`. | P1 análisis Claude Code: hueco natural entre L81 y L84 del archivo pre-2b. Fases con responsabilidad única. |
| Modelo de datos de grupo | Record privado `ValidGroup(String clave, List<ValidRow> filas)`. | P2 análisis Claude Code: análogo a `ValidRow` existente, preserva orden CSV explícitamente. |
| Granularidad del savepoint | Bifurcar en `insertarFilas` (renombrado de `insertarEnTransaccion`) e `insertarGrupos`. | P3 análisis Claude Code: estructura idéntica, coste de bifurcar bajo (8 líneas), evita doble concern. |
| Switch de procesamiento | Bifurcar en `procesarFila` (planos, sin cambios) y `procesarGrupo` (parent-child). | P4 análisis Claude Code: cada switch limpio por su lado. |
| Semántica de `ImportResult` para parent-child | `insertadas`/`actualizadas` cuentan **entidades**, no filas CSV. `descartadas = filas.size() - filasOk`, donde `filasOk` cuenta filas CSV consumidas con éxito en grupos OK. | Opción B + ajuste del cálculo. `procesarGrupo` devuelve `int[]{insertadas, actualizadas, filasDelGrupo}`. |
| Riesgo 1 (atomicidad parent-child) | Fallos pre-INSERT-cabecera retornan `{0,0,0}` con RowError. Fallos post-INSERT-cabecera lanzan `SQLException` para que el savepoint rollee. | Detectado por Claude Code. Documentado en Javadoc de `procesarGrupo`. |
| Riesgo 2 (clave de agrupación blank) | `agruparEnFase2_5` valida `vals.get(claveAgrupacion)` no-blank antes de aceptar la fila. RowError individual + descarte de la fila (no del grupo). | Detectado por Claude Code. Implementado en 3C-paso-2b. |

### Sub-decisiones tomadas en 3C-paso-2c (no rediscutir)

| Sub-decisión | Valor | Razón |
|---|---|---|
| Granularidad del RowError de inconsistencia | **Un RowError por fila del grupo** (no uno por campo discrepante), indicando el primer campo discrepante encontrado en orden de `spec.campos()`. | Si difieren 3 campos en 5 filas, 1 RowError × fila = 5 errores legibles; 1 × campo × fila = 15, ruido. La fila "canon" para comparar es la primera del grupo; si difiere de las demás, todas quedan marcadas por igualdad de efecto. |
| Normalización de la comparación | `String.equals()` directo. Sin `lower()`, sin normalización de tildes. La sanitización Fase 1 ya hizo `trim()`. | Coherente con política estricta: "María" ≠ "Maria" debe detectarse, no enmascararse. |
| Lugar del método auxiliar | Método privado nuevo `detectarInconsistenciaGrupo(spec, clave, filas)` justo después de `agruparEnFase2_5`, invocado dentro del bucle final de éste. | Separación de fases. Permite test aislado futuro si llega el caso. |

---

## ESTADO TÉCNICO AL CIERRE DE ESTA SESIÓN

### Git
- **Rama:** `master`
- **HEAD:** `2310588` — `feat: detectar inconsistencia entre filas del mismo grupo parent-child` (commit 2c ya pusheado en esta sesión).
- **Commits del sprint en orden cronológico:**
  - `c43118a` — Bloque 2A: importación CSV de Nóminas.
  - `50d5902` — Bloque 2B: importación CSV de Pedidos.
  - `9798225` — Bloque 2C: fix UPDATE path Pedido.
  - `0bb82c7` — Bloque 3C-paso-1: ampliar EntityImportSpec parent-child.
  - `fc63a50` — Bloque 3C-paso-2a: CAMPOS_FECHA + validación ISO Fase 2.
  - `a8b596f` — Bloque 3C-paso-2b: infraestructura parent-child (ValidGroup, agruparEnFase2_5, insertarGrupos, procesarGrupo vacío, bloqueo UPDATE_EXISTING).
  - `2310588` — Bloque 3C-paso-2c: `detectarInconsistenciaGrupo` (cierra D1).
- **origin/master:** sincronizado con `HEAD`.
- **Working tree:** limpio salvo `Resumen.md` untracked. Conviene committearlo en algún momento para no perderlo.

### Tests
- **29/29 verdes** confirmados al cierre de 3C-paso-2c.
- Desglose:
  - `ClienteDAOTest` — 2
  - `ImportBackupServiceTest` — 12
  - `EntityImportServiceNominaTest` — 5
  - `EntityImportServicePedidoTest` — 10 (incluye `validaFormatoFechaEnFase2` añadido en 2a)
- Sin tests nuevos en 3C-paso-1 ni 3C-paso-2b ni 3C-paso-2c. Los specs parent-child reales llegan en 3C-paso-3 y traerán los primeros tests parent-child.

### Estado del archivo `EntityImportService.java` (post-2c)
- Package: `org.gipsybuho.service`.
- Constantes: `MAX_FILAS=10_000`, `MAX_LEN=255`, `MAX_LEN_LIBRE=1_000`, `CAMPOS_NUMERICOS`, `CAMPOS_FECHA` (3 elementos: `"fecha"`, `"fecha_entrega_prevista"`, `"fecha_entrega_real"`), `CAMPOS_LIBRES` (`"notas"`, `"descripcion"`).
- Records privados: `ValidRow(int numero, Map<String,String> vals)`, `ValidGroup(String clave, List<ValidRow> filas)`.
- Fase 1 `mapearValores` y `sanitizar`: sin cambios desde 2a.
- Fase 2 `validarFila`: `CAMPOS_NUMERICOS` + `CAMPOS_FECHA` (2a) + `CAMPOS_LIBRES`.
- **Fase 2.5** `agruparEnFase2_5`: agrupa por `spec.claveAgrupacion()`, valida no-blank, e **invoca `detectarInconsistenciaGrupo` por cada grupo formado**. Si la lista devuelta no está vacía, vuelca los errores y descarta el grupo. **Modificado en 2c.**
- Helper nuevo en 2c: `detectarInconsistenciaGrupo(EntityImportSpec spec, String clave, List<ValidRow> filas)` → `List<RowError>`. Compara la primera fila ("canon") contra cada otra en todos los campos de `spec.campos()`. Devuelve `List.of()` si el grupo es coherente o un `RowError` por fila si hay inconsistencia.
- Fase 3 bifurcada:
  - `insertarFilas` (renombrado en 2b): savepoint por fila, switch `procesarFila` con los 6 casos planos.
  - `insertarGrupos` (nuevo en 2b): savepoint por grupo, switch `procesarGrupo`. Solo `default → throw` hasta 3C-paso-3.
- Bloqueo `UPDATE_EXISTING` para parent-child al inicio de `importar()` (desde 2b).
- Cálculo de `ImportResult`: `descartadas = filas.size() - filasOk`. Las filas de grupos descartados por inconsistencia NO entran en `insertarGrupos`, NO suman a `filasOk`, y por tanto SÍ cuentan como descartadas. Coherente sin cambio adicional.

---

## PLAN DE BLOQUES — ESTADO

| Bloque | Descripción | Estado |
|---|---|---|
| 1 | Análisis Nómina/Albarán (lectura, sin código) | ✅ HECHO |
| 2A | Implementar Nómina + tests | ✅ HECHO. Commit `c43118a`. |
| 2B | Implementar Pedido + extraer `resolverFkPorNombre` + tests | ✅ HECHO. Commit `50d5902`. |
| 2C | Fix deuda 4: UPDATE path Pedido | ✅ HECHO. Commit `9798225`. |
| 3A | Análisis parent-child (DAOs, modelos línea, motor) | ✅ HECHO. Entregado por Claude Code. |
| 3B | Diseño parent-child (decisiones D1–D6) | ✅ HECHO. |
| 3C-paso-1 | Ampliar `EntityImportSpec` con campos opcionales parent-child | ✅ HECHO. Commit `0bb82c7`. |
| 3C-paso-2a | `CAMPOS_FECHA` + validación ISO Fase 2 (cierra deuda 1) | ✅ HECHO. Commit `fc63a50`. |
| 3C-paso-2b | Infraestructura parent-child (ValidGroup, agruparEnFase2_5, insertarGrupos, procesarGrupo vacío, bloqueo UPDATE_EXISTING) | ✅ HECHO. Commit `a8b596f`. |
| 3C-paso-2c | Detección de inconsistencia entre filas del mismo grupo (cierra D1) | ✅ HECHO. Commit `2310588`. |
| **3C-paso-3** | **Implementar Presupuesto end-to-end: `Presupuesto.IMPORT_SPEC` parent-child, `case "Presupuestos"` en `procesarGrupo`, uso de `calcularTotales()`, retirar `@SuppressWarnings("unused")` de `procesarGrupo`, cableado de `PresupuestosView`, tests del primer caso parent-child real.** | **⏳ PENDIENTE. Punto exacto de continuación.** |
| 4 | Implementar Factura con motor parent-child (línea idéntica a Presupuesto). Tests. Cablear `FacturasView`. | PENDIENTE |
| 5 | Implementar Albarán. Decidir transacción explícita en `AlbaranDAO.save()`. Decidir resolución FK opcionales (deuda D5 pendiente). Considerar Gemini para revisión arquitectónica. | PENDIENTE |
| 6 | Actualizar `MIGRACION_HISTORICO.md`: quitar Alerts ya resueltos, documentar formato CSV ancho con ejemplo. | PENDIENTE |

---

## PUNTO EXACTO DE CONTINUACIÓN

El próximo bloque a redactar es **3C-paso-3**: implementar Presupuesto end-to-end usando la infraestructura parent-child ya montada. Primer caso real que ejercitará `agruparEnFase2_5`, `detectarInconsistenciaGrupo`, `insertarGrupos`, `procesarGrupo` y el constructor parent-child de `EntityImportSpec`.

### Decisiones probables a tomar antes de redactar 3C-paso-3

1. **Forma de `Presupuesto.IMPORT_SPEC` y `LineaPresupuesto.IMPORT_SPEC`.** Definir campos exactos, obligatoriedad, `claveAgrupacion="numero"`, `campoLineas="lineas"`. Para esto necesito leer `Presupuesto.java` y `LineaPresupuesto.java`.
2. **Tratamiento del cliente en el spec.** Tres campos opcionales: `cliente_nif`, `cliente_nombre`, `cliente_apellidos`. Reusar `resolverClienteId` que ya existe (extraído en 2B). Decisión funcional ya tomada: nif explícito → exacto sin fallback; nif vacío → nombre+apellidos.
3. **Política de duplicados Presupuesto.** Clave natural `numero`. `SKIP_IF_EXISTS` por defecto, `UPDATE_EXISTING` bloqueado (ya garantizado por 2b), `CREATE_NEW` permitido pero requiere `numero` distinto, lo que en CSV ancho con un solo grupo por `numero` significa "duplicar el grupo entero con `numero` nuevo" — confirmar comportamiento con el usuario o decidir.
4. **Totales.** ¿Se importan del CSV y se confía, o se recalculan con `calcularTotales()` al ensamblar? Apuesta del coordinador: recalcular siempre. El CSV humano puede traer redondeos inconsistentes; `calcularTotales()` es la verdad. Confirmar con el usuario.
5. **Cableado UI.** `PresupuestosView` tiene Alert "próximamente". Sustituir por flujo idéntico a `PedidosView`. Verificar que el modelo de tabs Servicios+Materiales no estorba.
6. **Tests.** Primer test parent-child. Mínimo: import OK con 1 grupo de 2 líneas, detección de inconsistencia descarta grupo, FK cliente por nif OK, FK cliente por nif inexistente falla, FK cliente por nombre+apellidos OK, `UPDATE_EXISTING` lanza al inicio.

### Archivos a pedir al usuario al arrancar la nueva sesión

- `Presupuesto.java` — verificar firma exacta de `calcularTotales()`, getters/setters, tipos de fecha. **Importante**: el handoff dice "fecha String, fechaValidez String", pero después de la lección de Pedido (que era `LocalDate`), NO asumir y leer el archivo.
- `LineaPresupuesto.java` — getters/setters exactos y constructores disponibles.
- `PresupuestoDAO.java` — firma exacta de `save(Presupuesto)`, manejo de líneas, excepciones (¿`SQLException` o `Exception`?), id tras insert.
- `Tarifa.java` o `Pedido.java` (uno de los dos, el que esté más cerca del patrón a copiar) — referencia literal de `IMPORT_SPEC` para evitar errores de sintaxis del builder/constructor.
- `PresupuestosView.java` — punto exacto del Alert "próximamente" a sustituir.
- `EntityImportSpec.java` y `FieldSpec.java` — confirmación de constructores disponibles. Probablemente ya inspeccionados pero conviene releer en sesión nueva.

Verificación de estado de arranque siempre: `git log --oneline -5`, `git status`, `.\mvnw.cmd test`.

---

## DEUDAS TÉCNICAS — ESTADO

| ID | Descripción | Estado |
|---|---|---|
| 1 | Validación de fechas en Fase 2 (`validarFila`) en lugar de Fase 3 | **CERRADA** en 3C-paso-2a vía `CAMPOS_FECHA`. Cubre los 3 campos `LocalDate` de Pedido. Presupuesto/Factura/Albarán se sumarán cuando se introduzcan sus specs. |
| 2 | Filtro `activo=1` en `resolverEmpleadoId` puede romper nóminas históricas | ABIERTA. Reevaluar con archivos reales. |
| 3 | `lower()` en SQLite no normaliza tildes ("María" vs "Maria") | ABIERTA. Considerar normalización en Java antes de SQL si aparece el caso. |
| 4 | UPDATE path Pedido sin guard de errores post-`aplicarValoresPedido` | **CERRADA** en commit `9798225`. Verificación adicional: el test `rechazaActualizacionConFechaMalFormada` sigue verde tras 2a, confirmando que el guard no entra en conflicto con la validación de Fase 2. |
| 5 | Contrato `filtroExtraSql` admite `null` pero rechaza blank con excepción | ABIERTA. Riesgo bajo. Documentar si se añade caller externo. |
| 6 | `UPDATE_EXISTING` para Nómina y Pedido sin test directo | PARCIALMENTE CUBIERTA en 2C (test `rechazaActualizacionConFechaMalFormada` ejercita UPDATE path de Pedido). Nómina sigue sin test directo. |
| 7 | Asimetría: `aplicarValoresNomina` no recibe `errores` (firma distinta a `aplicarValoresPedido`) | ABIERTA. Sin bug observable hoy. Reevaluar si se añaden campos validables en Nómina. |
| 8 | `mostrarResultadoImportacion` duplicado en 5+ vistas | ABIERTA. Refactor UI fuera del sprint. |
| 9 | `AlbaranDAO.save()` sin transacción explícita BEGIN/COMMIT | ABIERTA. Mitigada por transacción del importador. Decidir en Bloque 5. |
| 10 | `saveLineas` hace DELETE+INSERT total → riesgo destructivo con `UPDATE_EXISTING` en parent-child | **CERRADA** en 3C-paso-2b vía bloqueo de `UPDATE_EXISTING` al inicio de `importar()` para specs parent-child. |
| 11 | `fecha_alta` de Empleado sin validación ISO (no incluido en `CAMPOS_FECHA`) | ABIERTA, riesgo bajo. `MIGRACION_HISTORICO.md` normaliza fechas Excel a ISO antes del importador. Reevaluar si aparece caso real que rompa. |
| 12 | `@SuppressWarnings("unused")` en `procesarGrupo` mientras no haya casos | ABIERTA por diseño. Se retira en 3C-paso-3 al añadir el primer `case "Presupuestos"`. |
| 13 | Detección de inconsistencia parent-child sin test ejecutable | ABIERTA por diseño. Se cubre en 3C-paso-3 con el primer spec real (test "descarta grupo con cabecera inconsistente"). |

---

## ERRORES COMETIDOS EN ESTE SPRINT (para no repetirlos)

1. **Asumí el tipo de `Pedido.fecha` sin verificarlo** (sesión 2B). Lo di por `String` como en `Presupuesto`. Era `LocalDate`. Casi mandé al usuario a revertir un parseo necesario. Lección: antes de juzgar una decisión de un agente, leer el modelo afectado.

2. **Fui demasiado cómplice al inicio con decisiones del usuario** (sesión 2B). Aprobé "fallback nif→nombre" sin discutir, y "filtro activo=1" sin pensar. Ambas tuve que matizarlas después. Lección: si veo un problema en una decisión, decirlo en el momento.

3. **Redacté el Bloque 2B juntando dos cambios** (entidad Pedido + extracción de resolver genérico). Defendible pero discutible según Karpathy. Funcionó, pero asumí riesgo innecesario. Lección: cuando hay duda, partir bloques.

4. **Confundí `UPDATE_IF_EXISTS` con `UPDATE_EXISTING`** (sesión 2C). Mi memoria del enum era incorrecta. Codex copió literal lo que le dicté. Fix de una línea pero retrasó el commit. Lección: declarar nombres literales solo cuando se han verificado leyendo el archivo.

5. **Perdí genéricos al reescribir `EntityImportSpec` completo** (sesión 3C-paso-1). `List<FieldSpec>` quedó como `List`. Codex obedeció literal el reemplazo, todo el proyecto dejó de compilar por raw types en cascada en 4 archivos. Fix `3C-paso-1-FIX` con verificación explícita de chevrons añadida al criterio. Lección: cuando dicto "reemplazo completo de archivo", comparar carácter a carácter con el original que el usuario me pasó.

6. **Dicté switch expression sin brazos con valor** (sesión 3C-paso-2b). `procesarGrupo` con solo `default -> throw` no compila: Java exige que algún brazo produzca valor para tipar la expresión. Fix trivial sustituyendo por `throw` directo. Lección: antes de dictar Java nuevo, simular `javac` mentalmente. En switch expressions: ¿hay al menos un brazo que devuelva valor del tipo declarado?

7. **Entré en pánico falso por `git status` "limpio"** (sesión 3C-paso-1). El usuario había commiteado y pusheado correctamente; yo vi un `nothing added to commit` posterior y asumí pérdida de cambios. Lección: ante salidas raras de `git status`, mirar primero `git log --oneline` antes de declarar emergencia. `git log` dice la verdad sobre lo que existe; `git status` solo dice qué hay sin commitear ahora.

8. **El usuario pegó por error respuesta idéntica del turno anterior** (sesión 3C-paso-2b fix). Las dos salidas eran carácter por carácter iguales. Lección: si dos salidas consecutivas del agente parecen idénticas, pedir verificación con `findstr` o `type` antes de actuar.

9. **Casi redacto 3C-paso-2c sin pedir el archivo objetivo** (sesión 3C-paso-2c). Iba a dictar un reemplazo dentro de `agruparEnFase2_5` apoyándome solo en el recuerdo del handoff. Lo evité a tiempo pidiendo el archivo. Lección consolidada: **antes de redactar bloques que editen métodos concretos, pedir el archivo de la sesión actual**, no fiarse del recuerdo del handoff.

---

## ARCHIVOS YA INSPECCIONADOS — NO PEDIRLOS DE NUEVO

Estos archivos están analizados y resumidos. NO pedirle al usuario que los suba salvo cambio explícito o necesidad de releer al arrancar 3C-paso-3 (marcado abajo donde aplique):

- `CLAUDE.md` — reglas operativas Multi-IA.
- `MIGRACION_HISTORICO.md` — procedimiento Vía A.
- `DuplicatePolicy.java` — enum: `SKIP_IF_EXISTS`, `UPDATE_EXISTING`, `CREATE_NEW`.
- `Tarifa.java` — patrón de IMPORT_SPEC para entidades planas.
- `Pedido.java` — modelo plano con `LocalDate` en las 3 fechas.
- `Presupuesto.java` — modelo cabecera (numero, clienteId, fecha String, fechaValidez String, estado, totales) + `List<LineaPresupuesto> lineas` + `calcularTotales()`. **Releer al arrancar 3C-paso-3 para confirmar tipos exactos antes de redactar el spec.**
- `LineaPresupuesto.java`, `LineaFactura.java`, `LineaAlbaran.java` — modelos de línea. **Releer `LineaPresupuesto.java` al arrancar 3C-paso-3.**
- `PresupuestoDAO.java`, `FacturaDAO.java`, `AlbaranDAO.java` — `save()` orquesta cabecera+líneas, `saveLineas()` es `private`, sin transacción explícita, IDs autoincrement, orden asignado por DAO, DELETE+INSERT total al actualizar líneas. **Releer `PresupuestoDAO.java` al arrancar 3C-paso-3 para confirmar firma exacta.**
- `EntityImportSpec.java` — record con 7 componentes (4 + 3 opcionales parent-child). Constructor secundario para retrocompat. Validación de coherencia. Helper `esParentChild()`.
- `FieldSpec.java` — record `(String clave, String etiqueta, boolean obligatorio)`. Sirve tal cual para campos de línea.
- `EntityImportService.java` — estado al cierre 3C-paso-2c. Bifurcación parent-child completa con detección de inconsistencia activa (Opción A, estricta). `procesarGrupo` sigue vacío (solo default → throw). Inspeccionado completo en 3C-paso-2c.
- `TarifasView.java`, `NominasView.java`, `PedidosView.java` — patrón de cableado del botón Importar.
- `PresupuestosView.java` — vista con Alert "próximamente" y modelo de tabs Servicios+Materiales. **Releer al arrancar 3C-paso-3 para localizar punto exacto del Alert.**

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

1. Confirmar contexto cargado: HEAD `2310588` post-2c, 29/29 verdes, decisiones D1–D6 cerradas y aplicadas, próximo paso 3C-paso-3.
2. Pedir los 3 datos de verificación del estado:
   - `git log --oneline -5` — confirmar que HEAD sigue en el commit de 2c y no hay commits intermedios desconocidos.
   - `git status` — confirmar working tree limpio.
   - `.\mvnw.cmd test` — confirmar 29/29 verdes.
3. Pedir los archivos clave para 3C-paso-3: `Presupuesto.java`, `LineaPresupuesto.java`, `PresupuestoDAO.java`, `PresupuestosView.java`, y un `IMPORT_SPEC` de referencia (`Tarifa.java` o `Pedido.java`). Lección 3C-paso-2c: pedir el archivo antes de redactar el bloque, no después.
4. Una vez verificado el estado y leídos los archivos, plantear las decisiones probables del bloque (especialmente: recalcular totales con `calcularTotales()` o confiar en el CSV; semántica de `CREATE_NEW` en parent-child) y, con las respuestas, redactar 3C-paso-3.

Si la verificación de estado revela divergencia (otro commit, tests rotos, working tree sucio), diagnosticar antes de avanzar.

FIN DEL HANDOFF.
