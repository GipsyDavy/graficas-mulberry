# HANDOFF — Graficas Mulberry · Sprint Importación CSV
# Versión: 3.3 · Fecha cierre: 19/05/2026 · Checkpoint: Bloque 3 cerrado al completo. Siguiente: Bloque 4 (Factura parent-child).

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
- **Pide el archivo antes de redactar bloques sobre métodos concretos.** Lección 3C-paso-2c y reforzada en 3C-paso-3a: no fiarse del recuerdo del handoff sobre la forma exacta de un método si la sesión actual no lo ha leído. Pedir y leer es barato; redactar a ciegas un reemplazo carácter-por-carácter es caro.
- **Regla de inicio "_Gipsybuho_, procedo a ejecutar..."** del `CLAUDE.md`: aplícala solo en mensajes conversacionales claros. En respuestas técnicas, bloques, parches, listas de pasos, no aplica.
- **Releer el bloque redactado antes de pegarlo.** Lección 3C-paso-3a: detecté dos basuras en mi propia redacción al releer (ternario inútil `apellidos.isBlank() ? nombre : nombre` y línea sentinela rara con comparación absurda). Releer una vez antes de dar por bueno el bloque cazó ambos. Releer es barato.
- **Si un archivo aparece como modificado al arrancar sesión sin commit previo identificable, declararlo explícitamente.** Lección 3C-paso-3b: `Resumen.md` arrastró cambios v3.2 entre sesiones porque nunca se commiteó y se acumuló como "ruido" durante 3a y 3b; al revertir un fallo de `str_replace` se perdió ese trabajo intermedio. En el primer pase de cada sesión, verificar `git diff --stat` y declarar scope del archivo no-commiteado antes de empezar a editar nuevos archivos. PowerShell con encoding CP-850 muestra UTF-8 como salsa rara (`Versi├│n`, `ÔÇö`) y oculta cambios reales; usar `Format-Hex` cuando el diff se vea sospechoso.
- **Si Codex declara un cambio funcional no pedido, parar y pedir el diff antes de aprobar.** Lección 3C-paso-3a: Codex añadió default `fecha = LocalDate.now().toString()` para que sus tests pasaran. La decisión era defendible (análoga a D-IVA y D-EST) pero no estaba autorizada. Aunque los tests estén verdes, revisar diff antes del commit y, si el cambio se acepta, declararlo como decisión explícita en el mensaje.

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
- `src/main/java/org/gipsybuho/service/` — lógica de servicios. **`EntityImportService` está aquí** con package `org.gipsybuho.service`, no en `.importer`. Verificado leyendo el archivo.
- `src/main/java/org/gipsybuho/service/importer/` — tipos auxiliares del importador: `EntityImportSpec`, `FieldSpec`, `ColumnMatcher`, `DuplicatePolicy`, `ImportResult`, `RowError`, `ErrorTipo`.
- `src/main/java/org/gipsybuho/ui/` — vistas JavaFX.
- `src/test/java/org/gipsybuho/service/importer/` — tests del importador. **Convención establecida:** todos los tests nuevos del importador van aquí.

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
- **Codex (en IDE):** edición local, ejecución de comandos, parches quirúrgicos, inspección. Ejecutor de los bloques blindados. Confirmado en 3C-paso-2c y 3a/3b: obedece reemplazo carácter por carácter y respeta el "no commitear" cuando se le dice. Sigue las reglas de "fallar y revertir" sin improvisar (verificado en intento fallido de actualización del handoff).
- **Gemini (en IDE):** contexto amplio, arquitectura, segunda opinión, investigación. **No usado todavía en este sprint.** Reservado para Bloque 5 (Albarán + transacción explícita + FKs opcionales) o segunda opinión si Claude Code y yo discrepamos.

