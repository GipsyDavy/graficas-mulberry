# Estado operativo — Gráficas Mulberry

Fuente única de verdad para HEAD, tests y sprint activo.
Actualizar tras cada sprint cerrado.

**Última actualización:** 2026-06-24 (Sprint ASISTENTES-NAV + AUDIT-LOGGING — 158/158 — instalador v15.0.0 reempaquetado)

---

### ESTADO AL CIERRE DE SESIÓN 2026-06-24 (Sprint ASISTENTES-NAV + AUDIT-LOGGING — v15.0.0 reempaquetada)

Agente líder: Claude Code. Multi-IA no invocado en ningún cierre de esta sesión — todos los cambios fueron mecánicos/bajo riesgo (nav UI, logging defensivo) verificables con `mvnw test`/`mvnw clean compile`, sin ambigüedad arquitectónica ni auth/permisos/datos sensibles tocados. VibeSec (skill `BehiSecc/VibeSec-Skill`, instalada esta sesión en `~/.claude/skills/VibeSec-Skill/`) ejecutado al cierre — sin hallazgos explotables.

**1. ASISTENTES: dos subsecciones directas en vez de TabPane (commit `8a5a6f9`):** el grupo lateral "ASISTENTES" abría una única subsección con pestañas ("Asistente IA"/"Asistente Visual"). A petición del usuario, ahora son dos botones de navegación directos bajo el mismo grupo, sin contenedor de pestañas intermedio. `AsistentesView.java` eliminado (quedó obsoleto). `MainView.TITULO_A_MODULO` y el switch de tooltips actualizados a las nuevas claves `asistentes.tab.ia`/`asistentes.tab.visual`. Mismo permiso `UserPermissions.IA` en ambos botones — sin cambio de control de acceso.

**2. Asistente Visual arranca siempre desconectado (mismo commit `8a5a6f9`):** `VisualAssistantView` leía el estado `activo` persistido en SQLite al iniciar, arrastrando la conexión de la sesión anterior. Ahora la instancia principal siempre arranca con `activo=false` (`activo.set(modoEmbebido)`); las instancias embebidas (onboarding/ayuda) no se ven afectadas. `setActivo()` sigue persistiendo el estado para uso intra-sesión, solo deja de leerse al arrancar.

**3. Auditoría honesta de código sucio (a petición explícita del usuario, sin subagentes — los `Agent`/Explore devolvieron respuestas rotas interceptadas por hook, descartados, auditoría rehecha manualmente con Grep/Read directos):** revisión completa de `dao/db/model/service/ui/util` (~39k líneas). Hallazgos relevantes:
   - **Alto, no remediado:** `ExportService.java` (3445 líneas/99 métodos) — god-class, métodos de exportación repetidos por entidad (CSV/SQL/JSON/PDF/Word). Refactor grande, riesgo medio-alto, fuera de alcance sin autorización explícita.
   - **Alto, no remediado:** ausencia total de framework de logging (SLF4J/Log4j/`java.util.logging`) en código de aplicación — decisión de arquitectura/nueva dependencia, fuera de alcance sin autorización explícita.
   - **Medio, remediado (commit `4cbbb46`):** 23 sitios de `catch (Exception/SQLException ignored) {}` que ocultaban fallos reales con valor diagnóstico real — convertidos a `System.err.println` con contexto (método + mensaje, o SQL para los helpers parametrizados): 17 en `EstadisticasService` (KPIs del dashboard), 2 en `ExportService` (`tablaExiste`/`contarRegistros`, backups), 1 en `ImportarClientesService` (columnas dinámicas de import), 1 en `FacturasView` + 1 en `PresupuestosView` (selector de tarifa), 1 en `IAView` (`printStackTrace()` → mensaje acotado en export de chat). Dejados sin tocar ~25 sitios donde el silencio es patrón legítimo (parseo defensivo, UI cosmética, migraciones idempotentes, cleanup en `finally`), con justificación por categoría.
   - **Bajo, no remediado:** lógica de `quoteIdentifier`/`requireSqlIdentifier` duplicada entre `DatabaseManager` e `ImportBackupService` (DRY, sin riesgo de seguridad — ambas implementaciones son correctas).
   - Archivos >1000 líneas identificados pero no auditados línea a línea: `VisualAssistantView` (2344), `ImportBackupService` (2216), `ImportService` (1811), `EntityImportService` (1230), `PedidosView` (1288), `ImportView` (1283), `ConfiguracionView` (1209), `MaterialesView` (1152).

**4. VibeSec instalado de verdad esta sesión:** primera comprobación (`~/.claude/plugins/`) dio negativo — corregido tras confirmar con el usuario que es una skill suelta (`BehiSecc/VibeSec-Skill`, clonada a `~/.claude/skills/VibeSec-Skill/`), no un plugin de marketplace. `/security-review` sigue sin existir en ningún sitio conocido. VibeSec aplicado retroactivamente sobre los 2 commits de esta sesión (filtrando secciones web-only que no aplican a app de escritorio): sin hallazgos explotables — SQL siempre parametrizado o con literales hardcoded, mismo permiso de acceso antes/después, único hallazgo cosmético (interpolación de `tabla` —siempre interna, nunca input de usuario— en mensaje de log de `ExportService.tablaExiste()`) sin vector de explotación real.

**5. Empaquetado:** proceso `java.exe` residual (PID 20856) cerrado con autorización explícita antes de `mvn clean`. `.\build-nsis.ps1` regenerado **sin bump de versión** (sigue v15.0.0 — el usuario pidió "el nuevo .exe con las implementaciones nuevas", no una subida de versión) → `output/GraficasMulberry-Instalador-v15.0.0.exe`.

**Push:** `8c05583..4cbbb46` a `origin/master` (2 commits: `8a5a6f9` nav+desconexión, `4cbbb46` logging).

Validación: `mvnw clean compile` limpio + `mvnw test` → 158/158 verdes antes de cada commit.

**Próximo paso recomendado:** definir con el usuario si se ataca el god-class `ExportService`, se introduce un framework de logging, o se sigue auditando el resto de archivos >1000 líneas — ninguno tiene sprint abierto todavía.

---

### ESTADO AL CIERRE DE SESIÓN 2026-06-23 (Sprint ASISTENTES + bump v15.0.0)

Agente líder: Claude Code. Multi-IA usado en la parte de diseño (bloque IDE Gemini pegado por el usuario, arquitectura de contexto ERP); resto sin Multi-IA — cambios mecánicos/quirúrgicos verificados con `mvnw test` y compilación, sin ambigüedad arquitectónica.

**1. Unificación de módulos de asistente (commit `8c05583`):**
- Módulo lateral "Asistente" → **"Asistentes"** (`AsistentesView.java`, nuevo), con pestañas "Asistente IA" (chat) y "Asistente Visual" (antes en Configuración → Asistente; esa pestaña se eliminó de `ConfiguracionView`).
- Asistente visual desactivado por defecto en instalación nueva (antes activo por defecto).
- Quitado el acompañamiento del asistente visual durante la instalación de Ollama/modelos (`OllamaInstallerDialog` perdió el campo y el método `asistente(...)` completo).
- **Fix raíz "0 clientes":** `ContextoERPService` solo mandaba `clienteDAO.count()` al LLM, nunca nombres reales. Ahora lista nombre+ciudad+teléfono de `findAll()`. Añadidas secciones EMPLEADOS, TARIFAS, ALBARANES, NOMINAS; PRESUPUESTOS/FACTURAS con top-5. Arquitectura híbrida (resumen+top-N estático + `detalleBajoDemanda(promptUsuario)` por coincidencia de nombre) recomendada por Gemini para no superar `OllamaService.MAX_CONTEXT_CHARS=20_000`/`MAX_TOTAL_PROMPT_CHARS=60_000`.
- Chips "Crear presupuesto"/"Generar factura" eliminados (la IA no genera documentos reales) → sustituidos por "Ver clientes".
- Catálogo de modelos: añadidos `llama3.3`, `mistral-nemo`, `phi4-mini`. Kimi explícitamente no implementado (decisión del usuario). Qwen/DeepSeek sin cambios — versiones más recientes que devolvía WebFetch (`qwen3.6`, `deepseek-v4-pro`, `mistral-medium-3.5`) no se pudieron verificar como reales, descartadas por riesgo de alucinación tras confirmación del usuario.
- Fix contraste modo oscuro en títulos de grupo del sidebar (`derive()` en CSS siempre aclara; override añadido en `styles.css`).
- Ayuda contextual revisada: artículos del módulo "asistente" hablaban por error del asistente de bienvenida (bug preexistente, corregido a petición del usuario). Onboarding reubicado a `GEN-PS-2.html` nuevo. Changelog `GEN-NEW-1.html` actualizado.
- Push: `985d933..8c05583` a `origin/master`.

**2. Bump de versión v14.1.0 → v15.0.0 + reempaquetado (a petición del usuario, "genera el .exe para instalar en cualquier pc"):**
- Actualizados los 4 ficheros canónicos (`AppConstants.APP_VERSION`, `pom.xml`, `build-nsis.ps1`, `installer.nsi` — 8 ocurrencias en este último). Grep confirmó sin residuos de `14.1.0` en `src/` (los únicos restantes son registros históricos fechados de sesiones previas en este mismo `STATE.md`, que por regla del proyecto no se tocan).
- `mvnw clean compile` + `mvnw test` → 158/158 verdes.
- 2 procesos `java.exe` residuales cerrados con autorización explícita del usuario antes de `mvn clean` (vía `AskUserQuestion`).
- `.\build-nsis.ps1` completó Maven package + jpackage + NSIS → `output/GraficasMulberry-Instalador-v15.0.0.exe` (122.520.111 bytes / 116.8 MB).
- **Sin commit todavía** de los ficheros de versión — pendiente de que el usuario lo pida explícitamente en este o próximo turno.

**Limitación:** no se probó instalación real del .exe en `%LOCALAPPDATA%` ni arranque de la app empaquetada tras este bump — solo se confirmó generación y tamaño del instalador.

**Próximo paso recomendado:** si se retoma "crear presupuesto desde el asistente IA", `OllamaService` no tiene tool-calling (solo `/api/generate` texto→texto) — requeriría capa intermedia determinista (JSON estructurado del LLM → validación contra BD vía DAOs → confirmación humana). Candidatos pendientes sin sprint abierto: preguntas de seguridad de baja entropía en `AuthService`, cifrado en reposo de SQLite.

---

### ESTADO AL CIERRE DE SESIÓN 2026-06-23 (Sprint IA-CONTEXTO-MATERIALES + AUDIT-CSV-INJECTION — v14.1.0 reempaquetada)

Agente líder: Claude Code. Sin Multi-IA en ningún cierre de esta sesión — los tres cambios son mecánicos/bajo riesgo (1 archivo cada uno) con validación objetiva (`mvnw test`) suficiente; la auditoría de seguridad fue manual (Claude Code), sin consultar Codex/Gemini, justificado por ser revisión exploratoria sin ambigüedad arquitectónica.

**1. Bug real reportado por el usuario — contexto IA no daba nombres reales de materiales:** `ContextoERPService.generarContexto()` solo mandaba a Ollama el conteo total de materiales y los nombres de los que tenían stock bajo. Sin nombres reales en el contexto, el modelo rellenaba con placeholders (`Material 1`, `Material 2`...) al pedirle el listado. Fix: la sección MATERIALES ahora itera `materialDAO.findAll()` completo y añade nombre+categoría+stock de cada material; la alerta de stock bajo se conserva igual. Commit `ff2292e`.

**2. Auditoría de seguridad manual (a petición del usuario) — hallazgo nuevo no cubierto por `SECURITY_AUDIT_2026-06-13.md`:** CSV/Formula Injection (CWE-1236) en `ExportService.csvEscapar()` — solo escapaba sintaxis CSV (`;`, comillas, saltos de línea), no neutralizaba valores que empiezan por `=+-@\t`, que Excel/LibreOffice interpretan como inicio de fórmula al abrir el CSV exportado. Vector: campos libres (notas, nombre) rellenados manualmente o importados de fichero de terceros. Fix: antepone un apóstrofo cuando el primer carácter es uno de los disparadores de fórmula, antes del escapado existente. `csvEscapar` pasó de `private` a package-private (`static`) para poder testearlo sin reflection. 7 tests nuevos en `ExportServiceTest` (nuevo fichero). Validado con gitleaks sobre los ficheros tocados (sin secretos); semgrep `p/java` no pudo ejecutar — mismo bloqueo `SSLCertVerificationError` contra `semgrep.dev` ya documentado en `SECURITY_REMEDIATION_2026-06-13.md`, no es regresión nueva. Resto de la auditoría manual (SQL injection, command injection, backdoors, secretos) sin hallazgos — confirmé puntualmente que los fixes de la auditoría previa (`SingleInstanceLock` loopback-only, verificación Authenticode en `OllamaInstallerDialog`) siguen intactos. Commit `4d98a4f`.

**3. Limitación honesta de esta auditoría:** revisión manual (Grep + lectura de código), sin relanzar semgrep/osv-scanner/spotbugs/clamav completos como en la auditoría del 2026-06-13 — no sustituye esa pasada con herramientas, la complementa. Observaciones menores registradas pero no corregidas (fuera de alcance, no autorizadas): preguntas de seguridad de baja entropía en `AuthService.SECURITY_QUESTIONS`, SQLite sin cifrado en reposo para datos personales/nóminas.

**4. Empaquetado:** `.\build-nsis.ps1` ejecutado dos veces esta sesión (tras cada fix), generando ambas veces `output/GraficasMulberry-Instalador-v14.1.0.exe` (116.8 MB). En las dos ejecuciones hubo que cerrar con autorización del usuario un proceso `java.exe` residual que bloqueaba `target\classes` antes de `mvn clean`.

**5. Push a origin:** `git push origin master` → `06cb04f..97675f0` (127 commits, incluye todo el trabajo de sesiones previas más esta). Repo remoto: `https://github.com/GipsyDavy/graficas-mulberry.git`.

Validación: `mvnw clean compile` + `mvnw test` → **158/158 verdes** (151 base + 7 nuevos de `ExportServiceTest`).

**Próximo paso recomendado:** si se retoma la idea de "crear presupuesto desde el asistente IA" (consultada y descartada por ahora por el usuario tras explicación de que requiere capa determinista de validación, no tool-calling directo del LLM), revisar esta sección de handoff antes de diseñar. Si se quiere cerrar el hallazgo de preguntas de seguridad de baja entropía o el cifrado en reposo de SQLite, son candidatos pendientes sin sprint abierto.

---

### ESTADO AL CIERRE DE SESIÓN 2026-06-23 (Sprint PACKAGE-JDK26-JSOBJECT — v14.1.0 reempaquetada)

Agente líder: Codex. Sin Multi-IA — diagnóstico reproducible localmente con el app-image generado y verificación objetiva del runtime; no se tocaron auth, BD ni datos sensibles. Se consultaron los docs de seguridad por tratarse de empaquetado/instalador.

**Causa raíz del `.exe` que no arrancaba tras empaquetar:** `build-nsis.ps1` usaba `C:\Program Files\Java\jdk-26`. JDK 26 ya no contiene el módulo `jdk.jsobject`, pero `javafx-web-21.0.4-win.jar` declara `requires jdk.jsobject`. Resultado: el launcher nativo de `jpackage` fallaba durante la inicialización del boot layer antes de mostrar UI y salía con código `1`, sin diálogo visible. Reproducción: `output\GraficasMulberry\GraficasMulberry.exe` salía con `EXITED code=1`; ejecución manual con Java mostraba `java.lang.module.FindException: Module jdk.jsobject not found, required by javafx.web`.

**Fix aplicado:** `build-nsis.ps1` ya no hardcodea JDK 26; selecciona un JDK compatible que tenga `bin\jpackage.exe` y el módulo `jdk.jsobject`, prefiriendo JDK 21 si existe y usando JDK 25/24 como fallback local. En esta máquina seleccionó JDK 25.0.1. `pom.xml` actualizado de `14.0.1` a `14.1.0` para alinear Maven con `AppConstants`, `build-nsis.ps1` e `installer.nsi`.

**Validación:** `.\build-nsis.ps1` completó Maven package + jpackage + NSIS y generó `output/GraficasMulberry-Instalador-v14.1.0.exe` (122,516,113 bytes / 116.8 MB). `output\GraficasMulberry\runtime\release` confirma `JAVA_VERSION="25.0.1"` y contiene `jdk.jsobject`. `output\GraficasMulberry\GraficasMulberry.exe` quedó vivo tras 7 segundos (`RUNNING pid=3408`) y se cerró manualmente tras la prueba. `.\mvnw.cmd test` → 151/151 verdes.

**Limitación:** no se ejecutó una instalación NSIS real en `%LOCALAPPDATA%` en esta sesión; se validó generación del instalador y arranque del app-image que el instalador copia. Recomendación pendiente: instalar JDK 21 para releases futuras y dejar JDK 25 solo como fallback mientras JavaFX 21 siga siendo la versión del proyecto.

---

### ESTADO AL CIERRE DE SESIÓN 2026-06-22 (Sprint AYUDA-OLLAMA-SIDEBAR — v14.1.0 empaquetada)

Agente líder: Claude Code. Sin Multi-IA — diagnósticos verificados directamente (jpackage `--add-modules`, `LOCALAPPDATA` real del entorno, conteo de call sites) y cambios mecánicos/bajo riesgo sin auth/BD/datos sensibles; usuario revisó cada paso en vivo (`mvn javafx:run`) antes de autorizar el siguiente.

**1. Ayuda (F1 + icono) no funcionaba en el `.exe` empaquetado — causa raíz real:** `build-nsis.ps1` pasaba `--add-modules` a jpackage sin `javafx.web` ni `javafx.media`. Los jars sí se copiaban a `mods/`, pero el módulo nunca se resolvía en runtime → `new WebView()` en `HelpView` lanzaba `NoClassDefFoundError` silencioso (tragado por el handler por defecto de JavaFX, sin diálogo) en cada apertura, tanto por F1 como por el icono — parecía "desactivado". Bug presente desde el commit que introdujo F1 contextual (HELP-3), nunca detectado porque la validación siempre fue `mvn javafx:run` (classpath dev completo, módulo siempre presente ahí) o `mvnw test` (no toca UI/WebView), nunca el `.exe` real. Fix: `javafx.web,javafx.media` añadidos a `--add-modules`. Secundarios: `HelpView.MODULE_IDS` no incluía `"compras"` (grupo invisible en árbol aunque los artículos sí cargaban vía F1); icono footer de ayuda abría siempre el artículo general en vez de `HelpView.forModule(currentModuleId)` como F1 — homogeneizado.

**2. Ollama HTTP 500 — el fix defensivo del sprint OLLAMA-PATH anterior no arreglaba nada:** `OllamaManager.configurarRutaModelos()` construía `OLLAMA_MODELS` desde `%LOCALAPPDATA%`, que en este equipo vale `C:\Users\Gipsy Dávy\AppData\Local` — sigue teniendo tilde y espacio, el mismo bug de llama-server que se pretendía evitar. Verificado con `echo $LOCALAPPDATA` en vivo. Fix real: ruta fija `C:\ProgramData\GraficasMulberry\ollama-models` (independiente del perfil de usuario) + persistencia `setx` a nivel de usuario (defensa para autoarranque futuro de Windows) + reinicio automático (`detenerProcesosExternos`/`reiniciarConRutaCorrecta`) si Ollama ya estaba corriendo con la ruta rota al abrir la app (decisión del usuario, alternativa "solo instrucción manual" descartada). `OllamaInstallerDialog.descargarModelo()` también fijado a la misma ruta — antes el `pull` y el server podían usar rutas distintas. **Sin verificar en vivo con un pull real:** Ollama no estaba instalado en la máquina durante la sesión (confirmado: sin proceso, sin exe en rutas estándar, sin registro de desinstalación) — pendiente de probar con instalación real vía el wizard de la app.

