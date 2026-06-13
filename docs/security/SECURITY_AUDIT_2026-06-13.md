# Auditoria de seguridad - 2026-06-13

Proyecto: Graficas Mulberry, Java 21 + JavaFX 21 + SQLite/JDBC + Maven.

Alcance revisado: codigo `src/main/java`, recursos `src/main/resources`, `pom.xml`, wrapper Maven, scripts de instalacion, artefactos generados en `output/`, pruebas principales, documentacion operativa y superficies de importacion/exportacion.

Nota post-fix: los hallazgos de este informe fueron remediados y revalidados en `SECURITY_REMEDIATION_2026-06-13.md`. Mantener este archivo como baseline de auditoria y usar el documento de remediacion para el estado corregido, comandos ejecutados, artefactos post-fix y limitaciones.

## Herramientas usadas

| Herramienta | Version | Resultado |
| --- | ---: | --- |
| Semgrep | 1.166.0 | 17 hallazgos revisados manualmente |
| Gitleaks | 8.30.1 | Sin secretos reales en `src`; 2 falsos positivos en runtime generado |
| OSV Scanner | 2.3.8 | 1 advisory en `jackson-core` transitivo |
| SpotBugs | 4.9.4.1 plugin | 160 avisos, 9 de categoria `SECURITY` revisados |
| ClamAV | 1.5.2 | 1042 ficheros, 0 infecciones |
| Maven compile | 3.9.12 | `mvn -q -DskipTests compile` OK |

Intento no completado: OWASP Dependency-Check Maven plugin 12.1.0 se lanzo, pero excedio 10 minutos sin producir informe final. Se mantiene OSV Scanner como SCA efectivo de esta pasada.

Windows Defender no pudo usarse: `MpCmdRun.exe` fallo con `Product/Feature disabled`; `Get-MpComputerStatus` indico `AntivirusEnabled=False`.

## Hallazgos

### SEC-2026-06-13-01 - Medio - Instalador de Ollama ejecuta EXE descargado sin verificacion criptografica

Ubicacion:

- `src/main/java/org/gipsybuho/ui/OllamaInstallerDialog.java:25`
- `src/main/java/org/gipsybuho/ui/OllamaInstallerDialog.java:276`
- `src/main/java/org/gipsybuho/ui/OllamaInstallerDialog.java:308`
- `src/main/java/org/gipsybuho/ui/OllamaInstallerDialog.java:321`
- `src/main/java/org/gipsybuho/ui/OllamaInstallerDialog.java:335`

El flujo descarga `https://ollama.com/download/OllamaSetup.exe`, valida HTTP 200 y tamano minimo, y luego ejecuta el fichero con `ProcessBuilder`. No se valida firma Authenticode, publisher ni hash SHA-256 esperado.

Impacto: riesgo de supply chain si el binario descargado o la ruta de descarga se ve comprometida. No hay inyeccion de comandos porque se usa `ProcessBuilder` con ruta directa, pero si hay ejecucion de codigo externo.

Correccion recomendada: antes de `ProcessBuilder`, validar firma Authenticode y publisher esperado, o pinnear SHA-256 por version. Si no se puede garantizar, abrir pagina/instruccion manual en lugar de ejecutar automaticamente.

### SEC-2026-06-13-02 - Medio - Dependencia vulnerable transitiva: jackson-core 2.16.1

Ubicacion:

- `pom.xml:59`
- `pom.xml:60`
- `pom.xml:61`

OSV Scanner reporto `GHSA-72hv-8253-57qq` para `com.fasterxml.jackson.core:jackson-core 2.16.1`, transitiva de `jackson-databind 2.16.1`. Severidad maxima reportada: 6.9. El advisory afecta al parser async por bypass de limites de longitud numerica y puede derivar en DoS por memoria.

Observacion de explotabilidad: en el codigo revisado se usa `ObjectMapper.readTree(...)` sobre ficheros locales/importados y respuestas de Ollama, no se encontro uso directo del parser async. Aun asi, la version esta dentro del rango afectado reportado por OSV.

Correccion recomendada: subir Jackson a una version corregida en la rama elegida. OSV lista `2.18.6` como fix para la rama antigua y `2.21.1` para el rango 2.19+.

### SEC-2026-06-13-03 - Medio - Bloqueo de instancia expone puerto fijo en todas las interfaces

Ubicacion:

- `src/main/java/org/gipsybuho/SingleInstanceLock.java:8`
- `src/main/java/org/gipsybuho/SingleInstanceLock.java:20`
- `src/main/java/org/gipsybuho/SingleInstanceLock.java:24`

`new ServerSocket(LOCK_PORT)` escucha en el puerto fijo `54321` y, por defecto, en wildcard/all interfaces. Cualquier proceso local, y potencialmente un host de la red local si el firewall lo permite, puede ocupar el puerto y hacer que la aplicacion crea que ya hay otra instancia.

Impacto: DoS de arranque y superficie de red innecesaria.

Correccion recomendada: bind a loopback (`InetAddress.getLoopbackAddress()`) o reemplazarlo por `FileLock` en el perfil del usuario.

