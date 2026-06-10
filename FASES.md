# FASES.md — Plan de Trabajo: Auditoría y Adaptación Graficas Mulberry

Fecha de creación: 2026-06-02  
Proyecto: Graficas Mulberry (`C:\Users\GipsyDavy\MAVEN\Graficas Mulberry`)  
Agente líder: Claude Code  
Agentes de apoyo: Codex (IDE), Gemini (IDE)

> **Estado documental:** histórico cerrado. Este archivo describe la auditoría de 2026-06-02.
> Para estado vigente, HEAD actual, tests y cola activa, usar `Resumen.md`,
> `continuar.md`, `interfaz.md` y `MACRO-PROMPT-GRAFICAS-MULBERRY.md`.

---

## Objetivo general

1. Leer y analizar los archivos `.md` de referencia de los proyectos **Recetas Familiares** y **Nemeterial**.
2. Adaptar esos archivos al proyecto **Graficas Mulberry** y crear los que falten, incluyendo `MACRO-PROMPT-GRAFICAS-MULBERRY.md`.
3. Realizar una **auditoría completa** del proyecto como senior developer y experto en ciberseguridad: código, arquitectura, seguridad, vulnerabilidades, código sucio, diseño gráfico, UI/UX, animaciones, frontend y backend.
4. Apoyarse en **Codex** y **Gemini** vía bloques IDE (el usuario los copia y pega en el chat del IDE correspondiente).
5. Avanzar **fase por fase** con autorización explícita del usuario antes de cada una.

---

## Regla de sinceridad técnica (máxima prioridad)

> **Si no estás seguro, dilo claramente: "no lo sé" o "no puedo verificarlo".**  
> No inventar datos, causas, estados, rutas, APIs, resultados de tests ni conclusiones.  
> No hacer perder tiempo al usuario con información no verificada presentada como hecho.  
> Priorizar sinceridad técnica sobre aparentar certeza.

---

## Fases

### Fase 1 — Lectura y análisis de archivos `.md` de referencia ✅ COMPLETADA
- Leer los archivos de referencia en los proyectos `Recetas Familiares` y `Nemeterial`:
  - `interfaz.md`
  - `continuar.md`
  - `resumen.md`
  - `MACRO-PROMPT-RECETAS-FAMILIA.md`
  - `MACRO-PROMPT-NEMETERIAL.md`
  - `CLAUDE.md` (si existe en esos proyectos)
- Entender su estructura, propósito y convenciones para adaptarlas al proyecto actual.
- **Estado:** ✅ Completada (2026-06-02)

---

### Fase 2 — Lectura y análisis del proyecto Graficas Mulberry ✅ COMPLETADA
- Explorar la estructura completa del proyecto: Java, JavaFX, SQLite, Maven, recursos, tests, scripts.
- Identificar módulos principales, dependencias, convenciones de código y estado actual del proyecto.
- **Estado:** ✅ Completada (2026-06-02)

---

### Fase 3 — Creación y adaptación de archivos `.md` ✅ COMPLETADA
- Crear o adaptar al proyecto los siguientes archivos:
  - `interfaz.md` — guía de interfaz gráfica adaptada a Graficas Mulberry
  - `continuar.md` — instrucciones de continuación de sesión adaptadas
  - `resumen.md` — resumen del estado del proyecto adaptado
  - `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — macro-prompt principal adaptado al proyecto
- Respetar el estilo y propósito de los archivos de referencia.
- **Estado:** ✅ Completada (2026-06-02)

---

### Fase 4 — Auditoría técnica: código, arquitectura, seguridad ✅ COMPLETADA
- Revisar el código fuente como senior developer + experto en ciberseguridad.
- Buscar vulnerabilidades, fallos, código sucio, ruido, anti-patrones y problemas de arquitectura.
- Analizar backend: acceso a datos (DAO), servicios, modelos, integración Ollama, autenticación BCrypt.
- Identificar riesgos de seguridad: inyección SQL, acceso a ficheros, manejo de secretos, errores silenciados.
- **Estado:** ✅ Completada (2026-06-02)

---

### Fase 5 — Auditoría de diseño: UI/UX, gráficos, animaciones, frontend ✅ COMPLETADA
- Revisar las pantallas JavaFX: FXML, CSS, controladores y componentes visuales.
- Analizar la coherencia del diseño gráfico, la paleta de colores, tipografía y espaciado.
- Evaluar animaciones, micro-interacciones, tooltips, transiciones y estado hover/focus.
- Proponer mejoras concretas de UI/UX sin implementarlas hasta autorización.
- **Estado:** ✅ Completada (2026-06-02)

---

### Fase 6 — Bloque Gemini: análisis de contexto amplio y propuestas ✅ COMPLETADA
- Generar un bloque autocontenido para pegar en **Gemini Code Assist** (IDE).
- Objetivo: análisis arquitectónico, propuestas de mejora de alto nivel, revisión de alternativas técnicas.
- Claude Code indicará exactamente cuándo pegar el bloque y qué hacer con la respuesta.
- **Estado:** ✅ Completada (2026-06-02)

---

### Fase 7 — Bloque Codex: validación técnica local y tests ✅ COMPLETADA
- Bloque ejecutado por el usuario en Codex IDE. Respuesta incorporada en `AUDITORIA.md`.
- Hallazgos confirmados: SEC-1 (9 métodos UserDAO cierran singleton), SEC-2 (código exacto changePassword),
  SEC-4 (solo prompt text, sin validación lógica), UI-3 (tres sistemas de versión sin sincronía).
- Tests: **72/72 verdes** · Build: SUCCESS
- **Estado:** ✅ Completada (2026-06-02)

---

### Fase 8 — Informe consolidado ✅ COMPLETADA
- Generado `INFORME-FINAL.md` con todos los hallazgos de las 7 fases previas.
- Contiene: veredicto general, 20 hallazgos priorizados (P0→P3), lo que está bien,
  plan de 6 sprints secuenciado, y trazabilidad Multi-IA completa.
- **Estado:** ✅ Completada (2026-06-02)

---

## Trazabilidad

| Fase | Estado | Agente líder | Agentes consultados | Validación |
|------|--------|-------------|---------------------|------------|
| 1 | ✅ Completada | Claude Code | — | Archivos referencia leídos (Recetas + Nemeterial) |
| 2 | ✅ Completada | Claude Code | — | Estructura completa del proyecto explorada |
| 3 | ✅ Completada | Claude Code | Gemini + Codex | Archivos creados + validación visual |
| 4 | ✅ Completada | Claude Code | — | AUDITORIA.md generado |
| 5 | ✅ Completada | Claude Code | — | AUDITORIA.md Fase 5 añadida |
| 6 | ✅ Completada | Gemini (bloque IDE) | Claude Code | SEC-2 elevado a ALTO |
| 7 | ✅ Completada | Codex (bloque IDE) | Claude Code | 72/72 tests · SEC-1/SEC-2/SEC-4 confirmados |
| 8 | ✅ Completada | Claude Code | Todos | INFORME-FINAL.md generado |
