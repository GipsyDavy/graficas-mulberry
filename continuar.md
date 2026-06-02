# CONTINUAR.md — Gráficas Mulberry
## Guía de continuación de sesión

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

1. `CLAUDE.md` — reglas operativas, Multi-IA, cambios quirúrgicos.
2. `Resumen.md` — estado técnico completo, handoff, deudas, próximos sprints.
3. `interfaz.md` — estado UI/UX, diagnóstico visual, sprints pendientes.
4. `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — roles, arquitectura, filosofía, seguridad.
5. Este `continuar.md`.

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
.\mvnw.cmd test               # ejecutar tests (72/72 verdes en HEAD actual)
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

1. Leer: `CLAUDE.md`, `Resumen.md`, este `continuar.md` — en ese orden.
2. Verificar git: `git log --oneline -5` + `git status --short`
3. Ejecutar: `.\mvnw.cmd test`
4. Declarar estado y proponer el siguiente paso.

El usuario puede arrancar la sesión con solo decir **"continúa"** o **"¿qué toca?"**.

---

## Estado técnico al cierre de sesión (2026-06-02)

### Git
- **Rama:** `master`
- **HEAD:** `1fb1904` — `fix: restaurar botón Exportar base de datos en footer de sidebar`
- **Working tree:** cambios `.md` sin commitear (documentación de auditoría: `AUDITORIA.md`, `FASES.md`, `interfaz.md`, `INFORME-FINAL.md`, `continuar.md`, `Resumen.md`, `MACRO-PROMPT-GRAFICAS-MULBERRY.md`). Commitear antes de empezar código.
- **Sincronizado con `origin/master`** (código — la documentación no está pusheada)

### Tests
- **72/72 verdes** (D-ter 1d pendiente → 73/73 cuando se complete)

| Suite | Tests |
|-------|-------|
| `ClienteDAOTest` | 2 |
| `PresupuestoDAOTest` | 3 |
| `MaterialDAOTest` | 3 |
| `FacturaDAOTest` | 3 |
| `AlbaranDAOTest` | 4 |
| `TxAnidadaTest` | 3 |
| `ImportBackupServiceTest` | 12 |
| `EntityImportServiceAlbaranTest` | 11 |
| `EntityImportServiceFacturaTest` | 9 |
| `EntityImportServiceNominaTest` | 5 |
| `EntityImportServicePedidoTest` | 10 |
| `EntityImportServicePresupuestoTest` | 7 |

### Sprints completados (histórico)
- **Sprint B** — Transacciones explícitas en DAOs (5 commits, 12 tests nuevos).
- **Sprint D** — Defaults DDL TEXT en Presupuesto/Factura/Albarán (3 commits, 3 tests nuevos).
- **Sprint D-ter (parcial)** — Defaults DDL en EmpleadoDAO, MaterialDAO, PagoMaterialDAO (3 commits, 1 test nuevo).
- **Sprint UI/UX Bloques 1-10** — Modernización visual completa (14 commits).

### Sprint actual y cola de trabajo

**Inmediato: D-ter 1d** — `ClienteDAO.setBase` preserva DEFAULT en `tipo` ('empresa') y `ciudad` ('Almería'). Leer `ClienteDAOTest.java` antes de redactar el test.

**Cola después de 1d** (ver `INFORME-FINAL.md` para detalle):
1. Sprint SEC — 5 correcciones de seguridad (~2h). SEC-2 es crítica.
2. Sprint COD — eliminar dead code (~45 min).
3. Sprint UI-A/B/C — mejoras visuales CSS + Java (ver `interfaz.md`).
4. Sprint C — empleados inactivos (Deuda 2).
5. Refactor B2 — largo plazo.

**Antes de empezar código:** commitear los cambios `.md` de la sesión de auditoría.

---

## Deudas técnicas abiertas (resumen)

| ID | Descripción | Prioridad |
|----|-------------|-----------|
| 2 | Filtro `activo=1` en `resolverEmpleadoId` rompe nóminas históricas | Media |
| 20-bis | Defaults DDL numéricos primitivos (`double`→`Double`) | Baja |
| 20-ter | ClienteDAO 1d pendiente (único restante del scope TEXT) | INMEDIATA |
| 24 | Sin tests JDBC en 5 DAOs (EmpleadoDAO, PagoMaterialDAO, NominaDAO, PedidoDAO, PagoPedidoDAO) | Media |
| 3, 5, 6, 7... | Otras deudas menores | Baja |

Ver `Resumen.md` — sección DEUDAS TÉCNICAS para el listado completo.

---

## Próximos sprints candidatos

1. **Sprint D-ter 1d (INMEDIATO)** — ClienteDAO.setBase, coste mínimo.
2. **Sprint C** — Empleados inactivos (Deuda 2), coste bajo.
3. **Sprint UI/UX siguiente** — Ver `interfaz.md` para candidatos.
4. **Sprint cobertura tests** — Crear tests JDBC para 5 DAOs (Deuda 24).
5. **Refactor B2** — Inyectar Connection en DAOs (amplio, después de otros).

---

## Procedimiento al retomar

1. Leer: `CLAUDE.md`, `Resumen.md`, este `continuar.md`, `interfaz.md`.
2. Verificar estado git:
   ```powershell
   git log --oneline -8
   git status --short
   ```
3. Verificar tests:
   ```powershell
   .\mvnw.cmd test
   ```
   Esperado: 72/72 verdes.
4. Continuar con el sprint pendiente o preguntar qué arrancar.

---

## Valores conocidos — errores frecuentes

- **`DuplicatePolicy`**: valores válidos son `SKIP_IF_EXISTS`, `UPDATE_EXISTING`, `CREATE_NEW`. NO existe `UPDATE_IF_EXISTS`.
- **`DatabaseManager.getConnection()`**: singleton estático. Todos los DAOs comparten la misma Connection.
- **SQLite no aplica DEFAULT con NULL explícito** (`setString(n, null)` pisa el DEFAULT). Solo si la columna se omite del INSERT.
- **`Nothing to compile` no prueba compilación.** Usar `.\mvnw.cmd clean compile`.
- **`findstr /N "X \"Y\""` no escapa bien en PowerShell.** Usar `Select-String -Path 'archivo' -Pattern 'patron'`.

---

*continuar.md — Gráficas Mulberry — 2026-06-02*
