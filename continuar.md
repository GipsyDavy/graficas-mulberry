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
.\mvnw.cmd test               # ejecutar tests (110/110 verdes tras última validación documentada)
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

## Estado técnico al cierre de sesión (2026-06-12, actualizado post-auditoría SEC-NEW-1..5)

### Git
- **Rama:** `master`
- **HEAD:** `a352225` — `fix: añadir timeout de request a enviarConsulta en OllamaService (VULN-SR-001)`
- **Working tree:** no limpio. Pendientes: `interfaz.md`, vistas de módulos (`ClientesView`, `EmpleadosView`, `MainView`, `MaterialesView`, `TarifasView`), HTML de ayuda importación, `styles.css`, `TypedValueFormatter`, `DynamicColumnValueDAO`, `EntityImportService`, `ImportView`, `TypedValueFormatterTest`, `ImportServiceParsingTest`, `Resumen.md`, `continuar.md`.

### Tests
- **110/110 verdes** confirmados tras Sprint IMPORT-PARSER + MAPPING-GUARD con `.\mvnw.cmd test`.

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
| `HelpServiceTest` | 10 |
| `ImportBackupServiceTest` | 12 |
| `EntityImportServiceAlbaranTest` | 11 |
| `EntityImportServiceFacturaTest` | 9 |
| `EntityImportServiceNominaTest` | 6 |
| `EntityImportServicePedidoTest` | 10 |
| `EntityImportServicePresupuestoTest` | 7 |
| `TypedValueFormatterTest` | 5 |
| `ImportServiceParsingTest` | 6 |

### Sprints completados (histórico)
- **Sprint B** — Transacciones explícitas en DAOs (5 commits, 12 tests nuevos).
- **Sprint D / D-ter** — Defaults DDL TEXT en todos los DAOs afectados (7 commits, 5 tests nuevos).
- **Sprint UI/UX Bloques 1-10** + **UI-A/B/C/D** — Modernización visual completa (18 commits).
- **Sprint SEC** — 5 fixes de seguridad P0/P1. **COMPLETO.** (`8060734`→`7cd651d`)
- **Sprint COD / Sprint C** — Dead code + fix resolverEmpleadoId. **COMPLETO.**
- **Deuda 24** — 15 tests JDBC. **COMPLETO.** (`acc81a3`)
- **Sprint IMPORT-UPGRADE** — XLSB/XLSM, columnas dinámicas en import, ColumnMappingDialog "➕ Nuevo campo…". **COMPLETO.** (`4bc6c9c`)
- **Sprint HELP-0/1/2** — Sistema de ayuda offline 81 artículos + HelpView JavaFX. **COMPLETO.** (`39d060e`→`47e46dc`)
- **Sprint Import Wizard/Pivot UI** — wizard en módulos planos, modo expandido Tarifas, TableColumnSizing. **COMPLETO** en working tree (commits previos a `47e46dc` / `c57d793`).
- **Sprint COLUMN-TYPES** — tipos de dato en columnas dinámicas, hard delete, edición de tipo en columnas existentes (botón "Tipo…"), controles tipados en formularios (DatePicker/TextField numérico). **COMPLETO.** (`e12a687`→`d4db11b`)
- **Sprint WIZARD-VALIDATION** — paso 3.5 de validación IA entre mapeo e importación; `ValidationIssue` record; `ImportService.validateImportData()` + `corregirValor()` + validación local (NIF, email, precios, fechas, duplicados); `TableColumnSizing`; fix corrección condicional (no borrar issue si la IA no corrijo). **COMPLETO.** (`988a8fb`→`979cd06`)
- **Sprint COLUMN-FORMAT + IMPORT-REPAIR** — formato real de columnas dinámicas tipadas (`PRECIO` con €, `FECHA` con DatePicker/ISO, `NUMERICO` normalizado), conversión opcional de valores existentes al cambiar tipo con reporte de no convertibles + transacción, rechazo de edición inválida en celdas tipadas, botón "🤖 Reparar importación" para plan IA con campos dinámicos tipados/valores fijos/correcciones de celda, normalización determinista previa a importar. **COMPLETO en working tree. Revisión Gemini incorporada.**
- **Sprint IMPORT-PARSER + MAPPING-GUARD** — parser real probado contra carpetas del usuario (`CSV`, `excel`, `EXCEL_SEPARADO`, `files`, `TARIFAS_SEPARADAS`, `TARIFAS_SEPARADAS 1`, `todas_las_tarifas`): 288/288 archivos abren (110 CSV, 177 XLSX, 1 XLSB). `ImportService` detecta cabecera real, soporta XLSB vía extractor tabulado, evita CSV vacío con excepción, conserva tablas laterales, infiere cabeceras vacías (`UNIDADES`, `DESCRIPCIÓN`, `PRECIO`) y salta cabeceras repetidas/separadores. `mapearCampos()` ejecuta fallback local siempre, aunque Ollama devuelva 0 columnas; `Tarifa.IMPORT_SPEC` reconoce `UNIDADES`, `CONCEPTO`, `DESCRIPCIÓN`; `Material.IMPORT_SPEC` reconoce `tipo_papel`, `modelo`, `producto`, `familia`. `ImportView` activa técnica/categoría fija por defecto cuando falta y bloquea `Siguiente` si faltan obligatorios. **COMPLETO en working tree.**
- **Auditoría 2026-06-12 (SEC-NEW + COD-NEW + ARCH-NEW + Security Review)** — 9 hallazgos. Corregidos: SEC-NEW-1 (importar*SQL sin validación SQL — clasificación revisada: métodos son código muerto en UI, fix correcto como defensa en profundidad), SEC-NEW-2 (OllamaService getModelosConDetalles sin request timeout), SEC-NEW-3 (NPE getModelosConDetalles), ARCH-NEW-1 (OLLAMA_URL duplicada), COD-NEW-1 (6 dead constants JSON_*), VULN-SR-001 (enviarConsulta sin request timeout). Abiertos: SEC-NEW-4 (concurrencia OllamaService), SEC-NEW-5 (historial sin límite), COD-NEW-2 (STYLE_BURBUJA inline). Commits: `3d7f765`→`a352225`. **110/110 tests verdes.**

