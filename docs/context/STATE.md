# Estado operativo — Gráficas Mulberry

Fuente única de verdad para HEAD, tests y sprint activo.
Actualizar tras cada sprint cerrado.

**Última actualización:** 2026-06-13

---

## Estado git

| Campo | Valor |
|---|---|
| HEAD | `6268479` |
| Mensaje | `security: auditoría y remediación completa 2026-06-13 (SEC-01..10 + NEW-01..03)` |
| Rama | `master` |
| Tests | 142/142 verdes (`.\mvnw.cmd test`) |
| Versión app | v13.5.0 (`AppConstants.APP_VERSION`) |

---

## Sprint activo

**Sprint MIGRACION-COMPLEJA** — migración de archivos históricos con estructura humana compleja (Excel con celdas combinadas, múltiples mini-tablas, bloques laterales). Ver `MIGRACION_HISTORICO.md` para procedimiento completo.

---

## Cola prioritaria

1. Sprint MIGRACION-COMPLEJA (activo)
2. Refactor B2 (largo plazo — inyección de Connection en DAOs)

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
| Sprint UI-A/B/C/D | varios | CSS variables, FadeTransition, IAView, skeleton+overlay |
| Sprint SEC | — | 5 fixes seguridad P0/P1 |
| Sprint COD | — | Dead code eliminado |

---

## Deuda técnica conocida

- Hint bars en Facturas, Pedidos, Materiales, Empleados (solo ClientesView tiene barra ahora)
- Tests unitarios para `PreferenceService` (lectura/escritura con BD en memoria)
- Refactor B2: inyección de Connection en DAOs (largo plazo)
