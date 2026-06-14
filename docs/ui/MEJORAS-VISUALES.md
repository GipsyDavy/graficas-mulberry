# Análisis de mejoras visuales — Gráficas Mulberry

**Fecha:** 2026-06-13
**Autores:** Claude Code (análisis inicial) + Codex (revisión y correcciones)
**Estado:** Sprint UI-E en curso — ver tabla de prioridad para estado por ítem

---

## Correcciones post-revisión Codex

Estas afirmaciones del análisis inicial eran incorrectas:

- ~~Dashboard está inerte~~ → Ya tiene `TranslateTransition` hover en cards.
- ~~ContextMenu sin estilar~~ → `styles.css` ya tiene `.context-menu`.
- ~~Diálogos "desentonan completamente"~~ → `.dialog-pane` y `.alert` ya tienen estilos. Overlay custom sería más moderno pero no es urgente.

Codex recomienda evitar o poner como experimental:
- Glassmorphism sidebar — riesgo rendimiento, puede parecer moda en herramienta de trabajo.
- Hover tilt con perspectiva — bonito en demo, peligro de sentirse "juguete" en ERP.
- Floating labels masivos — coste medio/alto, riesgo de romper formularios.

Orden correcto según Codex: cerrar MIGRACION-COMPLEJA → RELEASE-GATE → INSTALLER-REPRO → después Sprint UI-E completo.

---

## Diagnóstico general

La app tiene base sólida pero conservadora. Sistema de temas correcto, componentes bien definidos, coherencia visual. El problema: se siente como material de 2019-2020. Estática, plana, sin vida.

Lo más llamativo: `VisualAssistantView` tiene `Timeline`, `KeyFrame`, `ScaleTransition`, shake, animaciones de escritura — todo el poder de JavaFX animado está ahí. El resto de la app no lo usa.

Hay 18 instancias de `dropshadow` en `styles.css`, todas estáticas. Solo existe `FadeTransition(220ms)` en cambio de vista y `RotateTransition(180ms)` en flechas de sidebar. Sin `ScaleTransition`, sin `Timeline`, sin `PerspectiveTransform` en ninguna otra vista.

---

## 1. Sistema de elevación

**Situación actual:** 18 `dropshadow` con valores casi idénticos, sin sistema de capas.

**Propuesta — 4 niveles:**

| Nivel | Uso | Blur | Opacidad |
|---|---|---|---|
| 0 | Superficie base | sin sombra | — |
| 1 | Cards en reposo | 4px | 4% |
| 2 | Cards en hover, dropdowns, toasts | 8px | 12% |
| 3 | Diálogos y modales | 20px | 25% |

Efecto: profundidad real sin cambiar un solo color.

---

## 2. Animaciones — lo que falta

Código de referencia disponible en `VisualAssistantView.java`. Replicar en el resto de la app:

### Transición entre vistas
- Actual: `FadeTransition(220ms)` simple
- Mejora: slide + fade combinado — nueva vista entra desde derecha (+30px X) mientras la anterior sale a la izquierda, ambas con fade. 280ms.

### Cards del dashboard
- `ScaleTransition` en hover: 1.0 → 1.02, 120ms ease-out
- Elevación animada con `Interpolator` sobre `dropshadow`

### KPI números animados
- `Timeline` + `KeyFrame` que interpola el número de 0 al valor real en 800ms con curva ease-out
- Patrón ya existe en `escrituraAnimacion` del asistente — reutilizar directamente

### Filas de tabla al cargar
- Aparición escalonada: `FadeTransition` + `TranslateTransition` (+15px Y → 0) con delay de 30ms por fila
- Solo las primeras 10 filas — el resto aparece instantáneo para no penalizar rendimiento

### Shimmer en skeleton loader
- El skeleton existe pero es estático (JavaFX no tiene `@keyframes` CSS)
- Solución: `Timeline` en Java que cicla la opacidad entre 0.4 y 0.9 cada 1200ms
- Patrón: `pulsoVozAnimacion` del asistente

