# Estado operativo — Gráficas Mulberry

Fuente única de verdad para HEAD, tests y sprint activo.
Actualizar tras cada sprint cerrado.

**Última actualización:** 2026-06-14 (sesión RELEASE-GATE P0 completo)

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

### Qué se hizo en la sesión 2026-06-14 (RELEASE-GATE MANUAL — P0 completo)

Sprint RELEASE-GATE iniciado. Matriz de pruebas generada por Gemini (40 casos P0/P1/P2).

**Correcciones a matriz Gemini:**
- "Recuperar contraseña → email" → INCORRECTO. App usa pregunta de seguridad. Adaptado.
- "Logout → LoginView" → No existe logout in-app. "Salir" cierra app. Adaptado.
- "Primer arranque / AdminSetup" → SKIP: BD ya tiene admin. Requiere entorno limpio.
- "Estabilidad 1h+" → SKIP: no ejecutable en sprint.
- "Rol EMPLEADO" → INCORRECTO. No existe en el sistema. Reemplazado por PRODUCCION (`UserRole.java` tiene: ADMINISTRADOR, COMERCIAL, PRODUCCION, CONTABILIDAD).

**Resultados P0 — COMPLETO:**

| Caso | Estado | Notas |
|---|---|---|
| P0-01 AdminSetup | ⏭ SKIP | BD ya tiene admin configurado — requiere entorno limpio |
| P0-02 Login correcto | ✅ PASS | Dashboard visible |
| P0-03 Login incorrecto | ✅ PASS | Mensaje error + shake en campo contraseña |
| P0-04 Lockout 5 intentos | ✅ PASS | "Demasiados intentos. Espera 5 minutos." |
| P0-05 Recuperar contraseña | ✅ PASS | Pregunta seguridad → nueva contraseña → mensaje verde en LoginView |
| P0-06 Salir | ✅ PASS | App cierra limpiamente |
| P0-07 Rol COMERCIAL | ✅ PASS | Sidebar correcto: Tarifas/Materiales/Estadísticas/PERSONAL ocultos |
| P0-08 Rol PRODUCCION | ✅ PASS | Sidebar correcto: Clientes/Presupuestos/Facturas/Albaranes/PERSONAL ocultos |
| P0-09 Clientes CRUD | ✅ PASS | Crear/editar/buscar/eliminar funciona |
| P0-10 Presupuestos CRUD | ✅ PASS | CRUD funciona; búsqueda/filtro ausente (mejora UX, no bloqueante) |
| P0-11 Flujo negocio | ✅ PASS parcial | Presupuesto→Albarán y Presupuesto→Factura funcionan; ver GAP-1 y GAP-2 |
| P0-12 Factura pago | ✅ PASS | Marcar pagada funciona y persiste |

**P0 resultado: 10/11 PASS (1 SKIP). Sin bloqueantes.**

**Gaps documentados (backlog, no bloqueantes para release):**
- **GAP-1**: No existe "Crear Pedido desde Presupuesto" — workaround: ir a Pedidos manualmente.
- **GAP-2**: No existe "Crear Factura desde Albarán" — no hay workaround directo.
- **GAP-3 UX**: No existe logout in-app — cerrar y reabrir app para cambiar de usuario. Añadir a backlog P1.
- **GAP-4 UX**: Lockout de login no se cancela tras reset de contraseña por pregunta de seguridad. Comportamiento seguro pero puede sorprender. P2 backlog.

**P1 y P2: pendientes (0/20 y 0/6 ejecutados)**

**Multi-IA:** Gemini (matriz 40 casos). Claude Code (ejecución P0). Codex: pendiente — invocar si aparecen bugs P0/P1 que requieran fix de código.

---

### Qué se hizo en la sesión 2026-06-14 (Sprint UI-E ítems 5 y 7)

- **UI-E ítem 5** (Shake en campos erróneos):
  - Método estático `TableColumnSizing.shake(Node)` — 6 KeyFrames, 350ms, translateX -6/6/-4/4/0
  - Aplicado en `LoginView.handleLogin()` (2 casos), `LoginView.showRecoveryDialog()` (4 casos), `AdminSetupView.handleCreate()` (5 casos), `UserManagementView.createUser()` (5 casos) y `changeSelectedPassword()` dialog (3 casos)
  - Shake apunta al campo concreto que falla, no al mensaje de error
  - Commit `38911b3` — 142/142 tests verdes. VibeSec: 0 vulnerabilidades.

- **UI-E ítem 7** (Pattern de fondo dashboard):
  - Canvas con grid de puntos 18px step, r=1.2px, opacity=0.05
  - `managed=false`, `mouseTransparent=true` — no interfiere con layout ni interacción
  - Se redibuja automáticamente al cambiar tamaño del panel
  - Commit `051e778`

- **Multi-IA:** Claude Code solo. Sin Gemini/Codex — cambios quirúrgicos sin incertidumbre arquitectónica. Sin auth/seguridad/BD. Validación local (compilación + tests) suficiente.
- **Verificación visual (2026-06-14, sesión cierre):**
  - Patrón de puntos confirmado: crop lossless del Dashboard muestra textura regular en grid (18px step, 5% opacidad).
  - Shake confirmado: clic en "Crear usuario" con campos vacíos → mensaje rojo "Todos los campos son obligatorios." visible → `createUser()` ejecutó hasta `TableColumnSizing.shake(usernameField)`. Animación 350ms ya terminada al capturar; comportamiento esperado. Fix Codex verificado (binding `tf.translateXProperty()`).
  - App cerrada limpiamente tras verificación.
