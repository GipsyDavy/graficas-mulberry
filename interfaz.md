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

| ID | Problema | Estado |
|----|----------|--------|
| C-1 | Tooltip hex hardcodeados en `styles.css` | ✅ RESUELTO — Sprint UI-A UA-1 (`467a2b5`) |
| C-2 | `.input-success` ausente — usuario no ve confirmación visual | ✅ RESUELTO — Sprint UI-A UA-4 (`cc0b2c2`) |
| C-3 | Status badges hardcodeados, desconectados del sistema de temas | ✅ RESUELTO — Sprint UI-A UA-2 (`467a2b5`) |

### 2.2 Problemas Altos 🟠 (degradan la experiencia)

| ID | Problema | Estado |
|----|----------|--------|
| A-1 | `theme-mulberry.css` sobreescribía `.text-field` y `.text-area` | ✅ RESUELTO — Sprint UI-A UA-9 (`5718971`) |
| A-2 | `theme-mulberry.css` sobreescribía `config-section-title` | ✅ RESUELTO — Sprint UI-A UA-9 (`5718971`) |
| A-3 | No existe clase CSS para estado de carga (skeleton loader) en tablas | ✅ RESUELTO — Sprint UI-D (`d4109c2`) |
| A-4 | Botón genérico sin color real en hover | 🟠 ABIERTO — bajo impacto |
| A-5 | No hay `.btn-toolbar-active` para estado seleccionado de filtros | ✅ RESUELTO — Sprint UI-A UA-3 (`cc0b2c2`) |
| A-6 | Espaciado vertical uniforme entre CommandBar y tabla | [VERIFICAR] — requiere inspección visual |
| A-7 | Estadísticas en modo oscuro con colores de tema | [VERIFICAR] — requiere inspección visual |
| A-8 | `IAView.java` sin CommandBar ni estilos del sistema | ✅ RESUELTO — Sprint UI-C (`34bca4c`) |

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
| AP-1 | `theme-mulberry.css` sobreescribía estilos de componentes en lugar de solo variables | ✅ RESUELTO — Sprint UI-A UA-9 (`5718971`). Los 5 temas ahora solo definen variables `.root`. |

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

### SPRINT UI-A — Correcciones CSS ✅ COMPLETO (2026-06-03)

Commits: `467a2b5`, `cc0b2c2`, `5718971`

| Tarea | Estado | Commit |
|-------|--------|--------|
| UA-1 Tooltip hex → variables CSS | ✅ | `467a2b5` |
| UA-2 Status badges → `-c-status-*` | ✅ | `467a2b5` |
| UA-3 `.btn-toolbar-active` | ✅ | `cc0b2c2` |
| UA-4 `.input-success` | ✅ | `cc0b2c2` |
| UA-5 DatePicker styles | ✅ | `cc0b2c2` |
| UA-6 ProgressBar/Indicator | ✅ | `cc0b2c2` |
| UA-7 RadioButton | ✅ | `cc0b2c2` |
| UA-8 ContextMenu | ✅ | `cc0b2c2` |
| UA-9 Sanear theme-mulberry.css (anti-patrón AP-1) | ✅ | `5718971` |

### SPRINT UI-B — Correcciones de código rápidas ✅ COMPLETO (2026-06-03)

Commits: `632e355`, `ef7ec17`

| Tarea | Estado | Commit |
|-------|--------|--------|
| UB-1 FadeTransition 150→220ms en `mostrarVista` | ✅ | `ef7ec17` |
| UB-2 Eliminar param `color` de `btn()` en 9 vistas | ✅ | `632e355` |
| UB-3 Versión desde `AppConstants.APP_VERSION` | ✅ | `ef7ec17` |
| UB-4 FadeTransition 150ms en expand de nav groups | ✅ | `ef7ec17` |

### SPRINT UI-C — Auditoría visual de módulos ✅ COMPLETO (2026-06-03)

Commit: `34bca4c`

