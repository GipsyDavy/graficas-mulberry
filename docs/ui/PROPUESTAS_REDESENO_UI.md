# Propuestas de Rediseño UI/UX Multi-Interfaz — Gráficas Mulberry

> **Estado:** `PLANIFICADA` (Solo documentación — pendiente de revisión y consenso del Triunvirato IA en el próximo sprint).  
> **Tarea en ledger:** `ui-redesign-2026-11` ([.agents/review-ledger.yaml](../../.agents/review-ledger.yaml))  
> **Objetivo:** Permitir que la aplicación abandone la interfaz actual sobrecargada y ofrezca **múltiples modos/interfaces seleccionables entre sí** (más combinaciones híbridas consensuadas), compartiendo intactos la capa de negocio, SQLite DAOs, permisos y seguridad.

---

## 1. Diagnóstico de la interfaz actual (evidencia en código)

Tras revisar las vistas JavaFX actuales (`src/main/java/org/gipsybuho/ui/*.java` y `styles.css`), los problemas percibidos por el usuario se explican por cinco patrones estructurales:

1. **Barra lateral permanente saturada (`MainView.java`):** `220px` fijos con logo, datos de sesión, buscador oculto (`Ctrl+K`), **5 grupos colapsables con ~15 módulos**, fila inferior de **6 botones solo-icono**, botón ancho de `Cerrar aplicación` y versión. La apertura en ventana independiente (`ModuloWindowManager`) queda oculta tras clic derecho.
2. **Barras de comandos sobrecargadas en vistas CRUD:**
   - `PresupuestosView.buildToolbar()` coloca buscador + contador + **10 botones de texto en fila** (`Nuevo`, `Editar`, `Borrar`, `Importar`, `Exportar`, `Crear Pedido`, `Crear Albarán`, `Crear Factura`, `Previsualizar`, `Columnas`).
   - `FacturasView`, `AlbaranesView` y `TarifasView` repiten **8–9 botones horizontales** sin jerarquía visual entre acción principal y acciones secundarias.
3. **Homogeneidad excesiva basada en `TableView`:** Clientes, Tarifas, Materiales, Presupuestos, Pedidos, Albaranes y Facturas comparten casi la misma estructura visual (*Título → Beginner Hint → Toolbar → Tabla a pantalla completa*), con escaso apoyo visual (sin Kanban, sin tarjetas de stock, sin panel de detalle lateral).
4. **Flujo comercial fragmentado:** Seguir un trabajo de principio a fin obliga a saltar por 4 pantallas separadas (`Presupuestos` → `Pedidos` → `Albaranes` → `Facturas`).
5. **Inicio, Login y Configuración poco visuales:** `DashboardView` solo muestra 5 tarjetas numéricas y una lista plana de notas; `LoginView` es una columna básica centrada; `ConfiguracionView` agrupa 7 pestañas prefijadas con emojis.

---

## 2. Catálogo de las 7 interfaces propuestas (para implementar y poder elegir entre ellas)

Cada propuesta define un **Layout / Modo de Interfaz seleccionable** por el usuario. Todas las capturas mockup se encuentran en [docs/ui/mockups/](mockups/).

### Propuesta 1 — Taller Visual Kanban + Rail Minimalista *(Estilo Linear / Notion)*
- **Navegación:** Rail izquierdo ultra-fino (`64px`, solo iconos con tooltip) agrupado en 5 áreas (`Inicio`, `Taller`, `Comercial`, `Clientes`, `Materiales` + `Finanzas`, `IA` y `Ajustes` abajo).
- **Pantallas clave:**
  - Tablero **Kanban** de 4 columnas (`Presupuesto enviado` → `En producción` → `Listo / Albarán` → `Facturado reciente`) con tarjetas por trabajo, badge de técnica, fecha límite y barra de cobro.
  - Vistas de **Presupuestos / Facturas en Master-Detail**: tabla limpia de solo 4 columnas + **drawer lateral derecho** con botones contextuales (`Crear Pedido`, `Crear Factura`, `Editar`) y **miniatura PDF en vivo**.
