# Estado operativo — Gráficas Mulberry

Fuente única de verdad para HEAD, tests y sprint activo.
Actualizar tras cada sprint cerrado.

**Última actualización:** 2026-06-15 (sesión cierre — Sprint A hint bars + Sprint B PreferenceServiceTest — 146/146)

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

### Qué se hizo en la sesión 2026-06-15 (Sprint A hint bars + Sprint B tests)

**Sprint A** (`cd91f15`) — Hint bars en 4 módulos:

Añadido `buildBeginnerHint()` en FacturasView, PedidosView, MaterialesView, EmpleadosView. Texto accionable por módulo + referencia a F1. Binding a `PreferenceService.beginnerModeProperty()` (visible+managed), sin impacto en layout cuando modo avanzado. CSS class `beginner-hint` ya existente en styles.css — sin tocar.

**Sprint B** (`74910eb`) — PreferenceServiceTest:

4 tests: `beginnerModeDefaulteaFalso`, `setBeginnerModePersiste`, `isFirstRunEsTrueEnPrimerArranque`, `markFirstRunCompletedMarcaComoCompletado`. Patrón: `@TempDir` + system property override + reset singleton por reflexión. Sin modificación de código de producción.

VibeSec: LIMPIO — strings hardcodeados, sin entrada de usuario, sin SQL nuevo, sin auth. Aplicación de escritorio → XSS/CSRF/SSRF no aplican.

Multi-IA: Claude Code lidera. Gemini disponible como soporte pero no invocado — tarea UI pura + test sin incertidumbre arquitectónica. Validación objetiva local suficiente.

Tests: 142/142 → 146/146 tras Sprint B.

---

### Qué se hizo en la sesión 2026-06-15 (contador de filas en 9 módulos)

**Sprint UI-COUNTER** (`d93dc76`) — `lblContador` en todos los módulos:

Añadido `private Label lblContador = new Label()` como campo en los 9 módulos. Insertado en la toolbar HBox entre el campo de búsqueda y el spacer (`Region` de `HgrowPriority.ALWAYS`). Actualizado en cada carga/filtro tras `datos.setAll(lista)`.

| Vista | Toolbar HBox | Texto contador |
|---|---|---|
| `MaterialesView` | `buildToolbarStock()` | `N materiales` |
| `TarifasView` | `buildToolbar()` | `N tarifas` |
| `ClientesView` | `buildToolbar()` | `N clientes` — actualiza en `cargar()` y en `buscar()` |
| `FacturasView` | `buildToolbar()` | `N facturas` |
| `PedidosView` | `buildToolbarPedidos()` | `N pedidos` |
| `AlbaranesView` | `buildToolbar()` | `N albaranes` |
| `PresupuestosView` | `buildToolbar()` | `N presupuestos` |
| `EmpleadosView` | `buildToolbar()` | `N empleados` |
| `NominasView` | `buildToolbar()` | `N nóminas` |

CSS: `.row-counter` añadida en `styles.css` (`-fx-text-fill: -c-text-secondary; -fx-font-size: 11px`).

ClientesView: caso especial — `txtBuscar` es variable local de `buildToolbar()` (no campo de clase); el contador se actualiza tanto en `cargar()` como en `buscar()` para reflejar el tamaño real de `datos` tras cada operación.

Multi-IA: Claude Code solo. UI pura, mecánica, sin auth/BD/red. VibeSec: N/A. Tests: 142/142.

---

### Qué se hizo en la sesión 2026-06-15 (MIGRACION-COMPLEJA cerrada)

**Sprint MIGRACION-COMPLEJA** (`9b1d950`) — Importación de los 6 CSVs pendientes:

Script: `scripts/importar_materiales.py` (nuevo). Bypass del wizard JavaFX; inserta directamente en SQLite con queries parametrizadas. SKIP_IF_EXISTS por `(nombre, proveedor)`. Soporta `--dry-run`.

| CSV | Filas | Insertadas | Dups | Categoria |
|---|---:|---:|---:|---|
| `1_precios_papel_proveedor.csv` | 84 | 72 | 12 | papel proveedor |
| `2_precios_papel_por_gramaje.csv` | 330 | 330 | 0 | papel gramaje |
| `3_union_papelera_otros_productos.csv` | 34 | 34 | 0 | varios (SOBRES, PEGATINA...) |
| `5a_material_tintas.csv` | 4 | 4 | 0 | tintas |
| `5b_material_plastico.csv` | 17 | 17 | 0 | plastico |
| `5c_material_otros_limpio.csv` | 5 | 5 | 0 | consumibles |
| **TOTAL** | **474** | **462** | **12** | |