### SEC-2026-06-13-04 - Medio - Restauracion SQL ejecuta sentencias de fichero con filtro regex amplio

Ubicacion:

- `src/main/java/org/gipsybuho/ui/ImportBackupView.java:123`
- `src/main/java/org/gipsybuho/ui/ImportBackupView.java:130`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:117`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:124`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:125`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:2051`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:2060`

La restauracion `.sql` lee el fichero completo y ejecuta sentencias si `esStatementSeguro()` las acepta. La regex permite cuerpos amplios de `CREATE TABLE` e `INSERT ... .*`; la validacion principal es por tipo de sentencia y tabla permitida.

Impacto: un backup SQL no confiable puede manipular datos de tablas permitidas o provocar DoS de la BD. No se encontro ejecucion de comandos del sistema, `ATTACH/DETACH/UPDATE/DELETE` quedan bloqueados por el filtro, y el flujo es una restauracion destructiva iniciada por el usuario. El riesgo real es tratar backups SQL de terceros como seguros.

Correccion recomendada: no ejecutar SQL libre para restaurar. Preferir JSON/CSV con parametros, o implementar un parser estricto que solo acepte la sintaxis exacta emitida por el exportador y rechace expresiones/subconsultas.

### SEC-2026-06-13-05 - Medio - Importaciones sin limites de tamano permiten DoS local por memoria/CPU

Ubicacion representativa:

- `src/main/java/org/gipsybuho/ui/ImportView.java:202`
- `src/main/java/org/gipsybuho/ui/ImportView.java:207`
- `src/main/java/org/gipsybuho/service/ImportService.java:136`
- `src/main/java/org/gipsybuho/service/ImportService.java:147`
- `src/main/java/org/gipsybuho/service/ImportService.java:342`
- `src/main/java/org/gipsybuho/service/ImportService.java:379`
- `src/main/java/org/gipsybuho/service/ImportService.java:456`
- `src/main/java/org/gipsybuho/service/ImportService.java:464`
- `src/main/java/org/gipsybuho/service/ImportService.java:1013`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:83`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:93`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java:118`

Los selectores permiten `Todos/*.*`, `parseFile()` intenta CSV para extensiones desconocidas, varios lectores usan `readAllBytes()` o `readTree()` completos, y el ZIP de backup lee cada entrada entera en memoria sin limite por entrada ni tamano descomprimido.

Impacto: un fichero local enorme, ZIP bomb, Excel/PDF complejo o JSON masivo puede congelar la UI, agotar memoria o consumir CPU.

Correccion recomendada: validar extension real en servicio, eliminar fallback a CSV para desconocidos, aplicar `Files.size()` maximo por tipo, limites de filas/columnas/celdas, streaming para CSV/JSON y limites de POI/PDFBox/ZIP antes de parsear.

### SEC-2026-06-13-06 - Bajo - Login y recuperacion sin bloqueo ni backoff

Ubicacion:

- `src/main/java/org/gipsybuho/ui/LoginView.java:154`
- `src/main/java/org/gipsybuho/ui/LoginView.java:155`
- `src/main/java/org/gipsybuho/ui/LoginView.java:202`
- `src/main/java/org/gipsybuho/ui/LoginView.java:207`
- `src/main/java/org/gipsybuho/service/AuthService.java:109`
- `src/main/java/org/gipsybuho/service/AuthService.java:113`
- `src/main/java/org/gipsybuho/db/DatabaseManager.java:91`

No hay contador de intentos, lockout temporal ni backoff para login o respuesta de seguridad. La migracion elimina claves antiguas de `max_login_attempts` y `login_lockout_minutes`.

Impacto: brute force local contra contraseña o respuesta de seguridad si alguien tiene acceso al equipo/sesion de la app.

Correccion recomendada: contador por usuario, retardo incremental, bloqueo temporal, registro de fallos y proteccion especifica para reset por pregunta de seguridad.

### SEC-2026-06-13-07 - Bajo - Invariante de SQL dinamico inconsistente en ClienteDAO.update()

Ubicacion:

- `src/main/java/org/gipsybuho/dao/ClienteDAO.java:57`
- `src/main/java/org/gipsybuho/dao/ClienteDAO.java:79`

El `INSERT` usa `quotedColumns()` y acaba llamando a `DatabaseManager.quoteIdentifier()`, pero el `UPDATE` concatena claves extra como `" + k + "`. Los flujos actuales revisados sanean esas claves antes de llegar al DAO:

- `src/main/java/org/gipsybuho/service/ImportarClientesService.java:120`
- `src/main/java/org/gipsybuho/service/ImportarClientesService.java:131`
- `src/main/java/org/gipsybuho/dao/ColumnConfigDAO.java:84`
- `src/main/java/org/gipsybuho/dao/ColumnConfigDAO.java:95`
- `src/main/java/org/gipsybuho/service/EntityImportService.java:1079`

Impacto actual bajo, pero el DAO no se defiende solo si un futuro caller entrega una clave extra sin sanear.

Correccion recomendada: construir los sets extra con `DatabaseManager.quoteIdentifier(k) + "=?";`.