| Módulo | Resultado |
|--------|-----------|
| `IAView.java` | ✅ `command-bar` añadida; `btn-toolbar` en botones; todos los `setStyle()` hardcodeados reemplazados por clases CSS; burbujas y estados del sistema de temas |
| `EstadisticasView.java` | ✅ `command-bar` + `btn-toolbar`; hex `#6B2D5E` eliminado |
| `NominasView.java` | ✅ Ya tenía `command-bar` y `empty-state` — sin cambios necesarios |
| `UserManagementView.java` | ✅ `command-bar` + `btn-toolbar`; `showSuccess/showError` usan `field-success-msg/field-error-msg` |
| `CalendarioView.java` | Sin cambios — `setStyle()` son para contenido dinámico programático (justificado) |
| `ConfiguracionView.java` | Sin cambios — `setStyle()` para previsualización de temas (justificado) |
| `VisualAssistantConfigView.java` | Sin cambios — hereda de `styles.css` correctamente |

### SPRINT UI-D — Skeleton loading / spinner en tablas

**Objetivo**: mostrar feedback visual durante la carga de datos en TableView.

| Tarea | Descripción | Estado |
|-------|-------------|--------|
| UD-1 | Añadir clase CSS `.skeleton-row` con animación shimmer | ✅ `d4109c2` — gradiente estático (JavaFX no soporta @keyframes) |
| UD-2 | Implementar un `ProgressIndicator` superpuesto en tablas durante carga | ✅ `d4109c2` — Clientes, Facturas, Pedidos |

---

## PARTE 5 — CAPA DE AYUDA INTEGRADA

### 5.1 Objetivo

Incorporar una capa completa de ayuda al usuario dentro de la aplicación: documentación
integrada, guías paso a paso, manuales, ayuda contextual, tooltips, onboarding, ejemplos,
preguntas frecuentes y asistencia para entender cada funcionalidad sin depender de
soporte externo.

La ayuda debe formar parte de la experiencia de usuario, no ser un añadido externo.
Cada pantalla debe explicar lo necesario en el momento adecuado, con el nivel de detalle
adecuado y sin bloquear el trabajo de usuarios expertos.

### 5.2 Componentes previstos

| Componente | Objetivo | Estado |
|---|---|---|
| Centro de ayuda | Punto único para buscar y navegar documentación dentro de la app | Pendiente |
| Ayuda contextual por pantalla | Explicar acciones, campos, estados y flujos desde cada módulo | Pendiente |
| Manual de usuario integrado | Manual offline mantenido con el comportamiento real de la aplicación | Pendiente |
| Guías paso a paso | Acompañar tareas principales: crear cliente, factura, pedido, importar CSV, exportar PDF | Pendiente |
| Primer arranque / onboarding | Orientar a usuarios nuevos en la primera ejecución o primer acceso a módulos clave | Pendiente |
| Tooltips avanzados | Sustituir tooltips pobres por ayuda breve, específica y accionable | Pendiente |
| Ejemplos de uso | Mostrar casos reales o plantillas de ejemplo para reducir ambigüedad | Pendiente |
| FAQ | Resolver dudas frecuentes sin soporte externo | Pendiente |
| Glosario de formatos | Explicar CSV, PDF, Excel, NIF, IVA, estados, roles, importes y columnas esperadas | Pendiente |
| Riesgos y advertencias | Explicar consecuencias antes de importaciones, borrados, cambios de rol o datos sensibles | Pendiente |
| Documentación offline | Empaquetar la ayuda con la aplicación, sin depender de internet | Pendiente |
| Buscador de ayuda | Buscar por módulo, palabra clave, acción, error o formato | Pendiente |
| Enlaces desde errores | Conectar mensajes de error con soluciones o guías relacionadas | Pendiente |
| Modo principiante / avanzado | Ajustar densidad de ayuda según experiencia del usuario | Pendiente |

### 5.3 Reglas de diseño

- La ayuda debe ser contextual: mostrar lo útil en la pantalla donde surge la duda.
- Los mensajes de error deben decir qué pasó, por qué importa y cómo corregirlo.
- Las advertencias deben explicar riesgos reales, no asustar por defecto.
- El modo avanzado no debe ocultar información crítica de seguridad o pérdida de datos.
- La documentación debe mantenerse offline y versionada con la app.
- Cada sprint funcional futuro debe incluir su impacto en esta capa de ayuda.

---

## PARTE 6 — CRITERIOS DE CALIDAD

### Por sprint