**Cómo redactar bloques para los agentes — lecciones consolidadas:**
- Instrucciones cerradas, sin espacio interpretativo libre.
- Restricciones negativas explícitas (qué NO modificar, qué NO ejecutar, qué NO invocar).
- Criterio de éxito verificable sin reejecutar (lista de archivos, salida de tests, decisiones declaradas).
- Una sola tarea por bloque (o sub-paso). Lección 3C-paso-3: 3a y 3b se redactan por separado aunque sean del mismo paso conceptual.
- Trazabilidad obligatoria al final.
- **Declarar nombres literales** de enums, métodos, paquetes. Si no los conoces con certeza, pídelos al usuario antes de redactar el bloque.
- **Verificar que el snippet de Java sería aceptado por `javac` aislado** antes de dictarlo: genéricos parametrizados, switch expressions exhaustivas con valor en al menos un brazo, imports presentes.
- **Añadir `git diff --stat` al criterio de éxito** como segunda verificación barata de scope.
- **Pedir el archivo objetivo antes de redactar bloques no triviales** sobre métodos que no se han leído en la sesión actual.
- **Releer el bloque redactado antes de darlo por bueno.** Lección 3C-paso-3a.
- **Para edits de archivos grandes con cambios extensos, preferir reescritura completa sobre N `str_replace`** si los cambios afectan a la mayoría de secciones. Lección 3C-paso-3 cierre: 12 `str_replace` para un handoff a medio actualizar fallaron por mismatch de cadena; reescritura íntegra evita ese fallo y produce diff más limpio.

**Aviso explícito del usuario:** "En acciones anteriores detectamos incoherencias, desobediencias y falta de fiabilidad en la ejecución de las instrucciones dadas por ti o por mi a los agentes IA del IDE". Por eso los bloques deben ser blindados y verificables. Validar siempre el entregable antes de aceptar.

**Convención de commits establecida:** un bloque = un commit + push. Mensaje con título imperativo (`feat:`, `fix:`, `docs:`) ≤72 chars, línea en blanco, cuerpo con párrafos separados por líneas en blanco. Editor configurado: `git config --global core.editor "notepad"`.

---

## SPRINT ACTUAL — IMPORTACIÓN CSV

### Pedido original del usuario (literal)
> "necesito que las clases presupuesto, factura, clientes, albaranes, materiales, empleados y nominas puedan importar con la misma funcionalidad que le hemos puesto a tarifa"

### Reinterpretación tras inspección de código

El importador ya soportaba 4 entidades planas antes del sprint (Tarifa, Cliente, Material, Empleado). Lo que faltaba:

1. Nómina (plana). **HECHO** (`c43118a`).
2. Pedido (plano). **HECHO** (`50d5902` + fix UPDATE path en `9798225`).
3. Presupuesto (parent-child). **HECHO** (`2a4ead5` motor+spec+tests + `2513a71` cableado UI).
4. Factura (parent-child). **PENDIENTE — siguiente.**
5. Albarán (parent-child). PENDIENTE.

Vistas con Alert "Funcionalidad próximamente" todavía pendientes: `FacturasView`, `AlbaranesView`. (`NominasView`, `PedidosView` y `PresupuestosView` ya cableadas.)

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
| Validación fechas Pedido | Detectar en `ensamblarPedido` comparando `errores.size()`. Cerrada por D6 vía `CAMPOS_FECHA`. | — |

### Decisiones del Bloque 3 (D1–D6, todas tomadas y aplicadas)

| ID | Decisión | Valor | Estado |
|---|---|---|---|
| D1 | Política inconsistencia entre filas del mismo grupo | Estricta + Opción A (todos los campos de `spec.campos()` cuentan). | CERRADA en 3C-paso-2c (`2310588`). |
| D2 | Modelado parent-child en `EntityImportSpec` | Ampliar con 3 campos opcionales. | HECHO en 3C-paso-1 (`0bb82c7`). |
| D3 | `UPDATE_EXISTING` para parent-child | Bloquear con `IllegalArgumentException` al inicio de `importar()`. | HECHO en 3C-paso-2b. |
| D4 | Deuda 4 (UPDATE path Pedido) | Cerrada. | HECHO (`9798225`). |
| D5 | FKs opcionales (Albarán→Factura/Pedido, Factura→Presupuesto) | Postergar a Bloque 4/5. | PENDIENTE. Revisar al arrancar Bloque 4. |
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

