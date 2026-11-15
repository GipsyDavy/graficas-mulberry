# HANDOFF — Gráficas Mulberry

> **Tarea activa:** `gov-2026-09-17` (Gobernanza multiagente formal con ledger canónico y scripts).  
> **Última tarea cerrada:** `Config Dependency-Check Maven 12.2.2` (2026-06-27).  
> **Estado general:** v15.0.0 empaquetada (`output/GraficasMulberry-Instalador-v15.0.0.exe`), tests 158/158 verdes.  
> **Rama activa:** `master` (en sincronía con `origin/master`).  
> **Ledger canónico de auditorías:** [.agents/review-ledger.yaml](file:///C:/Users/GipsyDavy/MAVEN/Graficas%20Mulberry/.agents/review-ledger.yaml).

Para el estado técnico detallado, consultar [docs/context/STATE.md](file:///C:/Users/GipsyDavy/MAVEN/Graficas%20Mulberry/docs/context/STATE.md).  
Para el archivo histórico de sesiones previas, consultar [docs/context/STATE_HISTORICO.md](file:///C:/Users/GipsyDavy/MAVEN/Graficas%20Mulberry/docs/context/STATE_HISTORICO.md).

---

## Punto de reanudación

1. **PRIORIDAD #1 (Próxima sesión — antes de implementar UI): Revisar el sistema multiagente IA y compararlo con otros proyectos:**
   - Auditar `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.agents/review-ledger.yaml`, `.agents/rules/`, `scripts/agents/governance/` y `docs/operations/MODEL_EFFORT_POLICY.md` frente al estándar utilizado en otros proyectos del usuario.
   - Regularizar en Git los archivos aún pendientes sin commitear en el workspace local de `gov-2026-09-17` y `Config Dependency-Check Maven 12.2.2` antes de cerrar su verificación `--authorship`.
2. **PRIORIDAD #2 — Consenso Triunvirato IA sobre Rediseño Multi-Interfaz (`ui-redesign-2026-11`, estado `PLANIFICADA`):**
   - Documentado en [docs/ui/PROPUESTAS_REDESENO_UI.md](file:///C:/Users/Gipsy%20D%C3%A1vy/MAVEN/Graficas%20Mulberry/docs/ui/PROPUESTAS_REDESENO_UI.md) con 14 mockups de las propuestas 1–7 en [docs/ui/mockups/](file:///C:/Users/Gipsy%20D%C3%A1vy/MAVEN/Graficas%20Mulberry/docs/ui/mockups) y 4 muestras candidatas (8–11) en [docs/ui/mockups/candidatas/](file:///C:/Users/Gipsy%20D%C3%A1vy/MAVEN/Graficas%20Mulberry/docs/ui/mockups/candidatas).
   - **Pendiente tras la revisión multiagente:** Decidir si se añaden opciones 8–11 al catálogo, recoger dictamen y consenso de **Claude Code** y **Codex** (ver `.agents/review-ledger.yaml`) sobre arquitectura `NavigationShell` y combinaciones híbridas, y planificar la implementación.
3. **Gate SCA Dependency-Check:**
   - Relanzar `.\mvnw.cmd verify` aislado (sin otro proceso compitiendo por `.m2`) para verificar la descarga completa de la base NVD y generación de `target/dependency-check-report.*`.
4. **Candidatos de refactorización y arquitectura pendientes:**
   - God-class `ExportService.java` (>3.400 líneas, 99 métodos): desacoplar métodos redundantes de exportación por formato.
   - Evaluación e incorporación de framework de logging estructurado (`SLF4J` / `Log4j2` / `java.util.logging`) para sustituir trazas dispersas en `System.err`.
   - Cifrado en reposo para base de datos SQLite (evaluar SQLCipher para nóminas y clientes).
5. **Mantenimiento UI / i18n:**
   - Ajustar preposición literal en patrón de fecha larga de `CalendarioView`.

---

## Comandos rápidos de validación

```powershell
# Compilación tras cambios:
.\mvnw.cmd clean compile

# Batería de tests JUnit 5:
.\mvnw.cmd test

# Empaquetado Windows con gate SCA:
.\mvnw.cmd clean verify -Ppackage-windows
.\build-nsis.ps1
```

---

## Reglas operativas para agentes

- **Ledger canónico:** `.agents/review-ledger.yaml` es la única fuente de verdad para el ciclo de auditorías y estados de tareas.
- **Cola al iniciar sesión:** Antes de empezar, todo agente consulta sus revisiones pendientes:
  ```powershell
  node scripts/agents/governance/validate-review-ledger.mjs --pending "<Agente>"
  ```
- **Validación del ledger:**
  ```powershell
  node scripts/agents/governance/validate-review-ledger.mjs
  ```
- **Niveles de tarea (L / M / H):**
  - **L (Ligero):** Formato, textos, UI menor, test aislado -> 1 agente suficiente.
  - **M (Medio):** Pantallas JavaFX, lógica de servicio acotada -> implementación + revisión recomendada.
  - **H (Alto/Crítico):** Auth BCrypt, SQLite DAOs/DDL, `ExportService`/`ImportService` (rutas y CSV), empaquetado/instalador o seguridad -> requiere revisión independiente obligatoria y validación ejecutada (`APROBADA` solo con 2 revisores favorables).
- **Trailers en commits:** Añadir siempre `Agent: <Claude Code | Codex | Google Antigravity>`.
- **Sincronización con GitHub (Pull al iniciar / Push al cerrar sesión):**
  - **Al iniciar sesión:** Ejecutar siempre `git pull` antes de inspeccionar o modificar código para partir del último estado remoto de GitHub.
  - **Al cerrar sesión o hacer relevo:** Por mandato permanente del usuario, tras verificar compilación (`.\mvnw.cmd test-compile`), actualizar `HANDOFF.md` y generar el commit atómico con trailer, es **obligatorio hacer `git push` a GitHub**. Esto garantiza la continuidad inmediata del desarrollo desde cualquier agente IA y desde cualquier ordenador.
- **Comunicación concisa y entrega de turno:** Reportar solo evidencia real, bloqueos, riesgos, **revisiones pendientes** (indicando qué revisión y qué agente debe hacerla, o "Revisiones pendientes: Ninguna") y decisiones adoptadas.
- **Invocación automatizada y cero bloques manuales:** Prohibido solicitar al usuario que copie o pegue bloques de texto entre chats. Para delegaciones automatizadas a Claude Code, usar `.\scripts\agents\Invoke-ClaudeCode.ps1`. La cola se atiende mediante `--pending "<Agente>"`.