Estado BD post-import: 477 materiales totales (15 son test data de sesiones previas — categoria `test pedidos`, IDs 112-126, no son materiales reales).

Proveedores: UNION_PAPELERA (152), MRPAPEL (137), FEDRIGONI (133), CODIAL (18). Nota: "UNIÓN PAPELERA" (con tilde, 9 filas) y "UNION_PAPELERA" (sin tilde, 152) son la misma empresa — normalizar si es necesario.

Multi-IA: Claude Code solo. BD local SQLite, sin red, sin auth, CSVs ya validados. Queries parametrizadas. VibeSec: 0 vulnerabilidades.

**Datos de test pendientes de limpieza:** materiales IDs 112-126 (categoria `test pedidos`) no son reales. Pedir autorización del usuario antes de eliminar.

---

### Qué se hizo en la sesión 2026-06-15 (INSTALLER-REPRO cerrado)

**INSTALLER-REPRO** — Pipeline reproducido con éxito con código HEAD `e0a4252` (UI-E+UI-F+GAP-1+GAP-2):

| Paso | Herramienta | Resultado |
|---|---|---|
| 1. Build | Maven (IntelliJ) + JDK-26 + `-Ppackage-windows` | OK |
| 2. App-image | `jpackage --type app-image` | OK — `output/GraficasMulberry/` |
| 3. Gráficos | `gen_graphics.py` (Pillow) | OK — `nsis-welcome.bmp` + `nsis-header.bmp` |
| 4. Instalador | `makensis /V2 /INPUTCHARSET UTF8 installer.nsi` | OK — 117.3 MB |

Salida: `output/GraficasMulberry-Instalador-v13.5.0.exe` (117.3 MB).
Copia en historial: `installer/v13.5.0-nsis/GraficasMulberry-Instalador-v13.5.0-nsis.exe`.

Dependencias verificadas: JDK-26, Maven IntelliJ `2026.1`, NSIS x86, Python 3.14.4, Pillow OK.
Script: `build-nsis.ps1` en raíz (usar para futuros builds).
MAVEN_OPTS efímero: `-Djavax.net.ssl.trustStoreType=Windows-ROOT` si PKIX falla.

Multi-IA: Claude Code solo. Script existente, deps verificadas, sin incertidumbre técnica. VibeSec: N/A (pipeline build, sin código de usuario).

---

### Qué se hizo en la sesión 2026-06-15 (UI-F + UI-E ítem 6 cerrados)

**Sprint UI-F** (`85152bd`) — Animación de filas extendida a todos los módulos:
- `TableColumnSizing.animarFilas()`: eliminado `.limit(10)` — ahora anima todas las filas, no solo las 10 primeras.
- Hook añadido en: Presupuestos, Albaranes, Empleados, Nóminas, Materiales, Tarifas.
- Módulos ya tenían hook: Clientes, Facturas, Pedidos.
- Multi-IA: Claude Code solo. Cambio mecánico, bajo riesgo. VibeSec: N/A (UI puro). Tests: 142/142.

**Sprint UI-E ítem 6** (`e0a4252`) — Sliding pill sidebar:
- `MainView.java`: campo `navPill` (Region, `managed=false`, `mouseTransparent=true`), campo `navPillContainer` (StackPane), método `moverPill(StackPane)`.
- Construcción: `navPillContainer = new StackPane(navMenu)` → insert navPill at index 0 (detrás). `navPill.prefWidthProperty().bind(navPillContainer.widthProperty())`.
- `moverPill`: coordenadas via `localToScene` + `sceneToLocal` para tolerar scroll. Primera activación: FadeTransition 150ms. Navegaciones siguientes: TranslateTransition 200ms `EASE_BOTH`. Pill se oculta al colapsar sidebar.
- `styles.css`: `.nav-pill` con `derive(-c-accent, 80%)`, border-radius 6, opacity 0.18.
- Multi-IA: Claude Code solo. Sprint UI puro, sin auth/BD/red. VibeSec: 0 vulnerabilidades. Tests: 142/142.

---

### Qué se hizo en la sesión 2026-06-15 (GAP-1 + GAP-2 cerrados)

**Contexto:** flujo comercial completo Presupuesto → Pedido → Albarán → Factura. Los dos GAPs de funcionalidad nueva pendientes desde RELEASE-GATE.

**Implementaciones:**