### SEC-2026-06-13-08 - Bajo - Maven Wrapper sin checksum fijado

Ubicacion:

- `.mvn/wrapper/maven-wrapper.properties:1`
- `.mvn/wrapper/maven-wrapper.properties:2`
- `.mvn/wrapper/maven-wrapper.properties:3`

El wrapper descarga Maven desde HTTPS, pero no hay `distributionSha256Sum`. Los scripts `mvnw`/`mvnw.cmd` soportan validacion si se configura.

Impacto: riesgo supply chain si se usa `./mvnw` en entornos nuevos.

Correccion recomendada: anadir `distributionSha256Sum` del ZIP Maven 3.9.11 o actualizar wrapper y checksum juntos.

### SEC-2026-06-13-09 - Bajo - Scripts archivados ejecutan instalador remoto con `iex`

Ubicacion:

- `_cajon-desastre/installer_v2.iss:44`
- `_cajon-desastre/installer_v2.iss:45`
- `_cajon-desastre/installer_v3.iss:44`
- `_cajon-desastre/installer_v3.iss:45`
- `_cajon-desastre/build-v2.ps1:80`
- `_cajon-desastre/build-v3.ps1:78`

Los instaladores Inno archivados ejecutan `irm https://ollama.com/install.ps1 | iex` con `ExecutionPolicy Bypass`. El instalador vigente `installer.nsi` no hace esto; copia `output\GraficasMulberry\*.*`.

Impacto: deuda operativa. Si alguien reutiliza esos scripts antiguos, vuelve a introducir ejecucion remota no pinneada.

Correccion recomendada: eliminar `_cajon-desastre` del repositorio, moverlo fuera del arbol versionado o marcar esos scripts como no ejecutables/deprecados.

### SEC-2026-06-13-10 - Bajo - Prompt de Ollama sin limite de longitud

Ubicacion:

- `src/main/java/org/gipsybuho/service/OllamaService.java:160`
- `src/main/java/org/gipsybuho/service/OllamaService.java:164`
- `src/main/java/org/gipsybuho/service/OllamaService.java:172`
- `src/main/java/org/gipsybuho/service/OllamaService.java:200`
- `src/main/java/org/gipsybuho/service/OllamaService.java:202`
- `src/main/java/org/gipsybuho/service/OllamaService.java:204`

El historial limita cantidad (`MAX_HISTORIAL=10`) y trunca respuestas a 2000 caracteres, pero el prompt de usuario y `contextoERP` se agregan sin limite de longitud.

Impacto: DoS local de CPU/RAM contra Ollama o bloqueo prolongado del flujo IA.

Correccion recomendada: limite maximo de caracteres/tokens para prompt, contexto ERP y cuerpo HTTP; truncado visible al usuario.

## Falsos positivos o hallazgos descartados

- Semgrep marco SQL dinamico en `ColumnConfigDAO`, `DynamicColumnValueDAO`, `EntityImportService`, `ExportService` e `ImportBackupService`. La mayoria se descarta como SQLi directa porque los identificadores se validan con regex/allowlist y los valores usan `PreparedStatement`.
- Gitleaks full-repo encontro 2 supuestos secretos en `output/GraficasMulberry/runtime/conf/security/java.security` (`jdk.tls.keyLimits` y `jdk.quic.tls.keyLimits`). Son falsos positivos de un runtime Java generado. El escaneo limitado a `src` no encontro secretos.
- `TextToSpeechService` usa `powershell.exe`, pero el texto y la voz se pasan por variables de entorno (`GM_TTS_TEXT`, `GM_TTS_VOICE`), no concatenados en el script.
- `HelpView` carga HTML local de recursos. No se encontraron `<script>`, `iframe`, `object`, `embed` ni handlers inline en la ayuda. Se detectan enlaces informativos a Ollama, pero no carga remota automatica de codigo.
- No se encontro puerta trasera obvia, keylogger, comando oculto persistente, descarga remota activa fuera de Ollama, ni secretos reales en codigo fuente.

## Artefactos generados

- `dependency-tree-2026-06-13.txt`
- `dependency-tree-2026-06-13.json`
- `semgrep-2026-06-13.json`
- `spotbugsXml-2026-06-13.xml`
- `osv-scanner-2026-06-13.json`
- `gitleaks-redacted-2026-06-13.json`
- `gitleaks-src-redacted-2026-06-13.json`
- `freshclam-2026-06-13.log`
- `clamscan-2026-06-13.log`
- `rg-sql-surface-2026-06-13.txt`
- `rg-auth-surface-2026-06-13.txt`
- `rg-file-surface-2026-06-13.txt`
- `rg-exec-network-surface-2026-06-13.txt`

## Resumen ejecutivo

No se encontraron indicios de malware en el arbol escaneado, ni secretos reales en fuente, ni SQL injection directa en los flujos principales parametrizados. Los riesgos principales son supply chain por ejecucion/descarga de terceros, DoS local por importaciones sin limites, restauracion SQL demasiado permisiva para backups no confiables y una dependencia vulnerable reportada por OSV.