- **Mockups:** [p1_kanban_taller.png](mockups/p1_kanban_taller.png) · [p1_master_detail_presupuestos.png](mockups/p1_master_detail_presupuestos.png)

### Propuesta 2 — Workspace por Pestañas + Bento Grid Ejecutivo *(Estilo Fluent ERP)*
- **Navegación:** Sin barra lateral. Cabecera superior en **3 bloques** (`Comercial`, `Producción y Stock`, `Equipo y Administración`) + **pestañas de documentos abiertos** tipo navegador/IDE (`Panel Ejecutivo`, `Pedido #P-2026-408`, `Cliente: Estudio Nómada`).
- **Pantallas clave:**
  - **Dashboard Bento Grid** con tarjetas KPI + mini-sparklines, comparativa gráfica mensual, barras de nivel de stock crítico y agenda próxima.
  - **Ficha integral de Cliente** (avatar, datos fiscales, historial en sub-pestañas) compatible con **ventanas flotantes desacopladas** (`PdfPreviewWindow` / `ModuloWindowManager`).
- **Mockups:** [p2_bento_dashboard.png](mockups/p2_bento_dashboard.png) · [p2_ficha_cliente_ventana.png](mockups/p2_ficha_cliente_ventana.png)

### Propuesta 3 — Estudio Editorial Cero Fricción *(Estilo Apple / Craft)*
- **Navegación:** Barra superior flotante tipo píldora (`Inicio`, `Comercial`, `Almacén`, `Equipo`, `Analítica`, `Ajustes`) + botón flotante discreto del **Asistente IA** abajo a la derecha.
- **Pantallas clave:**
  - **Login dividido moderno** + **Launcher de Inicio ("Buenos días")** con 4 tarjetas grandes de acción inmediata y lista de prioridades del día.
  - **Almacén de Materiales en Tarjetas Visuales** con filtros por categoría, medidor gráfico de stock (verde/ámbar/rojo) y enlace directo `Pedir a proveedor →`.
- **Mockups:** [p3_login_y_launcher.png](mockups/p3_login_y_launcher.png) · [p3_materiales_tarjetas.png](mockups/p3_materiales_tarjetas.png)

### Propuesta 4 — Estación Creativa con Dock Flotante + IA Acoplable *(Estilo Figma / Studio)*
- **Navegación:** **Dock flotante inferior centrado** que deja el 100% del ancho de ventana libre.
- **Pantallas clave:**
  - **Calendario mensual visual** con píldoras de color por tipo de evento y **panel lateral acoplado del Asistente IA (Ollama)** con contexto ERP en vivo.
  - **Estadísticas y Analítica Visual** sin barras saturadas: KPIs limpios, gráfico comparativo mensual (*Ingresos vs Gastos vs Nóminas*) y gráfico Donut de *Facturación por Técnica*.
- **Mockups:** [p4_calendario_ia_dock.png](mockups/p4_calendario_ia_dock.png) · [p4_estadisticas_visuales.png](mockups/p4_estadisticas_visuales.png)

### Propuesta 5 — Ciclo Comercial Unificado 1-Clic *(Estilo Stripe / Holded)*
- **Navegación:** Barra superior minimalista y **selector segmentado de fase** dentro del módulo comercial.
- **Pantallas clave:**
  - **Pipeline Unificado:** fusiona `Presupuestos`, `Pedidos`, `Albaranes` y `Facturas` en una sola pantalla con barra de progreso inline de 4 pasos (`Presup. → Pedido → Albarán → Factura`) y **un único botón de siguiente paso** por fila.
  - **Configuración limpia en 2 columnas** (estilo macOS Settings), eliminando las 7 pestañas con emojis y añadiendo previsualización visual de temas y toggles modernos.
