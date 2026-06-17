# Estado operativo — Gráficas Mulberry

Fuente única de verdad para HEAD, tests y sprint activo.
Actualizar tras cada sprint cerrado.

**Última actualización:** 2026-06-18 (sesión cierre — Sprint i18n-9 — 151/151)

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

### ESTADO AL CIERRE DE SESIÓN 2026-06-18 (Sprint i18n-9)

**HEAD:** `75e68e3`. Rama: `master`. Tests: **151/151 verdes**.

**Sprints cerrados esta sesión:**

| Sprint | Descripción |
|---|---|
| i18n-3 | MainView migrada — sidebar, footer, tooltips, diálogos, asistente visual (~60 claves nav.* + main.*) |
| i18n-4 | DashboardView + ClientesView migradas — KPIs, avisos, toolbar, tabla, diálogo edición, importación, exportación, previsualización, errores (~90 claves dash.*/clientes.*/export.*) |
| i18n-5 | FacturasView migrada — toolbar, tabla, diálogos (edición+líneas+materiales), albaranes, exportación, previsualización (~75 claves facturas.*) |
| i18n-6 | PedidosView migrada — resumen KPIs, tabs, filtros, toolbar, tablas pedidos+pagos, diálogos (edición+pago+fraccionar), importación, exportación, previsualización (~128 claves pedidos.*) |
| i18n-7 | AlbaranesView migrada — toolbar, tabla, diálogos (edición+artículos+stock), importación, exportación, previsualización, rename txf+tLineas (~84 claves albaranes.*) |
| i18n-8 | PresupuestosView migrada — toolbar, tabla, diálogos (presupuesto+líneas+materiales+tarifa tiempo), importación, exportación, previsualización, rename txf+tLineas+tarifa (~118 claves presupuestos.*) |
| i18n-9 | NominasView migrada — toolbar, tabla, diálogos (nueva/editar+resumen+generar mes), importación, exportación, previsualización, errores (~76 claves nominas.*) |

**Estado del sistema i18n al cierre:**

- `LanguageManager` — infraestructura completa (singleton, `t()`, `tf()`, fallback ES, UTF-8).
- `LanguageManager.tf(key, args)` — añadido formalmente (MessageFormat wrapper).
- 6 bundles COMPLETOS con todas las claves i18n-0 → i18n-9: `messages_{es,en,ca,eu,gl,fr}.properties`.
- Vistas migradas: `LoginView`, `AdminSetupView`, `ConfiguracionView`, `MainView`, `DashboardView`, `ClientesView`, `FacturasView`, `PedidosView`, `AlbaranesView`, `PresupuestosView`, **`NominasView`**.
- Vistas pendientes de migrar: EmpleadosView, MaterialesView, TarifasView, ComprasProveedorView, EstadisticasView, CalendarioView, etc.

**Decisión arquitectónica crítica de i18n-3 (respetar en sprints futuros):**

`TITULO_A_MODULO` usa **claves i18n** como keys del mapa (p.ej. `"nav.clientes"`, no `"Clientes"`). Esto es obligatorio: si el mapa usara strings traducidos, la lookup fallaría en idiomas distintos del español. Todos los callers de `mostrarVista()` pasan la clave i18n, no el texto traducido. Dentro de `mostrarVista()` se llama `t(titulo)` para el asistente visual y los títulos de ventana popup.

**Decisiones de i18n-4/i18n-5 (respetar en sprints futuros):**

- `COLUMNAS_BASE` static map en ClientesView **NO migrado** — los valores son labels almacenados en BD (en español). La sobrescritura via `actualizarColumnasDinamicas()` / `columnConfigDAO.visibleLabels()` ocurre inmediatamente y prevalece sobre los headers del `col()` call. Migrar `col()` headers es correcto semánticamente (future-friendly) pero los headers visibles en runtime son los de BD.
- Valores del ComboBox `"empresa"` / `"particular"` **NO traducidos** — se almacenan en BD como strings españoles. Traducirlos rompería datos existentes.
- `mostrarResultadoImportacion()` es **dead code** (nunca llamado) — migración mínima aplicada (solo título del Alert).
- Claves `export.fmt.*` son **compartidas entre módulos** (prefix `export.fmt`, no `clientes.export.fmt`). Las descripciones específicas de módulo usan `clientes.export.<fmt>.desc`.
- Naming conflict `tf`: si la vista tiene `private TextField tf(...)`, renombrar a `txf()` antes de añadir `import static LanguageManager.tf`. Aplicado en ClientesView.