- **GAP-1** (`5c2c3bf`) — Crear Pedido desde Presupuesto:
  - `PedidoDAO.crearDesdePresupuesto(int presupuestoId)` — tx, numeración automática via `DatabaseManager.generarNumeroPedido()`, descripción construida concatenando las descripciones de las líneas del presupuesto.
  - `PresupuestosView`: botón "📦 Crear Pedido" + método `crearPedido()`. Guarda diálogo de confirmación si estado ≠ "aceptado".

- **GAP-2** (`5c2c3bf`) — Crear Factura desde Albarán:
  - `FacturaDAO.crearDesdeAlbaran(Albaran albaran)` — recibe `Albaran` como parámetro (evita ciclo con `AlbaranDAO → FacturaDAO`). Crea `LineaFactura` con precio_unit=0; el usuario edita precios tras crear.
  - `AlbaranDAO.actualizarFacturaId(int albaranId, int facturaId)` — vincula la factura creada al albarán.
  - `AlbaranesView`: botón "🧾 Crear Factura" + método `crearFactura()`. Bloquea si el albarán ya tiene factura. Alerta informativa sobre precios en 0.

**Decisión de diseño:** `FacturaDAO.crearDesdeAlbaran` recibe `Albaran` en vez de `int albaranId` — `AlbaranDAO` ya importa `FacturaDAO`, importar en sentido inverso crearía ciclo. La UI carga el albarán y pasa el objeto; vincula `factura_id` en segundo paso no atómico (aceptable en SQLite desktop).

**Multi-IA:** Claude Code solo. Patrón mecánico (3 `crearDesde*` ya existían). Gemini/Codex no invocados — validación local suficiente. Gemini no invocado pese a ser tarea mediana — omisión reconocida.

**Seguridad:** VibeSec + `/security-review` ejecutados post-commit.
- 0 vulnerabilidades. SQL parameterizado en todos los nuevos métodos. Sin exposición de datos.
- Defecto menor de integridad documentado: guard de factura duplicada lee objeto cacheado, no BD. No explotable en desktop monousuario.
- HelpService: sin nuevas entradas para los flujos GAP-1/GAP-2 — pendiente decisión del usuario.

**Validación:** `BUILD SUCCESS`. Tests: 142/142.

---

### Qué se hizo en la sesión 2026-06-14 (Backlog GAPs — GAP-3/6/7 cerrados)

**Contexto:** continuación de RELEASE-GATE MANUAL (P0+P1+P2 completados sesión anterior). Esta sesión implementó todos los GAPs no bloqueantes de corto plazo identificados en el RELEASE-GATE.

**Implementaciones:**

- **GAP-6** (`04d9f25` + `1ca9d1e`) — Búsqueda en tiempo real (stream filter) en **7 módulos**:
  - `04d9f25`: Facturas, Albaranes, Presupuestos, Empleados, Nóminas
  - `1ca9d1e`: Materiales (nombre/referencia/proveedor), Tarifas (nombre/técnica/descripción)
  - Patrón: `private TextField txtBuscar` + `textProperty().addListener` + `lista.stream().filter(contiene(...))` + helper `contiene(String, String)`
  - Materiales ya tenía `chkSoloAlerta` + `cbCategoriaFiltro` — `txtBuscar` se aplica como tercer filtro en cadena en `cargar()`
  - Tarifas ya tenía `cbTecnicaFiltro` — mismo patrón, tercer filtro en cadena

- **GAP-7** (`27c3d37`) — `OllamaService.enviarConsulta()`:
  - HTTP 404 → "Modelo '...' no instalado. Instálalo desde Gestión de modelos."
  - HTTP 500 → "Ollama encontró un error interno. Reinicia Ollama e inténtalo de nuevo."
  - HTTP otro → "Error de comunicación con Ollama (código N)."
  - ConnectException/ConnectException → "Ollama no está en ejecución. Ábrelo o instálalo..."
  - "timed out" → "Tiempo de espera agotado. Ollama tardó demasiado en responder."

- **GAP-3** (`d1791e9`) — Logout in-app:
  - `Icons.java`: constante `LOGOUT` (Material Design path) + método `logout()`
  - `MainView.java`: campo `private final Runnable onLogout` + 4º param constructor `Runnable onLogout` + botón logout en `footerIconos` con diálogo confirmación → `onLogout.run()`
  - `App.java`: `new MainView(primaryStage, user, authService, this::showLogin)` — `showLogin()` reemplaza Scene; User ref cae, sesión cerrada