### Sub-decisiones tomadas en 3C-paso-2b (no rediscutir)

| Sub-decisión | Valor | Razón |
|---|---|---|
| Punto de inserción Fase 2.5 | Método privado nuevo entre Fase 2 y Fase 3, invocado desde `importar()` solo si `spec.esParentChild()`. | Hueco natural entre L81 y L84 del archivo pre-2b. |
| Modelo de datos de grupo | Record privado `ValidGroup(String clave, List<ValidRow> filas)`. | Análogo a `ValidRow`. |
| Granularidad del savepoint | `insertarFilas` (renombrado) e `insertarGrupos`. | Estructura idéntica, coste de bifurcar bajo. |
| Switch de procesamiento | `procesarFila` (planos) y `procesarGrupo` (parent-child). | Cada switch limpio. |
| Semántica `ImportResult` para parent-child | `insertadas`/`actualizadas` cuentan **entidades**, no filas. `descartadas = filas.size() - filasOk`. `procesarGrupo` devuelve `int[]{insertadas, actualizadas, filasDelGrupo}`. | Opción B + ajuste del cálculo. |
| Atomicidad parent-child | Fallos pre-INSERT-cabecera retornan `{0,0,0}` con RowError. Fallos post-INSERT-cabecera lanzan `SQLException` para que el savepoint rollee. | — |
| Clave agrupación blank | `agruparEnFase2_5` valida `vals.get(claveAgrupacion)` no-blank. RowError individual + descarte de la fila. | — |

### Sub-decisiones tomadas en 3C-paso-2c (no rediscutir)

| Sub-decisión | Valor | Razón |
|---|---|---|
| Granularidad RowError inconsistencia | Un RowError por fila del grupo, indicando primer campo discrepante. | Si difieren 3 campos en 5 filas: 5 errores legibles vs 15 ruidosos. |
| Normalización de la comparación | `String.equals()` directo. Sin `lower()`, sin tildes. | Política estricta: "María" ≠ "Maria" debe detectarse. |
| Lugar del método auxiliar | `detectarInconsistenciaGrupo` privado nuevo, invocado dentro del bucle de `agruparEnFase2_5`. | Separación de fases. |

---

## ESTADO TÉCNICO AL CIERRE DE ESTA SESIÓN

### Git
- **Rama:** `master`
- **HEAD:** `2513a71` — `feat: cablear importación CSV en PresupuestosView (3C-paso-3b)`. Sincronizado con `origin/master`.
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
- **Working tree:** sólo `Resumen.md` modificado (este mismo archivo, en curso de actualización a v3.3 en el commit `docs:` que cierra Bloque 3). Tras ese commit quedará limpio.

### Tests
- **36/36 verdes** al cierre. Reparto:
  - `ClienteDAOTest` — 2
  - `ImportBackupServiceTest` — 12
  - `EntityImportServiceNominaTest` — 5
  - `EntityImportServicePedidoTest` — 10
  - `EntityImportServicePresupuestoTest` — 7