- **Mockups:** [p5_pipeline_documentos.png](mockups/p5_pipeline_documentos.png) · [p5_configuracion_limpia.png](mockups/p5_configuracion_limpia.png)

### Propuesta 6 — Consola Oscura de Taller CMYK *(Estilo Studio Pro / Táctil)*
- **Navegación:** Barra superior limpia + filtro lateral izquierdo de **estaciones del taller** (`Serigrafía`, `DTF Textil`, `Bordado`, `Vinilo / Plotter`) y alertas inmediatas de stock/entregas.
- **Pantallas clave:**
  - **Cola Activa de Producción** en modo oscuro de alto contraste (acentos cian/magenta/amarillo CMYK) con panel derecho de consumo de materiales, pagos y botones táctiles amplios.
  - **Calculadora Visual de Tarifas por Tramos** en tiempo real (técnica + volumen auto-aplicado + setup + resumen instantáneo de presupuesto).
- **Mockups:** [p6_dark_taller_console.png](mockups/p6_dark_taller_console.png) · [p6_dark_tarifas_calculador.png](mockups/p6_dark_tarifas_calculador.png)

### Propuesta 7 — Interfaz Adaptativa por Rol + Paleta Universal `Ctrl+K` *(Estilo Superhuman)*
- **Navegación:** Selector superior de **Modo de Trabajo** (`Atención al Cliente`, `Producción Taller`, `Contabilidad y Nóminas`) + **Paleta Universal de Comandos (`Ctrl+K`)** centrada.
- **Pantallas clave:**
  - **Vista de Rol Contabilidad y Nóminas** que reúne en una sola pantalla *Facturas y Cobros Pendientes*, *Nóminas del mes* y *Compras a Proveedores por pagar*.
  - **Command Palette (`Ctrl+K`)** para crear documentos, saltar a cualquier módulo o abrir ventanas independientes en dos pulsaciones.
- **Mockups:** [p7_rol_contabilidad_nominas.png](mockups/p7_rol_contabilidad_nominas.png) · [p7_command_palette_popup.png](mockups/p7_command_palette_popup.png)

---

## 3. Arquitectura recomendada para permitir elegir entre las 7 interfaces (sin duplicar lógica)

Para implementar las 7 interfaces (y sus híbridos) manteniendo **158/158 tests**, la integridad SQLite y los permisos `UserPermissions`, la refactorización recomendada en JavaFX 21 se apoya en **tres capas desacopladas**:

```text
[ TemaManager (Paletas + Light/Dark) ]   ← Ortogonal al layout
                 +
[ PreferenceService: ui.layout.mode ]    ← Selector persistido por usuario/equipo
                 │
                 ▼
┌────────────────────────────────────────────────────────────────┐
│ 1. NavigationShell (sustituye el BorderPane rígido de MainView)│
│    Implementaciones intercambiables:                           │
│    • RailShell (P1)       • TabWorkspaceShell (P2)             │
│    • PillNavShell (P3)    • DockStudioShell (P4)               │
│    • TopPipelineShell(P5) • DarkConsoleShell (P6)              │
│    • RoleCommandShell(P7) • ClassicSidebarShell (fallback)     │
└────────────────────────────────────────────────────────────────┘
                 │ invoca mediante ModuleDescriptor (id, permiso, factory)
                 ▼
┌────────────────────────────────────────────────────────────────┐
│ 2. Vistas Modulares con Modo de Presentación                   │
│    • Vista Comercial unificable (Kanban / Pipeline / Tabla)    │
│    • Vista Materiales (Tarjetas stock / Tabla)                 │
│    • Barra de acciones estándar: [Buscar] [⋯ Más] [+ Primario] │
│    • DetailDrawer lateral reutilizable (resumen + preview PDF) │
└────────────────────────────────────────────────────────────────┘
                 │ lee/escribe siempre a través de
                 ▼
┌────────────────────────────────────────────────────────────────┐
│ 3. Servicios, DAOs SQLite, AuthService y ExportService         │
│    (SIN CAMBIOS en DDL, BCrypt, CSV ni rutas)                  │
└────────────────────────────────────────────────────────────────┘
```