### Shake en campos erróneos
- Ya implementado en `VisualAssistantView` (KeyFrames, translateX -6/6/−4/4/0, 350ms total)
- Copiar exactamente a formularios cuando falla validación

---

## 3. Diálogos y ventanas emergentes

**Situación actual:** `Alert` y `Dialog` nativos de JavaFX. Aspecto de OS antiguo. Desentonan completamente.

**Propuesta — overlay custom:**
- `StackPane` overlay sobre el contenido principal
- Backdrop `rgba(0,0,0,0.45)` con `FadeTransition` de entrada, 180ms
- Panel central con `ScaleTransition`: 0.85x → 1.0x, 220ms, `Interpolator.EASE_OUT`
- Border-radius 12px, elevación nivel 3
- Cierre con click en backdrop + `ScaleTransition` de salida (1.0x → 0.9x) + fade-out

---

## 4. Glassmorphism — sidebar y modales

**Propuesta para el sidebar:**
- Snapshot del contenido detrás + `GaussianBlur`
- Opacidad del panel: 0.88 en lugar de 100%
- Borde interior de 1px con color del tema a 40% de opacidad
- Efecto: sidebar flotante, profundidad real

**Propuesta para toasts y tooltips:**
- Fondo `rgba` con `GaussianBlur` en backdrop
- Borde translúcido de 1px

⚠️ Usar con moderación — solo sidebar y modales. En tablas y formularios sería distractor.

---

## 5. Dashboard

**Situación actual:** tarjetas con número, icono y texto de contexto. Funcional pero inerte.

### Mini sparklines
- `LineChart` o `Canvas` reducido dentro de cada KPI card
- Tendencia de los últimos 7 días
- Sin datos: skeleton animado

### Anillo de progreso
- `Arc` con `lengthProperty` animado — dibuja el porcentaje al cargar
- Alternativa o complemento al número plano

### Color contextual dinámico
- Ventas bajaron vs mes anterior → borde-izquierdo en `-c-danger`
- Ventas subieron → borde-izquierdo en `-c-success`
- Sin texto adicional, feedback inmediato

### Hover tilt en cards
- `setOnMouseMoved` + `Rotate` sobre eje Y de 3-5° siguiendo al cursor
- Efecto "lenticular" — muy moderno, bajo esfuerzo

---

## 6. TableView

**Indicador de fila activa:**
- Barra vertical de 3px en lado izquierdo de la fila seleccionada, color `-c-primary`
- Aparece con `TranslateTransition` desde fuera del borde (desplazamiento X)

**Hover:**
- Color actual + micro-desplazamiento de +1px Y (sensación de levitar)

**Fila expandible:**
- Click → panel de detalle debajo con `FadeTransition` + altura animada
- Evita navegar a otra vista para detalles simples

**Encabezados de columna:**
- Icono de sort con `RotateTransition` 180° al cambiar ASC/DESC

---

## 7. Sidebar

**Sliding pill de navegación:**
- Una `Region` con color `-c-primary` que se desliza verticalmente entre items activos con `TranslateTransition`
- En lugar de solo cambiar color del botón activo
- Efecto reconocible en Linear, Notion, Vercel
- Impacto visual: ★★★★★ — Esfuerzo: Bajo

**Icono animado en click:**
- `ScaleTransition` 1.0 → 1.2 → 1.0, 200ms en el icono del nav item al clickar

**Badge contador:**
- Número de alertas/pendientes con animación "pop" al aparecer
- `ScaleTransition` 0 → 1.2 → 1.0 en el badge

---

## 8. Formularios

**Floating labels (Material Design):**
- El label sube y se reduce cuando el campo tiene foco o contenido
- `TranslateTransition` + `ScaleTransition` en el label
- Reconocible, moderno, reduce clutter visual

**Animated underline en focus:**
- Solo la línea inferior crece desde el centro hacia los extremos al hacer focus
- `Timeline` sobre `prefWidth` de un `Rectangle`
- En lugar de borde completo cambiando color

---

## 9. Identidad visual — artes gráficas

