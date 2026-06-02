# interfaz.md — Análisis, Diagnóstico y Plan UI/UX
## Proyecto: Gráficas Mulberry — Desktop JavaFX

---

## INSTRUCCIÓN DE TRABAJO

Este documento rige toda la ejecución de mejoras visuales del proyecto.
El agente que ejecute estas tareas debe comportarse como un **experto senior en diseño gráfico,
diseño de interfaces UI/UX y programación JavaFX**, siguiendo estos principios inamovibles:

1. **Pensar antes de programar**: antes de tocar cualquier archivo, visualizar mentalmente el
   resultado final — cómo se verá en pantalla, cómo interactuará el usuario.
2. **Coherencia visual total**: cada decisión de color, tipografía, espaciado y animación debe
   ser coherente en todos los módulos. El usuario que pase de Clientes a Facturas debe sentir
   que es el mismo producto.
3. **Cambios quirúrgicos**: no refactorizar lo que no esté roto. Solo modificar lo necesario.
4. **Diseño con propósito**: cada elemento visual tiene una función. Nada decorativo sin función.
   Nada funcional sin estética.
5. **Experiencia operacional**: la app es una herramienta de trabajo. Debe sentirse rápida,
   clara y sin ambigüedades. La productividad es la métrica principal.
6. **Validar antes de entregar**: compilar y verificar cada cambio antes de marcar una tarea
   como completada.

---

## PARTE 1 — ESTADO ACTUAL DEL PROYECTO

### 1.1 Stack Visual

```
Java 21 + JavaFX 21
CSS con variables looked-up (sin FXML — layouts construidos en Java)
5 temas CSS × Light/Dark = 10 esquemas de color
Fuente: Segoe UI Variable / Segoe UI (sistema Windows)
Iconos: SVG paths via Shape/Path JavaFX
Animaciones: FadeTransition, TranslateTransition, RotateTransition (JavaFX built-in)
```

### 1.2 Archivos CSS

```
src/main/resources/org/gipsybuho/
├── styles.css               ← estilos base (~1150 líneas) con variables CSS
└── themes/
    ├── theme-mulberry.css   ← morado vino — identidad corporativa (DEFAULT)
    ├── theme-azul.css       ← azul profesional
    ├── theme-verde.css      ← verde productivo
    ├── theme-rojo.css       ← rojo energético
    └── theme-claro.css      ← minimalista light
```

### 1.3 Variables CSS del Sistema de Diseño (verified en styles.css)

```css
/* Colores principales */
-c-primary:         #6B2D5E  (mulberry — morado vino)
-c-primary-dark:    #2D1A28
-c-accent:          #E891D0  (rosa brillante)
-c-muted:           #C4A8BC
-c-bg:              #F5F0F4  (fondo general)
-c-card-bg:         white
-c-border:          #D4B8CC

/* Texto */
-c-text:            #2D1A28
-c-text-secondary:  #7A5A72
-c-text-muted:      #7A5A72

/* Sidebar */
-c-sidebar-logo:    #1A0F18
-c-sidebar-sep:     #4A2A42
-c-sidebar-text:    #D4B8CC
-c-sidebar-nav-text: #C4A8BC
-c-sidebar-version-text: #7A5A72
-c-nav-hover:       #3D2038

/* Filas de tablas */
-c-row-hover:       #F8EEF6
-c-row-selected:    #F0E6EF
-c-row-alt:         #FBF6FA
-c-row-border:      #F0E8EE
-c-tab-bg:          #EDE3EB

/* Modo oscuro — todos los -c-* redefinidos */
```

### 1.4 Componentes CSS disponibles (verificados en styles.css)