- `.\mvnw.cmd clean compile` — BUILD SUCCESS obligatorio.
- `.\mvnw.cmd test` — todos los tests en verde (sin regresiones).
- Verificación visual del módulo modificado en modo claro y modo oscuro.
- Verificación en el tema mulberry (primario) y al menos un tema alternativo.
- Verificación de ayuda al usuario: documentación integrada, ayuda contextual,
  tooltips avanzados, guía paso a paso, FAQ, glosario, onboarding o manual cuando aplique.

### Checklist de coherencia visual

- [x] Todos los módulos principales tienen `CommandBar` con `.btn-toolbar` — Sprint UI-C.
- [x] Todos los módulos con lista tienen `empty state` — Bloque 7 + Sprint UI-C.
- [x] Todos los módulos con columna de estado usan `status badge` — Bloque 5.
- [x] Todos los campos de formulario usan `.text-field` / `.text-area` / `.combo-box` estilizados.
- [x] Tooltips funcionan en modo oscuro (sin hardcode de hex) — Sprint UI-A UA-1.
- [x] `DatePicker` estilizado coherentemente con el resto de inputs — Sprint UI-A UA-5.
- [x] `ProgressBar` / `ProgressIndicator` estilizados — Sprint UI-A UA-6.
- [x] Transiciones entre vistas (fadeIn al cambiar módulo) — Sprint UI-B UB-1.
- [x] Estado de carga visual en tablas (skeleton loader) — Sprint UI-D (`d4109c2`).
- [ ] Capa completa de ayuda integrada por módulo — centro de ayuda, manual, guías, FAQ, buscador y ayuda contextual.
- [ ] Verificación visual en modo oscuro con cada tema — requiere `mvn javafx:run`.

---

## PARTE 7 — AUDITORÍA POST-BLOQUE 10

### 7.1 Lo que se implementó bien

- El sistema de temas CSS con variables looked-up es sólido y extensible.
- La separación de `styles.css` (base) + `theme-X.css` (sobreescritura de variables) es la arquitectura correcta.
- Los iconos SVG en navegación eliminaron la dependencia de fuentes externas.
- El ToastService con fade es reutilizable y bien abstraído en `service/ToastService.java`.
- Los status badges son semánticamente correctos (color por significado, no por capricho).
- El sidebar con grupos colapsables y búsqueda Ctrl+K mejora significativamente la navegación.

### 7.2 Estado post-Sprint UI-A/B/C/D (2026-06-03)

**Resuelto:**
- ✅ Tooltip hardcodeados → variables CSS (UA-1)
- ✅ Status badges → variables CSS (UA-2)
- ✅ `.btn-toolbar-active` añadido (UA-3)
- ✅ `.input-success` añadido (UA-4)
- ✅ DatePicker, ProgressBar, RadioButton, ContextMenu estilizados (UA-5 a UA-8)
- ✅ `theme-mulberry.css` saneado — solo variables (UA-9)
- ✅ Transición entre vistas 220ms (UB-1)
- ✅ IAView con `command-bar` y sistema de temas completo (UI-C)
- ✅ `.skeleton-row` CSS + `ProgressIndicator` overlay en Clientes, Facturas, Pedidos (UI-D, `d4109c2`)

**Pendiente:**
- Verificación visual en modo oscuro con cada tema (requiere `mvn javafx:run`).

### 7.3 Próximo sprint UI recomendado

Sin sprints UI inmediatos pendientes. Para próximos sprints funcionales, incorporar
**la capa completa de ayuda integrada**: centro de ayuda, ayuda contextual, manual,
guías paso a paso, onboarding, tooltips avanzados, ejemplos, FAQ, glosario, buscador,
enlaces desde errores a soluciones, documentación offline y modo principiante/avanzado.
Cola activa técnica: **Refactor B2** (inyectar Connection en DAOs — largo plazo, amplio impacto).

### 7.4 Validación de arquitectura CSS por Gemini

✅ **Confirmado**: el patrón variables looked-up + theme files es correcto para JavaFX.
Es equivalente a CSS Custom Properties en web y es el enfoque óptimo.

⚠️ **Riesgo confirmado**: `theme-mulberry.css` sobreescribe componentes (no solo variables).
Los otros 4 temas están limpios. Debe corregirse antes de añadir más temas.

### 7.5 Componentes JavaFX críticos para un ERP (lista Gemini)

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

*interfaz.md — Gráficas Mulberry — 2026-06-03 — Sprints UI-A/B/C completados*