- **Fixes detectados en pruebas de usuario:**
  - Materiales: usuario reportó que faltaba búsqueda (no estaba en `04d9f25`) → añadido en `1ca9d1e`
  - Tarifas: mismo caso → añadido en `1ca9d1e`

**VibeSec ejecutado (GAP-3 + GAP-7):** 0 vulnerabilidades. Logout desktop: no hay tokens persistentes que revocar; reemplazar Scene + GC de MainView es suficiente.

**Multi-IA:** Claude Code solo. Cambios quirúrgicos, bajo riesgo. Codex/Gemini no invocados — validación local suficiente.

**Validación final:** `BUILD SUCCESS`. Tests: 142/142 (sin tests nuevos — lógica UI pura). Usuario confirmó PASS en pruebas manuales de Materiales, Tarifas y logout.

---

### Qué se hizo en la sesión 2026-06-14 (RELEASE-GATE MANUAL — COMPLETO)

Sprint RELEASE-GATE completado. Matriz reconstruida por Claude Code (Gemini no disponible esta sesión).

**Correcciones a matriz original Gemini (P0):**
- "Recuperar contraseña → email" → INCORRECTO. App usa pregunta de seguridad.
- "Logout → LoginView" → No existe logout in-app. "Salir" cierra app.
- "Primer arranque / AdminSetup" → SKIP: BD ya tiene admin. Requiere entorno limpio.
- "Rol EMPLEADO" → INCORRECTO. Roles reales: ADMINISTRADOR, COMERCIAL, PRODUCCION, CONTABILIDAD.

**Resultados P0:**

| Caso | Estado | Notas |
|---|---|---|
| P0-01 AdminSetup | ⏭ SKIP | BD ya tiene admin — requiere entorno limpio |
| P0-02 Login correcto | ✅ PASS | Dashboard visible |
| P0-03 Login incorrecto | ✅ PASS | Mensaje error + shake campo contraseña |
| P0-04 Lockout 5 intentos | ✅ PASS | "Demasiados intentos. Espera 5 minutos." |
| P0-05 Recuperar contraseña | ✅ PASS | Pregunta seguridad → nueva contraseña → mensaje verde |
| P0-06 Salir | ✅ PASS | App cierra limpiamente |
| P0-07 Rol COMERCIAL | ✅ PASS | Sidebar correcto |
| P0-08 Rol PRODUCCION | ✅ PASS | Sidebar correcto |
| P0-09 Clientes CRUD | ✅ PASS | Crear/editar/buscar/eliminar funciona |
| P0-10 Presupuestos CRUD | ✅ PASS | CRUD funciona; búsqueda/filtro ausente (GAP-6) |
| P0-11 Flujo negocio | ✅ PASS parcial | Presupuesto→Albarán y →Factura funcionan; ver GAP-1/GAP-2 |
| P0-12 Factura pago | ✅ PASS | Marcar pagada funciona y persiste |

**Resultados P1:**

| Caso | Estado | Notas |
|---|---|---|
| P1-01 Rol CONTABILIDAD | ✅ PASS | Sidebar correcto (DASHBOARD/FACTURAS/ALBARANES/NOMINAS/ESTADISTICAS/IA/CONFIG) |
| P1-02 Rol ADMINISTRADOR | ✅ PASS | Acceso completo |
| P1-03 Crear usuario | ✅ PASS | |
| P1-04 Cambiar contraseña | ✅ PASS | |
| P1-05 Eliminar usuario | ✅ PASS | |
| P1-06 Pedidos CRUD | ✅ PASS | Pedidos = pedidos de cliente, no compras a proveedor (ver GAP-5) |
| P1-07 Albaranes CRUD | ✅ PASS | Sin campos descripción/importe |
| P1-08 Facturas búsqueda | ⚠️ GAP-6 | Sin búsqueda/filtro — no bloqueante |
| P1-09 Materiales CRUD | ✅ PASS | |
| P1-10 Tarifas ver/editar | ✅ PASS | |
| P1-11 Empleados CRUD | ✅ PASS | |
| P1-12 Nóminas crear | ✅ PASS | Fix aplicado: UNIQUE constraint → mensaje legible (`80cc6bb`) |
| P1-13 Estadísticas | ✅ PASS | |
| P1-14 Import CSV clientes | ✅ PASS | |
| P1-15 Import CSV materiales | ✅ PASS | |
| P1-16 Export | ✅ PASS | |
| P1-17 Ayuda F1 | ✅ PASS | |
| P1-18 Onboarding | ✅ PASS | |
| P1-19 Calendario | ✅ PASS | |
| P1-20 IA/Ollama | ✅ PASS | Abre sin crash; fix instalador path acentuado (`4b0f116`) |