| Componente | Clase CSS | Estado |
|-----------|-----------|--------|
| Sidebar | `.sidebar`, `.nav-btn`, `.nav-btn-active`, `.nav-btn-pane` | ✅ Completo |
| Sidebar colapsado | `.sidebar-collapsed` | ✅ Implementado |
| Buscador sidebar | `.sidebar-search` | ✅ Implementado |
| Grupos nav | `.nav-group`, `.nav-group-header`, `.nav-group-title` | ✅ Implementado |
| Footer sidebar | `.sidebar-footer-icons`, `.sidebar-footer-btn` | ✅ Implementado |
| Iconos SVG | `.nav-icon`, `.content-icon` | ✅ Implementado |
| Tooltips | `.tooltip` | ✅ Estilizados |
| Tablas | `.data-table` + rows completo | ✅ Completo |
| Formularios | `.text-field`, `.text-area`, `.combo-box` | ✅ Estilizados |
| Validación visual | `.input-error`, `.field-error-msg` | ✅ Bloque 9 |
| Status badges | `.status-badge-*` (success/warning/danger/info/neutral) | ✅ Bloque 5 |
| Toast notifications | `.toast`, `.toast-success/warning/error/info` | ✅ Bloque 8 |
| Empty states | `.empty-state`, `.empty-state-icon`, `.empty-state-text` | ✅ Bloque 7 |
| Dashboard cards | `.dashboard-card`, `.card-title`, `.card-value`, `.card-context` | ✅ Bloque 4 |
| Avisos dashboard | `.avisos-panel`, `.aviso-hoy/urgente/proximo` | ✅ Bloque 4 |
| CommandBar | `.command-bar` | ✅ Bloque 3 |
| Botones toolbar | `.btn-toolbar` (light + dark) | ✅ Bloque 3 |
| Configuración | `.tema-card`, `.tema-card-activa`, `.config-*` | ✅ Completo |
| Calendario | `.cal-*` (dia, hoy, nota, nav) | ✅ Completo |
| Asistente visual | `.visual-assistant-*` | ✅ Completo |
| Gráficas (charts) | `.chart-*`, `.axis` | ✅ Estilizados |
| Tabs | `.tab-pane .tab-*` | ✅ Estilizados |
| Scrollbars | `.scroll-bar .thumb/track` | ✅ Estilizados |
| Checkboxes | `.check-box` | ✅ Estilizados |
| Diálogos / Alertas | `.dialog-pane`, `.alert` | ✅ Estilizados |

### 1.5 Sprint UI/UX Bloques 1-10 — COMPLETADOS (2026-05-xx)

| Bloque | Commit | Contenido implementado |
|--------|--------|------------------------|
| 1 | `7ab849d` + `f9196fb` | Iconos SVG en nav. Reemplazar emojis en KPI y avisos. |
| 2 | `3dedb9b` | Tipografía mejorada e inputs con estilos uniformes. |
| 3 | `ebfdbcb` + `0b69b11` | CommandBar y btn-toolbar estandarizados en todas las vistas. |
| 4 | `44e5f2a` | Dashboard FlowPane + tarjeta KPI con contexto. |
| 5 | `3745770` | Status badges en columnas de estado (pills). |
| 6 | `d842cb7` | Sidebar colapso/expansión + búsqueda Ctrl+K. |
| 7 | `043608b` | Empty states en 9 tablas principales. |
| 8 | `7e219c0` | ToastService con animación fade y auto-dismiss. |
| 9 | `49d70a5` | Validación visual en formularios (.input-error). |
| 10 | `2fedf46` | Micro-interacciones fade y hover lift. |

**Post-polish (4 commits después del Bloque 10):**
- `6561d3e` — Sidebar: iconos 18px, grupos expandidos por defecto, bordes redondeados.
- `b3b4de5` — Refactor arquitectura navegación sidebar (código más limpio).
- `ea2960e` — Tooltips en todos los módulos, navegación y botones de footer.
- `1fb1904` — Fix: restaurar botón Exportar base de datos en footer.

---

## PARTE 2 — DIAGNÓSTICO COMPLETO DE PROBLEMAS

### ⚠️ ADVERTENCIA: diagnóstico parcial

Los problemas listados en esta sección combinan lo que se ha podido verificar
leyendo el código CSS y la estructura del proyecto, y lo que requiere inspección
visual en tiempo de ejecución o lectura de los archivos de vistas individuales.
Los items marcados con `[VERIFICAR]` deben confirmarse antes de implementar.