**3. Sidebar — varios ajustes UX/UI a petición del usuario, todos confirmados visualmente en `mvn javafx:run` antes de cerrar:**
- Grupos (CLIENTES/COMERCIAL/PERSONAL/ANALÍTICA/ASISTENTE IA) arrancan colapsados siempre (antes expandidos por defecto, sin persistencia de estado entre sesiones).
- Cabecera de grupo con tinte `derive(-c-accent, 88%)` (mismo patrón que `.nav-pill`, se adapta solo a los 5 temas) + texto en `-c-text` (color oscuro de marca, no blanco — el primer intento con texto blanco quedó ilegible sobre el tinte claro, corregido en la misma sesión) + letter-spacing + opacidad 0.75/1.0 en hover.
- Icono de colapsar/expandir sidebar: nuevo `Icons.sidebarToggle()` (doble chevron 18px) sustituye al `Icons.navArrow()` de 10px reutilizado que apenas se veía como control principal.
- Tooltips: nuevo helper `Tooltips.java` (paquete `ui`), sustituido mecánicamente `new Tooltip(` → `Tooltips.of(` en 102 call sites / 14 archivos (vía sed, mismo patrón que las migraciones DAO de B2). Delay 300 ms, `setShowDuration(Duration.INDEFINITE)` — el valor intermedio de 20s probado primero seguía dando sensación de parpadeo (se ocultaba sola a destiempo mientras el usuario seguía mirando).
- Grupo COMERCIAL reordenado: Materiales, Compras, Pedidos, Presupuestos, Albaranes, Facturas.
- "Asistente IA" extraído de ANALÍTICA a su propio grupo (misma cabecera visual, reusa `navGrupo()`) — clave `nav.grupo.asistente` añadida en los 6 idiomas (es/en/ca/eu/gl/fr): "ASISTENTE IA"/"AI ASSISTANT"/"ASSISTENT IA"/"IA LAGUNTZAILEA"/"ASISTENTE IA"/"ASSISTANT IA".

**4. Bug adicional encontrado al empaquetar:** `installer.nsi` tenía la versión `14.0.1` hardcodeada en 8 sitios (OutFile, textos MUI, `VIProductVersion`, claves de registro) totalmente desacoplada de `$APP_VERSION` en `build-nsis.ps1` — el primer build de esta sesión generó un `.exe` correctamente nombrado v14.1.0 según el log del script, pero el archivo real en disco seguía siendo `v14.0.1.exe` por dentro y por fuera. Corregido (sed mecánico, mismos 8 literales) y reempaquetado — confirmado el segundo build sí generó `GraficasMulberry-Instalador-v14.1.0.exe` (117.5 MB). **Riesgo no resuelto:** `installer.nsi` sigue sin leer la versión de una fuente única (`$APP_VERSION` no se pasa vía `/D` a `makensis`) — cualquier futuro bump de versión debe tocar manualmente `AppConstants.java` + `build-nsis.ps1` + `installer.nsi` (3 sitios), o repetirá este mismo bug. Pendiente de una futura sesión: parametrizar `installer.nsi` con `!define APP_VERSION` inyectado por `/DAPP_VERSION=...` desde `build-nsis.ps1`.

VibeSec ejecutado en cada cierre parcial de esta sesión (ayuda, Ollama, sidebar) — sin hallazgos: `detenerProcesosExternos()` mata solo `ollama.exe` por nombre exacto, `setx` con valor fijo sin input externo, ruta `ProgramData` fija sin partes dinámicas, firma Authenticode del instalador de Ollama intacta. `/security-review` no aplicable (sin auth/datos personales/permisos tocados).

Validación: `mvnw clean compile` + `mvnw test` → 151/151 en cada uno de los ~6 ciclos de esta sesión. Instalador final: `output/GraficasMulberry-Instalador-v14.1.0.exe`.

**Próximo paso recomendado:** probar instalación real de Ollama vía el wizard de la app en esta máquina (no se pudo verificar en vivo en esta sesión) y confirmar que el chat IA funciona; considerar parametrizar la versión de `installer.nsi` para evitar regresiones futuras del punto 4.

---

## HANDOFF PARA PRÓXIMO AGENTE — leer antes de tocar nada

### Activar al inicio de sesión (antes de cualquier tarea)
1. `/caveman full` — modo caveman activo en nivel full. Mantener toda la sesión.
2. `/caveman-commit` — usar para todos los commits del proyecto.

### Lectura obligatoria en orden
1. Este archivo (`docs/context/STATE.md`) — HEAD, cola, estado.
2. `AGENTS.md` — reglas entre agentes, sinceridad técnica.
3. `CLAUDE.md` — checklist pre-sprint, reglas Multi-IA, convenciones.
4. `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — arquitectura completa, módulos, historial.

### ESTADO AL CIERRE DE SESIÓN 2026-06-22 (Sprint OLLAMA-PATH — v14.0.1 empaquetada)

**HEAD:** commit `e7e32ba` — `chore: bump version a v14.0.1 y empaquetar instalador`. Rama: `master`. Tests: **151/151 verdes**.

**Commits finales de la sesión (orden cronológico del cierre):**
1. `5e8f841` — `fix(i18n): Locale dinamico en CalendarioView segun idioma activo` (Sprint CALENDARIO-LOCALE — cierra último gap arquitectónico i18n conocido).
2. `e772b78` — `docs(state): cerrar Sprint CALENDARIO-LOCALE`.
3. `dc9cd62` — `chore: bump version a v14.0.0 y empaquetar instalador` (de la sesión anterior, no de hoy — listado aquí solo como referencia de orden).
4. `edfec47` — `fix(ui): corregir version residual v13.5.0 a v14.0.0` (bug real: `ConfiguracionView` tenía `"Versión 13.5.0"` hardcodeado, no leía `AppConstants.APP_VERSION`).
5. `fe69651` — `fix(ollama): fijar OLLAMA_MODELS sin tildes al arrancar Ollama` (Sprint OLLAMA-PATH — diagnóstico + fix defensivo del bug HTTP 500 persistente en el chat IA).
6. `cfddd86` — `docs(state): cerrar Sprint OLLAMA-PATH`.
7. `e7e32ba` — `chore: bump version a v14.0.1 y empaquetar instalador` (**HEAD actual**).

**Instalador generado:** `output/GraficasMulberry-Instalador-v14.0.1.exe` (117.5 MB) + copia histórica en `installer/v14.0.1-nsis/`. Pipeline `build-nsis.ps1` ejecutado sin errores (mvn package → jpackage app-image → gen_graphics.py → makensis).

**Punto exacto para continuar:** no hay sprint pendiente ni cola activa. Ver sección "### Punto de entrada exacto para el próximo sprint" más abajo para detalle completo (candidatos: traducir "de" literal en `CalendarioView`, fix manual de Ollama pendiente en la máquina del usuario — `OLLAMA_MODELS` + repull —, o nueva instrucción del usuario). **Preguntar al usuario por dónde continuar antes de empezar nada.**

**Trazabilidad resumida del cierre:** Agente líder Claude Code en CALENDARIO-LOCALE, version-residual y OLLAMA-PATH. Sin Multi-IA en esos tres cierres — todos mecánicos/bajo riesgo tras diagnóstico directo (CALENDARIO-LOCALE: 2 ficheros, getter trivial; version-residual: grep + 3 ficheros; OLLAMA-PATH: causa raíz confirmada por reproducción `curl` directa, fix 1 fichero). VibeSec ejecutado en cada cierre — sin hallazgos. `/security-review` no aplicable en esos tres cierres (sin auth/datos sensibles tocados). Validación en cada sprint: `mvnw clean compile` + `mvnw test` → 151/151. Los sprints B2 previos del mismo día sí usaron Multi-IA cuando correspondía; ver sus bloques dedicados.

---

### ESTADO AL CIERRE DE SESIÓN 2026-06-19 (Sprint i18n-16 — gap cobertura cerrado)

**HEAD:** commit `b1d6b3a` (i18n-16, ver tabla abajo). Rama: `master`. Tests: **151/151 verdes**.

**Trazabilidad i18n-16:** Agente líder Claude Code. Codex consultado vía bloque IDE (revisión post-implementación, sin cambios aplicados, confirmó gap import sin traducir fuera de alcance). Gemini no consultado — tarea mecánica de bajo riesgo, sin justificar coste de cuota. VibeSec ejecutado al cierre — sin hallazgos (valores de bundles internos, sin input de usuario en los puntos tocados). `/security-review` no aplicable — no toca auth/datos personales/permisos. Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151.

**Sprints cerrados esta sesión:**

| Sprint | Descripción |
|---|---|
| i18n-3 | MainView migrada — sidebar, footer, tooltips, diálogos, asistente visual (~60 claves nav.* + main.*) |
| i18n-4 | DashboardView + ClientesView migradas — KPIs, avisos, toolbar, tabla, diálogo edición, importación, exportación, previsualización, errores (~90 claves dash.*/clientes.*/export.*) |
| i18n-5 | FacturasView migrada — toolbar, tabla, diálogos (edición+líneas+materiales), albaranes, exportación, previsualización (~75 claves facturas.*) |
| i18n-6 | PedidosView migrada — resumen KPIs, tabs, filtros, toolbar, tablas pedidos+pagos, diálogos (edición+pago+fraccionar), importación, exportación, previsualización (~128 claves pedidos.*) |
| i18n-7 | AlbaranesView migrada — toolbar, tabla, diálogos (edición+artículos+stock), importación, exportación, previsualización, rename txf+tLineas (~84 claves albaranes.*) |
| i18n-8 | PresupuestosView migrada — toolbar, tabla, diálogos (presupuesto+líneas+materiales+tarifa tiempo), importación, exportación, previsualización, rename txf+tLineas+tarifa (~118 claves presupuestos.*) |
| i18n-9 | NominasView migrada — toolbar, tabla, diálogos (nueva/editar+resumen+generar mes), importación, exportación, previsualización, errores (~76 claves nominas.*) |
| i18n-10 | EmpleadosView migrada — toolbar, tabla, diálogo alta/edición, baja/reactivar, importación, exportación, previsualización, errores, título diálogo columnas + prefijo fichero exportado (~75 claves empleados.*); rename `tf()`→`txf()` |
| i18n-11 | MaterialesView migrada — toolbar stock, tabla, diálogo material, tab consumo por técnica, tab pagos a proveedores, importación, exportación, previsualización, errores, título diálogo columnas + prefijo fichero exportado (142 claves materiales.*); rename `tf()`→`txf()` |
| i18n-12 | TarifasView migrada — toolbar, tabla, diálogo tarifa, gestión de tramos (sub-diálogo + tabla), importación, exportación, previsualización, título diálogo columnas + prefijo fichero exportado, filtro técnica + sentinel "Todas" (74 claves tarifas.*); rename `tf()`→`txf()`; rename `Tarifa t`→`tarifa` (param de método completo, no solo lambdas) |
| i18n-13 | ComprasProveedorView migrada — resumen KPIs, toolbar filtros, tabla, diálogo pago, marcar pagado, eliminar, validaciones (60 claves compras.*); rename `tf()`→`txf()` (4 call sites); `FORMAS_PAGO` y códigos internos de filtro NO traducidos (valores BD/lógica) |
| i18n-14 | EstadisticasView migrada — títulos, tabs, KPIs, títulos de gráficos, nombres de series, previsualización, exportación PDF, errores (49 claves estadisticas.*); sin renombrados por colisión (único `t` local es `Tab t` dentro de `tab()`, sin llamada interna a `t()` global); PALETA/COLOR_*/CHART_COLOR_*/"—"/"…"/`ex.getMessage()` NO traducidos |
| i18n-15 | CalendarioView migrada — título, navegación, días, diálogo de nota (título, botones, labels, prompts), error de guardado (18 claves calendario.*); sin conflictos de naming (sin `t`/`tf` previos); flechas/punto/cerrar/CSS/log consola NO traducidos; `Locale esES` para nombre mes/día queda en español (gap arquitectónico conocido, no corregido). **Última vista pendiente — i18n al 100%.** |
| i18n-16 | Gap cobertura DynamicColumnRuntime/export cerrado en Clientes/Facturas/Pedidos/Albaranes/Presupuestos/Nóminas: título diálogo columnas vía `t("nav.*")`, prefijo `setInitialFileName`, y `ExtensionFilter` de exportación vía `tf("*.export.filtro", ...)` (gap adicional hallado en Clientes/Facturas/Pedidos, no documentado antes; 18 claves nuevas `*.export.filtro`). Gap nuevo detectado por Codex, NO corregido (fuera de alcance acordado): filtro de **importación** sin traducir en FacturasView/PedidosView. |

**Estado del sistema i18n al cierre:**

- `LanguageManager` — infraestructura completa (singleton, `t()`, `tf()`, fallback ES, UTF-8).
- `LanguageManager.tf(key, args)` — añadido formalmente (MessageFormat wrapper).
- 6 bundles COMPLETOS con todas las claves i18n-0 → i18n-16: `messages_{es,en,ca,eu,gl,fr}.properties`. Paridad de claves `*.export.filtro` verificada (18 idénticas en los 6 bundles, 0 diffs).
- Vistas migradas: `LoginView`, `AdminSetupView`, `ConfiguracionView`, `MainView`, `DashboardView`, `ClientesView`, `FacturasView`, `PedidosView`, `AlbaranesView`, `PresupuestosView`, `NominasView`, `EmpleadosView`, `MaterialesView`, `TarifasView`, `ComprasProveedorView`, `EstadisticasView`, **`CalendarioView`**.
- Vistas pendientes de migrar: **ninguna — i18n completo al 100%** (gap conocido no resuelto: `Locale esES` fijo en CalendarioView para nombres de mes/día, ver checklist i18n-15).

**Hallazgo i18n-10 (Codex, revisión post-implementación):** en `EmpleadosView` el segundo argumento de `DynamicColumnRuntime(...)` (título visible en el diálogo "⚙ Columnas" vía `ColumnConfiguratorDialog`) y el prefijo de `fc.setInitialFileName(...)` en exportación SÍ se migraron (`t("nav.empleados")`). MaterialesView (i18n-11) replicó la misma migración desde el inicio (`t("nav.materiales")`). Las 8 vistas restantes (Clientes, Facturas, Pedidos, Albaranes, Presupuestos, Nóminas, Tarifas, ComprasProveedor) **dejan estos dos puntos sin traducir** (string literal en español) — gap de cobertura real, no regresión, pendiente de homogeneizar en una futura pasada de limpieza si se decide.

**Decisión arquitectónica crítica de i18n-3 (respetar en sprints futuros):**

`TITULO_A_MODULO` usa **claves i18n** como keys del mapa (p.ej. `"nav.clientes"`, no `"Clientes"`). Esto es obligatorio: si el mapa usara strings traducidos, la lookup fallaría en idiomas distintos del español. Todos los callers de `mostrarVista()` pasan la clave i18n, no el texto traducido. Dentro de `mostrarVista()` se llama `t(titulo)` para el asistente visual y los títulos de ventana popup.

**Decisiones de i18n-4/i18n-5 (respetar en sprints futuros):**

- `COLUMNAS_BASE` static map en ClientesView **NO migrado** — los valores son labels almacenados en BD (en español). La sobrescritura via `actualizarColumnasDinamicas()` / `columnConfigDAO.visibleLabels()` ocurre inmediatamente y prevalece sobre los headers del `col()` call. Migrar `col()` headers es correcto semánticamente (future-friendly) pero los headers visibles en runtime son los de BD.
- Valores del ComboBox `"empresa"` / `"particular"` **NO traducidos** — se almacenan en BD como strings españoles. Traducirlos rompería datos existentes.
- `mostrarResultadoImportacion()` es **dead code** (nunca llamado) — migración mínima aplicada (solo título del Alert).
- Claves `export.fmt.*` son **compartidas entre módulos** (prefix `export.fmt`, no `clientes.export.fmt`). Las descripciones específicas de módulo usan `clientes.export.<fmt>.desc`.
- Naming conflict `tf`: si la vista tiene `private TextField tf(...)`, renombrar a `txf()` antes de añadir `import static LanguageManager.tf`. Aplicado en ClientesView.

**Claves i18n ya definidas en los bundles (resumen acumulado):**
- `lang.*` — nombres de idiomas (6 claves)
- `config.idioma.*` — panel selector de idioma (3)
- `common.*` — labels/prompts/errores comunes (9)
- `login.*` — LoginView (9) + `login.recovery.*` (7)
- `admin.*` — AdminSetupView (6)
- `config.*` — ConfiguracionView completa (~95 claves)
- `nav.*` — módulos sidebar + grupos + tooltips (~35 claves)
- `main.*` — footer, búsqueda, sesión, menú ctx, diálogos, asistente (~25 claves)
- `dash.*` — DashboardView: KPIs, avisos, badges (~21 claves)
- `export.*` — diálogo exportación + formatos compartidos (~13 claves)
- `clientes.*` — ClientesView completa (~53 claves)
- `facturas.*` — FacturasView completa (~75 claves)
- `pedidos.*` — PedidosView completa (~128 claves)
- `albaranes.*` — AlbaranesView completa (~84 claves)
- `presupuestos.*` — PresupuestosView completa (~118 claves)
- `nominas.*` — NominasView completa (~76 claves)
- `empleados.*` — EmpleadosView completa (~75 claves)
- `materiales.*` — MaterialesView completa (142 claves)
- `tarifas.*` — TarifasView completa (74 claves)
- `compras.*` — ComprasProveedorView completa (60 claves)
- `estadisticas.*` — EstadisticasView completa (49 claves)
- `calendario.*` — CalendarioView completa (18 claves)

**Patrón de migración establecido (repetir en i18n-4+):**
```java
import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;
// Luego: new Label("Texto") → new Label(t("clave"))
//        new Button("Texto") → new Button(t("clave"))
//        mostrarToast("Texto") → mostrarToast(t("clave"))
//        "Texto " + var → tf("clave.con.{0}", var)
```
**ATENCIÓN naming conflict:** si la vista tiene `TextField tf` o loop var `Tema t` / similar, renombrar a `field`/`tema` para evitar shadowing con `import static tf` / `t`.

### CHECKLIST SPRINT i18n-16 — Gap cobertura DynamicColumnRuntime/export — ✅ EJECUTADO

Cerrado el gap documentado en i18n-10 (2 puntos: título diálogo columnas vía `DynamicColumnRuntime` 2º arg, prefijo `setInitialFileName`) en las 6 vistas que faltaban: Clientes, Facturas, Pedidos, Albaranes, Presupuestos, Nóminas. Clientes y Pedidos no usan `DynamicColumnRuntime` (no aplica ese punto), solo `setInitialFileName`.

**Gap adicional encontrado durante el sprint (no documentado previamente):** `ExtensionFilter` de exportación sin `tf()` en Clientes/Facturas/Pedidos (`fmt[3].toUpperCase() + " — Clientes"` literal) mientras Albaranes/Presupuestos/Nóminas/Empleados/Materiales/Tarifas ya usaban `tf("X.export.filtro", ...)`. Incluido en el sprint tras confirmación del usuario: 18 claves nuevas `clientes/facturas/pedidos.export.filtro` en los 6 bundles, valor `{0} — <Módulo>`, paridad verificada.

VibeSec ejecutado (skill genérico web, mayoría no aplica a app de escritorio JavaFX) — sin hallazgos: valores vienen de bundles internos, no de input de usuario; sin riesgo de path traversal en `setInitialFileName`/`ExtensionFilter`. `/security-review` no invocado — justificado, sprint no toca auth/datos personales/permisos.

Revisión Codex (post-implementación): sin errores en los puntos tocados. Confirmó 1 hallazgo fuera de alcance (ver gap nuevo abajo), no aplicado por ser cambio quirúrgico fuera de lo solicitado.

**Gap nuevo detectado por Codex, pendiente para futuro sprint:** `FacturasView.java` (líneas ~554-555) y `PedidosView.java` (líneas ~643-644) tienen el filtro de **importación** (`FileChooser.ExtensionFilter("Archivos importables (CSV, Excel, JSON)", ...)` y `"Todos los archivos"`) sin migrar — literal español. El resto de vistas (ej. `AlbaranesView`) ya usan `t("albaranes.importar.filtro")` / `t("albaranes.importar.todos_archivos")`. Replicar ese patrón con claves `facturas.importar.filtro`/`facturas.importar.todos_archivos` y `pedidos.importar.filtro`/`pedidos.importar.todos_archivos` en los 6 bundles.

Validación final: `mvnw clean compile` limpio + `mvnw test` → **151/151 BUILD SUCCESS**.

Commit: `b1d6b3a` — `feat(i18n): cerrar gap cobertura DynamicColumnRuntime/export — Sprint i18n-16`.

---

### Sprint B2-1 — TarifaTramoDAO — ✅ CERRADO (2026-06-19)

Primer DAO del Refactor B2 (inyección de Connection). Trazabilidad: Claude Code lidera. Gemini consultado ANTES vía bloque IDE (obligatorio por tocar BD/lógica crítica) — propuso inyección por constructor + `DAOFactory` transicional + orden de migración por fases de riesgo. Claude Code, con confirmación explícita del usuario, descartó el `DAOFactory` por YAGNI/KISS del proyecto (capa de indirección innecesaria para app monousuario sin pool) — inyección directa: cada DAO migrado de forma atómica, sus propios call sites actualizados en el mismo sprint, sin clase intermedia. Codex no consultado en esta ronda — cambio mecánico de 3 ficheros, sin incertidumbre tras compilación+tests en verde.

**Hallazgo propio corregido antes de cerrar:** primer intento usó `try (Connection conn = DatabaseManager.getConnection())` en los call sites — habría cerrado la Connection singleton compartida de toda la app al salir del bloque (gestionada por `DatabaseManager` como singleton de vida completa, solo se cierra al apagar la app). Corregido a `DatabaseManager.getConnection()` sin try-with-resources sobre la Connection.

Cambios: `TarifaTramoDAO(Connection conn)` reemplaza las 4 llamadas internas a `DatabaseManager.getConnection()`; 5 call sites actualizados (`TarifasView` ×4, `PresupuestosView` ×1). Import `DatabaseManager` añadido en `TarifasView` (no lo tenía).

VibeSec ejecutado al cierre — limpio (SQL parametrizado intacto, sin fuga de Connection, sin input HTTP/red aplicable a app de escritorio). `/security-review` no aplicable — no toca auth/datos personales/permisos.

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `9127d62`.

**Próximo DAO recomendado (Fase 1 Gemini, bajo riesgo):** `ColumnConfigDAO` o `NotaCalendarioDAO` — sin dependencias cruzadas con otros DAOs.

---

### Sprint B2-2 — NotaCalendarioDAO — ✅ CERRADO (2026-06-19)

Segundo DAO del Refactor B2. Trazabilidad: Claude Code lidera. Gemini no re-consultado — patrón de migración ya validado y confirmado en Sprint B2-1, sin incertidumbre arquitectónica nueva. Codex no consultado — cambio mecánico, 5 ficheros, compilación + 151/151 tests en verde como validación objetiva suficiente.

Cambios: `NotaCalendarioDAO(Connection conn)` reemplaza las 6 llamadas internas a `DatabaseManager.getConnection()` (`insertar`, `actualizar`, `eliminar`, `findByFecha`, `fechasConNotas`, `findProximas`). 4 call sites actualizados: `App.java` y `DashboardView.java` (inline, dentro de try/catch ya existente, sin cambio estructural) + `CalendarioView.java` y `ContextoERPService.java` (estructural, ver técnica nueva abajo).

**Técnica nueva confirmada — inicializador de campo + excepción comprobada:** dos call sites (`CalendarioView`, `ContextoERPService`) tenían `private final XxxDAO dao = new XxxDAO();` como inicializador de campo. Al requerir el constructor una `Connection` obtenida vía `DatabaseManager.getConnection()` (declara `throws SQLException`), el inicializador de campo ya no compila sin manejar la excepción comprobada. Solución aplicada: declarar el campo sin inicializador (`private final XxxDAO dao;`) e instanciar dentro del constructor de la clase envolviendo en `try { dao = new XxxDAO(DatabaseManager.getConnection()); } catch (SQLException e) { throw new RuntimeException(e); }` — fail-fast, sin ocultar el error (no hay catch silencioso ni log que trague la excepción), coherente con app de escritorio monousuario sin recuperación elegante de fallo de conexión inicial. `ContextoERPService` no tenía constructor explícito previo (dependía del implícito); se añadió uno nuevo solo para este campo — los otros 5 DAOs del servicio (`presupuestoDAO`, `facturaDAO`, `pedidoDAO`, `materialDAO`, `clienteDAO`) quedan con su inicializador de campo original, fuera de alcance de este sprint. **Patrón reutilizable para próximos DAOs en cola** que tengan call sites como inicializador de campo (candidato detectado: `ColumnConfigDAO`, instanciado como campo en varias vistas).

VibeSec ejecutado al cierre — sin hallazgos: SQL parametrizado intacto (6 queries con `?`, ninguna concatena input), sin fuga de la Connection singleton (nunca se cierra dentro del DAO, solo `PreparedStatement`/`ResultSet` vía try-with-resources, igual que el patrón original), sin fuga de recursos nueva (el `ResultSet` no envuelto en su propio try-with-resources en `findByFecha`/`fechasConNotas`/`findProximas` es patrón preexistente no tocado en este sprint), patrón `try/catch→RuntimeException` no oculta errores. `/security-review` no aplicable — no toca auth/datos personales/permisos.

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `33ef82b`.

**Próximo DAO recomendado (Fase 1 Gemini, bajo riesgo):** `ColumnConfigDAO` — sin dependencias cruzadas con otros DAOs; aplicar técnica de inicializador de campo si procede.

---

### Sprint B2-3 — ColumnConfigDAO — ✅ CERRADO (2026-06-20)

Tercer DAO del Refactor B2. Trazabilidad: Claude Code lidera. Gemini no re-consultado — patrón ya validado en B2-1/B2-2. Codex no consultado — cambio mecánico, 6 ficheros, compilación + 151/151 tests en verde como validación objetiva suficiente.

Cambios: `ColumnConfigDAO(Connection conn)` reemplaza las 8 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `rename`, `updateDataType`, `setColumnVisible`, `deleteDynamic`, `upsertConfig`, `ensureConfigTable`, `physicalColumns`). Las llamadas a `DatabaseManager.requireSqlIdentifier`/`addColumn`/`dropColumn`/`quoteIdentifier` (validación de nombres de tabla/columna dinámicos, usados en `PRAGMA table_info`/`ALTER TABLE`) quedan intactas — verificado explícitamente con VibeSec, sin riesgo de SQL injection introducido por el refactor.

6 call sites actualizados: `ImportView` (4, inline dentro de try/catch existentes) y `ColumnMappingDialog` (1, inline) solo necesitaron import `DatabaseManager` + pasar `DatabaseManager.getConnection()`. `DynamicColumnRuntime`, `ColumnConfiguratorDialog` y `ClientesView` (3) reutilizan la técnica de inicializador de campo + excepción comprobada confirmada en B2-2 — campo sin inicializador, instanciación movida al constructor real con `try/catch SQLException->RuntimeException`. En `ColumnConfiguratorDialog` (constructores encadenados vía `this(...)`) la instanciación se colocó en el constructor base de 5 argumentos (el único que no delega más, donde corren los inicializadores de campo). `DynamicColumnValueDAO` en `DynamicColumnRuntime` queda fuera de alcance (su propio sprint futuro).

VibeSec ejecutado al cierre — sin hallazgos: validación de identificadores SQL intacta (15 llamadas verificadas línea por línea), sin fuga de Connection singleton, sin fuga de recursos nueva, patrón `try/catch→RuntimeException` no oculta errores. `/security-review` no aplicable.

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `1a7e1b0`.

**Próximo DAO recomendado (Fase 1 Gemini, bajo riesgo):** `DynamicColumnValueDAO` — natural seguir aquí porque ya se tocó en este sprint vía `DynamicColumnRuntime` (campo `valueDAO` sin migrar, queda explícito el siguiente paso).

---

### Sprint B2-4 — DynamicColumnValueDAO — ✅ CERRADO (2026-06-20)

Cuarto DAO del Refactor B2. Trazabilidad: Claude Code lidera. Gemini no re-consultado — patrón ya validado en B2-1/B2-2/B2-3, sin incertidumbre arquitectónica nueva. Codex no consultado — cambio mecánico, 4 ficheros, compilación + 151/151 tests en verde como validación objetiva suficiente.

Cambios: `DynamicColumnValueDAO(Connection conn)` reemplaza las 5 llamadas internas a `DatabaseManager.getConnection()` (`findValues` ×2, `updateValue`, `findUnconvertibleValues`, `normalizeColumnValues` — esta última tenía un `var conn = DatabaseManager.getConnection()` local redundante, eliminado en favor del campo `conn`). Las 8 llamadas a `DatabaseManager.requireSqlIdentifier`/`quoteIdentifier` quedan intactas — verificado, sin riesgo de SQL injection introducido.

3 call sites actualizados: `EntityImportService.insertarFilas` (inline — reutiliza la `Connection conn` ya abierta en esa misma transacción, sin pedir una segunda vía `DatabaseManager.getConnection()`), `ColumnConfiguratorDialog.normalizeExistingValuesIfRequested` (inline, el método ya declara `throws SQLException` por lo que no requirió try/catch nuevo), `DynamicColumnRuntime` (campo `valueDAO`, ya sin inicializador desde B2-3 — se extendió el try/catch existente del campo `configDAO` para resolver una sola `Connection` y compartirla entre ambos DAOs).

VibeSec ejecutado al cierre — sin hallazgos: 8 llamadas de validación de identificadores intactas, valores siempre parametrizados via `?`, sin fuga de la Connection singleton, lógica de autocommit/savepoint en `normalizeColumnValues` sin cambio de semántica (mismo objeto Connection compartido que antes). `/security-review` no aplicable.

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `50f9d45`.

**Próximo DAO recomendado (cola de bajo riesgo):** `ConsumoMaterialDAO`, `TarifaDAO`, `NominaDAO` o `PedidoDAO` — sin dependencias cruzadas pendientes documentadas.

---

### Sprint B2-5 — ConsumoMaterialDAO — ✅ CERRADO (2026-06-20)

Quinto DAO del Refactor B2. Trazabilidad: Claude Code lidera. Gemini no re-consultado — patrón ya validado en B2-1/B2-2/B2-3/B2-4, sin incertidumbre arquitectónica nueva. Codex no consultado — cambio mecánico, 3 ficheros, compilación + 151/151 tests en verde como validación objetiva suficiente.

Cambios: `ConsumoMaterialDAO(Connection conn)` reemplaza las 5 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findByTecnica`, `insert`, `update`, `delete`). DAO más simple de la cola hasta ahora — sin identificadores dinámicos, las 5 queries ya usaban placeholders `?`. Import `DatabaseManager` eliminado del DAO (quedó sin uso tras el refactor).