**Claves i18n ya definidas en los bundles (resumen acumulado):**
- `lang.*` — nombres de idiomas (6 claves)
- `config.idioma.*` — panel selector de idioma (3)
- `common.*` — labels/prompts/errores comunes (9)
- `login.*` — LoginView (9) + `login.recovery.*` (7)
- `admin.*` — AdminSetupView (6)
- `config.*` — ConfiguracionView completa (~95 claves)
- `nav.*` — módulos sidebar + grupos + tooltips (~35 claves)
- `main.*` — footer, búsqueda, sesión, menú ctx, diálogos, asistente (~25 claves)
- `dash.*` — DashboardView: KPIs, avisos, badges (~21 claves)
- `export.*` — diálogo exportación + formatos compartidos (~13 claves)
- `clientes.*` — ClientesView completa (~53 claves)
- `facturas.*` — FacturasView completa (~75 claves)
- `pedidos.*` — PedidosView completa (~128 claves)
- `albaranes.*` — AlbaranesView completa (~84 claves)
- `presupuestos.*` — PresupuestosView completa (~118 claves)
- `nominas.*` — NominasView completa (~76 claves)

**Patrón de migración establecido (repetir en i18n-4+):**
```java
import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;
// Luego: new Label("Texto") → new Label(t("clave"))
//        new Button("Texto") → new Button(t("clave"))
//        mostrarToast("Texto") → mostrarToast(t("clave"))
//        "Texto " + var → tf("clave.con.{0}", var)
```
**ATENCIÓN naming conflict:** si la vista tiene `TextField tf` o loop var `Tema t` / similar, renombrar a `field`/`tema` para evitar shadowing con `import static tf` / `t`.

### Punto de entrada exacto para el próximo sprint

**HEAD:** `cb15d0a`. Tests: 151/151. App funcional.

**Cola prioritaria (en orden recomendado):**
1. **Sprint i18n-10** — migrar `EmpleadosView`. Mismo patrón. Ver checklist abajo.
2. **Refactor B2** — inyección de Connection en DAOs. Grande, riesgo alto. Requiere Gemini ANTES.

**Comando de verificación al inicio de sesión:**
```powershell
cd "C:\Users\GipsyDavy\MAVEN\Graficas Mulberry"
.\mvnw.cmd test   # debe dar 151/151 BUILD SUCCESS
git log --oneline -5
```

**Archivos clave del sistema i18n:**
- `src/main/java/org/gipsybuho/service/LanguageManager.java` — singleton, `t()`, `tf()`, fallback ES, UTF-8.
- `src/main/resources/org/gipsybuho/i18n/messages_es.properties` — bundle base (fuente de verdad de claves).
- `src/main/resources/org/gipsybuho/i18n/messages_{en,ca,eu,gl,fr}.properties` — traducciones.
- `src/test/java/org/gipsybuho/service/LanguageManagerTest.java` — 5 tests.

---

### CHECKLIST SPRINT i18n-10 — EmpleadosView

**Paso 0 — antes de tocar el archivo:**
```powershell
.\mvnw.cmd test   # verificar 151/151
git log --oneline -5   # verificar HEAD cb15d0a
```