---

### 2.1 Problemas Críticos 🔴 (bloquean coherencia o funcionalidad visual)

| ID | Problema | Verificado | Archivo probable |
|----|----------|-----------|-----------------|
| C-1 | `styles.css` tiene variables de tooltip hardcodeadas (`#2D1A28`, `#D4B8CC`, `#4A2A42`) que no usan las variables `-c-*`. En modo oscuro y otros temas el tooltip no cambia. | ✅ Código | `styles.css:360-370` |
| C-2 | Validación de formularios (`input-error`) no tiene indicador de éxito (`.input-success`). El usuario no sabe cuándo un campo queda correcto. | ✅ Código | `styles.css:503-516` |
| C-3 | Status badges usan colores hardcodeados (`#27AE60`, `#E74C3C`, etc.) desconectados del sistema de temas. Pueden chocar con ciertos temas. | ✅ Código | `styles.css:890-894` |

### 2.2 Problemas Altos 🟠 (degradan la experiencia)

| ID | Problema | Verificado | Archivo probable |
|----|----------|-----------|-----------------|
| A-1 | `theme-mulberry.css` sobreescribe `.text-field` y `.text-area` con `background-color: -c-border, -c-card-bg` usando background layers. Puede causar inconsistencia con el selector de `styles.css`. | ✅ Código | `theme-mulberry.css:60-65` |
| A-2 | `theme-mulberry.css` sobreescribe `config-section-title` con `color: -c-primary` en lugar de `-c-text`. Inconsistencia de semántica de color entre temas. | ✅ Código | `theme-mulberry.css:29-33` |
| A-3 | No existe clase CSS para estado de carga (skeleton loader / spinner) en tablas. Cuando una tabla carga datos lentamente, no hay feedback visual. | ✅ Código | `styles.css` (ausente) |
| A-4 | Botones `.button` genérico solo tiene `opacity: 0.88` en hover — sin color real de hover. Contraste bajo en algunos temas. | ✅ Código | `styles.css:580-593` |
| A-5 | No hay clase CSS para el estado "seleccionado" de los `.btn-toolbar`. Si un filtro está activo, no hay indicador visual diferente. | ✅ Código | `styles.css` (ausente) |
| A-6 | [VERIFICAR] ¿Los módulos que reciben `Bloque 3 (CommandBar)` tienen un espaciado vertical uniforme entre la CommandBar y la tabla? | [VERIFICAR] | Vistas individuales |
| A-7 | [VERIFICAR] ¿La vista de Estadísticas muestra correctamente los gráficos en modo oscuro con los colores de tema? | [VERIFICAR] | `EstadisticasView.java` |
| A-8 | `IAView.java` no tiene CommandBar (`.btn-toolbar`) ni empty state (`.empty-state`) en su construcción. Verificado en las primeras 80 líneas por Codex — puede existir más adelante pero es improbable que no sea visible en el constructor. Módulo incompleto visualmente. | ✅ Parcial | `IAView.java` |

### 2.3 Problemas Medios 🟡 (polish)

| ID | Problema | Verificado |
|----|----------|-----------|
| P-1 | No hay transición animada entre vistas principales (cambio de módulo). El contenido cambia abruptamente. Coste alto (JavaFX Transition). | [VERIFICAR] |
| P-2 | No hay clase CSS para "fila de formulario" consistente. Cada vista puede tener márgenes/padding distintos. Sin cuadrícula de espaciado 4px/8px. | [VERIFICAR] |
| P-3 | `DatePicker` JavaFX no estilizado. Usa el look del sistema. Inconsistente. Gemini lo marca como crítico para un ERP. | ✅ Código (ausente) |
| P-4 | `ProgressBar` y `ProgressIndicator` sin estilos. Aspecto anticuado. | ✅ Código (ausente) |
| P-5 | `RadioButton` sin estilos en `styles.css`. Rompe coherencia en formularios. | ✅ Código (ausente) |
| P-6 | `ContextMenu` sin estilar. Los menús contextuales usan look del sistema, desentonan. Identificado por Gemini. | ✅ Código (ausente) |
| P-7 | [VERIFICAR] ¿Las columnas de tablas tienen anchuras mínimas definidas para evitar truncado en ventanas pequeñas? | [VERIFICAR] |
| P-8 | [VERIFICAR] ¿El módulo de Nóminas tiene empty state? | [VERIFICAR] |

