# CONTINUAR.md — Gráficas Mulberry
## Guía de continuación de sesión

---

## ⛔ PROTOCOLO OBLIGATORIO DE INICIO — ANTES DE CUALQUIER ACCIÓN

**Esta regla tiene prioridad absoluta sobre cualquier otra instrucción.**

Antes de escribir una sola línea de código, antes de responder sobre el proyecto,
antes de proponer cualquier cambio: leer COMPLETOS y en este orden exacto los 5 archivos siguientes.

| # | Archivo | Por qué es obligatorio |
|---|---------|------------------------|
| 1 | `MACRO-PROMPT-GRAFICAS-MULBERRY.md` | Roles activos, skills, arquitectura real, seguridad, Multi-IA, filosofía del producto |
| 2 | `continuar.md` (este archivo) | Estado de sesión, HEAD, tests, cola activa, entorno, errores frecuentes |
| 3 | `CLAUDE.md` | Reglas del proyecto: cambios quirúrgicos, commits atómicos, convenciones, seguridad |
| 4 | `interfaz.md` | Estado UI/UX, diagnóstico visual, sprints visuales pendientes o cerrados |
| 5 | `Resumen.md` | Handoff técnico completo: HEAD, tests, deudas técnicas, sprints completados |

**Un agente que omite cualquiera de estos archivos opera en modo genérico y producirá
cambios incorrectos, incoherentes o regresivos. No es una recomendación: es el protocolo.**

Tras leerlos, verificar estado git:
```powershell
git log --oneline -5
git status --short
```
Y si es inicio de sprint, ejecutar:
```powershell
.\mvnw.cmd test
```

---

Este documento resume el estado real del proyecto para continuar en una nueva sesión sin perder contexto.

---

## Raíz correcta del proyecto

Abrir siempre:

```
C:\Users\GipsyDavy\MAVEN\Graficas Mulberry
```

No abrir subcarpetas como proyecto principal.

---

## De qué va la aplicación

**Gráficas Mulberry** es una aplicación ERP de escritorio monoplataforma (Windows) para la gestión integral
de una empresa de artes gráficas. Es una herramienta de trabajo real usada en producción.

Funcionalidades principales:
- Gestión de clientes, empleados, proveedores.
- Presupuestos, facturas, albaranes, pedidos.
- Control de materiales y stock.
- Nóminas de empleados.
- Tarifas y tramos de precios.
- Estadísticas e informes gráficos.
- Calendario corporativo con notas.
- Exportación a PDF, Excel, CSV.
- Importación masiva desde CSV.
- Asistente IA local (Ollama) integrado.
- Asistente visual con TTS.
- Sistema de temas y configuración visual.
- Gestión de usuarios con roles y permisos (BCrypt).

---

## PERFIL DE TRABAJO PERMANENTE (máxima prioridad)

Claude Code opera **siempre y simultáneamente** con los cuatro perfiles expertos siguientes, sin excepción:

1. **Desarrollador de software senior experto** — código correcto, mínimo viable, limpio, mantenible y robusto.
2. **Diseñador gráfico e interfaces senior experto** — visual profesional, coherente y funcional.
3. **Experto en UI/UX senior** — experiencia fluida, eficiente y sin fricción.
4. **Experto en seguridad y ciberseguridad** — revisión activa de vulnerabilidades, código inseguro, inyecciones SQL, acceso a ficheros y datos sensibles.

**Implicaciones obligatorias:**
- En todo código que se lea, escriba o modifique: revisar y corregir vulnerabilidades de forma quirúrgica.
- Ningún cambio se da por terminado si contiene código inseguro o con puertas traseras detectables.
- La calidad visual y la UX se evalúan en cada pantalla o componente que se cree o edite.
- El rol de seguridad NO es opcional: aplica siempre, en todo sprint.

---

## Documentos del proyecto que DEBES leer antes de actuar