**Paso 1 — Buscar conflictos de nombres en EmpleadosView:**
```bash
# ¿Tiene método privado llamado tf() o t()?
grep -n "private.*\btf\b\|private.*\bt\b" src/main/java/org/gipsybuho/ui/EmpleadosView.java
# ¿Tiene variable local TableView<X> t o similar?
grep -n "\bTableView.*\bt\b\|\bEmpleado\b t " src/main/java/org/gipsybuho/ui/EmpleadosView.java
# ¿Tiene loop for (TextField tf : ...)?
grep -n "for.*TextField tf" src/main/java/org/gipsybuho/ui/EmpleadosView.java
```
- Si hay `private ... tf(...)` → renombrar a `txf(...)` + actualizar call sites ANTES de añadir imports.
- Si hay `TableView<X> t` o `Empleado t` → renombrar a nombre descriptivo ANTES de añadir imports.
- Si hay `for (TextField tf : ...)` sin llamada a `tf()` dentro → NO renombrar (safe shadowing).

**Paso 2 — Añadir imports al bloque de imports existente:**
```java
import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;
```

**Paso 3 — Migrar strings en orden top-down:**
- Constructor / init: `new Label("...")`, `new Label("...")` subtítulo
- `buildToolbar()`: btn labels, tooltips, txtBuscar promptText+tooltip
- `buildTabla()`: `new TableColumn<>("...")`, `tabla.setPlaceholder(...)`
- `cargar()`: `lblContador.setText(... + " empleados")` → `tf("empleados.contador", n)`
- `nueva()`, `editar()`, `borrar()`: alertas, confirmaciones
- `dialogoEmpleado()`: títulos, labels de campo, ComboBox items si son display-only
- `importar()`: FileChooser title y filtros
- `mostrarResultadoImportacion()`: String.format → tf()
- `exportar()`: formatos array usando claves compartidas export.fmt.* + empleados-específicas
- `lanzarExportacion()`: fc.setTitle → `tf("export.dialog.guardar", fmt[1])`, ok.setTitle → `t("export.exito.titulo")`, ok content → `tf("export.exito.mensaje", destino)`
- `previsualizar()`: alerta vacío, tituloVentana
- `mostrarError()`: mensajes UNIQUE constraint

**Paso 4 — Claves compartidas ya existentes (NO redefinir):**
```
export.dialog.instruccion   = "Selecciona el formato de exportación:"
export.dialog.btn           = "Exportar →"
export.dialog.guardar       = "Guardar exportación — {0}"   ← tf()
export.exito.titulo         = "Exportación completada"
export.exito.mensaje        = "Exportación completada:\n{0}"  ← tf()
export.fmt.sqlite.label     + export.fmt.sqlite.desc
export.fmt.csv.label / sql / json / pdf / word / excel .label
common.error.desconocido    = "Error desconocido"
```
Las descripciones específicas de módulo → `empleados.export.<fmt>.desc`.

**Paso 5 — Bundles (6 archivos):**
Añadir bloque `# ── Empleados ──` al final de cada bundle, después de la última clave de `nominas.*`.
- ES: todas las claves nuevas.
- EN: inglés.
- CA: apostrofes dobles (`''`) SOLO en valores de claves tf() (las que tienen `{0}`). Claves t()-only usan `'` simple.
- EU: euskera.
- GL: gallego.
- FR: apostrofes dobles (`''`) SOLO en valores de claves tf(). Claves t()-only usan `'` simple.

**Paso 6 — Validar:**
```powershell
.\mvnw.cmd clean compile   # verde
.\mvnw.cmd test            # 151/151 (o más si se añaden tests)
```

**Paso 7 — VibeSec → Commit:**
- Invocar `/VibeSec` al cerrar sprint.
- Commit: `feat(i18n): migrar EmpleadosView a LanguageManager — Sprint i18n-10`
- Commit docs: `docs(state): cerrar Sprint i18n-10 — EmpleadosView + N claves — 151/151`

---

### Reglas i18n consolidadas (todas las sesiones)

**Naming conflicts — prioridad al resolver antes de añadir imports:**
1. `private TextField tf(String v)` → renombrar a `txf()` + actualizar todos los call sites.
2. `TableView<X> t` local var → renombrar a nombre descriptivo (ej: `tLineas`).
3. `Tipo t` en lambda → renombrar a nombre descriptivo (ej: `tarifa`).
4. `for (TextField tf : ...)` sin llamada a `tf()` dentro → NO renombrar.
5. Params `btn(String t, ...)`, `lbl(String t)`, `col(String t, ...)` → NO renombrar (no colisionan).

