# Analisis de cierre de aplicacion

Proyecto: Graficas Mulberry  
Fecha: 2026-06-13  
Estado de referencia: `docs/context/STATE.md`  
HEAD documentado: `6268479`  
Tests documentados: 142/142 verdes

Este documento recoge el analisis de cierre previo a continuar con nuevos sprints.
No es una auditoria de codigo nueva ni sustituye a la documentacion de seguridad.

## Veredicto

La aplicacion no parece lejos de una version usable interna. El nucleo funcional ya
esta construido: modulos principales, importacion/exportacion, backups, ayuda offline,
onboarding, roles, autenticacion, seguridad auditada/remediada, instalador Windows y
suite de tests documentada.

Lo que queda no parece desarrollo base de meses. Es trabajo de cierre de producto:
migracion real, validacion instalada, reproducibilidad del instalador, pulido UX y
documentacion final de uso.

## Riesgo principal

El mayor riesgo ya no es programar mas funcionalidad, sino validar que la aplicacion
resuelve el flujo real del negocio con datos reales.

El punto critico es `Sprint MIGRACION-COMPLEJA`: el importador ya abre los archivos
soportados y maneja CSV/Excel limpio, pero todavia falta cerrar la migracion de tablas
humanas complejas con celdas combinadas, varias mini-tablas, filas decorativas,
formulas y bloques laterales.

Sin ese cierre, la aplicacion puede funcionar bien tecnicamente, pero el cliente puede
quedarse bloqueado al trasladar historico real.

## Trabajo pendiente real

### 1. Migracion real del cliente

Completar al menos un piloto de archivo complejo real:

- inspeccionar estructura;
- convertir a CSV limpio o plantilla especifica;
- importar en base de prueba;
- validar visualmente en la app;
- documentar reglas de normalizacion.

Referencia viva: `MIGRACION_HISTORICO.md`.

### 2. Validacion manual de ejecucion

La suite automatica cubre DAOs, servicios e importacion, pero falta una matriz manual
de ejecucion instalada:

- primer arranque;
- login y recuperacion;
- roles y permisos;
- CRUD por modulo principal;
- exportacion PDF/Excel;
- backup y restauracion;
- ayuda F1;
- modo oscuro;
- Ollama ausente/presente;
- instalacion/desinstalacion.

### 3. Release e instalador

Existe pipeline con Maven, `jpackage` y NSIS. El riesgo operativo visible es que
`build-nsis.ps1` depende de rutas absolutas locales a JDK, Maven de IntelliJ y proyecto.

Para cierre profesional conviene hacerlo reproducible o documentar claramente que el
build oficial se realiza desde una maquina concreta.

### 4. Pulido UX menor

El modo principiante existe, pero las barras de ayuda estan documentadas como deuda
en varios modulos. Ahora `ClientesView` tiene hint bar; faltan Facturas, Pedidos,
Materiales y Empleados si se mantiene esa promesa de producto.

Tambien quedan verificaciones visuales menores: DatePicker, ContextMenu, tablas en
ventanas pequenas y coherencia en modo oscuro.

### 5. Deuda tecnica no bloqueante

Hay clases grandes en zonas complejas:

- `ExportService`;
- `ImportBackupService`;
- `ImportService`;
- `VisualAssistantView`;
- algunas vistas JavaFX de alto volumen.

No deberian bloquear una entrega si los flujos reales pasan validacion. Refactor B2
queda como trabajo post-release salvo que aparezca un bug real ligado a transacciones
o ciclo de vida de conexiones.

## Sprints recomendados

### Sprint 1 - MIGRACION-COMPLEJA PILOTO

Objetivo: importar un archivo complejo real completo.

Prioridad recomendada: `PRECIOS PAPEL PROVEEDORES Formulas.xlsx`, por tener destino
claro en Materiales y estructura suficientemente estable para prototipo.

Criterio de cierre:

- CSV limpio generado;
- reglas documentadas;
- importacion en BD de prueba;
- validacion visual en la app;
- limitaciones anotadas.

### Sprint 2 - RELEASE-GATE MANUAL

Objetivo: validar la aplicacion como usuario final.

Criterio de cierre:

- matriz manual ejecutada;
- bugs P0/P1 cerrados;
- bugs menores clasificados para backlog;
- sin cambios amplios de arquitectura.

### Sprint 3 - INSTALLER-REPRO

Objetivo: cerrar instalador y versionado.

Criterio de cierre:

- build reproducible o runbook exacto;
- version unica confirmada en `pom.xml`, `AppConstants`, NSIS y docs;
- instalacion/desinstalacion probada;
- acceso directo y arranque validados.

### Sprint 4 - DATA-SAFETY / BACKUP-GATE

Objetivo: demostrar que no se pierden datos.

Criterio de cierre:

- backup creado desde app;
- restauracion probada;
- importacion fallida no corrompe datos;
- limites de archivo comprobados;
- procedimiento de recuperacion documentado.

### Sprint 5 - UX-HINTS / FINAL POLISH

Objetivo: cerrar promesas visibles de UX.

Criterio de cierre:

- hints de modo principiante extendidos o feature recortada claramente;
- revision visual de Facturas, Pedidos, Materiales, Empleados;
- DatePicker, ContextMenu y tablas pequenas revisados;
- sin introducir redisenos grandes.

### Sprint 6 - DOCS ENTREGA

Objetivo: dejar documentacion de usuario final.

Criterio de cierre:

- guia corta de instalacion;
- primer arranque;
- backups;
- importacion;
- exportacion;
- recuperacion de acceso;
- Ollama como funcionalidad opcional;
- recomendaciones de seguridad operacional.

## Post-release

Refactor B2: inyeccion de `Connection` en DAOs.

Es recomendable para mantenimiento y consistencia tecnica, pero no imprescindible para
cerrar la aplicacion si la validacion funcional, seguridad, migracion y release pasan.

## Conclusion

La aplicacion esta cerca de cierre. El nucleo esta hecho y el estado de seguridad es
fuerte segun la auditoria documentada. Los siguientes pasos deben centrarse en datos
reales, validacion instalada y entrega operativa, no en ampliar funcionalidad.