2 call sites actualizados: `FacturaDAO.descontarMateriales` (inline, el método ya declaraba `throws SQLException`, sin try/catch nuevo) y `MaterialesView` (campo `consumoDao` sin inicializador, try/catch en el constructor — los campos hermanos `dao`/`pagoDao` quedan con su inicializador original, fuera de alcance de este sprint).

VibeSec ejecutado al cierre — sin hallazgos: 5 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva. `/security-review` no aplicable.

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `5432805`.

**Próximo DAO recomendado (cola de bajo riesgo):** `TarifaDAO`, `NominaDAO` o `PedidoDAO` — sin dependencias cruzadas pendientes documentadas.

---

### Sprint B2-6 — TarifaDAO — ✅ CERRADO (2026-06-20)

Sexto DAO del Refactor B2. Trazabilidad: Claude Code lidera. Gemini no re-consultado — patrón ya validado en B2-1→B2-5, sin incertidumbre arquitectónica nueva. Codex no consultado — cambio mecánico, validación objetiva (compilación + 151/151 tests) suficiente, aunque el número de call sites fue mayor que en sprints previos.

Cambios: `TarifaDAO(Connection conn)` reemplaza las 6 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findByTecnica`, `findById`, `insert`, `update`, `delete`). Sin identificadores dinámicos, las 6 queries ya usaban placeholders `?`. Import `DatabaseManager` eliminado del DAO.

7 call sites actualizados — el DAO más referenciado hasta ahora en la cola: `EntityImportService.procesarTarifa` (inline, reusa el `conn` de la transacción ya en curso), `ImportService.importarTarifas` (inline, import `DatabaseManager` añadido), `FacturasView` y `PresupuestosView` (inline dentro de un `catch (Exception ignored)` ya existente para el combo de tarifas; `DatabaseManager` añadido como import nuevo en `FacturasView`), `TarifasView` (campo `dao` sin inicializador, try/catch en el constructor — mismo patrón que `MaterialesView` en B2-5; import `SQLException` nuevo), y `EntityImportServiceTarifaTest` (2 ocurrencias en assertions, `DatabaseManager` ya importado en el test).

VibeSec ejecutado al cierre — sin hallazgos: 6 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva. `/security-review` no aplicable.

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `79183dd`.

**Próximo DAO recomendado (cola de bajo riesgo):** `NominaDAO` o `PedidoDAO` — sin dependencias cruzadas pendientes documentadas.

---

### Sprint B2-7 — NominaDAO — ✅ CERRADO (2026-06-20)

Séptimo DAO del Refactor B2. Trazabilidad: Claude Code lidera. Gemini no re-consultado — patrón ya validado en B2-1→B2-6, sin incertidumbre arquitectónica nueva. Codex no consultado — cambio mecánico, validación objetiva (compilación + 151/151 tests) suficiente.

Cambios: `NominaDAO(Connection conn)` reemplaza las 6 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findByPeriodo`, `findById`, `insert`, `update`, `delete`). Sin identificadores dinámicos, las 6 queries ya usaban placeholders `?`. Import `DatabaseManager` eliminado del DAO.

5 call sites actualizados: `EntityImportService.procesarNomina` (inline, reusa el `conn` de la transacción ya en curso), `NominasView` (campo `dao` sin inicializador, try/catch en el constructor — campo hermano `empleadoDAO` queda con su inicializador original, fuera de alcance; imports `DatabaseManager`/`SQLException` añadidos), y los tests `NominaDAOTest` (4 ocurrencias) y `EntityImportServiceNominaTest` (5 ocurrencias) — `new EmpleadoDAO()` en ambos tests queda intacto, fuera de alcance de este sprint.

VibeSec ejecutado al cierre — sin hallazgos: 6 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva. `/security-review` no aplicable.

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `1e6bdd3`.

**Próximo DAO recomendado (cola de bajo riesgo):** `PedidoDAO` — sin dependencias cruzadas pendientes documentadas.

---

### Sprint B2-8 — PedidoDAO — ✅ CERRADO (2026-06-21)

Octavo DAO del Refactor B2. Trazabilidad: Claude Code lidera. **Multi-IA usado esta vez** (primer wrinkle nuevo desde B2-1: método con transacción manual + DAO hermano anidado) — Gemini consultado vía bloque IDE (planificación, confirmó: usar `this.conn` en vez de `DatabaseManager.getConnection()` local en `crearDesdePresupuesto` es seguro porque es la misma instancia singleton; dejar `PresupuestoDAO` hermano sin migrar es correcto, solo hace lectura; replicar patrón de `calendarioDAO` en `ContextoERPService` es correcto). Codex consultado vía bloque IDE (verificación de inventario de call sites antes de implementar, vía grep): confirmó los 8 call sites de producción/test sin omisiones, corrigió detalle menor (`PedidoDAOTest` tiene 5 ocurrencias, no 4 como se contó a mano), y señaló que `DatabaseManager.generarNumeroPedido()` depende internamente de `DatabaseManager.getConnection()` pero vive en `DatabaseManager`, no en `PedidoDAO` — fuera de alcance de este sprint, queda intacto.

Cambios: `PedidoDAO(Connection conn)` reemplaza las 5 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findById`, `insert`, `update`, `delete`) vía sed. Sin identificadores dinámicos, las 5 queries ya usaban placeholders `?`. `crearDesdePresupuesto()` cambiado de `Connection conn = DatabaseManager.getConnection();` (variable local) a usar directamente el campo `this.conn` inyectado — mismo objeto singleton, sin cambio de comportamiento transaccional (`externalTx`/commit/rollback intactos). `new PresupuestoDAO()` dentro de `crearDesdePresupuesto` queda sin migrar (hermano fuera de alcance, solo lectura). Import `DatabaseManager` se mantiene en el DAO (lo sigue usando `generarNumeroPedido()`).

8 call sites de producción/test actualizados: `EntityImportService.procesarPedido` (inline, reusa el `conn` de la transacción ya en scope), `ContextoERPService` (campo `pedidoDAO` sin inicializador, resuelto en el constructor reusando el mismo `conn` que `calendarioDAO`; import `Connection` añadido), `PresupuestosView.crearPedido` (inline dentro de un `try/catch(Exception)` ya existente), `PedidosView` (campo `pedidoDao` sin inicializador, try/catch en el constructor — hermanos `pagoDao`/`clienteDao` quedan con su inicializador original, fuera de alcance; import `SQLException` añadido), y 4 archivos de test (`PedidoDAOTest` 5 ocurrencias, `PagoPedidoDAOTest` 1, `EntityImportServiceAlbaranTest` 1, `EntityImportServicePedidoTest` 8 — 19 ocurrencias totales vía sed, `DatabaseManager` ya importado en los 4).

VibeSec ejecutado al cierre — sin hallazgos: 5 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva, transacción manual revisada y confirmada equivalente. `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151.

**Próximo DAO recomendado (cola de bajo riesgo agotada — pasa a riesgo moderado):** `MaterialDAO`, `ClienteDAO`, `EmpleadoDAO`, `PagoMaterialDAO`, `PagoPedidoDAO` o `PresupuestoDAO`.

---

### Sprint B2-9 — MaterialDAO — ✅ CERRADO (2026-06-21)

Noveno DAO del Refactor B2, primero de la cola de riesgo moderado. Trazabilidad: Claude Code lidera. **Multi-IA usado** (tamaño superior a B2-8: 13 ficheros vs 8, y mismo patrón de wrinkle — transacción manual + DAO sin migrar anidado — visto en `PedidoDAO.crearDesdePresupuesto`). Gemini consultado vía bloque IDE (planificación/validación): confirmó que quitar `Connection conn = DatabaseManager.getConnection();` local en `ajustarStock()` para que resuelva al campo inyectado es semánticamente equivalente; confirmó que el guard `externalTx` sigue funcionando igual cuando `MaterialDAO` se invoca desde `FacturaDAO.descontarMateriales()` (que ya tiene su propia transacción abierta, `autoCommit=false`, antes de instanciar `MaterialDAO`); confirmó razonable reutilizar el mismo `Connection conn` ya resuelto en `ContextoERPService` para un tercer DAO (`materialDAO`, junto a `calendarioDAO`/`pedidoDAO`); sin riesgo arquitectónico en migrar `MaterialDAO` antes que `FacturaDAO` (sigue sin migrar). Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep): confirmó los 13 ficheros (10 producción + 3 test) y números de línea sin discrepancias; confirmó 27 instanciaciones totales de `new MaterialDAO(`, 9 de ellas en `MaterialDAOTest`; confirmó 8 llamadas a `DatabaseManager.getConnection()` en el DAO original (7 simples + 1 en `ajustarStock()`); confirmó que ningún test depende sutilmente de que `MaterialDAO` abra una `Connection` distinta a la del propio test (singleton).