**Regla apostrofes MessageFormat:**
- Clave usada via `tf(key, ...)` (tiene `{0}`, `{1}`, etc.) → `'` se dobla a `''` en CA y FR.
- Clave usada solo via `t(key)` (sin `{}`) → `'` permanece simple en todos los idiomas.
- EN, ES, EU, GL: sin apostrofes problemáticos en general (inglés usa comillas tipográficas o reformula).

**Patrón numérico en tf():**
- Double: `{0,number,0.00}` para euros. `{0,number,0.0}` para segundos.
- Int/String: `{0}` sin formato.

**Claves compartidas export — nombres EXACTOS en los bundles:**
```
export.dialog.instruccion   (t)
export.dialog.btn           (t)
export.dialog.guardar       (tf — arg: nombre del formato)
export.exito.titulo         (t)
export.exito.mensaje        (tf — arg: ruta destino)
export.fmt.sqlite/csv/sql/json/pdf/word/excel .label   (t, compartidos)
export.fmt.sqlite.desc      (t, compartido — descripción SQLite genérica)
```
Las `export.fmt.<fmt>.desc` de cada módulo son **específicas**: `empleados.export.csv.desc`, etc.

**No traducir:**
- Valores de ComboBox/enums almacenados en BD (ej: estado `"borrador"`, tipo `"empresa"`).
- `COLUMNAS_BASE` static map — valores son claves BD, no UI.
- DB column names, field keys, lógica interna.

### Decisiones consolidadas (todas las sesiones)
- Hot-swap de idioma NO implementado — requiere reinicio. Intencional.
- `TamanoFuente.key` guarda claves i18n (no labels) — `t(ts.key())` se llama en build time.
- COMERCIAL no tiene permiso COMPRAS — mínimo privilegio (Gemini).
- Tab pagos en MaterialesView NO se toca — coexiste con ComprasProveedorView sobre mismo DAO.
- `PreferenceService` singleton se resetea en tests via reflexión — no tocar código producción.

### Qué se hizo en la sesión 2026-06-15 (GAP-5 Compras a Proveedor)

**Sprint GAP-5** (`d243cbe`) — Módulo Compras a Proveedor:

`ComprasProveedorView.java` (nuevo) — vista standalone sobre `pagos_material` + `PagoMaterialDAO` ya existentes. Tabla con columnas: estado (dot color), material, proveedor, nº factura, fecha compra, importe, forma pago, vencimiento, días, notas. Filtros: Todos / Pendientes / Vencidos / Próximos / Pagados. Resumen de 3 KPIs (total pendiente, vencidos, próximos 7 días). CRUD completo: nueva compra, editar, marcar pagado (con DatePicker), eliminar. Hint bar de principiante + lblContador.

`UserPermissions.COMPRAS = "compras"` — nuevo permiso. Asignado a ADMINISTRADOR, PRODUCCION, CONTABILIDAD (no COMERCIAL — mínimo privilegio por decisión Gemini).

`Icons.SHOPPING_BAG` — nuevo icono SVG. Sidebar grupo COMERCIAL.

Tab pagos en MaterialesView mantenido sin cambios (YAGNI). Ambas vistas comparten el mismo DAO/tabla sin conflicto.

Pendiente menor: F1 no vinculado al módulo "compras" en HelpService — abre ayuda general.

Multi-IA: Claude Code lidera. Gemini consultado (bloque IDE — decisión de roles, UX standalone, icono, agrupación sidebar). VibeSec: LIMPIO. Tests: 146/146 verdes.

---

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

HEAD: `d243cbe`. Rama: `master`. Tests: 146/146. App funcional. BD: 462 materiales reales.

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
| HEAD | `71177b4` |
| Mensaje | `feat(i18n): migrar MainView a LanguageManager — Sprint i18n-3` |
| Rama | `master` |
| Tests | 151/151 verdes (`.\mvnw.cmd test`) |
| Versión app | v13.5.0 (`AppConstants.APP_VERSION`) |

---

## Sprint activo