### 2.4 Anti-patrón detectado — Riesgo futuro ⚠️

| ID | Problema | Verificado |
|----|----------|-----------|
| AP-1 | `theme-mulberry.css` sobreescribe estilos de **componentes** (`.text-field`, `.tab`, `config-section-title`) en lugar de solo sobreescribir variables `.root`. **Si otros temas siguen este patrón, se pierde la ventaja de centralización en `styles.css` y se duplican estilos.** Gemini confirma que los archivos de tema deben modificar SOLO variables, no componentes. Los temas azul/verde/rojo/claro no cometen este error — solo `theme-mulberry.css`. | ✅ Código |

---

## PARTE 3 — SISTEMA DE TEMAS

### 3.1 Arquitectura actual (verificada)

```
styles.css          ← variables por defecto (= tema mulberry light)
  + tema-X.css      ← sobreescribe solo .root con sus valores de color
    + .modo-oscuro  ← clase en .root que sobreescribe para dark mode
```

El sistema es correcto en su arquitectura. Los problemas son de implementación
(tooltips hardcodeados, status badges hardcodeados).

### 3.2 Temas disponibles — VERIFICADOS POR CODEX (2026-06-02)

| Tema | Color primario | Variables completas | Estado |
|------|---------------|---------------------|--------|
| mulberry | #6B2D5E (morado vino) | ✅ Completas | ✅ Verificado |
| azul | #1A56A6 (azul profesional) | ✅ Completas | ✅ Verificado |
| verde | #2D6A4F (verde bosque) | ✅ Completas | ✅ Verificado |
| rojo | #A03030 (rojo corporativo) | ✅ Completas | ✅ Verificado |
| claro | #4A5568 (gris neutro) | ✅ Completas | ✅ Verificado |

**Resultado:** todos los temas definen las 16 variables requeridas. El sistema de temas
está completo y coherente. No hay deuda técnica en los archivos CSS de temas.

### 3.3 Tipografía actual

| Tipo | Fuente | Tamaño | Evaluación |
|------|--------|--------|------------|
| Base | Segoe UI Variable / Segoe UI | 14px | Correcto para Windows |
| Títulos `.view-title` | idem | 22px bold | Bueno |
| Subtítulos `.view-subtitle` | idem | 13px | Bueno |
| Celdas de tabla | idem | 13px | Funcional |
| Labels configuración | idem | 14px bold | Correcto |
| Sidebar nav | idem | 13px | Correcto |
| Toast | idem | 13px | Correcto |

**Evaluación general:** La tipografía es funcional pero genérica. No hay jerarquía
diferenciada entre familia serif y sans-serif. Para una app de artes gráficas con
identidad visual propia, considerar añadir una fuente de display para títulos en futuros sprints.
**No implementar hasta solicitud explícita.**

---

## PARTE 4 — PLAN DE IMPLEMENTACIÓN

### SPRINT UI-A — Correcciones CSS (solo styles.css + theme-mulberry.css)

**Objetivo**: corregir coherencia del sistema de temas y añadir componentes faltantes.
**Coste bajo-medio. Solo CSS. Sin tocar Java. Sin riesgo de regresión en tests.**
**Priorización validada por Gemini (2026-06-02).**