### Reglas de diseño transversales (aplican a las 7)
- **Regla 3-botones-máximo en toolbars:** Toda vista tendrá como máximo 1 botón primario (`+ Nuevo`), 1 selector de filtro/vista y 1 menú `⋯` para acciones secundarias (`Importar`, `Exportar`, `Columnas`, `Previsualizar`).
- **Command Palette (`Ctrl+K`) global:** Disponible en *todos* los layouts (no solo en el P7) como overlay en `StackPane` raíz.
- **Preservación de estado (`IAView`):** El patrón de cacheo de instancia única introducido en `MainView` se mantiene en `ModuleRegistry` para no perder historial ni estado al cambiar de vista o de shell.
- **Cambio de interfaz en caliente:** Al cambiar el `UiLayoutMode` desde Configuración o desde un selector rápido en cabecera, se reconstruye únicamente el `NavigationShell` sobre el `Scene` actual preservando la sesión de `User` y `AuthService`.

---

## 4. Opinión, combinaciones recomendadas (Híbridos) y consenso del Triunvirato IA

### 4.1 Matriz de compatibilidad para combinar piezas entre interfaces

| Pieza / Patrón | Combina mejor con... | Evitar combinar con... | Motivo |
|---|---|---|---|
| **Rail fino 64px (P1)** | Kanban (P1), Master-Detail (P1), Pipeline (P5), Config 2-col (P5) | Dock inferior (P4) o Píldora superior (P3) | Duplicaría navegación fija y robaría altura |
| **Pestañas de documentos (P2)** | Bento Dashboard (P2), Ficha Cliente (P2), Ventana PDF flotante | Pipeline 1-pantalla (P5) si no hay documentos multi-ficha | Las pestañas brillan cuando abres varios clientes/pedidos a la vez |
| **Tarjetas de Stock (P3)** | Todos los shells (P1–P7) | — | Es un modo de vista interno de `MaterialesView` (conmutable con tabla) |
| **Asistente IA acoplable (P4)** | Calendario (P4), Rail (P1), Pestañas (P2) | Consola táctil estrecha | Aportación muy alta en pantalla ancha (>= 1280px) |
| **Pipeline 1-Clic (P5)** | Rail (P1), Rol Modo (P7), Config 2-col (P5) | Duplicar 4 módulos separados en la barra principal | Si activas Pipeline unificado, los 4 módulos pasan a ser filtros del mismo |
| **Calculadora Tarifas (P6)** | Consola Oscura (P6), Master-Detail (P1), Pipeline (P5) | — | Resuelve el dolor real de presupuestar rápido en imprenta |
| **Paleta `Ctrl+K` (P7)** | **Todas (P1 a P7)** | — | Debe ser transversal a toda la aplicación |

### 4.2 Tres combinaciones híbridas candidatas a "Modo Recomendado por Defecto"

- **Híbrido A — "Mulberry Estudio Total" *(P1 + P3 + P5 + P7)*:**
  - *Shell:* Rail fino de iconos (P1) + Paleta `Ctrl+K` (P7).
  - *Inicio:* Launcher "Buenos días" + tarjetas de alertas (P3) con Bento KPIs (P2).
  - *Comercial:* Conmutador `Kanban (P1)` | `Pipeline 1-Clic (P5)` | `Tabla + Drawer PDF (P1)`.
  - *Materiales:* Tarjetas visuales de stock (P3).
  - *Ajustes:* Configuración limpia en 2 columnas (P5).