Cambios: `MaterialDAO(Connection conn)` reemplaza las 8 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findBajoStock`, `countBajoStock`, `findById`, `insert`, `update`, `delete`, `ajustarStock`) vía edición directa (no sed, por mezcla de `createStatement`/`prepareStatement` con nombre de variable `conn` ya usado en el método). Sin identificadores dinámicos, todas las queries ya usaban placeholders `?`. `ajustarStock()` cambiado de `Connection conn = DatabaseManager.getConnection();` (variable local) a usar directamente el campo `this.conn` inyectado — mismo objeto singleton, guard `externalTx`/commit/rollback intactos, confirmado seguro por Gemini incluso bajo la transacción anidada de `FacturaDAO`. Import `DatabaseManager` eliminado del DAO (sin otros usos).

13 ficheros actualizados: **producción (10)** — `EntityImportService.procesarMaterial` (inline, reusa el `conn` de la transacción ya en scope), `ContextoERPService` (campo `materialDAO` sin inicializador, resuelto en el constructor reusando el mismo `conn` que `calendarioDAO`/`pedidoDAO`, ya tres DAOs comparten la instancia), `AlbaranesView.dialogoDesdeStock` (inline, try/catch ya existente), `DashboardView.cargarDatos` (inline, try/catch ya existente; import `DatabaseManager` añadido — no estaba presente en este fichero), `ComprasProveedorView` (campo `materialDao` con inicializador inline → movido a constructor con try/catch nuevo; hermano `pagoDao`/`PagoMaterialDAO` queda con su inicializador original, fuera de alcance; imports `DatabaseManager`/`SQLException` añadidos), `ImportService.importarMateriales` (inline, método ya declara `throws Exception`), `FacturaDAO.descontarMateriales` (inline, junto al `ConsumoMaterialDAO` hermano ya migrado en sprint anterior; `FacturaDAO` en sí permanece sin migrar, fuera de alcance), `FacturasView` (inline, try/catch ya existente), `MaterialesView` (campo `dao` con inicializador inline → movido al try/catch ya existente de `consumoDao` en el constructor, reusando el mismo `conn`; hermano `pagoDao`/`PagoMaterialDAO` queda con su inicializador original, fuera de alcance; import `Connection` añadido), `PresupuestosView.dialogoLineaPedido` (inline, try/catch ya existente). **Test (3)** — `MaterialDAOTest` (9 ocurrencias vía sed, incl. los 2 tests que manipulan `conn.setAutoCommit(false)` manualmente para verificar el comportamiento transaccional de `ajustarStock()` — sin impacto, mismo singleton), `EntityImportServiceMaterialTest` (7 ocurrencias vía sed), `PagoMaterialDAOTest` (1 ocurrencia vía sed) — `DatabaseManager` ya importado en los 3.

Verificación post-cambio: `grep -rn "new MaterialDAO()" src/` → 0 resultados (ninguna instanciación sin `Connection` quedó atrás); `grep -rn "new MaterialDAO(" src/` → 27 ocurrencias, coincide exactamente con el conteo de Codex.

VibeSec ejecutado al cierre — sin hallazgos: 8 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva (todos los `Statement`/`PreparedStatement` siguen en try-with-resources), semántica transaccional de `ajustarStock()` preservada bajo `FacturaDAO`. `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `799811d`.

**Próximo DAO recomendado (riesgo moderado, continúa):** `ClienteDAO`, `EmpleadoDAO`, `PagoMaterialDAO`, `PagoPedidoDAO` o `PresupuestoDAO` — revisar dependencias cruzadas antes de elegir (p.ej. `PagoMaterialDAO`/`PagoPedidoDAO` probablemente referencian `MaterialDAO`/`PedidoDAO`, ya migrados, lo que podría simplificar su propio sprint).

---

### Sprint B2-10 — PagoMaterialDAO — ✅ CERRADO (2026-06-21)

Décimo DAO del Refactor B2. Trazabilidad: Claude Code lidera. **Multi-IA usado** (riesgo moderado, norma confirmada en B2-8/B2-9). Gemini consultado vía bloque IDE (planificación ANTES de implementar): confirmó el plan de mover `pagoDao` al try/catch ya existente en `MaterialesView` reusando el mismo `conn` que `dao`/`consumoDao`; confirmó resolver `conn` una sola vez en `ComprasProveedorView` para `materialDao` y `pagoDao` (antes resolvía conexiones separadas); confirmó que `PagoMaterialDAO` no tiene wrinkles (sin identificadores dinámicos, sin transacción manual, sin DAO anidado); confirmó razonable cerrar el sprint sin tocar `MaterialDAO`/`ConsumoMaterialDAO`. Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep): confirmó las 5 ocurrencias de `new PagoMaterialDAO(` (3 en `PagoMaterialDAOTest`, 1 en `ComprasProveedorView`, 1 en `MaterialesView`) sin discrepancias; confirmó las 10 llamadas a `DatabaseManager.getConnection()` en el DAO original, todas dentro de try-with-resources; confirmó ausencia de `requireSqlIdentifier`/`quoteIdentifier`/`setAutoCommit`; confirmó que `pagos_material` no es padre de cascada de ningún otro DAO no migrado (`material_id` es `ON DELETE SET NULL`, no `CASCADE`).

Cambios: `PagoMaterialDAO(Connection conn)` reemplaza las 10 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findPendientes`, `findVencidos`, `findProximosVencimientos`, `countVencidos`, `totalPendiente`, `insert`, `update`, `marcarPagado`, `delete`). Sin identificadores dinámicos, todas las queries ya usaban placeholders `?`. Import `DatabaseManager` eliminado del DAO (sin otros usos, solo queda mención en comentario).

4 ficheros actualizados: `PagoMaterialDAOTest` (3 ocurrencias, `new PagoMaterialDAO(DatabaseManager.getConnection())`, `DatabaseManager` ya importado), `MaterialesView` (campo `pagoDao` con inicializador inline → sin inicializador, añadido al try/catch ya existente del constructor junto a `consumoDao`/`dao`, mismo `conn`), `ComprasProveedorView` (campo `pagoDao` con inicializador inline → sin inicializador; constructor cambiado de resolver `conn` solo para `materialDao` a resolverlo una vez y pasarlo a ambos DAOs; import `Connection` añadido — no estaba presente en este fichero).

VibeSec ejecutado al cierre — sin hallazgos: 10 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva (todos los `Statement`/`PreparedStatement` siguen en try-with-resources). `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `478d0ef`.

**Próximo DAO recomendado (riesgo moderado, continúa):** `ClienteDAO`, `EmpleadoDAO`, `PagoPedidoDAO` o `PresupuestoDAO`. Grep propio (B2-10) confirma: `PagoPedidoDAO` y `EmpleadoDAO` sin wrinkles (perfil tan simple como `PagoMaterialDAO`); `PresupuestoDAO` tiene transacción manual (`setAutoCommit`, mismo patrón que `PedidoDAO`/`MaterialDAO`); `ClienteDAO` usa `quoteIdentifier` (identificadores dinámicos) — el más sensible de los 4 restantes.

---

### Sprint B2-11 — PagoPedidoDAO — ✅ CERRADO (2026-06-21)

Undécimo DAO del Refactor B2. Trazabilidad: Claude Code lidera. **Multi-IA usado** (riesgo moderado, norma confirmada en B2-8/B2-9/B2-10). Gemini consultado vía bloque IDE (planificación ANTES de implementar): confirmó el plan de compartir un único `conn` entre `pedidoDao` y `pagoDao` en el constructor de `PedidosView`; confirmó dejar `clienteDao` sin tocar (fuera de alcance); confirmó que `PagoPedidoDAO` no tiene wrinkles (sin identificadores dinámicos, sin transacción manual); confirmó razonable cerrar el sprint sin tocar `ClienteDAO`. Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep): confirmó las 4 ocurrencias de `new PagoPedidoDAO(` (3 en `PagoPedidoDAOTest`, 1 en `PedidosView`) sin discrepancias; confirmó las 8 llamadas a `DatabaseManager.getConnection()` en el DAO original (líneas 24, 35, 54, 70, 78, 87, 95, 103); confirmó ausencia de `requireSqlIdentifier`/`quoteIdentifier`/`setAutoCommit`; confirmó que el helper `crearPedido()` del test usa `PedidoDAO` con la misma Connection singleton, sin riesgo adicional; confirmó que `pagos_pedido` es hijo CASCADE de `pedidos` pero no bloquea ningún DAO no migrado. Sin discrepancias en ninguna de las dos revisiones.

Cambios: `PagoPedidoDAO(Connection conn)` reemplaza las 8 llamadas internas a `DatabaseManager.getConnection()` (`findByPedido`, `findAll`, `insert`, `update`, `marcarPagado`, `delete`, `countVencidos`, `totalPendiente`). Sin identificadores dinámicos, todas las queries ya usaban placeholders `?`. Import `DatabaseManager` eliminado del DAO (sin otros usos).

3 ficheros actualizados: `PagoPedidoDAOTest` (3 ocurrencias, `new PagoPedidoDAO(DatabaseManager.getConnection())`, `DatabaseManager` ya importado), `PedidosView` (campo `pagoDao` con inicializador inline → sin inicializador, resuelto en el constructor junto a `pedidoDao` compartiendo el mismo `conn`; import `Connection` añadido — no estaba presente en este fichero; `clienteDao` queda intacto, fuera de alcance).

VibeSec ejecutado al cierre — sin hallazgos: 8 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva (todos los `Statement`/`PreparedStatement` siguen en try-with-resources), 0 referencias residuales a `DatabaseManager` en el DAO. `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `981e787`.

**Próximo DAO recomendado (riesgo moderado, continúa):** `ClienteDAO`, `EmpleadoDAO` o `PresupuestoDAO`. `EmpleadoDAO` sin wrinkles (perfil tan simple como `PagoPedidoDAO`/`PagoMaterialDAO`); `PresupuestoDAO` tiene transacción manual (`setAutoCommit`, mismo patrón que `PedidoDAO`/`MaterialDAO`); `ClienteDAO` usa `quoteIdentifier` (identificadores dinámicos) — el más sensible de los 3 restantes, candidato natural para cerrar la cola de riesgo moderado al final.

---

### Sprint B2-12 — EmpleadoDAO — ✅ CERRADO (2026-06-21)

Duodécimo DAO del Refactor B2. Trazabilidad: Claude Code lidera. **Multi-IA usado** (riesgo moderado, norma confirmada en B2-8→B2-11). Gemini consultado vía bloque IDE (planificación ANTES de implementar): confirmó reusar el parámetro `Connection conn` ya recibido por `EntityImportService.procesarEmpleado(...)`; confirmó inline `new EmpleadoDAO(DatabaseManager.getConnection())` en `ImportService.importarEmpleados(...)` (ya declara `throws Exception`, sin `Connection` en scope); confirmó el patrón estándar de mover el campo `dao` de `EmpleadosView` al constructor con try/catch; confirmó refactorizar el constructor de `NominasView` para resolver `conn` una sola vez y compartirlo entre `dao` (`NominaDAO`) y `empleadoDAO`; sin riesgos detectados. Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep): confirmó las 8 llamadas a `DatabaseManager.getConnection()` en el DAO original (`findAll`, `findAllIncluirBajas`, `findById`, `insert`, `update`, `delete`, `reactivar`, `count`); confirmó ausencia de `requireSqlIdentifier`/`quoteIdentifier`/`setAutoCommit`; **corrigió el conteo inicial de call sites de 13 a 12** — 2 de las 14 menciones de `new EmpleadoDAO(` encontradas por `rg -uu` eran referencias documentales (`docs/context/STATE.md`, `docs/security/rg-file-surface-2026-06-13.txt`), no código fuente; confirmó que `EmpleadosView`/`NominasView` no importaban `java.sql.Connection` (había que añadirlo) y que `NominasView` ya importaba `SQLException`; confirmó que ningún otro DAO no migrado depende de `EmpleadoDAO` de forma bloqueante (FK `nominas.empleado_id` sin `ON DELETE CASCADE`). Sin discrepancias de contenido en ninguna de las dos revisiones — solo la corrección de conteo (13→12), que no afectó al plan de implementación.

Cambios: `EmpleadoDAO(Connection conn)` reemplaza las 8 llamadas internas a `DatabaseManager.getConnection()`. Sin identificadores dinámicos, todas las queries ya usaban placeholders `?`. Import `DatabaseManager` eliminado del DAO (solo queda mención en comentario).

8 ficheros actualizados: `EmpleadoDAOTest` (5 ocurrencias), `NominaDAOTest` (1 ocurrencia), `EntityImportServiceNominaTest` (2 ocurrencias) — los 3 ya importaban `DatabaseManager`; `EntityImportService.procesarEmpleado()` reusa el `conn` ya recibido como parámetro; `ImportService.importarEmpleados()` inline `new EmpleadoDAO(DatabaseManager.getConnection())`; `EmpleadosView` (campo `dao` con inicializador inline → sin inicializador, nuevo try/catch en el constructor, imports `Connection`/`SQLException` añadidos — no estaban presentes); `NominasView` (campo `empleadoDAO` con inicializador inline → sin inicializador, constructor refactorizado para resolver `conn` una sola vez y compartirlo con `dao`, import `Connection` añadido).

VibeSec ejecutado al cierre — sin hallazgos: 8 queries parametrizadas vía `?` sin cambios, sin fuga de la Connection singleton, sin fuga de recursos nueva, 0 referencias residuales a `DatabaseManager` en el DAO (solo comentario), 12/12 call sites migrados verificados vía grep. `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `8e975c9`.

**Próximo DAO recomendado (riesgo moderado, continúa):** `ClienteDAO` o `PresupuestoDAO` — cola de riesgo moderado en sus 2 últimos candidatos. `PresupuestoDAO` tiene transacción manual (`setAutoCommit`, mismo patrón ya resuelto en `PedidoDAO`/`MaterialDAO`); `ClienteDAO` usa `quoteIdentifier` (identificadores dinámicos) — el más sensible de toda la cola moderada, candidato natural para cerrarla al final antes de pasar a los orquestadores (`FacturaDAO`, `AlbaranDAO`) y finalmente `UserDAO` (auth-sensible, requiere `/security-review`).

---

### Sprint B2-13 — ClienteDAO — ✅ CERRADO (2026-06-21)

Decimotercer DAO del Refactor B2 — penúltimo de la cola de riesgo moderado, único de toda la cola B2 con identificadores SQL dinámicos (`quoteIdentifier`). Trazabilidad: Claude Code lidera. **Multi-IA usado** (riesgo moderado + identificadores dinámicos, norma confirmada desde B2-8). Gemini consultado vía bloque IDE (planificación ANTES de implementar): confirmó que conservar el import `DatabaseManager` en `ClienteDAO.java` es correcto (no se marca "unused" porque `quoteIdentifier` sigue en uso); confirmó que tocar el constructor no afecta `quotedColumns()`/`update()`; confirmó el patrón de try/catch autocontenido para `clienteDAO` en `AlbaranesView`/`FacturasView`/`PresupuestosView` (DAOs hermanos sin migrar); sin riesgos no contemplados. Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep independiente): confirmó las 25 ocurrencias de `new ClienteDAO(` reportadas y los conteos exactos en el DAO (8 `getConnection()` en líneas 18/28/38/60/82/92/100/109; 3 `quoteIdentifier` en líneas 78/79/121, incluyendo el method reference); confirmó ausencia de otros usos de `DatabaseManager` aparte de comentarios; confirmó ausencia total de imports `java.sql.Connection`/`java.sql.SQLException` en `AlbaranesView`, `FacturasView` y `PresupuestosView`; **corrigió el número de ficheros únicos de 13 a 21** (11 test + 4 servicios + 6 vistas) — el conteo de ocurrencias (25) ya era correcto, solo el conteo de ficheros únicos estaba mal. Sin discrepancias de contenido en ninguna de las dos revisiones.

Cambios: `ClienteDAO(Connection conn)` reemplaza las 8 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `search`, `findById`, `insert`, `update`, `delete`, `count`, `obtenerColumnasExtra`). A diferencia de los 12 sprints anteriores, el import `DatabaseManager` **se conserva** porque `quoteIdentifier` (identificadores dinámicos de columnas extra de `Cliente.getExtras()`) sigue en uso — esa lógica queda completamente intacta, fuera de alcance.

25 call sites migrados en 21 ficheros: 15 en tests (11 ficheros, todos ya importaban `DatabaseManager`, inline `new ClienteDAO(DatabaseManager.getConnection())`); `EntityImportService.procesarCliente(Connection conn,...)` reusa el `conn` ya recibido como parámetro; `ImportarClientesService.procesarTabla()` e `ImportService.importarClientes()` inline (ambos ya declaran `throws Exception`); `ContextoERPService` (campo `clienteDAO` con inicializador → sin inicializador, se añade al try existente compartiendo el mismo `conn` que `calendarioDAO`/`pedidoDAO`/`materialDAO`; `presupuestoDAO`/`facturaDAO` sin migrar quedan intactos); `ClientesView` (campo `dao` con inicializador → comparte `conn` con `columnConfigDAO` en el try existente, import `Connection` añadido); `DashboardView.cargarDatos()` (inline, mismo estilo que `MaterialDAO` ya migrado en la misma línea); `AlbaranesView`/`FacturasView`/`PresupuestosView` (campo `clienteDAO` con inicializador → try/catch nuevo y autocontenido, ya que el DAO principal de cada vista —`AlbaranDAO`/`FacturaDAO`/`PresupuestoDAO`— no está migrado aún; imports `Connection`/`SQLException` añadidos, ausentes hasta ahora); `PedidosView` (campo `clienteDao` → se añade al try existente de B2-11, comparte `conn` con `pedidoDao`/`pagoDao`).

VibeSec ejecutado al cierre — sin hallazgos: 16 llamadas `setString`/`setInt` parametrizadas sin cambios, lógica de `quoteIdentifier`/`quotedColumns` intacta, sin fuga de la Connection singleton, sin fuga de recursos nueva, 0 referencias residuales a `DatabaseManager.getConnection()` en el DAO, 25/25 call sites migrados verificados vía grep (`new ClienteDAO()` sin argumento: 0 resultados). `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `b686534`.

**Próximo DAO recomendado (riesgo moderado, último de la cola):** `PresupuestoDAO` — cierra la cola de riesgo moderado. Tiene transacción manual (`setAutoCommit`/`commit`/`rollback`), mismo patrón ya resuelto en `PedidoDAO`/`MaterialDAO`. Tras `PresupuestoDAO`, la cola pasa a orquestadores (`FacturaDAO`, `AlbaranDAO`) y finalmente `UserDAO` (auth-sensible, requiere `/security-review` extra, no solo VibeSec).

---

### Sprint B2-14 — PresupuestoDAO — ✅ CERRADO (2026-06-22)

Decimocuarto DAO del Refactor B2 — cierra la cola de riesgo moderado (la más larga hasta ahora: 32 call sites en 13 ficheros). Trazabilidad: Claude Code lidera. **Multi-IA usado** (riesgo moderado + transacción manual, norma confirmada desde B2-8). Gemini consultado vía bloque IDE (planificación ANTES de implementar): confirmó mover la variable local `conn` de `save()` al campo `this.conn` inyectado bajo transacción manual (mismo análisis ya validado en `PedidoDAO`/`MaterialDAO`); confirmó el plan para los 3 call sites de `crearDesdePresupuesto` anidados en DAOs hermanos — `PedidoDAO` (ya migrado) reusa `this.conn`, `AlbaranDAO`/`FacturaDAO` (no migrados) usan `DatabaseManager.getConnection()` inline; sin riesgo de integridad referencial (el refactor solo cambia el origen de la `Connection`, no la lógica SQL); sin wrinkles adicionales detectados. Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep independiente): **corrigió el conteo de 30 a 32 ocurrencias** de `new PresupuestoDAO(` en 13 ficheros únicos (`PresupuestoDAOTest` real 8, no 7; `EntityImportServicePresupuestoTest` real 7, no 6); confirmó ausencia de `requireSqlIdentifier`/`quoteIdentifier` en el DAO; confirmó que `AlbaranDAO`/`FacturaDAO` ya importan `DatabaseManager`; confirmó imports `Connection`/`SQLException` ya presentes en `ContextoERPService`/`PresupuestosView`, ausentes en `DashboardView` (no necesarios al ser llamada inline sin variable declarada); confirmó `lineas_presupuesto.presupuesto_id` con `ON DELETE CASCADE`, `facturas.presupuesto_id` sin cascada, sin `presupuesto_id` en `pedidos`/`albaranes` (no bloquea el refactor de Connection). Discrepancia de conteo corregida antes de implementar, sin impacto en el plan.

Cambios: `PresupuestoDAO(Connection conn)` reemplaza las 11 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findById`, `findLineas`, `save` [variable local → campo], `insert`, `update`, `saveLineas` ×2, `updateEstado`, `delete`, `countByEstado`). `save()` cambiado de `Connection conn = DatabaseManager.getConnection();` (variable local) a usar directamente `this.conn` — mismo objeto singleton, guard `externalTx`/commit/rollback/finally intactos. Sin identificadores dinámicos. Import `DatabaseManager` eliminado del DAO (sin otros usos, solo quedan menciones en comentarios).