### Estado del archivo `EntityImportService.java` (post-3a)
- Package: `org.gipsybuho.service`.
- Constantes: `MAX_FILAS=10_000`, `MAX_LEN=255`, `MAX_LEN_LIBRE=1_000`, `CAMPOS_NUMERICOS`, `CAMPOS_FECHA` (4 elementos: `"fecha"`, `"fecha_validez"`, `"fecha_entrega_prevista"`, `"fecha_entrega_real"`), `CAMPOS_LIBRES` (3 elementos: `"notas"`, `"descripcion"`, `"condiciones"`).
- Records privados: `ValidRow(int numero, Map<String,String> vals)`, `ValidGroup(String clave, List<ValidRow> filas)`.
- Fase 2.5 `agruparEnFase2_5` con `detectarInconsistenciaGrupo` invocado por grupo.
- Fase 3 bifurcada: `insertarFilas` + `insertarGrupos`.
- Dispatchers: `procesarFila` (planos, 6 casos) y `procesarGrupo` (parent-child). `procesarGrupo` ya tiene `case "Presupuestos"` real (sin `@SuppressWarnings`).
- Bloqueos al inicio de `importar()` para parent-child: `UPDATE_EXISTING` y `CREATE_NEW`. Solo `SKIP_IF_EXISTS` permitido.
- Métodos privados de Presupuesto: `procesarPresupuesto`, `ensamblarPresupuesto`, `aplicarValoresPresupuestoCabecera`, `ensamblarLineaPresupuesto`. Aplican defaults `fecha`/`estado`/`iva_porcentaje` antes de `setLineas`+`calcularTotales`.

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
| 3C-paso-3a | Presupuesto: spec parent-child + `case "Presupuestos"` + `procesarPresupuesto`/`ensamblarPresupuesto`/`ensamblarLineaPresupuesto` + `CAMPOS_FECHA += fecha_validez` + `CAMPOS_LIBRES += condiciones` + bloqueo `CREATE_NEW` + 7 tests | ✅ HECHO. `2a4ead5`. |
| 3C-paso-3b | Cableado `PresupuestosView.importar()` (sustituir Alert por flujo idéntico a `PedidosView`) | ✅ HECHO. `2513a71`. |
| **4** | **Implementar Factura con motor parent-child (línea análoga a Presupuesto). Cablear `FacturasView`. Decidir si D5 (FK opcional `presupuesto_id`) se cierra aquí o se posterga a Bloque 5.** | **🔄 SIGUIENTE.** |
| 5 | Implementar Albarán. Decidir transacción explícita en `AlbaranDAO.save()`. Resolución FK opcionales (D5). Considerar Gemini. | PENDIENTE. |
| 6 | Actualizar `MIGRACION_HISTORICO.md`. | PENDIENTE. |

---

## PUNTO EXACTO DE CONTINUACIÓN

**Siguiente: Bloque 4 — Importación CSV de Factura (parent-child).**

Patrón a seguir: copia literal de 3C-paso-3 con tres diferencias previsibles:
- Entidad: Factura + LineaFactura.
- Clave de agrupación: probablemente `numero` (verificar leyendo `Factura.java`; podría ser `numero_factura`).
- FK opcional `presupuesto_id` (Factura puede generarse desde un Presupuesto). D5 pendiente: ¿se cierra aquí o se posterga a Bloque 5?

### Antes de redactar el bloque 4 — lectura obligada en sesión

NO redactar nada sobre Factura sin leer primero:

- `Factura.java` — cabecera + lineas + `calcularTotales()`. Comparar contra `Presupuesto.java`.
- `LineaFactura.java` — campos vs `LineaPresupuesto`.
- `FacturaDAO.java` — patrón `save` / `saveLineas` / `findById`. Verificar si reconstruye `lineas` en `findById`.
- `FacturasView.java` — método `importar()` actual (debe tener Alert "Funcionalidad próximamente"), nombre del método de recarga local, presencia o no de `mostrarResultadoImportacion`.
- Esquema BD: `presupuestos.fecha` es NOT NULL (de ahí D-FECHA-DEFAULT). Verificar restricciones equivalentes en `facturas` antes de decidir defaults.

### Decisión preliminar a tomar al arrancar Bloque 4

- **D5 (FK opcional `presupuesto_id`):** si `Factura` tiene esta FK y la importación parent-child encaja con el patrón Presupuesto sin sobreingeniería, cerrar D5 en Bloque 4. Si requiere lógica nueva no contemplada (ej. soft-link, validación cruzada con estado del Presupuesto), postergar a Bloque 5 y dejar Factura sin FK importable en el primer pase.

### Lo que tiene que pasar al arrancar la nueva sesión

1. **Verificar estado:** `git log --oneline -5`, `git status`, `.\mvnw.cmd test`. Si HEAD es `2513a71` (o el commit `docs:` v3.3 inmediatamente encima), working tree limpio, 36/36 verdes → estado intacto.
2. **Pedir lectura** de los 4 archivos de Factura listados arriba antes de redactar nada.
3. **Decidir D5** con el código a la vista.
4. **Redactar Bloque 4 partido en sub-pasos** análogos a 3C-paso-3:
   - 4a: motor (case nuevo en dispatcher) + spec en `Factura.java` + tests `EntityImportServiceFacturaTest`.
   - 4b: cableado `FacturasView.importar()`.