- **Híbrido B — "Oficina Multiventana Pro" *(P2 + P4 + P6 + P7)*:**
  - *Shell:* Pestañas de documentos abiertos arriba (P2) + `Ctrl+K` (P7).
  - *Dashboard:* Bento Grid con sparklines (P2) + Calendario con panel IA acoplable opcional (P4).
  - *Tarifas/Presupuestos:* Calculador rápido en vivo (P6) + Ventana PDF flotante (P2).
- **Híbrido C — "Modo Taller Rápido" *(P6 + P1 + P3)*:**
  - Estación oscura CMYK por defecto (P6) con tablero Kanban por técnica (P1) y tarjetas grandes de stock crítico (P3).

---

### 4.3 Posición inicial y Espacio de Consenso (Triunvirato IA)

#### 1) Dictamen de **Google Antigravity** *(Arquitectura, UX y Riesgo)*
- **Viabilidad de implementar las 7 interfaces seleccionables:** **Alta**, siempre que NO se creen 7 copias de cada `*View.java`. Debe definirse un `NavigationShell` único + `ModuleRegistry` + componentes compartidos (`CommandHeader`, `DetailDrawer`, `CommandPaletteOverlay`) y dejar que cada propuesta varíe el **shell** y los **modos de visualización** (Kanban, Pipeline, Tarjetas, Bento, Tabla limpia).
- **Riesgo principal a vigilar:** Mantener intactos `UserPermissions` (un usuario sin permiso `FACTURAS` o `NOMINAS` no debe ver esa tarjeta ni en Bento, ni en Pipeline, ni en `Ctrl+K`), `ModuloWindowManager` (soporte multiventana sin perder estado `dirty`) y cero cambios en SQLite DDL / `AuthService` / `ExportService`.
- **Recomendación de consenso:** Implementar la arquitectura `Multi-Shell` con **7 presets elegibles** en Configuración + **Híbrido A ("Mulberry Estudio Total")** como experiencia recomendada por defecto al arrancar.
- **Veredicto inicial:** `PROPUESTA LISTA PARA CONSENSO`

#### 2) Dictamen pendiente de **Claude Code** *(UI/UX JavaFX, CSS, Coherencia visual y Dirección técnica)*
- **Estado:** `PENDIENTE`
- **Preguntas clave a responder en revisión:**
  1. ¿Qué estructura de `NavigationShell` y tokens CSS en `styles.css` minimiza duplicación entre los 7 layouts y ambos modos (claro/oscuro)?
  2. ¿Validación del `Híbrido A` vs `Híbrido B` como experiencia recomendada y criterios de accesibilidad/legibilidad en JavaFX 21?
  3. ¿Plan de migración gradual por sprints (Shell primero → Comercial unificado/Kanban → Materiales/Config → Limpieza)?

#### 3) Dictamen pendiente de **Codex** *(Rendimiento JavaFX, Persistencia, Concurrencia y Tests JUnit 5)*
- **Estado:** `PENDIENTE`
- **Preguntas clave a responder en revisión:**
  1. Impacto en memoria/arranque al cachear vistas vs recrear `NavigationShell` al cambiar de interfaz en caliente (`PreferenceService`).
  2. Qué consultas DAO agregadas requiere el Pipeline unificado / Bento Grid para no bloquear el hilo JavaFX (`Platform.runLater` / cargas asíncronas).
  3. Cobertura de tests JUnit 5 necesaria para `ModuleRegistry`, filtrado por `UserPermissions` en `Ctrl+K`/Pipeline y persistencia de preferencias.

---

## 5. Alcance para el próximo sprint (tras consenso)

1. Cerrar dictamen de **Claude Code** y **Codex** en este documento y en `.agents/review-ledger.yaml`.
2. Decidir si se incorporan opciones adicionales solicitadas por el usuario antes de congelar el catálogo.
3. Abrir sprint de implementación **Nivel H/M** (por cambio estructural de `MainView`, permisos transversales en `Ctrl+K` y vistas comerciales).
