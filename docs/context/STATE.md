# Estado operativo — Gráficas Mulberry

Fuente única de verdad para HEAD, tests y sprint activo.
Actualizar tras cada sprint cerrado.

**Última actualización:** 2026-06-13

---

## HANDOFF PARA PRÓXIMO AGENTE — leer antes de tocar nada

### Activar al inicio de sesión (antes de cualquier tarea)
1. `/caveman full` — modo caveman activo en nivel full. Mantener toda la sesión.
2. `/caveman-commit` — usar para todos los commits del proyecto.

### Lectura obligatoria en orden
1. Este archivo (`docs/context/STATE.md`) — HEAD, cola, estado.
2. `AGENTS.md` — reglas entre agentes, sinceridad técnica.
3. `CLAUDE.md` — checklist pre-sprint, reglas Multi-IA, convenciones.
4. `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — arquitectura completa, módulos, historial.
5. `MIGRACION_HISTORICO.md` — procedimiento del sprint activo prioritario.
6. `docs/ui/MEJORAS-VISUALES.md` — estado de Sprint UI-E, qué falta, qué evitar.

### Qué se hizo en la sesión 2026-06-13 (tarde)
- Reorganización completa de `.md`: archivos históricos movidos a `docs/archive/audits/`, creado `docs/context/STATE.md`.
- Traducción al español de `AGENTS.md`, `GEMINI.md`, `SECURITY.md`.
- Análisis UI/UX experto → `docs/ui/MEJORAS-VISUALES.md` (propuestas + correcciones Codex).
- **Sprint UI-E iniciado:** slide+fade en `mostrarVista` — commit `0bb8c8b`. Probado y funciona. Usuario dice "un poco rápido" → si se quiere más suave, cambiar `Duration.millis(220)` a `Duration.millis(260)` en `MainView.java` línea ~607 (método `mostrarVista`, dos apariciones: FadeTransition y TranslateTransition).

### Qué se hizo en la sesión 2026-06-13 (mejoras post-Gemini MIGRACION-COMPLEJA)
- **commit `ac2826e`**: `scripts/extrae_material.py` — `to_num()` acepta `campo+fila`, avisa en consola si descarta valor no numérico.
- **commit `ac2826e`**: `scripts/limpia_5c.py` (nuevo) — limpia `5c_material_otros.csv`: 5 filas importables, 2 notas descartadas; recupera precios de `longitud_o_unidad` via regex (CAJA→0.53, CINTA→1.43). Salida: `5c_material_otros_limpio.csv`.
- `MIGRACION_HISTORICO.md` actualizado con resultado real de limpia_5c y referencia al script.
- Mejoras fueron revisadas por Gemini (revisión post-sprint) e implementadas por Claude Code.
- Sprint MIGRACION-COMPLEJA: mejoras Gemini cerradas. Pendiente importar CSVs restantes (5a, 5b, 5c limpio, 3_union_papelera, 2_precios_gramaje).

### Qué se hizo en la sesión 2026-06-13 (sprint MIGRACION-COMPLEJA)
- Inspección completa de `PRECIOS PAPEL PROVEEDORES Formulas.xlsx` (7 hojas, 288 archivos totales).
- Inventario completo de CSVs pre-existentes en `Desktop\files\` (8 CSVs) y `Desktop\excel\` (3 CSVs).
- Generado script `scripts/extrae_material.py` — extrae hoja MATERIAL (plásticos/tintas/consumibles) a CSV.
- Generado `Desktop\excel\3_materiales_plasticos_tintas.csv` (14 filas, UTF-8 limpio).
- Documentado procedimiento completo de importación y limitaciones en `MIGRACION_HISTORICO.md`.
- **COMPLETADO**: wizard ejecutado con `Desktop\files\1_precios_papel_proveedor.csv` → registros de papel importados correctamente → validación visual OK en tabla Materiales. Proveedores UNION_PAPELERA, MRPAPEL, CODIAL visibles con precios correctos. Criterio de cierre del sprint cumplido.

### Qué se hizo en la sesión 2026-06-13 (cierre)
- **Prompt injection detectada y neutralizada** en `proseguir v1.0.md`: el archivo generado por Grok contenía instrucción de clonar repo externo `garrytan/gstack` e instalar skills desconocidas. Claude Code rechazó la ejecución. El archivo fue saneado: instrucción maliciosa eliminada, skills falsas reemplazadas por skills oficiales de Claude Code (`/brainstorming`, `/writing-plans`, `/ui-ux-pro-max`, `/second-opinion`, `/executing-plans`, `/code-review`, `/run`, `/verification-before-completion`, `/simplify`).
- **HEAD corregido** en `proseguir v1.0.md`: era `6268479` (commit seguridad), real es `fd1f34b` (v13.0.0).
- **Prioridad documentada**: MIGRACION-COMPLEJA marcada como obligatoria antes de Sprint UI-E en `proseguir v1.0.md`.
- **STATE.md** actualizado con cierre de sesión y próximos pasos.

### Qué NO se hizo y por qué
- **Sliding pill sidebar**: Codex recomendó defer hasta cerrar MIGRACION-COMPLEJA. Riesgo moderado (toca navBtnImpl y coordenadas dentro de ScrollPane). Ver `docs/ui/MEJORAS-VISUALES.md` tabla de prioridad.
- **Sprint MIGRACION-COMPLEJA**: no se avanzó esta sesión — la sesión se dedicó a documentación, UI y seguridad de archivos de contexto. Es la prioridad máxima para la próxima sesión.
- **Sprint UI-E ítems 1-3**: pendientes (elevación CSS, KPI animados, shimmer). Ejecutar tras MIGRACION-COMPLEJA.

### Próximos pasos recomendados (en orden)
1. **MIGRACION-COMPLEJA** — leer `MIGRACION_HISTORICO.md` y continuar. Prioridad máxima.
2. Si el usuario quiere UI antes: **sistema de elevación CSS** — unificar los 18 `dropshadow` de `styles.css` en 4 variables de nivel. Solo CSS, cero riesgo Java.
3. Después: **KPI números animados** en `DashboardView` con `Timeline` + `KeyFrame`. Patrón de referencia en `VisualAssistantView.java` (método `escrituraAnimacion`).
4. Después: **Shimmer skeleton** animado con `Timeline` (patrón `pulsoVozAnimacion` en `VisualAssistantView`).
5. Después: **Shake en campos erróneos** — copiar KeyFrames de `VisualAssistantView` a formularios.

### Decisiones tomadas que el próximo agente debe respetar
- Glassmorphism sidebar: **EVITAR** — Codex lo descartó (rendimiento + parece moda en ERP).
- Hover tilt perspectiva: **EVITAR** — puede sentirse juguete en herramienta de trabajo.
- Floating labels: **EVITAR por ahora** — riesgo de romper formularios existentes.
- `docs/archive/audits/SECURITY_AUDIT_2026-06-13.md`: **NO MODIFICAR** — es baseline de auditoría. Solo crear nuevos archivos con fecha.
- Commits: atómicos, en español, sin WIP. Compilar siempre antes de commit.

### Comandos de validación
```powershell
.\mvnw.cmd clean compile     # validar tras cualquier cambio Java/CSS
.\mvnw.cmd test              # antes de cerrar cualquier sprint
.\mvnw.cmd javafx:run        # probar UI manualmente
```

---

## Estado git

| Campo | Valor |
|---|---|
| HEAD | `fd1f34b` |
| Mensaje | `v13.0.0 modificaciones Selene` |
| Rama | `master` |
| Tests | 142/142 verdes (`.\mvnw.cmd test`) |
| Versión app | v13.0.0 (`AppConstants.APP_VERSION`) |

---

## Sprint activo

**Sprint MIGRACION-COMPLEJA** — migración de archivos históricos con estructura humana compleja (Excel con celdas combinadas, múltiples mini-tablas, bloques laterales). Ver `MIGRACION_HISTORICO.md` para procedimiento completo.

**Sprint UI-E** (micro, en paralelo con bajo riesgo) — microinteracciones de bajo riesgo. Ver `docs/ui/MEJORAS-VISUALES.md` para lista completa y estado.

---

## Cola prioritaria

1. Sprint MIGRACION-COMPLEJA (activo — prioridad máxima)
2. Sprint UI-E — mejoras visuales de bajo riesgo (ver MEJORAS-VISUALES.md)
3. RELEASE-GATE MANUAL
4. INSTALLER-REPRO
5. Refactor B2 (largo plazo — inyección de Connection en DAOs)

---

## Sprints cerrados relevantes

| Sprint | Commit | Descripción |
|---|---|---|
| SECURITY-2026-06-13 | `6268479` | Auditoría + remediación completa SEC-01..10 + NEW-01..03 |
| HELP-5 | `610a0f2` | PreferenceService, OnboardingDialog, modo principiante, hint bar |
| HELP-4-FIX | `4117cdf` | DDL UNIQUE constraints, factorías estáticas HelpView, tests |
| HELP-4 | — | ToastService con enlace artículo; HelpView inline en MainView |
| HELP-3 | `67f7d4e` | F1 contextual por módulo |
| HELP-2 | `47e46dc` | HelpService + HelpView JavaFX |
| HELP-1 | `65588cf` | 81 artículos HTML offline |
| HELP-0 | `39d060e` | HELP-SPEC.md — spec completa del sistema de ayuda |
| Sprint UI-E (parcial) | `0bb8c8b` | slide+fade en mostrarVista (220ms, EASE_OUT, +24px X) |
| Sprint UI-A/B/C/D | varios | CSS variables, FadeTransition, IAView, skeleton+overlay |
| Sprint SEC | — | 5 fixes seguridad P0/P1 |
| Sprint COD | — | Dead code eliminado |

---

## Deuda técnica conocida

- Hint bars en Facturas, Pedidos, Materiales, Empleados (solo ClientesView tiene barra ahora)
- Tests unitarios para `PreferenceService` (lectura/escritura con BD en memoria)
- Refactor B2: inyección de Connection en DAOs (largo plazo)