| Tarea | Archivo | Descripción | Coste | Prioridad |
|-------|---------|-------------|-------|-----------|
| UA-1 | `styles.css` | Tooltip: reemplazar hex hardcodeados por `-c-primary-dark`, `-c-sidebar-text`, `-c-sidebar-sep` | Bajo | 🔴 Alta |
| UA-2 | `styles.css` | Status badges: añadir variables `-c-status-success/warning/danger/info/neutral` en `.root` y usar en `.status-badge-*` | Bajo | 🔴 Alta |
| UA-3 | `styles.css` | Añadir `.btn-toolbar-active` (fondo `-c-primary`, texto blanco, borde `-c-primary`) | Bajo | 🟠 Media |
| UA-4 | `styles.css` | Añadir `.input-success` (borde verde, texto feedback positivo) | Bajo | 🟠 Media |
| UA-5 | `styles.css` | Añadir estilos `DatePicker` coherentes con `.text-field` | Medio | 🟠 Media |
| UA-6 | `styles.css` | Añadir estilos `ProgressBar` / `ProgressIndicator` | Bajo | 🟡 Media |
| UA-7 | `styles.css` | Añadir estilos `RadioButton` | Bajo | 🟡 Media |
| UA-8 | `styles.css` | Añadir estilos `ContextMenu` (identificado por Gemini) | Medio | 🟡 Media |
| UA-9 | `theme-mulberry.css` | Mover estilos de componentes (`.text-field`, `.tab`, `.config-section-title`) a `styles.css` con variables. El tema solo debe definir variables `.root`. | Medio | 🟠 Media (anti-patrón AP-1) |

### SPRINT UI-B — Correcciones de código rápidas (verificado en Fase 5)

**Objetivo**: correcciones menores en Java tras la auditoría de código.
**NOTA:** La transición entre vistas YA EXISTE — `mostrarVista()` tiene `FadeTransition(150ms)`.
No hay que crearla, solo ajustar su duración.

| Tarea | Archivo | Descripción | Esfuerzo |
|-------|---------|-------------|----------|
| UB-1 | `MainView.java:560` | Cambiar `Duration.millis(150)` → `Duration.millis(220)` | 5 min |
| UB-2 | 9 vistas con `btn()` | Eliminar parámetro `color` de `btn()` — es dead code ignorado | 30 min |
| UB-3 | `MainView.java:245` | Versión leída desde `AppConstants.APP_VERSION` (constante faltante) | 20 min |
| UB-4 | `MainView.java:488` | `FadeTransition(150ms)` en expand de grupos de navegación | 20 min |

### SPRINT UI-C — Auditoría visual de módulos [REQUIERE VERIFICACIÓN VISUAL]

**Objetivo**: revisar que todos los módulos tienen CommandBar, empty state y estilos uniformes.

Módulos a auditar visualmente:
- `IAView.java` — ¿tiene CommandBar? ¿empty state? ¿estilos específicos?
- `EstadisticasView.java` — ¿gráficas en modo oscuro correctas?
- `NominasView.java` — ¿empty state? ¿status badges en estado de nómina?
- `CalendarioView.java` — ¿integración con el sistema de temas correcta?
- `UserManagementView.java` — ¿CommandBar? ¿empty state?
- `ConfiguracionView.java` — ¿coherencia con el resto de vistas?
- `VisualAssistantConfigView.java` — ¿estilos propios o hereda de styles.css?

### SPRINT UI-D — Skeleton loading / spinner en tablas

**Objetivo**: mostrar feedback visual durante la carga de datos en TableView.

| Tarea | Descripción |
|-------|-------------|
| UD-1 | Añadir clase CSS `.skeleton-row` con animación shimmer |
| UD-2 | Implementar un `ProgressIndicator` superpuesto en tablas durante carga |

---

## PARTE 5 — CRITERIOS DE CALIDAD

### Por sprint

- `.\mvnw.cmd clean compile` — BUILD SUCCESS obligatorio.
- `.\mvnw.cmd test` — todos los tests en verde (sin regresiones).
- Verificación visual del módulo modificado en modo claro y modo oscuro.
- Verificación en el tema mulberry (primario) y al menos un tema alternativo.

### Checklist de coherencia visual

- [ ] Todos los módulos tienen `CommandBar` con `.btn-toolbar`.
- [ ] Todos los módulos con lista tienen `empty state` con icono SVG y texto descriptivo.
- [ ] Todos los módulos con columna de estado usan `status badge`.
- [ ] Todos los campos de formulario usan `.text-field` / `.text-area` / `.combo-box` estilizados.
- [ ] Tooltips funcionan en modo oscuro (sin hardcode de hex).
- [ ] `DatePicker` estilizado coherentemente con el resto de inputs.
- [ ] `ProgressBar` / `ProgressIndicator` estilizados.
- [ ] Transiciones entre vistas (fadeIn al cambiar módulo).