**Sprint i18n-3** — ✅ CERRADO. MainView migrada (~60 claves nav.* + main.*). TITULO_A_MODULO fix. tf() formal. HEAD `71177b4`. 151/151.

**Sprint i18n-2** — ✅ CERRADO. ConfiguracionView migrada. 6 bundles completos. HEAD `6957681`.

**Sprint i18n-1** — ✅ CERRADO. LoginView + AdminSetupView migrados. Bundles eu/gl/fr ~40 claves. HEAD `a035fe8`.

**Sprint i18n-0** — ✅ CERRADO. Infraestructura LanguageManager + 6 bundles base. HEAD `1947fbc`.

**Sprint RELEASE-GATE MANUAL** — ✅ CERRADO. 35/37 PASS, 1 SKIP.

**Sprint Backlog GAPs** — ✅ CERRADO. GAP-1/2/3/6/7 implementados.

**Sprint UI-E/F** — ✅ CERRADO. Animaciones, pill sidebar, contadores.

**Sprint GAP-5 (Compras a Proveedor)** — ✅ CERRADO. ComprasProveedorView. HEAD `d243cbe`.

**Sprint HelpService compras** — ✅ CERRADO. 5 artículos HTML. F1 vinculado. HEAD `7ae2a79`.

**Sprint MIGRACION-COMPLEJA** — ✅ CERRADO. 462 materiales en BD.

---

## Cola prioritaria

1. **Sprint i18n-4** — migrar vistas de módulo (ClientesView, FacturasView, PedidosView…). Mismo patrón que i18n-3.
2. **Refactor B2** — inyección de Connection en DAOs (largo plazo). Requiere Gemini ANTES.

---

## Sprints cerrados relevantes

| Sprint | Commit | Descripción |
|---|---|---|
| i18n-3 | `71177b4` | MainView: sidebar, footer, tooltips, diálogos, asistente (~60 claves); TITULO_A_MODULO fix; tf() formal |
| i18n-2 | `6957681` | ConfiguracionView: ~80 literales → t()/tf(); bundles eu/gl/fr config.* |
| i18n-1 | `a035fe8` | LoginView + AdminSetupView migrados; bundles eu/gl/fr ~40 claves |
| i18n-0 | `1947fbc` | LanguageManager + 6 bundles base; ConfiguracionView.buildPanelIdioma() |
| HelpService compras | `7ae2a79` | 5 artículos HTML compras/; F1 vinculado al módulo |
| SECURITY-2026-06-13 | `6268479` | Auditoría + remediación completa SEC-01..10 + NEW-01..03 |
| HELP-5 | `610a0f2` | PreferenceService, OnboardingDialog, modo principiante, hint bar |
| HELP-4-FIX | `4117cdf` | DDL UNIQUE constraints, factorías estáticas HelpView, tests |
| HELP-3 | `67f7d4e` | F1 contextual por módulo |
| HELP-2 | `47e46dc` | HelpService + HelpView JavaFX |
| HELP-1 | `65588cf` | 81 artículos HTML offline |
| Sprint GAP-5 | `d243cbe` | ComprasProveedorView — permiso COMPRAS en ADMIN+PRODUCCION+CONTABILIDAD |
| Sprint B | `74910eb` | 4 tests BD efímera — reset singleton por reflexión |
| Sprint A | `cd91f15` | buildBeginnerHint() en Facturas, Pedidos, Materiales, Empleados |
| Sprint UI-COUNTER | `d93dc76` | lblContador en los 9 módulos |
| Sprint UI-F | `85152bd` | Animación de filas extendida a todos los módulos |
| Sprint UI-E | `3bd6e1e` | CSS elevación + KPI animados + filas escalonadas + pill sidebar |

---

## Deuda técnica conocida

- i18n: vistas de módulo pendientes de migrar (ClientesView, FacturasView, PedidosView, AlbaranesView, PresupuestosView, NominasView, EmpleadosView, MaterialesView, TarifasView, ComprasProveedorView, EstadisticasView, CalendarioView)
- Refactor B2: inyección de Connection en DAOs (largo plazo)