32 call sites migrados en 13 ficheros: **producción (7)** — `PedidoDAO.crearDesdePresupuesto` (`new PresupuestoDAO(this.conn)`, reusa el conn ya inyectado en B2-8), `AlbaranDAO.crearDesdePresupuesto` y `FacturaDAO.crearDesdePresupuesto` (ambos hermanos no migrados, `new PresupuestoDAO(DatabaseManager.getConnection())` inline), `ContextoERPService` (campo `presupuestoDAO` con inicializador inline → sin inicializador, añadido al try existente del constructor compartiendo el mismo `conn` que `calendarioDAO`/`pedidoDAO`/`materialDAO`/`clienteDAO`; `facturaDAO` hermano sin migrar queda con su inicializador original, fuera de alcance), `EntityImportService.procesarPresupuesto` (inline, reusa el `conn` ya recibido como parámetro), `DashboardView.cargarDatos` (inline, mismo estilo que `MaterialDAO`/`ClienteDAO` ya migrados en la misma línea), `PresupuestosView` (campo `dao` con inicializador inline → sin inicializador, añadido al try existente del constructor compartiendo el mismo `conn` que `clienteDAO`). **Test (6 ficheros, 25 ocurrencias)** — `AlbaranDAOTest`, `FacturaDAOTest`, `PresupuestoDAOTest` (8), `TxAnidadaTest` (6), `EntityImportServiceFacturaTest`, `EntityImportServicePresupuestoTest` (7) — todos vía sed, `DatabaseManager` ya importado en los 6.

VibeSec ejecutado al cierre — skill genérico web (XSS/CSRF/SSRF), mayoría no aplica a app de escritorio JavaFX. Verificación manual propia sin hallazgos: 11 queries parametrizadas vía `?`/batch sin cambios, todos los `Statement`/`PreparedStatement` siguen en try-with-resources, 0 referencias residuales a `DatabaseManager.getConnection()` en el DAO (`grep` confirma), 0 `.close()` nuevo sobre la Connection singleton, semántica transaccional de `save()` preservada, 32/32 call sites migrados verificados vía grep (`new PresupuestoDAO()` sin argumento: 0 resultados). `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `1fd730e` (docs cierre: `b56e80e`).

**Próximo DAO recomendado:** cola de riesgo moderado CERRADA (14/17). Siguen los orquestadores `FacturaDAO`/`AlbaranDAO` (ambos ya referencian `PresupuestoDAO` migrado en este sprint, ambos tienen transacción manual propia) y finalmente `UserDAO` (auth-sensible, requiere `/security-review` extra, no solo VibeSec, último DAO de la cola B2).

---

### Sprint B2-15 — FacturaDAO — ✅ CERRADO (2026-06-22)

Decimoquinto DAO del Refactor B2 — primer orquestador, el más complejo de toda la cola hasta ahora: 3 métodos con transacción manual propia (`save`, `crearDesdePresupuesto`, `crearDesdeAlbaran`) y 3 instanciaciones anidadas de DAOs hermanos ya migrados (`PresupuestoDAO`, `ConsumoMaterialDAO`, `MaterialDAO`). Trazabilidad: Claude Code lidera. **Multi-IA usado** (riesgo alto, orquestador). Gemini consultado vía bloque IDE (planificación ANTES de implementar): confirmó que mover la variable local `conn` de los 3 métodos transaccionales al campo `this.conn` es seguro incluso con `crearDesdePresupuesto` anidando su propia llamada a `save()` (cada guard `externalTx` detecta correctamente la transacción ya abierta sobre la misma instancia singleton); confirmó que las 3 instanciaciones anidadas pasando `this.conn` no rompen el guard de cada DAO hermano; confirmó el call site de `AlbaranDAO.crearDesdeFactura` (`new FacturaDAO(DatabaseManager.getConnection())` inline, hermano no migrado); confirmó que `descontarMateriales()` se comporta idéntico bajo la transacción anidada (mismo objeto Connection, solo cambia el origen); sin wrinkles adicionales detectados. Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep independiente): **corrigió el conteo de 14 a 19 llamadas** a `DatabaseManager.getConnection()` en el DAO original (mi lista de métodos era correcta, pero `crearDesdePresupuesto`, `descontarMateriales` y `saveLineas` tienen 2 llamadas cada uno, no 1); confirmó las 33 ocurrencias de `new FacturaDAO(` en 12 ficheros únicos pero **corrigió el conteo por fichero** (`FacturaDAOTest` real 7, no 6; `EntityImportServiceFacturaTest` real 10, no 9); confirmó ausencia de `requireSqlIdentifier`/`quoteIdentifier`; confirmó que `AlbaranDAO` ya importa `DatabaseManager` y que `FacturasView` ya importa `Connection`/`SQLException`; confirmó `lineas_factura.factura_id` con `ON DELETE CASCADE` y `albaranes.factura_id` con `ON DELETE SET NULL` (no bloquea el refactor, solo nulifica en cascada); confirmó que `descontarMateriales()` y `crearDesdePresupuesto()` son las únicas instanciaciones anidadas, sin otras escapadas. Discrepancias de conteo corregidas antes de implementar, sin impacto en el plan — mismo patrón recurrente desde B2-12: el conteo de ocurrencias/ficheros agrupados requiere verificación independiente.

Cambios: `FacturaDAO(Connection conn)` reemplaza las 19 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findById`, `findLineas`, `crearDesdePresupuesto` [instancia `PresupuestoDAO` + variable local `conn`], `crearDesdeAlbaran` [variable local `conn`], `descontarMateriales` [instancia `ConsumoMaterialDAO` + `MaterialDAO`], `save` [variable local `conn`], `insert`, `update`, `saveLineas` ×2, `updateEstado`, `delete`, `countByEstado`, `totalFacturadoAnio`, `totalFacturadoMes`, `topClientesMes`). Los 3 métodos transaccionales (`save`, `crearDesdePresupuesto`, `crearDesdeAlbaran`) cambiados de `Connection conn = DatabaseManager.getConnection();` (variable local) a usar directamente `this.conn` — mismo objeto singleton, guards `externalTx`/commit/rollback/finally intactos en los 3. Las 3 instanciaciones anidadas (`PresupuestoDAO`, `ConsumoMaterialDAO`, `MaterialDAO`) pasan a `new XxxDAO(this.conn)`. Sin identificadores dinámicos. Import `DatabaseManager` eliminado del DAO (las 2 llamadas a `DatabaseManager.generarNumeroFactura()` ya estaban fully-qualified como `org.gipsybuho.db.DatabaseManager.generarNumeroFactura()`, sin depender del import).

33 call sites migrados en 12 ficheros: **producción (7 ficheros, 8 ocurrencias)** — `AlbaranDAO.crearDesdeFactura` (hermano no migrado, `new FacturaDAO(DatabaseManager.getConnection())` inline), `ContextoERPService` (campo `facturaDAO` con inicializador inline → sin inicializador, añadido al try existente del constructor compartiendo el mismo `conn` que `calendarioDAO`/`pedidoDAO`/`materialDAO`/`clienteDAO`/`presupuestoDAO` — ya 6 DAOs comparten la instancia), `EntityImportService.procesarFactura` (inline, reusa el `conn` ya recibido como parámetro), `AlbaranesView.crearFactura` (inline, try/catch ya existente), `DashboardView.cargarDatos` (2 ocurrencias inline, mismo estilo que otros DAOs ya migrados en la misma línea), `FacturasView` (campo `dao` con inicializador inline → sin inicializador, añadido al try existente del constructor compartiendo el mismo `conn` que `clienteDAO`), `PresupuestosView.crearFactura` (inline, try/catch ya existente). **Test (5 ficheros, 25 ocurrencias)** — `AlbaranDAOTest`, `FacturaDAOTest` (7), `TxAnidadaTest` (6), `EntityImportServiceAlbaranTest`, `EntityImportServiceFacturaTest` (10) — todos vía sed, `DatabaseManager` ya importado en los 5.