---

## PARTE 6 — AUDITORÍA POST-BLOQUE 10

### 6.1 Lo que se implementó bien

- El sistema de temas CSS con variables looked-up es sólido y extensible.
- La separación de `styles.css` (base) + `theme-X.css` (sobreescritura de variables) es la arquitectura correcta.
- Los iconos SVG en navegación eliminaron la dependencia de fuentes externas.
- El ToastService con fade es reutilizable y bien abstraído en `service/ToastService.java`.
- Los status badges son semánticamente correctos (color por significado, no por capricho).
- El sidebar con grupos colapsables y búsqueda Ctrl+K mejora significativamente la navegación.

### 6.2 Lo que quedó pendiente / puede mejorar

- **Tooltip hardcodeados**: un problema estructural que rompe la coherencia de temas.
- **Sin estado de carga visual en tablas**: experiencia percibida como "congelada" durante cargas.
- **Sin transición entre vistas**: cambio abrupto entre módulos.
- **`DatePicker`, `ProgressBar`, `RadioButton` sin estilos**: elementos que rompen la coherencia.
- **Sin `.btn-toolbar-active`**: no se puede indicar visualmente un filtro activo.
- **Verificación visual pendiente** de los módulos menos frecuentes (IA, Estadísticas, etc.).

### 6.3 Próximo sprint UI recomendado

**Sprint UI-A** validado por Gemini + Codex (2026-06-02):

Orden de ejecución recomendado por impacto/coste:
1. **UA-1**: Tooltip hex hardcodeados → variables CSS. *(Crítico, 15 min)*
2. **UA-2**: Status badges hardcodeados → variables CSS. *(Crítico, 20 min)*
3. **UA-3**: `.btn-toolbar-active`. *(Bajo, 10 min)*
4. **UA-4**: `.input-success`. *(Bajo, 10 min)*
5. **UA-9**: Sanear `theme-mulberry.css` (mover componentes a `styles.css`). *(Medio, 30 min)*
6. **UA-5**: `DatePicker`. *(Medio, 45 min — selectores JavaFX complejos)*
7. **UA-6+UA-7**: `ProgressBar` + `RadioButton`. *(Bajo, 20 min)*
8. **UA-8**: `ContextMenu`. *(Medio, 30 min)*

Coste total estimado: **3-4 horas**, solo CSS. Sin tocar Java. Sin riesgo de regresión.

### 6.4 Validación de arquitectura CSS por Gemini

✅ **Confirmado**: el patrón variables looked-up + theme files es correcto para JavaFX.
Es equivalente a CSS Custom Properties en web y es el enfoque óptimo.

⚠️ **Riesgo confirmado**: `theme-mulberry.css` sobreescribe componentes (no solo variables).
Los otros 4 temas están limpios. Debe corregirse antes de añadir más temas.

### 6.5 Componentes JavaFX críticos para un ERP (lista Gemini)

Por impacto en percepción de calidad cuando no están estilizados:
1. **DatePicker** — crítico, muy visible en formularios con fechas
2. **TableView** (detalles internos, scrollbars) — ya parcialmente cubierto
3. **ContextMenu** — menús contextuales usan look del sistema
4. **Alert / Dialog** — parcialmente cubierto en `styles.css`
5. **ProgressBar** — aspecto anticuado sin estilo
6. **RadioButton / CheckBox** — pequeños pero rompen coherencia

> **Verificaciones ejecutadas (2026-06-02):** Gemini validó arquitectura CSS y priorizó mejoras.
> Codex confirmó que todos los temas tienen las 16 variables requeridas y que IAView no tiene CommandBar.
> Resultados incorporados en las secciones anteriores.

---

*interfaz.md — Gráficas Mulberry — 2026-06-02*
