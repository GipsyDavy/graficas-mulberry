# Punto de entrada — Gemini Code Assist

Proyecto: Gráficas Mulberry — Java 21 + JavaFX 21 + SQLite/JDBC + Maven.

Tratar `AGENTS.md` como el archivo de instrucciones principal entre agentes.

## Lectura obligatoria

- `AGENTS.md` — reglas entre agentes, lista de lectura obligatoria, sinceridad técnica.
- `CLAUDE.md` — flujo de trabajo, reglas Multi-IA, checklist de sprint.
- `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — contexto completo del proyecto.
- `docs/context/STATE.md` — HEAD actual, tests, sprint activo.
- `docs/security/README.md` — índice de documentación de seguridad.
- `docs/security/SECURITY_AUDIT_2026-06-13.md` — auditoría de seguridad completa más reciente.
- `docs/security/SECURITY_REMEDIATION_2026-06-13.md` — estado actual de remediación.
- `docs/security/SECURITY_AUDIT_RUNBOOK.md` — comandos reproducibles de auditoría.

## Reglas de rutas

- Raíz del proyecto: `C:\Users\GipsyDavy\MAVEN\Graficas Mulberry`
- Usar rutas relativas al proyecto en todas las sugerencias (p. ej. `src/main/java/...`).
- Evitar rutas con acentos o espacios al sugerir comandos de shell.
- La base de datos es SQLite únicamente. No sugerir MySQL, MongoDB ni ninguna BD remota.

## Modelo de invocación

Gemini se invoca exclusivamente mediante bloques IDE pegados por el usuario — nunca por CLI.
Al responder, limitar el análisis a la pregunta concreta del bloque.
No modificar archivos directamente salvo que el bloque lo solicite explícitamente.

## VibeSec

VibeSec es la skill de revisión de seguridad de este proyecto.
Invocarla de forma proactiva cuando la conversación toque autenticación, permisos,
rutas de archivos, import/export o datos sensibles. Si no está disponible, indicarlo explícitamente.
