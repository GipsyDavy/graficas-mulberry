# CLIENTE-GATE — Preparación para valoración cliente final

**Fecha inicio:** 2026-06-12  
**Objetivo:** decidir con evidencia si Gráficas Mulberry puede pasar a valoración del cliente final.

---

## Estado real inicial

La aplicación **no debe marcarse como entrega final cerrada** sin esta validación.
Sí puede aspirar a **valoración controlada del cliente** si pasa este gate.

Últimos commits relevantes:
- `c144f06` — `security(auth): unificar minimo password`
- `f3905d2` — `docs: clasificar migracion compleja`
- `ab485ac` — `docs: activar caveman por defecto`
- `3b2aa5b` — `fix(import): cubrir grupos vacios`
- `8857b50` — `refactor(import): eliminar modo expandido del wizard`
- `2f72be2` — `fix(import): expandir matrices de precios solo al importar tarifas`
- `938c474` — `fix(import): usar nombre como clave de negocio en materiales sin referencia`

Working tree al inicio:
- `.claude/settings.local.json` modificado, fuera de alcance.
- `.agents/` sin seguimiento, fuera de alcance.
- `skills-lock.json` sin seguimiento, fuera de alcance.

---

## Funcionalidad que aporta cada sprint reciente

| Sprint | Qué aporta al producto | Impacto para cliente |
|---|---|---|
| Import fixes materiales/tarifas | Materiales sin referencia importan; matrices de precios solo se expanden en Tarifas; modo expandido eliminado; grupos vacíos se rellenan. | Importación más simple para usuario no técnico. |
| MIGRACION-COMPLEJA | Clasifica Excel humanos antiguos y decide conversión a CSV/script por caso. | Facilita carga inicial de históricos; no es flujo diario. |
| CLIENTE-GATE | Verifica build, tests, seguridad, UX básica, importación y readiness. | Decide si procede demo/valoración. |

---

## Criterios para pasar a valoración cliente

| Área | Criterio | Estado |
|---|---|---|
| Build | `.\mvnw.cmd clean package -DskipTests` verde | OK |
| Tests | `.\mvnw.cmd test` verde | OK: 127/127 |
| Seguridad | VibeSec/security review sobre import/export/datos/SQL/roles | OK con deuda baja documentada |
| Multi-IA | Bloque Gemini preparado para segunda opinión | Pendiente usuario/Gemini |
| Importación | Probar o validar flujo Tarifas/Materiales con archivos reales | Parcial: tests/dry-run OK; prueba UI manual pendiente |
| UX | Revisar mensajes, bloqueo de errores, ausencia de modo expandido | OK técnico; validación visual manual pendiente |
| Handoff | Actualizar `.md` con resultado y decisión | OK |

---

## Security review scope

Revisión explícita requerida:
- Imports/exports: rutas, extensiones, backups, sobrescritura.
- SQL dinámico: identificadores validados y `PreparedStatement`.
- Datos sensibles: clientes, facturas, nóminas, usuarios.
- Roles/permisos: acceso por rol a módulos principales.
- IA/Ollama: no confiar en respuesta IA; límites, timeouts, no secretos.

Hallazgos abiertos ya conocidos:
- `SEC-NEW-4`: concurrencia inconsistente en `OllamaService`.
- `SEC-NEW-5`: historial de `OllamaService` sin límite de contenido.
- `COD-NEW-2`: `STYLE_BURBUJA` inline.

Estos abiertos no bloquean necesariamente demo controlada, pero sí deben constar como deuda.

### Resultado security review local

Revisión hecha con enfoque VibeSec sobre autenticación, import/export, SQL dinámico,
datos sensibles, roles y Ollama.

Hallazgo corregido en este gate:
- `SEC-GATE-1`: mínimo de contraseña inconsistente. `UserManagementView`
  permitía crear usuarios con 6 caracteres mientras alta admin y reset pedían 8.
  Fix: mínimo centralizado en `AuthService.MIN_PASSWORD_LENGTH = 8`,
  validación defensiva en `registerUser`, `changePassword`,
  `resetPasswordAdmin` y `resetPasswordWithAnswer`.
  Test nuevo: `AuthServiceTest` cubre rechazo de contraseña corta en registro,
  reset admin y cambio de contraseña.

Puntos revisados sin P0/P1 nuevo:
- SQL: usos dinámicos relevantes usan allowlist o validación de identificador
  (`quoteIdentifier` / `requireSqlIdentifier`) y valores con `PreparedStatement`.
- Import: CSV/Excel se parsean como datos; no se ejecuta contenido del archivo.
- Backup/import SQL: `ImportBackupService` mantiene whitelist de sentencias y rechaza
  instrucciones fuera del formato permitido.
- Export: tablas salen de allowlist interna; `VACUUM INTO ?` usa parámetro.
- Roles: permisos por `UserRole` siguen controlando módulos sensibles.

