# Punto de entrada para agentes IA

Proyecto: Gráficas Mulberry — Java 21 + JavaFX 21 + SQLite/JDBC + Maven.

Ruta raíz:

```text
C:\Users\GipsyDavy\MAVEN\Graficas Mulberry
```

Este archivo es el punto de entrada neutral para agentes IA como Codex, Claude Code,
Gemini Code Assist y otros agentes de IDE.

## Lectura obligatoria

Antes de modificar el proyecto, leer:

- `CLAUDE.md` — flujo de trabajo, reglas Multi-IA, estándares de código, checklist de sprint.
- `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — contexto completo: arquitectura, módulos, reglas de seguridad, UX.
- `docs/context/STATE.md` — HEAD actual, tests, sprint activo y deuda conocida (actualizar tras cada sprint).
- `docs/security/README.md` — índice de documentación de seguridad.
- `docs/security/SECURITY_AUDIT_2026-06-13.md` — auditoría de seguridad completa más reciente.
- `docs/security/SECURITY_REMEDIATION_2026-06-13.md` — estado actual de remediación.
- `docs/security/SECURITY_AUDIT_RUNBOOK.md` — comandos reproducibles de auditoría de seguridad.

Para contexto de UI y producto, leer:

- `docs/ui/interfaz.md` — arquitectura CSS, tabla de componentes, sistema de diseño, historial de sprints.
- `docs/ui/MEJORAS-VISUALES.md` — propuestas de modernización UI/UX (animaciones, elevación, glassmorphism, identidad visual).

Para migración de archivos históricos complejos, leer:

- `MIGRACION_HISTORICO.md` — procedimiento del Sprint MIGRACION-COMPLEJA (activo).

## Sinceridad técnica

> **Si no estás seguro, dilo claramente: "no lo sé" o "no puedo verificarlo".**
> No inventar datos, causas, estados, rutas, APIs, resultados de tests ni conclusiones.
> Priorizar sinceridad técnica sobre aparentar certeza.

## Estado de auditoría de seguridad

La auditoría de seguridad documentada más reciente es:

```text
docs/security/SECURITY_AUDIT_2026-06-13.md
```

El estado de remediación actual es:

```text
docs/security/SECURITY_REMEDIATION_2026-06-13.md
```

Los resultados de herramientas de soporte se almacenan en:

```text
docs/security/
```

No eliminar ni sobreescribir artefactos de auditorías anteriores. Para una nueva auditoría,
crear nuevos archivos con fecha y actualizar `docs/security/README.md`.

## Reglas de traspaso entre agentes

- Preservar los cambios existentes del usuario salvo instrucción explícita en contrario.
- Para cambios en seguridad, autenticación, base de datos, import/export, instaladores o
  sistema de archivos, consultar los docs de seguridad antes de editar.
- Si una herramienta externa no está disponible, registrar la limitación en las notas de auditoría.
- Si usas Gemini Code Assist, comenzar desde `GEMINI.md`, que apunta de vuelta aquí.