### Cola de trabajo

1. **Validar en producción** ← **SIGUIENTE INMEDIATO** — repetir manualmente el flujo que falló en la captura: `Tarifas > Importar > 01_TARJETAS_DE_VISITA.csv/xlsx`. En paso 3 debe verse mapeo local (`UNIDADES -> minimo_unidades`, `DESCRIPCIÓN -> nombre`, `PRECIO... -> precio_unit`) y la técnica fija debe salir prellenada desde el nombre del archivo si no se usa modo expandido. No debe volver a ocurrir `IA mapeó 0/20 columnas` + importación con `184 filas descartadas`.
2. **Sprint MIGRACION-COMPLEJA** — tablas complejas reales (Excel humano, PDF/Word). Ver `MIGRACION_HISTORICO.md`. El usuario ya aportó carpetas reales; siguiente paso es clasificar archivos con secciones internas (`NUEVAS TARIFAS...xlsb`, `PRECIOS PAPEL PROVEEDORES Formulas.xlsx`) y decidir limpieza A1/A2/B.
3. **HELP-3** — ayuda contextual F1 + enlaces desde errores a artículos.
4. **Refactor B2** — inyección de Connection en DAOs (amplio y de mayor riesgo).
5. **Sprint D-bis** — Defaults DDL numéricos primitivos `double`→`Double` (Deuda 20-bis, baja urgencia).

---

## Estado actual de importación nueva

### Estado validado el 2026-06-12

Se reprodujo el fallo reportado por captura: paso 4 importaba con `0/20 columnas` mapeadas y descartaba todas las filas por `técnica`/`nombre` obligatorios vacíos. La causa no era el parser sino que una respuesta mala/vacía de Ollama se aceptaba como mapeo válido.

Estado corregido:
- `ImportService.mapearCampos()` ejecuta fallback local siempre, incluso si Ollama responde JSON válido pero todo `null`.
- Para tarifas reales: `UNIDADES -> minimo_unidades`, `DESCRIPCIÓN/CONCEPTO -> nombre`, primera columna `PRECIO... -> precio_unit`.
- `ImportView` preactiva `Aplicar técnica/categoría fija` cuando el campo de agrupación no viene mapeado desde una columna.
- `ImportView` bloquea `Siguiente` si faltan campos obligatorios sin mapear, sin modo expandido que los cubra y sin valor fijo.
- Parser probado contra las rutas reales del usuario: 288/288 archivos soportados abren; 34 están vacíos/sin columnas reales.

Comandos de validación usados:
```powershell
.\mvnw.cmd test
# Esperado vigente: 110/110 verdes
```