5. **Si lectura revela divergencia importante** respecto al patrón Presupuesto (ej. `calcularTotales()` con firma distinta, `LineaFactura` con campos extra como impuestos por línea, etc.), reportar antes de redactar y ajustar plan.

### Avisos heredados de 3C-paso-3 que aplicarán a Factura

- Si la BD impone NOT NULL en `facturas.fecha` (probable), aplicar D-FECHA-DEFAULT por simetría con Presupuesto. **Declararla explícitamente en el bloque blindado original** esta vez, no como cambio emergente de Codex.
- `cliente_nif` debe estar en `spec.campos()` de cabecera como `obligatorio=false` para que `detectarInconsistenciaGrupo` lo detecte si difiere entre filas del mismo grupo.
- `EntityImportServicePresupuestoTest` sirve como plantilla literal. 7 tests es el mínimo razonable.
- Mensaje de commit del 4a: `feat: importar Facturas parent-child (motor + spec + tests)`. Del 4b: `feat: cablear importación CSV en FacturasView (4b)`.

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
| 8 | `mostrarResultadoImportacion` duplicado en N vistas | ABIERTA y reforzada en 3C-paso-3b (ahora 6+ vistas con `PresupuestosView`). Refactor UI fuera del sprint. Revisar tras Bloque 5. |
| 9 | `AlbaranDAO.save()` sin transacción explícita BEGIN/COMMIT | ABIERTA. Mitigada por transacción del importador. Decidir en Bloque 5. Aplica igual a `PresupuestoDAO.save()` y probablemente `FacturaDAO.save()`. |
| 10 | `saveLineas` hace DELETE+INSERT total → riesgo destructivo con `UPDATE_EXISTING` en parent-child | CERRADA en 3C-paso-2b (bloqueo). |
| 11 | `fecha_alta` de Empleado sin validación ISO | ABIERTA, riesgo bajo. |
| 12 | `@SuppressWarnings("unused")` en `procesarGrupo` mientras no haya casos | CERRADA en 3C-paso-3a (`2a4ead5`). |
| 13 | Detección de inconsistencia parent-child sin test ejecutable | CERRADA en 3C-paso-3a (`2a4ead5`, test `descartaGrupoConCabeceraInconsistente`). |
| 14 | `CREATE_NEW` para parent-child sin semántica clara | CERRADA en 3C-paso-3a (D-CN: bloqueo análogo a UPDATE_EXISTING). |
| 15 | Tests de Presupuesto no cubren campos cabecera opcionales (fecha válida explícita aplicada vs default, `iva_porcentaje` explícito, `fecha_validez` ISO mal formada, `condiciones` largas dentro de `MAX_LEN_LIBRE`) | ABIERTA, riesgo bajo. Cobertura mínima viable verde; deuda menor para sprints futuros. Aplicará también a Factura y Albarán por simetría. |

---

## ERRORES COMETIDOS EN ESTE SPRINT (para no repetirlos)

1. **Asumí el tipo de `Pedido.fecha` sin verificarlo** (2B). Era `LocalDate`, no `String`. Lección: antes de juzgar, leer el modelo.

2. **Fui demasiado cómplice al inicio con decisiones del usuario** (2B). Aprobé "fallback nif→nombre" y "filtro activo=1" sin discutir. Lección: si veo un problema, decirlo en el momento.

3. **Redacté el Bloque 2B juntando dos cambios.** Defendible pero discutible. Lección: cuando hay duda, partir bloques. **Aplicada en 3C-paso-3** (3a + 3b separados).

4. **Confundí `UPDATE_IF_EXISTS` con `UPDATE_EXISTING`** (2C). Lección: declarar nombres literales solo cuando se han verificado leyendo el archivo.