1. `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — **PRIMERO Y SIEMPRE.** Roles activos, skills, seguridad permanente, formato de respuesta, Multi-IA. Sin leerlo el agente opera en modo genérico.
2. `CLAUDE.md` — reglas operativas, cambios quirúrgicos, convenciones de commit.
3. `Resumen.md` — estado técnico completo, handoff, deudas, próximos sprints.
4. `interfaz.md` — estado UI/UX, diagnóstico visual, sprints pendientes.
5. Este `continuar.md`.

### Regla de seguridad permanente (del MACRO-PROMPT)

**VibeSec y security-review se invocan obligatoriamente en cualquier sprint que toque:**
- Autenticación, cambio de contraseña, sesiones (`AuthService`, `UserDAO`, `LoginView`)
- Roles y permisos (`UserRole`, `UserPermissions`, `UserManagementView`)
- Rutas de archivos en import/export (path traversal)
- Datos sensibles: nóminas, clientes, facturas
- Cualquier entrada de usuario que vaya a SQL

**Siempre:** PreparedStatements, BCrypt, mínimo privilegio. Nunca SQL por concatenación, nunca loguear contraseñas.

---

## Reglas técnicas clave

- Sin Lombok. Sin Spring. Sin ORM. Sin nuevas dependencias salvo justificación explícita.
- SQLite via JDBC directo. `DatabaseManager` singleton. `PRAGMA foreign_keys = ON`.
- PreparedStatements siempre. Nunca SQL por concatenación.
- Patrón transaccional con `externalTx` en DAOs que participen en cadenas.
- Override `graficas.mulberry.db.url` para tests JDBC con BD efímera.
- No modificar `target/`, `output/`, instaladores `.exe` salvo tarea explícita de build.
- `.\mvnw.cmd` (no `mvn` directo — no está en PATH).

---

## Entorno de desarrollo

### Java
- JDK 21 requerido.
- `.\mvnw.cmd` disponible en la raíz del proyecto.

### Build commands

```powershell
.\mvnw.cmd compile            # compilar
.\mvnw.cmd clean compile      # compilar desde cero (forzar tras ediciones)
.\mvnw.cmd test               # ejecutar tests (89/89 verdes en HEAD documentado)
.\mvnw.cmd javafx:run         # arrancar la aplicación
.\mvnw.cmd package            # generar JAR
.\mvnw.cmd package -Ppackage-windows  # generar instalador Windows
```

**NUNCA usar `Nothing to compile - all classes are up to date` como prueba de compilación.**
Usar siempre `.\mvnw.cmd clean compile` tras editar un archivo.

### Base de datos SQLite
- Archivo en `%LOCALAPPDATA%\GraficasMulberry\` (producción).
- Override para tests: `System.setProperty("graficas.mulberry.db.url", "jdbc:sqlite:/ruta/test.db")`.

### Override TLS Maven (si `mvnw` falla con PKIX)
```powershell
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd test
```
Este override es efímero (solo para la sesión PowerShell actual). No hacerlo permanente.

---

## Workflow Multi-IA

**No hay invocación directa por CLI desde Claude consumer.** El flujo es:

1. **Claude Code** coordina el sprint, redacta bloques autocontenidos.
2. **Usuario** pega el bloque en el chat del agente correspondiente en el IDE.
3. **El agente del IDE** (Codex / Gemini) ejecuta.
4. **Usuario** pega la respuesta del agente y Claude Code evalúa si continuar o corregir.

### Codex — cuándo usarlo
- Parches quirúrgicos concretos ya diseñados por Claude Code.
- Ejecutar Maven y reportar resultado.
- Verificar encoding de literales tras editar.
- Leer y verificar secciones concretas de archivos.

### Gemini — cuándo usarlo
- Diseñar estrategia de sprint UI/UX antes de codificar.
- Segunda opinión arquitectónica o de diseño.
- Análisis de impacto de refactorizaciones amplias.
- Investigación técnica.

### Reglas de bloque (lecciones acumuladas)
- Instrucciones cerradas, sin espacio interpretativo.
- Restricciones negativas explícitas ("NO modificar X").
- Criterio de éxito verificable (compilation + `git diff --stat`).
- Una sola tarea por bloque.
- Pre-requisito de lectura: "LEE Y VERIFICA" antes de editar.
- Releer el bloque antes de darlo al usuario.
- Inserciones puras > reemplazos cuando las líneas pueden conservarse.
- Truco "líneas idénticas en old_str y new_str" blinda el resultado en Codex.

---

## Protocolo de inicio de sesión

1. Leer: `MACRO-PROMPT-GRAFICAS-MULBERRY.md` primero, luego `CLAUDE.md`, `Resumen.md`, este `continuar.md`.
2. Verificar git: `git log --oneline -5` + `git status --short`
3. Ejecutar: `.\mvnw.cmd test`
4. Declarar estado y proponer el siguiente paso.

El usuario puede arrancar la sesión con solo decir **"continúa"** o **"¿qué toca?"**.

---

## Estado técnico al cierre de sesión (2026-06-11, actualizado)

### Git
- **Rama:** `master`
- **HEAD:** `65588cf` — `feat: Sprint HELP-1 — 81 artículos HTML de ayuda offline completos`
- **Working tree:** limpio al cierre del sprint

### Tests
- **89/89 verdes**

| Suite | Tests |
|---|---|
| `ClienteDAOTest` | 3 |
| `EmpleadoDAOTest` | 3 |
| `NominaDAOTest` | 3 |
| `PagoMaterialDAOTest` | 3 |
| `PedidoDAOTest` | 3 |
| `PagoPedidoDAOTest` | 3 |
| `PresupuestoDAOTest` | 3 |
| `FacturaDAOTest` | 3 |
| `MaterialDAOTest` | 3 |
| `AlbaranDAOTest` | 4 |
| `TxAnidadaTest` | 3 |
| `ImportBackupServiceTest` | 12 |
| `EntityImportServiceAlbaranTest` | 11 |
| `EntityImportServiceFacturaTest` | 9 |
| `EntityImportServiceNominaTest` | 6 |
| `EntityImportServicePedidoTest` | 10 |
| `EntityImportServicePresupuestoTest` | 7 |

### Sprints completados (histórico)
- **Sprint B** — Transacciones explícitas en DAOs (5 commits, 12 tests nuevos).
- **Sprint D** — Defaults DDL TEXT en Presupuesto/Factura/Albarán (3 commits, 3 tests nuevos).
- **Sprint D-ter** — Defaults DDL en EmpleadoDAO, MaterialDAO, PagoMaterialDAO, ClienteDAO (4 commits, 2 tests nuevos). **COMPLETO.**
- **Sprint UI/UX Bloques 1-10** — Modernización visual completa (14 commits).
- **Sprint SEC** — 5 fixes de seguridad P0/P1 (4 commits). **COMPLETO.** (`8060734`→`7cd651d`)
- **Sprint COD** — Dead code eliminado en AppConstants (1 commit). **COMPLETO.** (`2dd1f11`)
- **Sprint UI-A** — CSS: variables para tooltip, badges, DatePicker, ProgressBar, RadioButton, ContextMenu, btn-toolbar-active, input-success; sanear theme-mulberry (5 commits). **COMPLETO.** (`467a2b5`→`5718971`)
- **Sprint UI-B** — FadeTransition 150→220ms, param color muerto en btn() eliminado en 9 vistas, versión desde AppConstants, FadeTransition en nav groups (2 commits). **COMPLETO.** (`632e355`→`ef7ec17`)
- **Sprint UI-C** — IAView command-bar + 10 clases CSS IA; audit EstadisticasView y UserManagementView; eliminar setStyle() hardcodeados (1 commit). **COMPLETO.** (`34bca4c`)
- **Sprint C** — Fix `resolverEmpleadoId` filtro `activo=1` (Deuda 2), test empleado inactivo. **COMPLETO.** (`1851216`)
- **Deuda 24** — Tests JDBC: EmpleadoDAO, NominaDAO, PagoMaterialDAO, PedidoDAO, PagoPedidoDAO (15 tests). **COMPLETO.** (`acc81a3`)
- **Sprint UI-D** — `.skeleton-row` CSS + `ProgressIndicator` overlay en ClientesView, FacturasView, PedidosView (1 commit). **COMPLETO.** (`d4109c2`)
- **Sprint IMPORT-UPGRADE** — XLSB/XLSM vía `WorkbookFactory`; `EntityImportSpec.tableName()`; `procesarFila()` devuelve `int[3]` con entityId; escritura de columnas dinámicas en import; `ColumnMappingDialog` con `ObservableList` compartida + botón "➕ Nuevo campo…" con `ColumnConfigDAO`; filtros FileChooser actualizados en 10 vistas (1 commit). **COMPLETO.** (`4bc6c9c`)
- **Sprint HELP-0** — especificación `HELP-SPEC.md`: taxonomía 8 categorías, 19 módulos, 81 artículos ★, formato HTML, `index.json`, `HelpEntry` record, `HelpService` API, mapa F1. **COMPLETO.** (`39d060e`)
- **Sprint HELP-1** — 81 artículos HTML offline en 19 módulos + `help.css` + `index.json`. **COMPLETO.** (`65588cf`)

### Cola de trabajo

1. **Sprint HELP-2** ← **SIGUIENTE INMEDIATO** — `HelpService.java` (carga `index.json`, búsqueda) + `HelpView.java` (BorderPane: WebView centro, TreeView módulos izq., barra búsqueda arriba). Ver `HELP-SPEC.md` secciones 6-7.
2. **Sprint MIGRACION-COMPLEJA** — tablas complejas reales (Excel humano, PDF/Word). Ver `MIGRACION_HISTORICO.md`. Bloqueado hasta que el usuario aporte archivos.
3. **HELP-3** — ayuda contextual F1 + enlaces desde errores a artículos (después de HELP-2).
4. **Refactor B2** — inyección de Connection en DAOs (después de HELP-2, amplio y de mayor riesgo).
5. **Sprint D-bis** — Defaults DDL numéricos primitivos `double`→`Double` (Deuda 20-bis, baja urgencia).

---

## Deudas técnicas abiertas (resumen)

| ID | Descripción | Prioridad |
|---|---|---|
| 2 | **CERRADA** — Fix `resolverEmpleadoId` null en `1851216` | — |
| 20-bis | Defaults DDL numéricos primitivos (`double`→`Double`) | Baja |
| 20-ter | **CERRADA** — ClienteDAO.setBase completado en `d19a342` | — |
| 24 | **CERRADA** — 15 tests JDBC en `acc81a3` | — |
| 26 | Capa completa de ayuda integrada (**PARCIAL** — HELP-0+1 cerrados, pendiente HELP-2+3) | Media |
| 27 | Migración de tablas complejas históricas desde Excel/PDF/Word humanos | Alta |
| 3, 5, 6, 7... | Otras deudas menores | Baja |

Ver `Resumen.md` — sección DEUDAS TÉCNICAS para el listado completo.

---

## Próximos sprints candidatos

1. **Sprint HELP-2** ← **INMEDIATO** — `HelpService.java` + `HelpView.java` (JavaFX WebView). Ver `HELP-SPEC.md`.
2. **Sprint MIGRACION-COMPLEJA** — inventariar archivos reales, clasificar y documentar procedimiento. Bloqueado hasta que el usuario aporte archivos.
3. **HELP-3** — ayuda contextual F1 + enlaces desde errores (después de HELP-2).
4. **Refactor B2** — inyección de Connection en DAOs (amplio, de mayor riesgo, después de HELP-2).
5. **Sprint D-bis** — Defaults DDL numéricos primitivos (`double`→`Double`), bajo impacto operativo pero blast radius alto.

---

## Procedimiento al retomar

1. Leer: `MACRO-PROMPT-GRAFICAS-MULBERRY.md`, `CLAUDE.md`, `Resumen.md`, este `continuar.md`, `interfaz.md`.
2. Verificar estado git:
   ```powershell
   git log --oneline -8
   git status --short
   ```
3. Verificar tests:
   ```powershell
   .\mvnw.cmd test
   ```
   Esperado: 89/89 verdes.
4. Declarar: HEAD `65588cf`, Sprint HELP-2 es el siguiente inmediato.

---

## Valores conocidos — errores frecuentes

- **`DuplicatePolicy`**: valores válidos son `SKIP_IF_EXISTS`, `UPDATE_EXISTING`, `CREATE_NEW`. NO existe `UPDATE_IF_EXISTS`.
- **`DatabaseManager.getConnection()`**: singleton estático. Todos los DAOs comparten la misma Connection.
- **SQLite no aplica DEFAULT con NULL explícito** (`setString(n, null)` pisa el DEFAULT). Solo si la columna se omite del INSERT.
- **`Nothing to compile` no prueba compilación.** Usar `.\mvnw.cmd clean compile`.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Usar `Select-String -Path 'archivo' -Pattern 'patron'`.

---

*continuar.md — Gráficas Mulberry — 2026-06-11*