Dry-run con BD temporal (`graficas.mulberry.db.url=jdbc:sqlite:C:\tmp\gm-import-dryrun.db`) confirmó:
- `17_DISEÑOS.xlsx`: 18/18 importables.
- `19_OVALOS.xlsx`: 12/12 importables tras inferir `UNIDADES`/`DESCRIPCIÓN`.
- `40_IMANES.xlsx`: 31/31 importables.
- CSV de `Desktop\files`: importables limpios en muestras probadas.
- `NUEVAS TARIFAS (2) (version 1).xlsb`: abre y recupera 1020 filas, pero contiene muchas secciones internas; requiere limpieza o modo/criterio específico si se quiere 100% de aprovechamiento.

### Módulos con wizard nuevo

El wizard nuevo está conectado desde:
- `ClientesView`
- `MaterialesView`
- `EmpleadosView`
- `TarifasView`

El grupo `DATOS` del sidebar y la importación global se eliminaron porque duplicaban entrada y confundían. Las importaciones deben iniciarse desde el módulo destino.

### Modo expandido para Tarifas

Usar para hojas donde una fila contiene una cantidad/descripción y varias columnas de precio.

Flujo correcto para `01_TARJETAS_DE_VISITA.xlsx`:
1. Abrir `Tarifas > Importar`.
2. Verificar que fallback local mapea `UNIDADES -> minimo_unidades`; `DESCRIPCIÓN -> nombre`; alguna columna `PRECIO... -> precio_unit`.
3. Para importación simple de una sola columna de precio: dejar técnica fija activada con el nombre del archivo.
4. Para matriz de precios: dejar las columnas de precio como `(ignorar)`, activar `Modo expandido`, marcar solo las columnas de precio, poner `nombre de columna -> tecnica` y `valor de celda -> precio_unit`.
5. En modo expandido, desactivar técnica fija si entra en conflicto con `tecnica` como campo pivot.

Resultado esperado: cada fila del Excel se multiplica por cada columna de precio marcada.

### Agrupación

Primera fase sin cambiar esquema:
- Materiales se agrupan con el campo existente `categoria`.
- Tarifas se agrupan con el campo existente `tecnica`.

Si el usuario necesita grupos visuales independientes del campo de negocio, diseñar fase posterior con modelo explícito de grupos. No improvisar columnas nuevas sin diseño de datos.

### Scroll y columnas

Se añadió helper `TableColumnSizing`:
- fuerza `TableView.UNCONSTRAINED_RESIZE_POLICY`;
- calcula ancho inicial por cabecera y muestra de celdas;
- permite scroll horizontal real cuando hay muchas columnas.

Pendiente visual: verificar en ventana maximizada y con ficheros reales que las tablas no comprimen columnas ni solapan controles.

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

1. **Commit atómico de importación** — si el usuario valida manualmente el flujo corregido, preparar commit que agrupe parser/mapping guard/tests/documentación o separar en dos commits (`fix(import): parser/mapping guard`, `docs: handoff import real`).
2. **Sprint MIGRACION-COMPLEJA** — inventariar archivos reales restantes y decidir limpieza específica para libros con muchas secciones internas (`NUEVAS TARIFAS...xlsb`, `PRECIOS PAPEL PROVEEDORES Formulas.xlsx`).
3. **HELP-3** — ayuda contextual F1 + enlaces desde errores.
4. **Refactor B2** — inyección de Connection en DAOs (amplio, de mayor riesgo).
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
   Esperado: 110/110 verdes.
4. Declarar: HEAD `a352225`. Sprints COLUMN-FORMAT + IMPORT-REPAIR e IMPORT-PARSER + MAPPING-GUARD cerrados en working tree. Auditoría 2026-06-12 completa (SEC-NEW-1..5, ARCH-NEW-1, COD-NEW-1/2, VULN-SR-001 — 6 corregidos, 3 abiertos). Cola activa: commit atómico de sprints en working tree + validación manual del flujo `Tarifas > Importar`.

---

## Valores conocidos — errores frecuentes

- **`DuplicatePolicy`**: valores válidos son `SKIP_IF_EXISTS`, `UPDATE_EXISTING`, `CREATE_NEW`. NO existe `UPDATE_IF_EXISTS`.
- **`DatabaseManager.getConnection()`**: singleton estático. Todos los DAOs comparten la misma Connection.
- **SQLite no aplica DEFAULT con NULL explícito** (`setString(n, null)` pisa el DEFAULT). Solo si la columna se omite del INSERT.
- **`Nothing to compile` no prueba compilación.** Usar `.\mvnw.cmd clean compile`.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Usar `Select-String -Path 'archivo' -Pattern 'patron'`.

---

*continuar.md — Gráficas Mulberry — 2026-06-12*