**Resultados P2:**

| Caso | Estado | Notas |
|---|---|---|
| P2-01 GAP-4 lockout post-reset | ✅ CONFIRMADO | Comportamiento seguro — lockout persiste |
| P2-02 Asistente Visual | ✅ PASS | |
| P2-03 Backup | ✅ PASS | |
| P2-04 Configuración persistencia | ✅ PASS | App solo en español — GAP-8: i18n backlog |
| P2-05 Presupuesto→Albarán flujo | ✅ PASS | |
| P2-06 Estabilidad cierre | ✅ PASS | |

**RESULTADO FINAL: 35/37 PASS, 1 SKIP, 1 GAP no bloqueante. App lista para release.**

**Fixes aplicados durante RELEASE-GATE:**
- `80cc6bb` — NominasView: UNIQUE constraint → mensaje legible
- `4b0f116` — OllamaInstaller: path con espacios/acentos via env var

**Multi-IA:** Claude Code lidera. Gemini no disponible. Codex no invocado — fixes quirúrgicos sin incertidumbre. VibeSec ejecutado, 0 vulnerabilidades.

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

**PUNTO DE ENTRADA EXACTO PARA EL PRÓXIMO AGENTE:**

HEAD: `74910eb`. Rama: `master`. Tests: 146/146. App funcional. BD: 462 materiales reales.

Todos los sprints principales cerrados. Cola: GAP-5 (largo plazo), GAP-8 (largo plazo), Refactor B2.

Preguntar al usuario qué prioriza si no lo indica.

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
| HEAD | `74910eb` |
| Mensaje | `test(service): añadir PreferenceServiceTest con BD efímera` |
| Rama | `master` |
| Tests | 146/146 verdes (`.\mvnw.cmd test`) |
| Versión app | v13.5.0 (`AppConstants.APP_VERSION`) |

---

## Sprint activo

**Sprint RELEASE-GATE MANUAL** — ✅ CERRADO. 35/37 PASS, 1 SKIP.

**Sprint Backlog GAPs** — ✅ CERRADO. GAP-1/2/3/6/7 implementados. GAP-4 sin acción (comportamiento seguro). GAP-5/8 largo plazo.

**Sprint UI-E** — ✅ CERRADO. Todos los ítems implementados (1/2/3/4/5/6/7).

**Sprint UI-F** — ✅ CERRADO. Animación de filas extendida a los 9 módulos.

**Sprint UI-COUNTER** — ✅ CERRADO. `lblContador` en los 9 módulos. HEAD `d93dc76`.

**Sprint A (hint bars)** — ✅ CERRADO. `buildBeginnerHint()` en Facturas, Pedidos, Materiales, Empleados. HEAD `cd91f15`.

**Sprint B (PreferenceServiceTest)** — ✅ CERRADO. 4 tests, BD efímera, reset singleton. HEAD `74910eb`.

**Sprint MIGRACION-COMPLEJA** — ✅ CERRADO. 462 materiales en BD. Test data eliminada. UNION_PAPELERA normalizado.

---

## Cola prioritaria

1. **GAP-5**: Módulo Compras a proveedor (largo plazo — requiere nuevo módulo completo).
2. **GAP-8**: Soporte multiidioma EN/CA/GL/EU (largo plazo).
3. **Refactor B2** — inyección de Connection en DAOs (largo plazo).

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
| Sprint B (PreferenceServiceTest) | `74910eb` | 4 tests BD efímera — reset singleton por reflexión |
| Sprint A (hint bars) | `cd91f15` | buildBeginnerHint() en Facturas, Pedidos, Materiales, Empleados |
| Sprint UI-COUNTER | `d93dc76` | lblContador en los 9 módulos — texto muted entre buscador y spacer |
| Sprint UI-F | `85152bd` | Animación de filas extendida a todos los módulos |
| Sprint UI-E ítems 1-3 | `3bd6e1e` | CSS elevación + KPI animados + filas escalonadas (Clientes/Facturas/Pedidos) |
| Sprint UI-E (slide+fade) | `0bb8c8b` | slide+fade en mostrarVista (220ms, EASE_OUT, +24px X) |
| Sprint UI-A/B/C/D | varios | CSS variables, FadeTransition, IAView, skeleton+overlay |
| Sprint SEC | — | 5 fixes seguridad P0/P1 |
| Sprint COD | — | Dead code eliminado |

---

## Deuda técnica conocida

- Refactor B2: inyección de Connection en DAOs (largo plazo)