- **Fix Codex (commit `6abf2a2`):** TextField visible en modo "mostrar contraseña" no sacudía — binding `tf.translateXProperty().bind(pf.translateXProperty())` añadido a los 3 `wrapPasswordField()`. Detectado en revisión post-implementación. 142/142 tests verdes.
- **Multi-IA:** Claude Code (implementación) + Codex (revisión + fix). `/security-review` + `/VibeSec` ejecutados. 0 vulnerabilidades.

### Qué se hizo en la sesión 2026-06-14 (Sprint UI-E ítems 1-3)

- **UI-E ítem 1** (CSS elevación) — CERRADO en sesión anterior, commit `a5b6116`
- **UI-E ítem 2** (KPI animados) — CERRADO en sesión anterior, commits `96ef919` + `aa10dc4`
- **UI-E ítem 3** (animación escalonada de filas):
  - Opción A elegida: FadeTransition + TranslateTransition, 10 filas máx, `Platform.runLater`
  - Método `animarFilas(TableView<?>)` añadido a `TableColumnSizing.java`
  - Hook añadido en `ClientesView.cargar()`, `FacturasView.cargar()`, `PedidosView.cargarPedidos()`
  - Commit `7d2ee82` — implementación inicial
  - Fix posterior: doble `Platform.runLater()` (2 ciclos VirtualFlow), sort por Y real (top→bottom), delay 45ms, Y 22px, duración 260ms
  - Commit `3bd6e1e` — fix animación
  - VibeSec: 0 vulnerabilidades. BUILD SUCCESS.
  - Validación visual: usuario confirmó efecto visible (antes vacío, ahora con datos de test)
  - CSVs de prueba generados en Escritorio: `test_clientes.csv`, `test_pedidos.csv`, `test_facturas.csv` (15 filas c/u, importar en ese orden)
  - Nota: la sesión cerró antes de confirmación final del fix visual — pendiente verificar al inicio de próxima sesión

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
1. **RELEASE-GATE MANUAL** (en curso) — reanudar en **P1** (20 casos).
   - P0 completo: 10/11 PASS, 1 SKIP. Sin bloqueantes.
   - Gaps documentados: GAP-1 (Pedido desde Presupuesto), GAP-2 (Factura desde Albarán), GAP-3 (logout in-app), GAP-4 (lockout no se cancela tras reset).
   - Continuar: P1 (20 casos), luego P2 (6 casos).
2. **Sprint UI-E ítem 6** — Sliding pill sidebar: defer hasta cerrar RELEASE-GATE.
3. **INSTALLER-REPRO** — pipeline: mvn → jpackage → gen_graphics.py → makensis.
4. **MIGRACION pendiente** — importar CSVs restantes: 5c limpio, 5a tintas, 5b plástico, 3_union_papelera, 2_precios_gramaje (acción manual del usuario).

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
| HEAD | `3fe8da5` |
| Mensaje | `docs(state): cerrar P0 RELEASE-GATE — 10/11 PASS, sin bloqueantes` |
| Rama | `master` |
| Tests | 142/142 verdes (`.\mvnw.cmd test`) |
| Versión app | v13.5.0 (`AppConstants.APP_VERSION`) |

---

## Sprint activo

**Sprint RELEASE-GATE MANUAL** — EN CURSO. P0: 10/11 PASS (1 SKIP). P1: 0/20 pendiente. P2: 0/6 pendiente. Sin bloqueantes P0. Ver sección "Qué se hizo en sesión 2026-06-14 (RELEASE-GATE)".

**Sprint UI-E** — ítems 1/2/3/4/5/7 cerrados. Pendiente: ítem 6 (sliding pill, defer hasta cerrar RELEASE-GATE). Ver `docs/ui/MEJORAS-VISUALES.md`.

**Sprint MIGRACION-COMPLEJA** — CSVs restantes aún pendientes de importación manual por el usuario. Ver `MIGRACION_HISTORICO.md`.

---

## Cola prioritaria

1. **RELEASE-GATE MANUAL** (en curso — reanudar **P1**, 20 casos)
2. **Backlog features detectadas en RELEASE-GATE** (implementar tras cerrar P1/P2):
   - GAP-3: Cerrar sesión sin salir de la app (logout in-app) — P1
   - GAP-1: Crear Pedido desde Presupuesto — P1
   - GAP-2: Crear Factura desde Albarán — P1
   - GAP-4: Cancelar lockout tras reset de contraseña — P2
3. Sprint UI-E ítem 6 — sliding pill (defer hasta cerrar RELEASE-GATE)
4. INSTALLER-REPRO
5. Sprint MIGRACION-COMPLEJA — CSVs pendientes (acción manual usuario)
6. Refactor B2 (largo plazo — inyección de Connection en DAOs)

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
| Sprint UI-E ítems 1-3 | `3bd6e1e` | CSS elevación + KPI animados + filas escalonadas (Clientes/Facturas/Pedidos) |
| Sprint UI-E (slide+fade) | `0bb8c8b` | slide+fade en mostrarVista (220ms, EASE_OUT, +24px X) |
| Sprint UI-A/B/C/D | varios | CSS variables, FadeTransition, IAView, skeleton+overlay |
| Sprint SEC | — | 5 fixes seguridad P0/P1 |
| Sprint COD | — | Dead code eliminado |

---

## Deuda técnica conocida

- Hint bars en Facturas, Pedidos, Materiales, Empleados (solo ClientesView tiene barra ahora)
- Tests unitarios para `PreferenceService` (lectura/escritura con BD en memoria)
- Refactor B2: inyección de Connection en DAOs (largo plazo)