VibeSec ejecutado al cierre — skill genérico web, mayoría no aplica a app de escritorio JavaFX. Verificación manual propia sin hallazgos: 0 referencias residuales a `DatabaseManager.getConnection()` en el DAO, 0 `.close()` nuevo sobre la Connection singleton, 33/33 call sites migrados verificados vía grep (`new FacturaDAO()` sin argumento: 0 resultados), parametrización SQL intacta en las 19 queries/batch, las 3 transacciones manuales (incluida la anidada de `crearDesdePresupuesto`→`save`→`descontarMateriales`) preservan su semántica exacta. `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `8a24946` (docs cierre: `6a44574`).

**Próximo DAO recomendado:** `AlbaranDAO` — cierra la cola de orquestadores. Tiene transacción manual propia (`crearDesdeFactura`, `crearDesdePresupuesto`) y ya referencia `FacturaDAO`/`PresupuestoDAO` migrados (ambos pasarán a `this.conn` en vez de `DatabaseManager.getConnection()` inline). Tras `AlbaranDAO`: `UserDAO` (auth-sensible, requiere `/security-review` extra, no solo VibeSec, último DAO de toda la cola B2).

---

### Sprint B2-16 — AlbaranDAO — ✅ CERRADO (2026-06-22)

Decimosexto DAO del Refactor B2 — último orquestador, cierra la cola completa de orquestadores (queda solo `UserDAO`, auth-sensible). 3 métodos con transacción manual propia (`save`, `crearDesdeFactura`, `crearDesdePresupuesto`) y 2 instanciaciones anidadas de DAOs hermanos ya migrados (`FacturaDAO`, `PresupuestoDAO`). Trazabilidad: Claude Code lidera. **Multi-IA usado** (riesgo alto, orquestador). Gemini consultado vía bloque IDE (planificación ANTES de implementar): confirmó mover la variable local `conn` de los 3 métodos transaccionales al campo `this.conn` (mismo patrón validado en B2-15, sin anidamiento de transacción interna esta vez); confirmó las 2 instanciaciones anidadas pasando `this.conn`; confirmó que no hay riesgo de integridad referencial (solo cambia el origen de la `Connection`); sin wrinkles adicionales. Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep independiente): confirmó las 15 llamadas a `DatabaseManager.getConnection()` en el DAO original (mi conteo inicial de 16 tenía un error de suma, la lista de métodos era correcta); confirmó las 27 ocurrencias de `new AlbaranDAO(` en 6 ficheros únicos pero **corrigió el conteo por fichero** (`AlbaranDAOTest` real 10, no 8; `EntityImportServiceAlbaranTest` real 13, no 15 — total sigue siendo 27); confirmó ausencia de `requireSqlIdentifier`/`quoteIdentifier`; confirmó que `FacturasView`/`PresupuestosView` ya importan `DatabaseManager`; confirmó que ningún otro DAO de la cola instancia `new AlbaranDAO(` dentro de sí mismo (es el último orquestador); confirmó `lineas_albaran.albaran_id` con `ON DELETE CASCADE`. Discrepancias de conteo corregidas antes de implementar, sin impacto en el plan — mismo patrón recurrente desde B2-12.

Cambios: `AlbaranDAO(Connection conn)` reemplaza las 15 llamadas internas a `DatabaseManager.getConnection()` (`findAll`, `findById`, `findLineas`, `crearDesdeFactura` [instancia `FacturaDAO` + variable local `conn`], `crearDesdePresupuesto` [instancia `PresupuestoDAO` + variable local `conn`], `save` [variable local `conn`], `insert`, `update`, `saveLineas` ×2, `updateEstado`, `actualizarFacturaId`, `delete`). Los 3 métodos transaccionales cambiados de `Connection conn = DatabaseManager.getConnection();` (variable local) a usar directamente `this.conn` — mismo objeto singleton, guards `externalTx`/commit/rollback/finally intactos en los 3. Las 2 instanciaciones anidadas (`FacturaDAO`, `PresupuestoDAO`) pasan a `new XxxDAO(this.conn)`. Sin identificadores dinámicos. Import `DatabaseManager` se conserva (la llamada a `DatabaseManager.generarNumeroAlbaran()` en `crearDesdeFactura`/`crearDesdePresupuesto` sigue en uso, no fully-qualified como en `FacturaDAO`).

27 call sites migrados en 6 ficheros: **producción (4 ficheros, 4 ocurrencias)** — `EntityImportService.procesarAlbaran` (inline, reusa el `conn` ya recibido como parámetro), `AlbaranesView` (campo `dao` con inicializador inline → sin inicializador, añadido al try existente del constructor compartiendo el mismo `conn` que `clienteDAO`), `FacturasView.crearAlbaran` (inline, try/catch ya existente), `PresupuestosView.crearAlbaran` (inline, try/catch ya existente). **Test (2 ficheros, 23 ocurrencias)** — `AlbaranDAOTest` (10), `EntityImportServiceAlbaranTest` (13) — ambos vía sed, `DatabaseManager` ya importado en los 2.

VibeSec ejecutado al cierre — skill genérico web, mayoría no aplica a app de escritorio JavaFX. Verificación manual propia sin hallazgos: 0 referencias residuales a `DatabaseManager.getConnection()` en el DAO, 0 `.close()` nuevo sobre la Connection singleton, 27/27 call sites migrados verificados vía grep (`new AlbaranDAO()` sin argumento: 0 resultados), parametrización SQL intacta en las 15 queries/batch, las 3 transacciones manuales preservan su semántica exacta. `/security-review` no aplicable (no es `UserDAO`).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `38f6cef` (docs cierre: `0786ed1`).

**Próximo DAO recomendado:** `UserDAO` — **último DAO de toda la cola B2**. Auth-sensible (BCrypt, roles, login). Requiere `/security-review` extra además de VibeSec, y Multi-IA obligatorio. Cierra completamente el Refactor B2 (17/17).

---

### Sprint B2-17 — UserDAO — ✅ CERRADO (2026-06-22) — **REFACTOR B2 COMPLETO (17/17)**

Decimoséptimo y último DAO del Refactor B2 — único auth-sensible de toda la cola (login, BCrypt `password_hash`, roles `UserRole`, lockout persistente anti-bruteforce para login y recuperación). Estructuralmente el más simple de los últimos sprints: sin transacción manual, sin DAOs hermanos anidados — todos los métodos públicos ya capturaban `SQLException` internamente y degradaban (log a `System.err` + `false`/`Optional.empty()`/`0`), patrón que no se tocó. Trazabilidad: Claude Code lidera. **Multi-IA obligatorio** (auth-sensible, regla reforzada de CLAUDE.md: Multi-IA + VibeSec + `/security-review`). Gemini consultado vía bloque IDE (planificación ANTES de implementar, con énfasis en seguridad): confirmó que el patrón catch-interno-y-degradar es seguro de mantener intacto; confirmó que resolver la `Connection` una vez al arrancar `AuthService` vs. por cada llamada no cambia el comportamiento (misma instancia singleton en ambos casos); confirmó que la concatenación de columnas en `readLockout`/`writeLockout` sigue siendo segura (constantes fijas); confirmó el plan de añadir `throws SQLException` a los 2 métodos `@Test` afectados; recomendó VibeSec + `/security-review` explícitos al cierre (ya obligatorios por CLAUDE.md). Codex consultado vía bloque IDE (verificación de inventario antes de implementar, vía grep independiente): **corrigió el conteo de 16 a 11 llamadas** a `DatabaseManager.getConnection()` en el DAO original (mi conteo duplicaba, la lista de métodos era correcta); confirmó las 4 ocurrencias de `new UserDAO(` en 2 ficheros (`App.java` 1, `AuthServiceTest.java` 3); confirmó ausencia de `requireSqlIdentifier`/`quoteIdentifier`; señaló con precisión que `SELECT_COLS` es otra concatenación SQL del fichero (constante estática, sin riesgo); confirmó que `AuthServiceTest.java` no importaba `java.sql.SQLException`; identificó con exactitud los 2 métodos `@Test` que requerían `throws SQLException` nuevo; confirmó que `LoginView`/`AdminSetupView` no referencian `UserDAO` directamente (solo vía `AuthService`); confirmó ausencia de tests que dependan de conexiones distintas por llamada.

Cambios: `UserDAO(Connection conn)` reemplaza las 11 llamadas internas a `DatabaseManager.getConnection()` (`findByUsername`, `findById`, `createUser`, `updateUser`, `deleteUser`, `getAllUsers`, `updateSecurityQuestion`, `updateLastLogin`, `readLockout`, `writeLockout`, `hasAdmin`). Ningún método público cambió firma, lógica SQL, ni manejo de errores — el catch interno que degrada a `false`/`Optional.empty()`/log a `stderr` permanece exactamente igual. Import `DatabaseManager` eliminado del DAO (sin otros usos, a diferencia de `ClienteDAO`/`AlbaranDAO` que conservan el import por `quoteIdentifier`/`generarNumeroAlbaran`).

4 call sites migrados en 2 ficheros: `App.java:59` (único consumidor de producción, inline `new AuthService(new UserDAO(DatabaseManager.getConnection()))` dentro de `start(Stage) throws Exception`, sin try/catch nuevo); `AuthServiceTest.java` (3 ocurrencias vía sed — `setUp()` ya declaraba `throws Exception`; los 2 métodos `@Test` `lockoutLoginPersisteAlReiniciarAuthService()` y `lockoutRecuperacionPersisteAlReiniciarAuthService()` no declaraban `throws` y requirieron añadir `throws SQLException` + `import java.sql.SQLException;` nuevo — único wrinkle nuevo de todo el Refactor B2, no visto en sprints anteriores porque ningún otro DAO se instanciaba directamente dentro de un método `@Test` sin `throws` ya declarado).

VibeSec ejecutado al cierre — skill genérico web, mayoría no aplica a app de escritorio JavaFX; verificación manual propia centrada en lo auth-sensible: (1) `colFailed`/`colUntil` en los 8 call sites de `readLockout`/`writeLockout` usan exclusivamente las 4 constantes internas `COL_LOGIN_FAILED`/`COL_LOGIN_UNTIL`/`COL_REC_FAILED`/`COL_REC_UNTIL`, nunca un parámetro externo — sin riesgo SQLi; (2) los catches solo loguean `e.getMessage()` (mensaje JDBC), nunca `password_hash`/`security_answer_hash`; (3) 0 `.close()` en el fichero, sin fuga de la Connection singleton; (4) lógica de lockout anti-bruteforce intacta. `/security-review` ejecutado al cierre (obligatorio por tocar auth) — 0 hallazgos de alta confianza: resolver la Connection una vez al arrancar `AuthService` vs. por llamada no introduce regresión (misma instancia singleton); `SELECT_COLS` es constante estática sin input externo; columnas de lockout confirmadas como constantes internas. Reporte completo: "No high-confidence vulnerabilities identified."

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151 (incluye `AuthServiceTest` 7/7). Commit: `922c501` (docs cierre: `1e925ab`).

**Refactor B2 — COMPLETO.** 17/17 DAOs migrados a inyección de `Connection` por constructor. No queda ningún DAO pendiente en esta iniciativa.

---

### Sprint CALENDARIO-LOCALE — Locale dinámico en CalendarioView — ✅ CERRADO (2026-06-22)

Cierre del último gap arquitectónico i18n conocido (documentado desde i18n-15): `CalendarioView` usaba `private final Locale esES = new Locale("es", "ES")` fijo para nombres de mes (`getDisplayName(TextStyle.FULL, esES)`) y formato de fecha completa (`DateTimeFormatter.ofPattern(..., esES)`), ignorando el idioma activo de la app en los 6 idiomas soportados (es/en/ca/eu/fr/gl). Tarea pequeña, 2 ficheros, mecánica, bajo riesgo → **un agente, sin Multi-IA** (decisión documentada: cambio trivial — añadir un getter de lectura, sin lógica nueva — validación objetiva de compilación+151/151 fue suficiente, consultar Gemini/Codex no aportaba valor sobre el coste de cuota).

Cambios: `LanguageManager.getLocale()` nuevo (`Locale.forLanguageTag(idiomaActual)`, reutiliza el campo privado ya existente). `CalendarioView.java`: campo `esES` eliminado; sus 2 usos reemplazados por `LanguageManager.getInstance().getLocale()`.

Fuera de alcance (no tocado, cambio quirúrgico): el patrón `"d 'de' MMMM 'de' yyyy"` sigue con la palabra "de" literal en español hardcodeada — es un patrón de formato de fecha, no un nombre de mes/día, y traducirlo requeriría una clave i18n nueva por idioma con su propio patrón `DateTimeFormatter` (ca/eu/fr/gl tienen otra sintaxis). Nuevo gap documentado para sesión futura si se prioriza.

VibeSec manual: `idiomaActual` solo se fija vía `setIdioma(nv.codigo())` en `ConfiguracionView.java:426`, código viene de un selector (ComboBox), no de input de texto libre — sin riesgo de inyección de Locale arbitrario. `/security-review` no aplicable (sin auth/datos sensibles).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `5e8f841` (docs cierre: `e772b78`).

---

### Sprint OLLAMA-PATH — fix defensivo OLLAMA_MODELS en OllamaManager — ✅ CERRADO (2026-06-22)

Bug reportado por el usuario: chat IA siempre devuelve "Error: Ollama encontró un error interno. Reinicia Ollama e inténtalo de nuevo." (HTTP 500), persistente tras reiniciar Ollama N veces. Tarea pequeña, 1 archivo, mecánica tras diagnóstico → **un agente, sin Multi-IA** (decisión documentada: causa raíz no era ambigua tras reproducción directa, el fix es defensivo y trivial).

**Diagnóstico (no era bug de Gráficas Mulberry):** se reprodujo la llamada real con `curl http://localhost:11434/api/generate` (modelo `llama3.2` instalado) y Ollama devolvió el error real (nuestro código nunca lee/loguea el body en HTTP 500, solo el switch genérico por status code): `"llama-server process has terminated: exit status 1: error loading model: ... failed to load model from C:\Users\Gipsy Dávy\.ollama\models\blobs\sha256-..."`. Causa: el perfil de Windows del usuario es `C:\Users\Gipsy Dávy` (tilde + espacio); Ollama guarda modelos en `%USERPROFILE%\.ollama\models` por defecto, y el `llama-server.exe` interno (basado en llama.cpp) tiene un bug conocido en Windows al decodificar rutas no-ASCII pasadas al subproceso — la ruta se corrompe (mojibake) y falla la apertura del fichero del modelo, por eso el 500 es constante y no se arregla reiniciando. Se verificó que el blob (2.02 GB) y el manifest existen correctamente — no es descarga corrupta ni modelo ausente. `OllamaManager.java` no se había modificado nunca desde su creación (commit único `1ab7b4f`) — descartado como regresión de código; el cambio de seguridad SEC-10 en `OllamaService.java` (auditoría `6268479`) solo limita longitud de texto, no relacionado.

**Cambio aplicado (defensivo, no corrige la instalación ya rota del usuario):** `OllamaManager.startIfNeeded()` ahora llama a `configurarRutaModelos(pb)` antes de `pb.start()` — fija la variable de entorno `OLLAMA_MODELS` a `%LOCALAPPDATA%\GraficasMulberry\ollama-models` (ruta sin tildes/espacios, propia de la app) solo para el proceso `ollama.exe serve` que **nuestra app** arranca en frío. Si Ollama ya está corriendo (caso del usuario — autoarrancado por su propio instalador/servicio), `isRunning()` es `true` y la app nunca llama `ProcessBuilder`, por lo que este fix no toca esa instancia. Protege a futuros usuarios con caracteres no-ASCII en su perfil de Windows que dependan de que la app arranque Ollama por primera vez.

**Fix real para el usuario (manual, fuera del código):** variable de entorno `OLLAMA_MODELS=C:\OllamaModels` (o ruta similar sin tildes/espacios) + reiniciar Ollama + `ollama pull llama3.2` de nuevo para que el modelo se descargue en la ruta limpia.

VibeSec manual: `modelsDir` se construye solo con `LOCALAPPDATA` (env var del SO) + literales fijos, sin input de usuario — sin riesgo de path traversal. `pb.environment().put(...)` no usa shell, sin inyección. Sin secretos ni datos sensibles tocados. `/security-review` no aplicable (no toca auth/datos sensibles).

Validación: `mvnw clean compile` limpio + `mvnw test` → 151/151. Commit: `fe69651` (docs cierre: `cfddd86`; paquete v14.0.1: `e7e32ba`).

---

### Punto de entrada exacto para el próximo sprint

**HEAD real:** `e7e32ba` — `chore: bump version a v14.0.1 y empaquetar instalador`. Rama: `master`. Tests verificados por Claude Code antes del empaquetado: **151/151 verdes**. App funcional. Versión de aplicación: **v14.0.1** (`AppConstants.APP_VERSION`). Instalador generado: `output/GraficasMulberry-Instalador-v14.0.1.exe` (117.5 MB) + copia histórica `installer/v14.0.1-nsis/`.

**Estado de iniciativas grandes:** migración i18n de vistas completa; gaps i18n de import/export cerrados; `CalendarioView` ya usa Locale dinámico; **Refactor B2 completo 17/17 DAOs** (`TarifaTramoDAO`, `NotaCalendarioDAO`, `ColumnConfigDAO`, `DynamicColumnValueDAO`, `ConsumoMaterialDAO`, `TarifaDAO`, `NominaDAO`, `PedidoDAO`, `MaterialDAO`, `PagoMaterialDAO`, `PagoPedidoDAO`, `EmpleadoDAO`, `ClienteDAO`, `PresupuestoDAO`, `FacturaDAO`, `AlbaranDAO`, `UserDAO`).

**Ollama:** bug HTTP 500 diagnosticado como causa externa (`llama-server` + ruta Windows con tilde/espacio en `C:\Users\Gipsy Dávy\.ollama\models`). Código mitigado defensivamente en `OllamaManager` (`OLLAMA_MODELS` a ruta limpia cuando la app arranca Ollama). Para la máquina actual del usuario queda acción manual fuera del código: definir `OLLAMA_MODELS=C:\OllamaModels` o ruta limpia equivalente, reiniciar Ollama y ejecutar `ollama pull llama3.2` de nuevo.

**No hay sprint activo ni cola obligatoria abierta.** Próxima sesión: preguntar al usuario por el nuevo foco antes de implementar. Candidatos no bloqueantes: traducir el patrón literal `"de"` en el formato de fecha larga de `CalendarioView`; revisar `DatabaseManager` solo si aparece una necesidad real de concurrencia/multiusuario; o abordar nueva feature/bug que indique el usuario.

**Técnicas reutilizables confirmadas:**
1. PowerShell trata nombres de variable como case-insensitive (`$eA`/`$EA` colisionan). Usar nombres claramente distintos.
2. PowerShell `` `n `` dentro de string inserta salto real; para valores `.properties` que requieren dos caracteres, usar literal `\n`.
3. Antes de cerrar cualquier sprint i18n, revisar todas las claves `tf()` con `{0}` en los 6 bundles buscando apóstrofes simples sin escapar.
4. Al insertar líneas nuevas en `.properties` con emoji/acentos, preferir script UTF-8 controlado y revisar mojibake. Mantener alineación de `=` por longitud de clave, no copiando espacios a mano.

**Comando de verificación al inicio de sesión:**
```powershell
cd "C:\Users\GipsyDavy\MAVEN\Graficas Mulberry"
.\mvnw.cmd test   # debe dar 151/151 BUILD SUCCESS
git log --oneline -5
```

**Archivos clave del sistema i18n:**
- `src/main/java/org/gipsybuho/service/LanguageManager.java` — singleton, `t()`, `tf()`, fallback ES, UTF-8.
- `src/main/resources/org/gipsybuho/i18n/messages_es.properties` — bundle base (fuente de verdad de claves).
- `src/main/resources/org/gipsybuho/i18n/messages_{en,ca,eu,gl,fr}.properties` — traducciones.
- `src/test/java/org/gipsybuho/service/LanguageManagerTest.java` — 5 tests.

---

### CHECKLIST SPRINT i18n-10 — EmpleadosView — ✅ EJECUTADO (ver resultado abajo)

Pasos 0-6 ejecutados según el patrón. Hallazgo Paso 1: `private TextField tf(String v)` SÍ existía (línea 558 original) → renombrado a `txf()` + 10 call sites. `btn(String t,...)`/`col(String t,...)` no colisionan, sin tocar.

Codex (revisión post-implementación) detectó y corrigió 2 strings fuera del checklist original: título del diálogo de columnas (`DynamicColumnRuntime` 2º arg) y prefijo de fichero exportado — ambos migrados a `t("nav.empleados")`. Ver nota en "Estado del sistema i18n al cierre" arriba.

Validación final: `mvnw clean compile` limpio + `mvnw test` → **151/151 BUILD SUCCESS** (verificado independientemente por Claude Code y por Codex).

**Pendiente para cerrar la sesión:** commit (`feat(i18n): migrar EmpleadosView a LanguageManager — Sprint i18n-10` + `docs(state): cerrar Sprint i18n-10...`).

---

### CHECKLIST SPRINT i18n-11 — MaterialesView — ✅ EJECUTADO (ver resultado abajo)

Pasos 0-6 ejecutados según el patrón. Paso 1: sin colisión `private TextField tf(...)` previa al renombrado — se renombró igualmente el helper `private TextField tf(String v)` a `txf()` (10 call sites) por consistencia con el patrón establecido. `btn(String t,...)`/`lbl(String t)`/`col(String t,...)`/`colConsumo(String t,...)` no colisionan, sin tocar.

Decisión Paso 3 (aplicada proactivamente, sin Codex): `DynamicColumnRuntime` 2º arg y prefijo `fc.setInitialFileName(...)` migrados a `t("nav.materiales")` desde el inicio, replicando el hallazgo de Codex en i18n-10 en vez de perpetuar el gap.

142 claves `materiales.*` en los 6 bundles, paridad verificada (`diff` por nombre de clave, 0 diferencias). Sin apóstrofes problemáticos en CA/FR — todos los valores `tf()` reformulados para evitarlos (verificado por grep, 0 coincidencias).

Validación final: `mvnw clean compile` limpio + `mvnw test` → **151/151 BUILD SUCCESS**.

Commit: `0799fb5` — `feat(i18n): migrar MaterialesView a LanguageManager — Sprint i18n-11`.

---

### CHECKLIST SPRINT i18n-12 — TarifasView — ✅ EJECUTADO (ver resultado abajo)

Pasos 0-6 ejecutados según el patrón. Paso 1: conflictos más extensos que en sprints previos — `Tarifa t` se usaba como **parámetro de método completo** en `dialogo(Tarifa t)` (no solo lambda), más lambdas `t ->` en `cargar()` (x2), `nueva()`, `editar()`, `actualizarFiltroTecnicas()` (String técnica), y variables locales `Tarifa t` en `lanzarExportacion()`/`previsualizar()`. Todos renombrados a `tarifa`/`tec`. Helper `private TextField tf(String v)` renombrado a `txf()` (7 call sites). `btn(String t,...)`/`lbl(String t)` no colisionan, sin tocar.

Hallazgo propio (no Codex): el sentinel de filtro `"Todas"` (3 ocurrencias: comparación + 2 asignaciones) no es valor BD sino label de UI — migrado a `t("tarifas.filtro.todas")` consistentemente en las 3 ocurrencias para no romper el filtro en otros idiomas.

74 claves `tarifas.*` en los 6 bundles, paridad verificada (diff por nombre de clave, 0 diferencias). Reutilizadas claves compartidas `export.dialog.*`, `export.exito.*`, `export.fmt.*.label/.sqlite.desc` sin redefinir.

Decisión Paso 3 (aplicada desde el inicio): `DynamicColumnRuntime` 2º arg y prefijo `fc.setInitialFileName(...)` migrados a `t("nav.tarifas")`, replicando patrón i18n-10/i18n-11.

`mostrarError(Exception e)` con `"Error: " + e.getMessage()` — verificado que AlbaranesView y FacturasView (ya migradas) dejan el mismo patrón intacto; no se toca, consistente con precedente.

Validación final: `mvnw clean compile` limpio + `mvnw test` → **151/151 BUILD SUCCESS** (verificado independientemente por Claude Code y por Codex vía bloque IDE).

Codex (revisión post-implementación, bloque IDE): sin hallazgos bloqueantes, sin gap nuevo tipo i18n-10. Confirmó shadowing resuelto, rename `tf()→txf()` completo, paridad de bundles, placeholders `{0}` intactos en las 7 claves `tf()` en los 6 idiomas.

VibeSec: LIMPIO — app de escritorio, sin XSS/CSRF/SSRF/auth aplicable; solo extracción de strings UI + rename de variables.

---

### CHECKLIST SPRINT i18n-13 — ComprasProveedorView — ✅ EJECUTADO (ver resultado abajo)

Pasos 0-6 ejecutados según el patrón. Paso 1: único conflicto `private TextField tf(String v)` → renombrado a `txf()` (4 call sites). `btn(String t,...)`/`lbl(String t)` no colisionan, sin tocar. Sin conflictos `PagoMaterial p`/`Material m` (no se usaba `t` como param/lambda var en este archivo, a diferencia de TarifasView).

Decisión Paso 3: `ComprasProveedorView` no usa `DynamicColumnRuntime` ni import/export/previsualización (alcance más simple que sprints previos) — no aplica el gap de i18n-10.

Decisión arquitectónica: claves propias `compras.*` en vez de reutilizar `materiales.pagos.*` (i18n-11), aunque ambas vistas comparten `PagoMaterialDAO`/`PagoMaterial` — textos ligeramente distintos entre vistas y cambio quirúrgico por módulo independiente.

No traducido (valores BD/lógica interna): array `FORMAS_PAGO` y su uso en `calcVencimiento`; códigos de filtro internos `"todos"/"pendiente"/"vencido"/"proximo"/"pagado"`; `DateTimeFormatter.ofPattern("dd/MM/yyyy")`; guion `"—"` en celda días cuando pagado; `String.format("%.2f €", v)` en renderer numérico de columna importe.

60 claves `compras.*` en los 6 bundles, paridad verificada (diff por nombre de clave, 0 diferencias). Sin apóstrofes problemáticos en `tf()` de CA/FR — reformulados (`à modifier`/`à marquer`/`à supprimer` en vez de `l'achat`, "Erreur de sauvegarde" en vez de "Erreur de l'enregistrement", "Supprimer cet achat" en vez de "Supprimer l'achat") para evitar el escapado `''` de MessageFormat.

Incidencia técnica resuelta durante el sprint: inserción de emoji/€/acentos vía `Edit` tool en bloques grandes produjo mojibake (doble-codificación UTF-8) en el primer intento del bundle EN. Solución: reconstrucción vía `PowerShell` con `[System.IO.File]::WriteAllLines()` y caracteres especiales construidos por code-point Unicode explícito (`[char]0xXXXX`, `[System.Char]::ConvertFromUtf32(...)`), sin BOM. Repetido para CA/EU/GL/FR sin nueva corrupción. Dos erratas menores propias detectadas y corregidas tras verificación por `grep`: "Núm." sin acento en GL (variable PowerShell mal interpolada) y "é modifier"/"étre" en vez de "à modifier"/"être" en FR (code point equivocado). Ambas corregidas antes de cerrar.

Validación final: `mvnw clean compile` limpio + `mvnw test` → **151/151 BUILD SUCCESS**.

---

### CHECKLIST SPRINT i18n-14 — EstadisticasView — ✅ EJECUTADO (ver resultado abajo)

Pasos 0-6 ejecutados según el patrón. Paso 1: sin conflictos de naming. Único identificador `t` en el archivo es la variable local `Tab t = new Tab(texto, contenido)` dentro del helper privado `tab()`; ese método no llama a la función global `t()` en su scope, así que no requiere rename (regla 4 de "Reglas i18n consolidadas").

Decisión Paso 3: `EstadisticasView` no usa `DynamicColumnRuntime` ni diálogo de columnas — no aplica el gap de i18n-10. Sí tiene exportación PDF y previsualización, ambas migradas.

Categoría nueva en este sprint (primera vista de gráficos migrada): títulos de gráfico (`barChart`/`lineChart`/`pieChart`) y nombres de serie (leyenda) son texto visible al usuario y se migraron como claves `estadisticas.chart.*` / `estadisticas.serie.*`. La clave `estadisticas.chart.ingresos_tecnica` se reutiliza para un pie chart y un bar chart distintos (mismo texto español "Ingresos por técnica (€)" en ambos contextos).

No traducido (no son literales de UI o son datos/estilo): array `PALETA` y constantes `COLOR_INGRESOS/GASTOS/NOMINAS/PRIMARIO/AZUL` (valores hex); strings `"#HEXCOLOR"` pasados a `PieChart.Data`; estilos inline `"CHART_COLOR_N:..."`; guion `"—"` como placeholder de "sin clave" en `primeraClave()`; elipsis `"…"` de truncado en `serie()`; `ex.getMessage()` crudo en el `Alert` de error de exportación PDF (no es literal hardcodeado).

49 claves `estadisticas.*` en los 6 bundles, paridad verificada (diff por nombre de clave, 0 diferencias). 8 claves usan `tf()` con placeholders `{0}`; en FR se evitaron apóstrofes en esas 8 (no fue necesario reformular ninguna, ya que ninguna requería posesivo/elisión en francés).

Incidencia técnica nueva detectada y corregida: nombres de variable PowerShell son **case-insensitive** — `$eA` (é minúscula) y `$EA` (É mayúscula) se resolvieron como la misma variable, y la segunda asignación sobrescribió la primera, produciendo "BÉnÉfice"/"GÉnération"/"EnregistrÉ" con É mayúscula indebida en todo el bloque FR. Detectado vía `grep` post-escritura. Solución: eliminar bloque corrupto (incluyendo un duplicado accidental generado durante el primer intento de fix) restaurando el archivo a su longitud previa al sprint, y reescribir usando nombres de variable inequívocos (`$eLow`/`$eCap`) en vez de mayúscula/minúscula del mismo nombre. Una errata adicional de diacrítico en CA ("mès"→"més") detectada y corregida igual que en sprints previos.

Validación final: `mvnw clean compile` limpio + `mvnw test` → **151/151 BUILD SUCCESS**. Codex (revisión post-implementación, bloque IDE): sin hallazgos, sin literales sin migrar, paridad 49/49 confirmada, sin mojibake, scope respetado (PALETA/COLOR_*/CHART_COLOR_*/"—"/"…"/`ex.getMessage()` intactos).

VibeSec: LIMPIO — app de escritorio, sin XSS/CSRF/SSRF/auth aplicable; solo extracción de strings UI.

---

### CHECKLIST SPRINT i18n-15 — CalendarioView — ✅ EJECUTADO (última vista pendiente — i18n COMPLETO al 100%)

Pasos 0-6 ejecutados según el patrón. Paso 1: sin conflictos de naming — `CalendarioView.java` no tiene ningún identificador `t`/`tf` (ni local var ni método privado), import directo sin renombrados.

11 sitios de literal migrados a 18 claves `calendario.*` (título, botón "Hoy", 7 abreviaturas de día, título de diálogo con `tf()`, botones guardar/cerrar, labels notas existentes/nueva/añadir, prompts de título y detalle, error de guardado con `tf()`).

No traducido (no son literales de UI o son deuda arquitectónica pre-existente, no corregida en este sprint quirúrgico): flechas de navegación `"◀"`/`"▶"`, punto de nota `"●"`, botón cerrar `"✕"`; estilos inline `-fx-...`; `System.err.println(...)` de log de consola (no visible en UI, precedente nuevo este sprint); `Locale esES = new Locale("es","ES")` usado en `DateTimeFormatter` para nombre de mes/día — el calendario seguirá mostrando nombres de mes/día en español sin importar el idioma de la app; gap de i18n conocido y documentado, no corregido aquí por ser cambio de arquitectura fuera del alcance de una extracción quirúrgica de strings.

18 claves `calendario.*` en los 6 bundles, paridad verificada (0 diferencias). 2 claves usan `tf()` con placeholder `{0}` (`calendario.dialogo.titulo`, `calendario.error.guardar`).

Erratas propias detectadas y autocorregidas antes de la revisión de Codex: GL con variables PowerShell no definidas (`$eLowGL`/`$aLowGL` nunca asignadas) produjo días truncados "Mr"/"Sb" en vez de "Mér"/"Sáb"; FR con acento equivocado ("rèunion" grave en vez de "réunion" agudo); FR con salto de línea real (backtick-n de PowerShell) insertado dentro de un valor `.properties`, partiendo la línea en dos y dejando `{0}` huérfano.

**Hallazgo real de Codex (no autodetectado):** clave CA `calendario.error.guardar` (consumida vía `tf()`, con placeholder `{0}`) contenía un apóstrofe simple sin escapar en "s'ha" — inseguro para `MessageFormat`. Corregido por reformulación (no escaping `''`, consistente con la política del proyecto): `"No ha estat possible desar la nota:\n{0}"`. Confirma el valor de la revisión Codex obligatoria al cierre incluso en sprints "pequeños y mecánicos".

Validación final: `mvnw clean compile` limpio + `mvnw test` → **151/151 BUILD SUCCESS** (re-ejecutado tras el fix CA, sigue verde). Codex (revisión post-implementación, bloque IDE): 1 hallazgo real (apóstrofe CA, corregido), resto limpio — 16 `t()` + 2 `tf()` correctos, paridad 18/18, sin mojibake, scope respetado.

VibeSec: LIMPIO — app de escritorio, sin XSS/CSRF/SSRF/auth aplicable; solo extracción de strings UI.

**CIERRE i18n: las 17 vistas de la aplicación están migradas a `LanguageManager`. No queda ninguna vista pendiente.**

---

### Reglas i18n consolidadas (todas las sesiones)

**Naming conflicts — prioridad al resolver antes de añadir imports:**
1. `private TextField tf(String v)` → renombrar a `txf()` + actualizar todos los call sites.
2. `TableView<X> t` local var → renombrar a nombre descriptivo (ej: `tLineas`).
3. `Tipo t` en lambda → renombrar a nombre descriptivo (ej: `tarifa`).
4. `for (TextField tf : ...)` sin llamada a `tf()` dentro → NO renombrar.
5. Params `btn(String t, ...)`, `lbl(String t)`, `col(String t, ...)` → NO renombrar (no colisionan).

**Regla apostrofes MessageFormat:**
- Clave usada via `tf(key, ...)` (tiene `{0}`, `{1}`, etc.) → `'` se dobla a `''` en CA y FR.
- Clave usada solo via `t(key)` (sin `{}`) → `'` permanece simple en todos los idiomas.
- EN, ES, EU, GL: sin apostrofes problemáticos en general (inglés usa comillas tipográficas o reformula).

**Patrón numérico en tf():**
- Double: `{0,number,0.00}` para euros. `{0,number,0.0}` para segundos.
- Int/String: `{0}` sin formato.

**Claves compartidas export — nombres EXACTOS en los bundles:**
```
export.dialog.instruccion   (t)
export.dialog.btn           (t)
export.dialog.guardar       (tf — arg: nombre del formato)
export.exito.titulo         (t)
export.exito.mensaje        (tf — arg: ruta destino)
export.fmt.sqlite/csv/sql/json/pdf/word/excel .label   (t, compartidos)
export.fmt.sqlite.desc      (t, compartido — descripción SQLite genérica)
```
Las `export.fmt.<fmt>.desc` de cada módulo son **específicas**: `empleados.export.csv.desc`, etc.

**No traducir:**
- Valores de ComboBox/enums almacenados en BD (ej: estado `"borrador"`, tipo `"empresa"`).
- `COLUMNAS_BASE` static map — valores son claves BD, no UI.
- DB column names, field keys, lógica interna.

### Decisiones consolidadas (todas las sesiones)
- Hot-swap de idioma NO implementado — requiere reinicio. Intencional.
- `TamanoFuente.key` guarda claves i18n (no labels) — `t(ts.key())` se llama en build time.
- COMERCIAL no tiene permiso COMPRAS — mínimo privilegio (Gemini).
- Tab pagos en MaterialesView NO se toca — coexiste con ComprasProveedorView sobre mismo DAO.
- `PreferenceService` singleton se resetea en tests via reflexión — no tocar código producción.

### Qué se hizo en la sesión 2026-06-15 (GAP-5 Compras a Proveedor)

**Sprint GAP-5** (`d243cbe`) — Módulo Compras a Proveedor:

`ComprasProveedorView.java` (nuevo) — vista standalone sobre `pagos_material` + `PagoMaterialDAO` ya existentes. Tabla con columnas: estado (dot color), material, proveedor, nº factura, fecha compra, importe, forma pago, vencimiento, días, notas. Filtros: Todos / Pendientes / Vencidos / Próximos / Pagados. Resumen de 3 KPIs (total pendiente, vencidos, próximos 7 días). CRUD completo: nueva compra, editar, marcar pagado (con DatePicker), eliminar. Hint bar de principiante + lblContador.

`UserPermissions.COMPRAS = "compras"` — nuevo permiso. Asignado a ADMINISTRADOR, PRODUCCION, CONTABILIDAD (no COMERCIAL — mínimo privilegio por decisión Gemini).

`Icons.SHOPPING_BAG` — nuevo icono SVG. Sidebar grupo COMERCIAL.

Tab pagos en MaterialesView mantenido sin cambios (YAGNI). Ambas vistas comparten el mismo DAO/tabla sin conflicto.

Pendiente menor: F1 no vinculado al módulo "compras" en HelpService — abre ayuda general.

Multi-IA: Claude Code lidera. Gemini consultado (bloque IDE — decisión de roles, UX standalone, icono, agrupación sidebar). VibeSec: LIMPIO. Tests: 146/146 verdes.

---

### Qué se hizo en la sesión 2026-06-15 (Sprint A hint bars + Sprint B tests)

**Sprint A** (`cd91f15`) — Hint bars en 4 módulos:

Añadido `buildBeginnerHint()` en FacturasView, PedidosView, MaterialesView, EmpleadosView. Texto accionable por módulo + referencia a F1. Binding a `PreferenceService.beginnerModeProperty()` (visible+managed), sin impacto en layout cuando modo avanzado. CSS class `beginner-hint` ya existente en styles.css — sin tocar.

**Sprint B** (`74910eb`) — PreferenceServiceTest:

4 tests: `beginnerModeDefaulteaFalso`, `setBeginnerModePersiste`, `isFirstRunEsTrueEnPrimerArranque`, `markFirstRunCompletedMarcaComoCompletado`. Patrón: `@TempDir` + system property override + reset singleton por reflexión. Sin modificación de código de producción.

VibeSec: LIMPIO — strings hardcodeados, sin entrada de usuario, sin SQL nuevo, sin auth. Aplicación de escritorio → XSS/CSRF/SSRF no aplican.

Multi-IA: Claude Code lidera. Gemini disponible como soporte pero no invocado — tarea UI pura + test sin incertidumbre arquitectónica. Validación objetiva local suficiente.

Tests: 142/142 → 146/146 tras Sprint B.

---

### Qué se hizo en la sesión 2026-06-15 (contador de filas en 9 módulos)

**Sprint UI-COUNTER** (`d93dc76`) — `lblContador` en todos los módulos:

Añadido `private Label lblContador = new Label()` como campo en los 9 módulos. Insertado en la toolbar HBox entre el campo de búsqueda y el spacer (`Region` de `HgrowPriority.ALWAYS`). Actualizado en cada carga/filtro tras `datos.setAll(lista)`.

| Vista | Toolbar HBox | Texto contador |
|---|---|---|
| `MaterialesView` | `buildToolbarStock()` | `N materiales` |
| `TarifasView` | `buildToolbar()` | `N tarifas` |
| `ClientesView` | `buildToolbar()` | `N clientes` — actualiza en `cargar()` y en `buscar()` |
| `FacturasView` | `buildToolbar()` | `N facturas` |
| `PedidosView` | `buildToolbarPedidos()` | `N pedidos` |
| `AlbaranesView` | `buildToolbar()` | `N albaranes` |
| `PresupuestosView` | `buildToolbar()` | `N presupuestos` |
| `EmpleadosView` | `buildToolbar()` | `N empleados` |
| `NominasView` | `buildToolbar()` | `N nóminas` |

CSS: `.row-counter` añadida en `styles.css` (`-fx-text-fill: -c-text-secondary; -fx-font-size: 11px`).

ClientesView: caso especial — `txtBuscar` es variable local de `buildToolbar()` (no campo de clase); el contador se actualiza tanto en `cargar()` como en `buscar()` para reflejar el tamaño real de `datos` tras cada operación.

Multi-IA: Claude Code solo. UI pura, mecánica, sin auth/BD/red. VibeSec: N/A. Tests: 142/142.

---

### Qué se hizo en la sesión 2026-06-15 (MIGRACION-COMPLEJA cerrada)

**Sprint MIGRACION-COMPLEJA** (`9b1d950`) — Importación de los 6 CSVs pendientes:

Script: `scripts/importar_materiales.py` (nuevo). Bypass del wizard JavaFX; inserta directamente en SQLite con queries parametrizadas. SKIP_IF_EXISTS por `(nombre, proveedor)`. Soporta `--dry-run`.

| CSV | Filas | Insertadas | Dups | Categoria |
|---|---:|---:|---:|---|
| `1_precios_papel_proveedor.csv` | 84 | 72 | 12 | papel proveedor |
| `2_precios_papel_por_gramaje.csv` | 330 | 330 | 0 | papel gramaje |
| `3_union_papelera_otros_productos.csv` | 34 | 34 | 0 | varios (SOBRES, PEGATINA...) |
| `5a_material_tintas.csv` | 4 | 4 | 0 | tintas |
| `5b_material_plastico.csv` | 17 | 17 | 0 | plastico |
| `5c_material_otros_limpio.csv` | 5 | 5 | 0 | consumibles |
| **TOTAL** | **474** | **462** | **12** | |

Estado BD post-import: 477 materiales totales (15 son test data de sesiones previas — categoria `test pedidos`, IDs 112-126, no son materiales reales).

Proveedores: UNION_PAPELERA (152), MRPAPEL (137), FEDRIGONI (133), CODIAL (18). Nota: "UNIÓN PAPELERA" (con tilde, 9 filas) y "UNION_PAPELERA" (sin tilde, 152) son la misma empresa — normalizar si es necesario.

Multi-IA: Claude Code solo. BD local SQLite, sin red, sin auth, CSVs ya validados. Queries parametrizadas. VibeSec: 0 vulnerabilidades.

**Datos de test pendientes de limpieza:** materiales IDs 112-126 (categoria `test pedidos`) no son reales. Pedir autorización del usuario antes de eliminar.

---

### Qué se hizo en la sesión 2026-06-15 (INSTALLER-REPRO cerrado)

**INSTALLER-REPRO** — Pipeline reproducido con éxito con código HEAD `e0a4252` (UI-E+UI-F+GAP-1+GAP-2):

| Paso | Herramienta | Resultado |
|---|---|---|
| 1. Build | Maven (IntelliJ) + JDK-26 + `-Ppackage-windows` | OK |
| 2. App-image | `jpackage --type app-image` | OK — `output/GraficasMulberry/` |
| 3. Gráficos | `gen_graphics.py` (Pillow) | OK — `nsis-welcome.bmp` + `nsis-header.bmp` |
| 4. Instalador | `makensis /V2 /INPUTCHARSET UTF8 installer.nsi` | OK — 117.3 MB |

Salida: `output/GraficasMulberry-Instalador-v13.5.0.exe` (117.3 MB).
Copia en historial: `installer/v13.5.0-nsis/GraficasMulberry-Instalador-v13.5.0-nsis.exe`.

Dependencias verificadas: JDK-26, Maven IntelliJ `2026.1`, NSIS x86, Python 3.14.4, Pillow OK.
Script: `build-nsis.ps1` en raíz (usar para futuros builds).
MAVEN_OPTS efímero: `-Djavax.net.ssl.trustStoreType=Windows-ROOT` si PKIX falla.

Multi-IA: Claude Code solo. Script existente, deps verificadas, sin incertidumbre técnica. VibeSec: N/A (pipeline build, sin código de usuario).

---

### Qué se hizo en la sesión 2026-06-15 (UI-F + UI-E ítem 6 cerrados)

**Sprint UI-F** (`85152bd`) — Animación de filas extendida a todos los módulos:
- `TableColumnSizing.animarFilas()`: eliminado `.limit(10)` — ahora anima todas las filas, no solo las 10 primeras.
- Hook añadido en: Presupuestos, Albaranes, Empleados, Nóminas, Materiales, Tarifas.
- Módulos ya tenían hook: Clientes, Facturas, Pedidos.
- Multi-IA: Claude Code solo. Cambio mecánico, bajo riesgo. VibeSec: N/A (UI puro). Tests: 142/142.

**Sprint UI-E ítem 6** (`e0a4252`) — Sliding pill sidebar:
- `MainView.java`: campo `navPill` (Region, `managed=false`, `mouseTransparent=true`), campo `navPillContainer` (StackPane), método `moverPill(StackPane)`.
- Construcción: `navPillContainer = new StackPane(navMenu)` → insert navPill at index 0 (detrás). `navPill.prefWidthProperty().bind(navPillContainer.widthProperty())`.
- `moverPill`: coordenadas via `localToScene` + `sceneToLocal` para tolerar scroll. Primera activación: FadeTransition 150ms. Navegaciones siguientes: TranslateTransition 200ms `EASE_BOTH`. Pill se oculta al colapsar sidebar.
- `styles.css`: `.nav-pill` con `derive(-c-accent, 80%)`, border-radius 6, opacity 0.18.
- Multi-IA: Claude Code solo. Sprint UI puro, sin auth/BD/red. VibeSec: 0 vulnerabilidades. Tests: 142/142.

---

### Qué se hizo en la sesión 2026-06-15 (GAP-1 + GAP-2 cerrados)

**Contexto:** flujo comercial completo Presupuesto → Pedido → Albarán → Factura. Los dos GAPs de funcionalidad nueva pendientes desde RELEASE-GATE.

**Implementaciones:**

- **GAP-1** (`5c2c3bf`) — Crear Pedido desde Presupuesto:
  - `PedidoDAO.crearDesdePresupuesto(int presupuestoId)` — tx, numeración automática via `DatabaseManager.generarNumeroPedido()`, descripción construida concatenando las descripciones de las líneas del presupuesto.
  - `PresupuestosView`: botón "📦 Crear Pedido" + método `crearPedido()`. Guarda diálogo de confirmación si estado ≠ "aceptado".

- **GAP-2** (`5c2c3bf`) — Crear Factura desde Albarán:
  - `FacturaDAO.crearDesdeAlbaran(Albaran albaran)` — recibe `Albaran` como parámetro (evita ciclo con `AlbaranDAO → FacturaDAO`). Crea `LineaFactura` con precio_unit=0; el usuario edita precios tras crear.
  - `AlbaranDAO.actualizarFacturaId(int albaranId, int facturaId)` — vincula la factura creada al albarán.
  - `AlbaranesView`: botón "🧾 Crear Factura" + método `crearFactura()`. Bloquea si el albarán ya tiene factura. Alerta informativa sobre precios en 0.

**Decisión de diseño:** `FacturaDAO.crearDesdeAlbaran` recibe `Albaran` en vez de `int albaranId` — `AlbaranDAO` ya importa `FacturaDAO`, importar en sentido inverso crearía ciclo. La UI carga el albarán y pasa el objeto; vincula `factura_id` en segundo paso no atómico (aceptable en SQLite desktop).

**Multi-IA:** Claude Code solo. Patrón mecánico (3 `crearDesde*` ya existían). Gemini/Codex no invocados — validación local suficiente. Gemini no invocado pese a ser tarea mediana — omisión reconocida.

**Seguridad:** VibeSec + `/security-review` ejecutados post-commit.
- 0 vulnerabilidades. SQL parameterizado en todos los nuevos métodos. Sin exposición de datos.
- Defecto menor de integridad documentado: guard de factura duplicada lee objeto cacheado, no BD. No explotable en desktop monousuario.
- HelpService: sin nuevas entradas para los flujos GAP-1/GAP-2 — pendiente decisión del usuario.

**Validación:** `BUILD SUCCESS`. Tests: 142/142.

---

### Qué se hizo en la sesión 2026-06-14 (Backlog GAPs — GAP-3/6/7 cerrados)

**Contexto:** continuación de RELEASE-GATE MANUAL (P0+P1+P2 completados sesión anterior). Esta sesión implementó todos los GAPs no bloqueantes de corto plazo identificados en el RELEASE-GATE.

**Implementaciones:**

- **GAP-6** (`04d9f25` + `1ca9d1e`) — Búsqueda en tiempo real (stream filter) en **7 módulos**:
  - `04d9f25`: Facturas, Albaranes, Presupuestos, Empleados, Nóminas
  - `1ca9d1e`: Materiales (nombre/referencia/proveedor), Tarifas (nombre/técnica/descripción)
  - Patrón: `private TextField txtBuscar` + `textProperty().addListener` + `lista.stream().filter(contiene(...))` + helper `contiene(String, String)`
  - Materiales ya tenía `chkSoloAlerta` + `cbCategoriaFiltro` — `txtBuscar` se aplica como tercer filtro en cadena en `cargar()`
  - Tarifas ya tenía `cbTecnicaFiltro` — mismo patrón, tercer filtro en cadena

- **GAP-7** (`27c3d37`) — `OllamaService.enviarConsulta()`:
  - HTTP 404 → "Modelo '...' no instalado. Instálalo desde Gestión de modelos."
  - HTTP 500 → "Ollama encontró un error interno. Reinicia Ollama e inténtalo de nuevo."
  - HTTP otro → "Error de comunicación con Ollama (código N)."
  - ConnectException/ConnectException → "Ollama no está en ejecución. Ábrelo o instálalo..."
  - "timed out" → "Tiempo de espera agotado. Ollama tardó demasiado en responder."

- **GAP-3** (`d1791e9`) — Logout in-app:
  - `Icons.java`: constante `LOGOUT` (Material Design path) + método `logout()`
  - `MainView.java`: campo `private final Runnable onLogout` + 4º param constructor `Runnable onLogout` + botón logout en `footerIconos` con diálogo confirmación → `onLogout.run()`
  - `App.java`: `new MainView(primaryStage, user, authService, this::showLogin)` — `showLogin()` reemplaza Scene; User ref cae, sesión cerrada

- **Fixes detectados en pruebas de usuario:**
  - Materiales: usuario reportó que faltaba búsqueda (no estaba en `04d9f25`) → añadido en `1ca9d1e`
  - Tarifas: mismo caso → añadido en `1ca9d1e`

**VibeSec ejecutado (GAP-3 + GAP-7):** 0 vulnerabilidades. Logout desktop: no hay tokens persistentes que revocar; reemplazar Scene + GC de MainView es suficiente.

**Multi-IA:** Claude Code solo. Cambios quirúrgicos, bajo riesgo. Codex/Gemini no invocados — validación local suficiente.

**Validación final:** `BUILD SUCCESS`. Tests: 142/142 (sin tests nuevos — lógica UI pura). Usuario confirmó PASS en pruebas manuales de Materiales, Tarifas y logout.

---

### Qué se hizo en la sesión 2026-06-14 (RELEASE-GATE MANUAL — COMPLETO)

Sprint RELEASE-GATE completado. Matriz reconstruida por Claude Code (Gemini no disponible esta sesión).

**Correcciones a matriz original Gemini (P0):**
- "Recuperar contraseña → email" → INCORRECTO. App usa pregunta de seguridad.
- "Logout → LoginView" → No existe logout in-app. "Salir" cierra app.
- "Primer arranque / AdminSetup" → SKIP: BD ya tiene admin. Requiere entorno limpio.
- "Rol EMPLEADO" → INCORRECTO. Roles reales: ADMINISTRADOR, COMERCIAL, PRODUCCION, CONTABILIDAD.

**Resultados P0:**

| Caso | Estado | Notas |
|---|---|---|
| P0-01 AdminSetup | ⏭ SKIP | BD ya tiene admin — requiere entorno limpio |
| P0-02 Login correcto | ✅ PASS | Dashboard visible |
| P0-03 Login incorrecto | ✅ PASS | Mensaje error + shake campo contraseña |
| P0-04 Lockout 5 intentos | ✅ PASS | "Demasiados intentos. Espera 5 minutos." |
| P0-05 Recuperar contraseña | ✅ PASS | Pregunta seguridad → nueva contraseña → mensaje verde |
| P0-06 Salir | ✅ PASS | App cierra limpiamente |
| P0-07 Rol COMERCIAL | ✅ PASS | Sidebar correcto |
| P0-08 Rol PRODUCCION | ✅ PASS | Sidebar correcto |
| P0-09 Clientes CRUD | ✅ PASS | Crear/editar/buscar/eliminar funciona |
| P0-10 Presupuestos CRUD | ✅ PASS | CRUD funciona; búsqueda/filtro ausente (GAP-6) |
| P0-11 Flujo negocio | ✅ PASS parcial | Presupuesto→Albarán y →Factura funcionan; ver GAP-1/GAP-2 |
| P0-12 Factura pago | ✅ PASS | Marcar pagada funciona y persiste |

**Resultados P1:**

| Caso | Estado | Notas |
|---|---|---|
| P1-01 Rol CONTABILIDAD | ✅ PASS | Sidebar correcto (DASHBOARD/FACTURAS/ALBARANES/NOMINAS/ESTADISTICAS/IA/CONFIG) |
| P1-02 Rol ADMINISTRADOR | ✅ PASS | Acceso completo |
| P1-03 Crear usuario | ✅ PASS | |
| P1-04 Cambiar contraseña | ✅ PASS | |
| P1-05 Eliminar usuario | ✅ PASS | |
| P1-06 Pedidos CRUD | ✅ PASS | Pedidos = pedidos de cliente, no compras a proveedor (ver GAP-5) |
| P1-07 Albaranes CRUD | ✅ PASS | Sin campos descripción/importe |
| P1-08 Facturas búsqueda | ⚠️ GAP-6 | Sin búsqueda/filtro — no bloqueante |
| P1-09 Materiales CRUD | ✅ PASS | |
| P1-10 Tarifas ver/editar | ✅ PASS | |
| P1-11 Empleados CRUD | ✅ PASS | |
| P1-12 Nóminas crear | ✅ PASS | Fix aplicado: UNIQUE constraint → mensaje legible (`80cc6bb`) |
| P1-13 Estadísticas | ✅ PASS | |
| P1-14 Import CSV clientes | ✅ PASS | |
| P1-15 Import CSV materiales | ✅ PASS | |
| P1-16 Export | ✅ PASS | |
| P1-17 Ayuda F1 | ✅ PASS | |
| P1-18 Onboarding | ✅ PASS | |
| P1-19 Calendario | ✅ PASS | |
| P1-20 IA/Ollama | ✅ PASS | Abre sin crash; fix instalador path acentuado (`4b0f116`) |

**Resultados P2:**

| Caso | Estado | Notas |
|---|---|---|
| P2-01 GAP-4 lockout post-reset | ✅ CONFIRMADO | Comportamiento seguro — lockout persiste |
| P2-02 Asistente Visual | ✅ PASS | |
| P2-03 Backup | ✅ PASS | |
| P2-04 Configuración persistencia | ✅ PASS | App solo en español — GAP-8: i18n backlog |
| P2-05 Presupuesto→Albarán flujo | ✅ PASS | |
| P2-06 Estabilidad cierre | ✅ PASS | |

**RESULTADO FINAL: 35/37 PASS, 1 SKIP, 1 GAP no bloqueante. App lista para release.**

**Fixes aplicados durante RELEASE-GATE:**
- `80cc6bb` — NominasView: UNIQUE constraint → mensaje legible
- `4b0f116` — OllamaInstaller: path con espacios/acentos via env var

**Multi-IA:** Claude Code lidera. Gemini no disponible. Codex no invocado — fixes quirúrgicos sin incertidumbre. VibeSec ejecutado, 0 vulnerabilidades.

---

### Qué se hizo en la sesión 2026-06-14 (Sprint UI-E ítems 5 y 7)

- **UI-E ítem 5** (Shake en campos erróneos):
  - Método estático `TableColumnSizing.shake(Node)` — 6 KeyFrames, 350ms, translateX -6/6/-4/4/0
  - Aplicado en `LoginView.handleLogin()` (2 casos), `LoginView.showRecoveryDialog()` (4 casos), `AdminSetupView.handleCreate()` (5 casos), `UserManagementView.createUser()` (5 casos) y `changeSelectedPassword()` dialog (3 casos)
  - Shake apunta al campo concreto que falla, no al mensaje de error
  - Commit `38911b3` — 142/142 tests verdes. VibeSec: 0 vulnerabilidades.

- **UI-E ítem 7** (Pattern de fondo dashboard):
  - Canvas con grid de puntos 18px step, r=1.2px, opacity=0.05
  - `managed=false`, `mouseTransparent=true` — no interfiere con layout ni interacción
  - Se redibuja automáticamente al cambiar tamaño del panel
  - Commit `051e778`

- **Multi-IA:** Claude Code solo. Sin Gemini/Codex — cambios quirúrgicos sin incertidumbre arquitectónica. Sin auth/seguridad/BD. Validación local (compilación + tests) suficiente.
- **Verificación visual (2026-06-14, sesión cierre):**
  - Patrón de puntos confirmado: crop lossless del Dashboard muestra textura regular en grid (18px step, 5% opacidad).
  - Shake confirmado: clic en "Crear usuario" con campos vacíos → mensaje rojo "Todos los campos son obligatorios." visible → `createUser()` ejecutó hasta `TableColumnSizing.shake(usernameField)`. Animación 350ms ya terminada al capturar; comportamiento esperado. Fix Codex verificado (binding `tf.translateXProperty()`).
  - App cerrada limpiamente tras verificación.
- **Fix Codex (commit `6abf2a2`):** TextField visible en modo "mostrar contraseña" no sacudía — binding `tf.translateXProperty().bind(pf.translateXProperty())` añadido a los 3 `wrapPasswordField()`. Detectado en revisión post-implementación. 142/142 tests verdes.
- **Multi-IA:** Claude Code (implementación) + Codex (revisión + fix). `/security-review` + `/VibeSec` ejecutados. 0 vulnerabilidades.

### Qué se hizo en la sesión 2026-06-14 (Sprint UI-E ítems 1-3)

- **UI-E ítem 1** (CSS elevación) — CERRADO en sesión anterior, commit `a5b6116`
- **UI-E ítem 2** (KPI animados) — CERRADO en sesión anterior, commits `96ef919` + `aa10dc4`
- **UI-E ítem 3** (animación escalonada de filas):
  - Opción A elegida: FadeTransition + TranslateTransition, 10 filas máx, `Platform.runLater`
  - Método `animarFilas(TableView<?>)` añadido a `TableColumnSizing.java`
  - Hook añadido en `ClientesView.cargar()`, `FacturasView.cargar()`, `PedidosView.cargarPedidos()`
  - Commit `7d2ee82` — implementación inicial
  - Fix posterior: doble `Platform.runLater()` (2 ciclos VirtualFlow), sort por Y real (top→bottom), delay 45ms, Y 22px, duración 260ms
  - Commit `3bd6e1e` — fix animación
  - VibeSec: 0 vulnerabilidades. BUILD SUCCESS.
  - Validación visual: usuario confirmó efecto visible (antes vacío, ahora con datos de test)
  - CSVs de prueba generados en Escritorio: `test_clientes.csv`, `test_pedidos.csv`, `test_facturas.csv` (15 filas c/u, importar en ese orden)
  - Nota: la sesión cerró antes de confirmación final del fix visual — pendiente verificar al inicio de próxima sesión

### Qué se hizo en la sesión 2026-06-13 (tarde)
- Reorganización completa de `.md`: archivos históricos movidos a `docs/archive/audits/`, creado `docs/context/STATE.md`.
- Traducción al español de `AGENTS.md`, `GEMINI.md`, `SECURITY.md`.
- Análisis UI/UX experto → `docs/ui/MEJORAS-VISUALES.md` (propuestas + correcciones Codex).
- **Sprint UI-E iniciado:** slide+fade en `mostrarVista` — commit `0bb8c8b`. Probado y funciona. Usuario dice "un poco rápido" → si se quiere más suave, cambiar `Duration.millis(220)` a `Duration.millis(260)` en `MainView.java` línea ~607 (método `mostrarVista`, dos apariciones: FadeTransition y TranslateTransition).

### Qué se hizo en la sesión 2026-06-13 (mejoras post-Gemini MIGRACION-COMPLEJA)
- **commit `ac2826e`**: `scripts/extrae_material.py` — `to_num()` acepta `campo+fila`, avisa en consola si descarta valor no numérico.
- **commit `ac2826e`**: `scripts/limpia_5c.py` (nuevo) — limpia `5c_material_otros.csv`: 5 filas importables, 2 notas descartadas; recupera precios de `longitud_o_unidad` via regex (CAJA→0.53, CINTA→1.43). Salida: `5c_material_otros_limpio.csv`.
- `MIGRACION_HISTORICO.md` actualizado con resultado real de limpia_5c y referencia al script.
- Mejoras fueron revisadas por Gemini (revisión post-sprint) e implementadas por Claude Code.
- Sprint MIGRACION-COMPLEJA: mejoras Gemini cerradas. Pendiente importar CSVs restantes (5a, 5b, 5c limpio, 3_union_papelera, 2_precios_gramaje).

### Qué se hizo en la sesión 2026-06-13 (sprint MIGRACION-COMPLEJA)
- Inspección completa de `PRECIOS PAPEL PROVEEDORES Formulas.xlsx` (7 hojas, 288 archivos totales).
- Inventario completo de CSVs pre-existentes en `Desktop\files\` (8 CSVs) y `Desktop\excel\` (3 CSVs).
- Generado script `scripts/extrae_material.py` — extrae hoja MATERIAL (plásticos/tintas/consumibles) a CSV.
- Generado `Desktop\excel\3_materiales_plasticos_tintas.csv` (14 filas, UTF-8 limpio).
- Documentado procedimiento completo de importación y limitaciones en `MIGRACION_HISTORICO.md`.
- **COMPLETADO**: wizard ejecutado con `Desktop\files\1_precios_papel_proveedor.csv` → registros de papel importados correctamente → validación visual OK en tabla Materiales. Proveedores UNION_PAPELERA, MRPAPEL, CODIAL visibles con precios correctos. Criterio de cierre del sprint cumplido.

### Qué se hizo en la sesión 2026-06-13 (cierre)
- **Prompt injection detectada y neutralizada** en `proseguir v1.0.md`: el archivo generado por Grok contenía instrucción de clonar repo externo `garrytan/gstack` e instalar skills desconocidas. Claude Code rechazó la ejecución. El archivo fue saneado: instrucción maliciosa eliminada, skills falsas reemplazadas por skills oficiales de Claude Code (`/brainstorming`, `/writing-plans`, `/ui-ux-pro-max`, `/second-opinion`, `/executing-plans`, `/code-review`, `/run`, `/verification-before-completion`, `/simplify`).
- **HEAD corregido** en `proseguir v1.0.md`: era `6268479` (commit seguridad), real es `fd1f34b` (v13.0.0).
- **Prioridad documentada**: MIGRACION-COMPLEJA marcada como obligatoria antes de Sprint UI-E en `proseguir v1.0.md`.
- **STATE.md** actualizado con cierre de sesión y próximos pasos.

### Qué NO se hizo y por qué
- **Sliding pill sidebar**: Codex recomendó defer hasta cerrar MIGRACION-COMPLEJA. Riesgo moderado (toca navBtnImpl y coordenadas dentro de ScrollPane). Ver `docs/ui/MEJORAS-VISUALES.md` tabla de prioridad.
- **Sprint MIGRACION-COMPLEJA**: no se avanzó esta sesión — la sesión se dedicó a documentación, UI y seguridad de archivos de contexto. Es la prioridad máxima para la próxima sesión.
- **Sprint UI-E ítems 1-3**: pendientes (elevación CSS, KPI animados, shimmer). Ejecutar tras MIGRACION-COMPLEJA.

### Próximos pasos recomendados (en orden)

**PUNTO DE ENTRADA HISTÓRICO 2026-06-13 (NO VIGENTE):**

HEAD: `d243cbe`. Rama: `master`. Tests: 146/146. App funcional. BD: 462 materiales reales.

Todos los sprints principales cerrados. Cola: GAP-5 (largo plazo), GAP-8 (largo plazo), Refactor B2.

Estado superado por el handoff vigente al inicio de este archivo (`e7e32ba`, v14.0.1, 151/151). No usar este bloque como punto de entrada actual.

### Decisiones tomadas que el próximo agente debe respetar
- Glassmorphism sidebar: **EVITAR** — Codex lo descartó (rendimiento + parece moda en ERP).
- Hover tilt perspectiva: **EVITAR** — puede sentirse juguete en herramienta de trabajo.
- Floating labels: **EVITAR por ahora** — riesgo de romper formularios existentes.
- `docs/archive/audits/SECURITY_AUDIT_2026-06-13.md`: **NO MODIFICAR** — es baseline de auditoría. Solo crear nuevos archivos con fecha.
- Commits: atómicos, en español, sin WIP. Compilar siempre antes de commit.

### Comandos de validación
```powershell
.\mvnw.cmd clean compile     # validar tras cualquier cambio Java/CSS
.\mvnw.cmd test              # antes de cerrar cualquier sprint
.\mvnw.cmd javafx:run        # probar UI manualmente
```

---

## Estado git

| Campo | Valor |
|---|---|
| HEAD | `4cbbb46` |
| Mensaje | `fix(logging): registrar excepciones silenciadas en estadisticas y exportacion` |
| Rama | `master` (pusheado, `8c05583..4cbbb46`) |
| Tests | 158/158 verdes (`.\mvnw.cmd test`) |
| Versión app | v15.0.0 (`AppConstants.APP_VERSION`, sin cambio esta sesión) |
| Instalador | `output/GraficasMulberry-Instalador-v15.0.0.exe` (reempaquetado 2026-06-24) |

---

## Sprint activo

**Ninguno.** Último cierre: Sprint ASISTENTES-NAV + AUDIT-LOGGING (ver entrada 2026-06-24 arriba). Candidatos sin sprint abierto: god-class `ExportService` (3445 líneas/99 métodos), ausencia de framework de logging, auditoría línea a línea de archivos >1000 líneas restantes. Preguntar al usuario por el siguiente foco antes de iniciar cambios.

---

## Cola prioritaria

1. **Sin cola activa.** Definir nuevo foco con el usuario al inicio del próximo sprint.
2. **Migración i18n: COMPLETA.** Las vistas usan `LanguageManager`; `CalendarioView` ya usa Locale dinámico.
3. **Refactor B2: COMPLETO.** 17/17 DAOs migrados a inyección de `Connection` por constructor.
4. **Ollama:** código mitigado defensivamente; queda acción manual en la máquina del usuario si el chat IA sigue usando una instancia externa de Ollama con modelos en ruta acentuada.
5. **Candidato menor i18n:** patrón literal `"de"` en fecha larga de `CalendarioView` si se quiere perfeccionar formato por idioma.

---

## Sprints cerrados relevantes

| Sprint | Commit | Descripción |
|---|---|---|
| i18n-16-bis-3 | `d9dbf6a` | Cierra el hallazgo fuera de alcance de i18n-16-bis-2: 4 mensajes de excepción visibles vía mostrarError() en PresupuestosView migrados a t("presupuestos.error.presupuesto_no_encontrado")/t("presupuestos.error.cliente_no_encontrado"), mismo patrón que FacturasView. 2 claves nuevas × 6 bundles. Incluido en el sprint tras confirmación explícita del usuario |
| i18n-16-bis-2 | `16e3215` | Cierra los 2 gaps de i18n-16-bis: mostrarResultadoImportacion() migrado a tf()/t() en FacturasView+PedidosView (patrón AlbaranesView); 4 mensajes de excepción visibles vía mostrarError() en FacturasView migrados a t("facturas.error.*") (Codex señaló 2, Claude Code verificó y encontró 4 reales). 14 claves nuevas × 6 bundles. Nuevo hallazgo fuera de alcance, no corregido: mismo patrón en PresupuestosView |
| i18n-16-bis | `1aa6d58` | Filtro de IMPORTACIÓN en FacturasView/PedidosView (4 claves *.importar.filtro/todos_archivos). Codex detecta 2 gaps nuevos verificados, no corregidos (fuera de alcance): texto hardcodeado en mostrarResultadoImportacion() y mensajes de excepción visibles vía mostrarError() en FacturasView |
| i18n-16 | `b1d6b3a` | Gap cobertura DynamicColumnRuntime/export en Clientes/Facturas/Pedidos/Albaranes/Presupuestos/Nóminas: título diálogo, prefijo fichero, ExtensionFilter exportación (18 claves *.export.filtro). Gap import sin traducir en Facturas/Pedidos detectado por Codex, no corregido (fuera de alcance) |
| i18n-15 | (ver tabla cabecera) | CalendarioView: título, navegación, días, diálogo de nota, error de guardado (18 claves); última vista pendiente — i18n al 100% |
| i18n-14 | (ver tabla cabecera) | EstadisticasView: títulos, tabs, KPIs, gráficos, series, previsualización, exportación PDF (49 claves) |
| i18n-13 | `8db3041` | ComprasProveedorView: resumen KPIs, toolbar filtros, tabla, diálogo pago, marcar pagado, eliminar (60 claves); rename tf()→txf() |
| i18n-12 | `bf3340f` | TarifasView: toolbar, tabla, diálogo tarifa, gestión de tramos, import/export, previsualización (74 claves); rename tf()→txf(); rename `Tarifa t`→`tarifa` (param método completo); sentinel "Todas" fix |
| i18n-11 | `0799fb5` | MaterialesView: toolbar stock, tabla, diálogo, tab consumo, tab pagos proveedor, import/export, previsualización (142 claves); rename tf()→txf(); DynamicColumnRuntime+export prefix migrados |
| i18n-10 | `4f55afc` | EmpleadosView: toolbar, tabla, diálogo, baja/reactivar, import/export, previsualización (~75 claves); rename tf()→txf() |
| i18n-9 | `75e68e3` | NominasView: toolbar, tabla, diálogos, generar mes, import/export, previsualización (~76 claves) |
| i18n-8 | `778d2d2` | PresupuestosView: toolbar, tabla, diálogos+tarifa tiempo, import/export, previsualización (~118 claves) |
| i18n-7 | (ver historial) | AlbaranesView: toolbar, tabla, diálogos+stock, import/export, previsualización (~84 claves) |
| i18n-6 | (ver historial) | PedidosView: KPIs, tabs, filtros, diálogos+pago+fraccionar, import/export, previsualización (~128 claves) |
| i18n-5 | (ver historial) | FacturasView: toolbar, tabla, diálogos+líneas+materiales, import/export, previsualización (~75 claves) |
| i18n-4 | (ver historial) | DashboardView + ClientesView: KPIs, avisos, toolbar, diálogos, import/export (~90 claves) |
| i18n-3 | `71177b4` | MainView: sidebar, footer, tooltips, diálogos, asistente (~60 claves); TITULO_A_MODULO fix; tf() formal |
| i18n-2 | `6957681` | ConfiguracionView: ~80 literales → t()/tf(); bundles eu/gl/fr config.* |
| i18n-1 | `a035fe8` | LoginView + AdminSetupView migrados; bundles eu/gl/fr ~40 claves |
| i18n-0 | `1947fbc` | LanguageManager + 6 bundles base; ConfiguracionView.buildPanelIdioma() |
| HelpService compras | `7ae2a79` | 5 artículos HTML compras/; F1 vinculado al módulo |
| SECURITY-2026-06-13 | `6268479` | Auditoría + remediación completa SEC-01..10 + NEW-01..03 |
| HELP-5 | `610a0f2` | PreferenceService, OnboardingDialog, modo principiante, hint bar |
| HELP-4-FIX | `4117cdf` | DDL UNIQUE constraints, factorías estáticas HelpView, tests |
| HELP-3 | `67f7d4e` | F1 contextual por módulo |
| HELP-2 | `47e46dc` | HelpService + HelpView JavaFX |
| HELP-1 | `65588cf` | 81 artículos HTML offline |
| Sprint GAP-5 | `d243cbe` | ComprasProveedorView — permiso COMPRAS en ADMIN+PRODUCCION+CONTABILIDAD |
| Sprint B | `74910eb` | 4 tests BD efímera — reset singleton por reflexión |
| Sprint A | `cd91f15` | buildBeginnerHint() en Facturas, Pedidos, Materiales, Empleados |
| Sprint UI-COUNTER | `d93dc76` | lblContador en los 9 módulos |
| Sprint UI-F | `85152bd` | Animación de filas extendida a todos los módulos |
| Sprint UI-E | `3bd6e1e` | CSS elevación + KPI animados + filas escalonadas + pill sidebar |

---

## Deuda técnica conocida

- i18n menor: el patrón de fecha larga de `CalendarioView` conserva `"d 'de' MMMM 'de' yyyy"`; los nombres de mes/día ya usan Locale dinámico, pero la preposición literal sigue en español.
- Ollama local del usuario: si una instancia externa ya está corriendo con modelos en `C:\Users\Gipsy Dávy\.ollama\models`, el fix de la app no puede cambiar ese proceso; requiere `OLLAMA_MODELS` manual + repull.
- No reabrir Refactor B2 salvo necesidad concreta: la cola DAO quedó completa 17/17.