5. **Perdí genéricos al reescribir `EntityImportSpec`** (3C-paso-1). `List<FieldSpec>` quedó como `List`. Lección: cuando dicto "reemplazo completo de archivo", comparar carácter a carácter con el original.

6. **Dicté switch expression sin brazos con valor** (3C-paso-2b). Lección: antes de dictar Java nuevo, simular `javac` mentalmente.

7. **Entré en pánico falso por `git status` "limpio"** (3C-paso-1). Lección: `git log --oneline` antes de declarar emergencia.

8. **El usuario pegó por error respuesta idéntica del turno anterior** (3C-paso-2b fix). Lección: si dos salidas son idénticas, pedir verificación antes de actuar.

9. **Casi redacto 3C-paso-2c sin pedir el archivo objetivo.** Lección: pedir el archivo antes de redactar bloques que editen métodos concretos.

10. **Subida ambigua en sesión 3C-paso-3** (usuario subió `PresupuestosView.java` sin mensaje). Lección: ante subida sin texto, preguntar antes de actuar; no asumir reproceso.

11. **Dos basuras en mi propia redacción del bloque 3a** detectadas al releer: ternario inútil (`apellidos.isBlank() ? nombre : nombre`) y línea sentinela rara en el test. Lección: releer el bloque completo antes de darlo por bueno. Releer es barato y caza errores que `javac` no caza (los míos compilaban, pero eran código basura).

12. **Codex añadió un default no declarado en 3a** (`fecha = LocalDate.now().toString()` cuando el CSV no la trae) para que sus tests pasaran. La decisión era defendible (análoga a D-IVA y D-EST) pero no estaba autorizada. Lección: si Codex declara un cambio funcional no pedido, parar y pedir el diff antes de aprobar el commit, aunque los tests estén verdes. Tras revisar diff y tests se aceptó como D-FECHA-DEFAULT.

13. **`Resumen.md` arrastró sin commit entre sesiones** (detectado tras 3b). El archivo se actualizó a v3.2 al cierre de la sesión anterior pero nunca se commiteó. Apareció en `git status` durante toda la sesión como "modified" y casi se cuela acumulado en commits de código. Al revertir un fallo de `str_replace` posterior se perdió ese trabajo intermedio. Lección: verificar `git diff --stat` al arrancar y declarar scope del archivo no-commiteado en el primer mensaje técnico. PowerShell con encoding CP-850 muestra UTF-8 como salsa rara (`Versi├│n`, `ÔÇö`) y oculta cambios reales; usar `Format-Hex` cuando el diff se vea sospechoso.

14. **Intenté actualizar el handoff con 12 `str_replace` quirúrgicos** sobre un archivo que estaba a medio actualizar (v3.2 acumulada sin commit). Una de las cadenas no coincidió, Codex revirtió (correctamente) y volvió a v3.1, perdiendo el trabajo intermedio. Lección: para edits extensos a un archivo de documentación (handoff, READMEs largos), preferir reescritura completa sobre N reemplazos puntuales. La reescritura tiene diff más grande pero riesgo cero de fallo por mismatch de cadena.

---

## ARCHIVOS YA INSPECCIONADOS — NO PEDIRLOS DE NUEVO

Estos archivos están analizados y leídos. NO pedirlos de nuevo al arrancar Bloque 4 salvo cambio explícito:

- `CLAUDE.md` — reglas operativas Multi-IA.
- `MIGRACION_HISTORICO.md` — procedimiento Vía A.
- `DuplicatePolicy.java` — enum: `SKIP_IF_EXISTS`, `UPDATE_EXISTING`, `CREATE_NEW`.
- `Tarifa.java` — patrón `IMPORT_SPEC` para entidades planas, constructor 4-arg.
- `Pedido.java` — modelo plano con `LocalDate` en 3 fechas. `IMPORT_SPEC` 4-arg.
- `Presupuesto.java` — cabecera (numero, clienteId, fecha String, fechaValidez String, estado, ivaPorcentaje, baseImponible, ivaImporte, total, notas, condiciones, createdAt) + `List<LineaPresupuesto> lineas` + `calcularTotales()`. `IMPORT_SPEC` parent-child de 7 argumentos vía `buildSpec()` privado tras 3a.
- `LineaPresupuesto.java` — `(descripcion, tecnica, cantidad, precioUnit, descuento, total, orden)`. Constructor `(String, String, int, double, double)` que llama `calcularTotal()`. Setters disponibles.
- `PresupuestoDAO.java` — `save(p)` hace `insert(p)` o `update(p)` según `id==0`, luego `saveLineas(p)` (DELETE+INSERT total). `findById(id)` rellena lineas. Sin transacción explícita. Lanza `SQLException`.
- `PresupuestosView.java` — `importar()` ya cableado en 3b al patrón estándar. Botón `📥 Importar` en línea 83 con `this::importar`. Método de recarga local: `cargar()` (no `cargarPresupuestos()`). Helper `mostrarResultadoImportacion(ImportResult)` añadido en 3b (copia literal del patrón Pedido).
- `EntityImportSpec.java` — record con 7 componentes. Constructor secundario 4-arg para retrocompat. `esParentChild()`.
- `FieldSpec.java` — record `(String clave, String etiqueta, boolean obligatorio)`.
- `EntityImportService.java` — estado al cierre 3C-paso-3a (ver sección "Estado del archivo" arriba). Dispatcher `procesarGrupo` con `case "Presupuestos"` real; Bloque 4 añadirá `case "Facturas"`.
- `EntityImportServicePedidoTest.java` — plantilla literal del estilo de test plano. 10 tests con `@TempDir`, helpers `fila`/`mapping`/`crearCliente`.
- `EntityImportServicePresupuestoTest.java` — 7 tests con helpers `filaLinea`/`mapping`/`crearCliente`. Plantilla parent-child para Factura.
- `TarifasView.java`, `NominasView.java`, `PedidosView.java`, `PresupuestosView.java` — patrón de cableado del botón Importar. `PedidosView.importar()` líneas 606-653 + `mostrarResultadoImportacion` líneas 655-673 es la plantilla canónica.

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

1. **Confirmar contexto cargado:** HEAD `2513a71` (3C-paso-3b cerrado) o el commit `docs:` v3.3 inmediatamente encima, 36/36 verdes, Bloque 3 cerrado completo. Decisiones D1–D6 + D-CN/D-IVA/D-EST/D-FV/D-CON/D-DC/D-TOT/D-SCO/D-FECHA-DEFAULT aplicadas en código. Siguiente: Bloque 4 (Factura parent-child).

2. **Pedir verificación de estado:**
   - `git log --oneline -5` — confirmar HEAD en un commit `docs:` encima de `2513a71` o en el propio `2513a71`.
   - `git status` — confirmar working tree limpio.
   - `.\mvnw.cmd test` — confirmar 36/36 verdes con el reparto 2 + 12 + 5 + 10 + 7.

3. **Antes de redactar el bloque 4a, pedir al usuario** que pegue (o adjunte) los 4 archivos clave de Factura:
   - `src/main/java/org/gipsybuho/model/Factura.java`
   - `src/main/java/org/gipsybuho/model/LineaFactura.java`
   - `src/main/java/org/gipsybuho/dao/FacturaDAO.java`
   - `src/main/java/org/gipsybuho/ui/FacturasView.java`
   No redactar bloque 4a antes de tenerlos leídos en sesión. Lección 3C-paso-2c y 3a aplicada.

4. **Con los archivos a la vista, decidir D5** (FK opcional `presupuesto_id` en Factura: cerrar en Bloque 4 o postergar a 5). Razonamiento explícito en la respuesta.

5. **Redactar Bloque 4 partido en 4a (motor + spec + tests) y 4b (cableado UI)**, análogo al patrón 3C-paso-3. Bloques blindados para Codex. Releer antes de pegar.

6. Si la verificación de estado revela divergencia, diagnosticar antes de avanzar (mirar `git log --oneline -10` y comparar con commits listados en sección "Git" arriba).

FIN DEL HANDOFF.