Deuda no bloqueante:
- `SEC-NEW-4` y `SEC-NEW-5` en `OllamaService` siguen abiertos como deuda baja/media.
- No se ha ejecutado escáner externo de dependencias; revisión fue manual + tests.

### Validación ejecutada

```powershell
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
.\mvnw.cmd test
# Resultado: Tests run: 127, Failures: 0, Errors: 0, Skipped: 0

.\mvnw.cmd clean package -DskipTests
# Resultado: BUILD SUCCESS
# Jar: target\GraficasMulberry-13.5.0.jar
```

`git diff --check` sin errores; solo avisos CRLF normales de Windows.

### Prueba manual UI

2026-06-12 23:42: app lanzada para prueba manual con:

```powershell
.\mvnw.cmd javafx:run
```

Proceso inicial: `cmd` PID `2992`; JavaFX levantó procesos `java`.
Resultado funcional de la prueba manual: pendiente de validar por usuario en la ventana.

2026-06-12 23:55: prueba manual detecta dos bugs reales:
- `40_IMANES.xlsx`: paso 3.5 bloquea por `GRUPO` obligatorio vacío. Causa:
  los grupos parciales se rellenaban en paso 4, demasiado tarde para validación.
- Materiales/stock: archivos con `precio_unidad`/`longitud` podían acabar con
  `unidad` numérica (`0.08`, `100.0`) si la IA o el fallback elegían mal.

Fix aplicado:
- La validación del paso 3.5 usa `prepareImport(...)`, igual que la importación
  final. Así `GRUPO`/técnica fija o fallback existen antes de validar.
- El mapeo de Materiales ahora prioriza nombres exactos (`precio_unidad` gana a
  `precio_resma`) y descarta asignaciones no plausibles a `unidad`, `stock_actual`
  o `stock_minimo`.
- `EntityImportService` rechaza unidad de material puramente numérica y conserva
  `ud` por defecto.

Validación post-fix:
```powershell
.\mvnw.cmd test "-Dtest=ImportServiceParsingTest,EntityImportServiceMaterialTest"
# 17/17 verdes

.\mvnw.cmd test
# 129/129 verdes
```

Queda pendiente: limpiar o borrar de la BD real los materiales ya importados con
unidad incorrecta antes de reimportar.

---

## Bloque Gemini — segunda opinión obligatoria

Pegar en Gemini Code Assist para revisión externa del gate:

```text
## Proyecto
Gráficas Mulberry — ERP desktop Windows para empresa de artes gráficas.
Stack: Java 21 + JavaFX 21 + SQLite JDBC directo + Maven. Sin backend, sin Spring, sin ORM.
Ruta: C:\Users\GipsyDavy\MAVEN\Graficas Mulberry

## Contexto
Queremos decidir si la aplicación puede pasar a valoración controlada del cliente final.
No se pide entrega final cerrada, sino readiness para demo/validación.

Sprints recientes:
- Importación corregida: materiales sin referencia, matrices solo en Tarifas, modo expandido eliminado, grupos vacíos autocompletados.
- MIGRACION-COMPLEJA: inventario/clasificación de 288 archivos reales; piloto recomendado `PRECIOS PAPEL PROVEEDORES Formulas.xlsx`.
- Tests recientes esperados: 124/124 verdes según último cierre local.

Archivos a revisar:
- CLIENTE_GATE.md
- continuar.md
- Resumen.md
- AUDITORIA.md
- MIGRACION_HISTORICO.md
- src/main/java/org/gipsybuho/service/ImportService.java
- src/main/java/org/gipsybuho/service/EntityImportService.java
- src/main/java/org/gipsybuho/ui/ImportView.java
- src/main/java/org/gipsybuho/service/OllamaService.java

## Pregunta
Da una segunda opinión técnica: ¿qué bloquearía una valoración controlada del cliente final?
Prioriza riesgos P0/P1 reales. Distingue:
1. bloquea demo cliente,
2. no bloquea demo pero debe documentarse,
3. deuda posterior.

## Formato
Respuesta breve y accionable:
- Hallazgos ordenados por severidad.
- Recomendación final: "apto para valoración controlada" o "no apto".
- Máximo 20 líneas.
```

Estado: pendiente de respuesta Gemini. Bloque preparado, no ejecutable desde CLI
por el flujo Multi-IA real del proyecto.

---

## Decisión preliminar

**Apto para valoración controlada del cliente final**, no para entrega final cerrada.

Condiciones antes de demo con cliente:
1. Ejecutar prueba manual UI de importación en la app con `Desktop\excel`.
2. Pegar el bloque Gemini y registrar respuesta, o dejar constancia explícita de
   que la segunda opinión queda pendiente por disponibilidad.
3. Avisar al cliente de que MIGRACION-COMPLEJA es carga inicial/histórica, no
   flujo diario terminado al 100%.