La app sirve a una empresa de impresión y diseño gráfico. La UI no lo refleja.

**Fondo con pattern sutil:**
- Grid de puntos o líneas a 4-6% de opacidad en dashboard y áreas vacías
- Referencia: Figma, Sketch, herramientas de diseño creativo

**Color swatches en configuración de temas:**
- Círculos de color clicables con nombres de tema al estilo paletas Pantone
- En lugar de dropdown genérico

**Empty states con identidad:**
- Ilustraciones que evoquen máquinas de impresión, hojas, tintas
- No SVG genéricos — ilustraciones propias del negocio

**Ripple "efecto tinta" en botones primarios:**
- Ola de color que llena el botón desde el punto de click
- Metáfora de tinta expandiéndose — coherente con la identidad de la empresa

---

## 10. Lo que NO hacer

- ❌ Neumorphism — dated en 2026
- ❌ Animaciones > 400ms en interacciones de usuario — frustra
- ❌ Glassmorphism en tablas y formularios — distractor
- ❌ Gráficos 3D para datos — los peores para leer información
- ❌ Eliminar el asistente visual — es el elemento más animado y carismático. Extender su filosofía al resto

---

## Tabla de prioridad (revisada con Codex)

Orden de Sprint UI-E según prioridad conjunta Claude Code + Codex:

| # | Mejora | Impacto | Esfuerzo | Estado |
|---|---|---|---|---|
| 1 | Sistema de elevación (CSS) | ★★★★★ | Bajo | **HECHO** `a5b6116` — 18 dropshadow → 4 niveles L0/L1/L2/L3 |
| 2 | KPI números animados (Timeline) | ★★★★★ | Bajo | **HECHO** `96ef919` + `aa10dc4` — count-up con fix valor=0 |
| 3 | Animación escalonada filas | ★★★★☆ | Bajo | **HECHO** `7d2ee82` + `3bd6e1e` — fade+slide 260ms, 45ms/fila, Clientes+Facturas+Pedidos |
| 4 | **Slide+fade entre vistas** | ★★★★☆ | Bajo | **HECHO** `0bb8c8b` — 220ms puede subirse a 260ms si se quiere más suave |
| 5 | Shake en campos erróneos | ★★★☆☆ | Mínimo | **HECHO** `38911b3` + fix `6abf2a2` — LoginView, AdminSetupView, UserManagementView; binding translateX para modo mostrar contraseña |
| 6 | Sliding pill sidebar | ★★★★★ | Medio | pendiente — defer hasta RELEASE-GATE |
| 7 | Pattern de fondo dashboard | ★★★☆☆ | Mínimo | **HECHO** `051e778` — Canvas dots 18px step, r=1.2px, opacity=0.05, managed=false |

Para después de RELEASE-GATE + INSTALLER-REPRO (Sprint UI-F):

| Mejora | Notas |
|---|---|
| Overlay dialogs custom | más moderno que Alert nativo, medio esfuerzo |
| Mini sparklines dashboard | requiere datos históricos accesibles |
| Empty states con identidad gráfica | ilustraciones propias del negocio |

Experimental / evitar por ahora (recomendación Codex):

| Mejora | Motivo |
|---|---|
| Glassmorphism sidebar | riesgo rendimiento, no es herramienta de trabajo |
| Hover tilt perspectiva | puede sentirse juguete en ERP |
| Floating labels masivos | riesgo romper formularios existentes |

---

## Notas técnicas de implementación

- Todas las animaciones usan APIs JavaFX puras — sin dependencias externas
- Código de referencia existente en `VisualAssistantView.java`: Timeline, KeyFrame, ScaleTransition, shake, fade, escritura animada
- JavaFX no soporta `@keyframes` CSS ni `transition` CSS — toda animación va en Java
- `GaussianBlur`: efecto disponible en JavaFX, coste de rendimiento moderado — probar en hardware real antes de extender
- `PerspectiveTransform` disponible en JavaFX para efecto tilt de cards
- Shimmer: `Timeline` ciclando opacidad — sin librerías, sin `@keyframes`
