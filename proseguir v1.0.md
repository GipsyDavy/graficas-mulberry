## ⚠️ LEER ANTES DE EJECUTAR

**Prioridad máxima: Sprint MIGRACION-COMPLEJA.**
`STATE.md` y Codex lo marcan como sprint activo principal. Cerrar MIGRACION-COMPLEJA antes de continuar Sprint UI-E.
Ver procedimiento completo en `MIGRACION_HISTORICO.md`.

Primer piloto recomendado: `PRECIOS PAPEL PROVEEDORES Formulas.xlsx` → script Python → CSV limpio → wizard importación → validar en app.

---

## SPRINT UI-E - Mejoras Visuales Profesionales (pendiente tras MIGRACION-COMPLEJA)

Ítems pendientes (slide+fade ya HECHO en commit `0bb8c8b`):
- Sistema de elevación CSS en 4 niveles (18 dropshadow → 4 variables)
- Animación de números KPI en Dashboard (Timeline + KeyFrame, patrón en VisualAssistantView)
- Shimmer animado en skeleton loaders (Timeline ciclando opacidad, patrón pulsoVozAnimacion)

**Restricciones obligatorias:**
- Cumplir al 100% MACRO-PROMPT-GRAFICAS-MULBERRY.md, CLAUDE.md y MEJORAS-VISUALES.md
- Cambios quirúrgicos (solo modificar lo necesario)
- Sin nuevas dependencias externas
- Máxima compatibilidad con los 5 temas existentes + modo oscuro
- Mantener identidad profesional (no gamificar)
- HEAD actual: fd1f34b (v13.0.0), tests 142/142 verdes

**Skills utilizadas (todas oficiales de Claude Code):**
- `/brainstorming` — alineación de objetivos y riesgos
- `/writing-plans` — planificación detallada del sprint
- `/ui-ux-pro-max` — revisión desde perspectiva de negocio/usuario
- `/second-opinion` — revisión técnica independiente JavaFX
- `/executing-plans` — implementación cuidadosa paso a paso
- `/code-review` — revisión de código generado
- `/VibeSec` — revisión de seguridad (sprint completo)
- `/security-review` — revisión de seguridad en cambios críticos
- `/run` + `/verification-before-completion` — pruebas visuales y de rendimiento
- `/simplify` — limpieza de código tras implementación

### FLUJO OBLIGATORIO DEL SPRINT:

1. **/brainstorming** → Confirmar objetivo, riesgos y alineación con filosofía del producto.

2. **/writing-plans** → Generar plan detallado completo del Sprint UI-E, incluyendo archivos a tocar, orden de implementación, riesgos de rendimiento y mitigaciones.

3. **/ui-ux-pro-max** → Revisión desde perspectiva de dueño de empresa de artes gráficas (¿profesional? ¿útil en el día a día?).

4. **/second-opinion** → Revisión técnica JavaFX 21 (rendimiento, compatibilidad temas, mantenimiento).

5. **/executing-plans** → Implementar Sistema de Elevación (styles.css + cualquier clase Java necesaria).

6. **/executing-plans** → Implementar animación de números KPI + shimmer skeleton en DashboardView.

7. **/code-review** → Revisión completa de código generado.

8. **/VibeSec** + **/security-review** → Revisión de seguridad.

9. **/run** + **/verification-before-completion** → Pruebas visuales y de rendimiento (modo claro/oscuro, cambio de tema, tablas grandes, etc.).

10. **/brainstorming** → Retrospectiva del sprint: qué funcionó, qué mejorar.

11. **Paso manual** → Actualizar docs/context/STATE.md y docs/ui/MEJORAS-VISUALES.md.

Actúa como líder del sprint y ejecuta este flujo paso a paso. En cada paso indica claramente qué skill estás usando.