# Remediacion de seguridad 2026-06-13

Seguimiento de los hallazgos del informe `SECURITY_AUDIT_2026-06-13.md`.

## Estado

| ID | Estado | Cambio aplicado |
|---|---|---|
| SEC-2026-06-13-01 | Corregido | `OllamaInstallerDialog` verifica firma Authenticode valida y firmante con `Ollama` antes de ejecutar el EXE descargado. |
| SEC-2026-06-13-02 | Corregido | Jackson actualizado a `jackson-databind:2.21.4`; el arbol resuelve `jackson-core:2.21.4`. |
| SEC-2026-06-13-03 | Corregido | `SingleInstanceLock` enlaza el puerto fijo solo a `InetAddress.getLoopbackAddress()`. |
| SEC-2026-06-13-04 | Corregido | Restauracion/importacion SQL exige cabecera de exportacion Graficas Mulberry, limita tamano y falla cerrado ante sentencias rechazadas. |
| SEC-2026-06-13-05 | Corregido | Importaciones tienen limites de tamano, filas, columnas, ZIP descomprimido y entradas ZIP. |
| SEC-2026-06-13-06 | Corregido | `AuthService` aplica bloqueo temporal en login y recuperacion tras 5 fallos. |
| SEC-2026-06-13-07 | Corregido | `ClienteDAO.update()` usa `DatabaseManager.quoteIdentifier()` tambien para columnas extra. |
| SEC-2026-06-13-08 | Corregido | Maven Wrapper fija `distributionSha256Sum` para Maven 3.9.11. |
| SEC-2026-06-13-09 | Corregido | Instaladores archivados ya no ejecutan `install.ps1`; abren la descarga oficial en navegador. |
| SEC-2026-06-13-10 | Corregido | `OllamaService` limita prompt de usuario, contexto ERP, historial y prompt total. |

## Hallazgos adicionales identificados y corregidos en la misma sesion (2026-06-13)

Identificados durante verificacion manual post-audit en la misma sesion. Todos corregidos en commit `6268479`.

| ID | Estado | Cambio aplicado |
|---|---|---|
| NEW-01 | Corregido | `HelpView.init()`: `engine.setJavaScriptEnabled(false)` desactiva JS en WebView. El contenido HTML es classpath-only; JS no aporta nada y amplía superficie de ataque. |
| NEW-02 | Corregido | `_cajon-desastre/` eliminado del tracking git (`git rm -rf`). Contenía scripts de instalador legacy con `iex` y ejecucion remota de PowerShell sin verificar. |
| NEW-03 | Corregido | Bloqueo de login/recuperacion migrado de `ConcurrentHashMap` en memoria a 4 columnas SQLite (`login_failed`, `login_locked_until`, `recovery_failed`, `recovery_locked_until`). El bloqueo ahora persiste entre reinicios. Nuevos metodos publicos en `UserDAO`. `AuthService` delega completamente en `UserDAO`. 2 nuevos tests de persistencia en `AuthServiceTest`. |

## Archivos principales modificados

- `.gitleaks.toml`
- `.mvn/wrapper/maven-wrapper.properties`
- `pom.xml`
- `src/main/java/org/gipsybuho/SingleInstanceLock.java`
- `src/main/java/org/gipsybuho/dao/ClienteDAO.java`
- `src/main/java/org/gipsybuho/dao/UserDAO.java` (NEW-03)
- `src/main/java/org/gipsybuho/db/DatabaseManager.java` (NEW-03: 4 columnas DDL + migraciones)
- `src/main/java/org/gipsybuho/service/AuthService.java`
- `src/main/java/org/gipsybuho/service/ImportBackupService.java`
- `src/main/java/org/gipsybuho/service/OllamaService.java`
- `src/main/java/org/gipsybuho/ui/HelpView.java` (NEW-01)
- `src/main/java/org/gipsybuho/ui/LoginView.java`
- `src/main/java/org/gipsybuho/ui/OllamaInstallerDialog.java`
- `src/test/java/org/gipsybuho/dao/ClienteDAOTest.java`
- `src/test/java/org/gipsybuho/service/AuthServiceTest.java` (NEW-03: 2 tests nuevos)
- `src/test/java/org/gipsybuho/service/ImportBackupServiceTest.java`
- `_cajon-desastre/` eliminado del repositorio (NEW-02)

## Validaciones ejecutadas

```powershell
.\mvnw.cmd -q clean compile
.\mvnw.cmd -q test
.\mvnw.cmd dependency:tree "-Dincludes=com.fasterxml.jackson.core:*" "-DoutputFile=docs/security/dependency-tree-jackson-postfix-2026-06-13.txt"
osv-scanner scan source -r . --format json | Out-File -Encoding utf8 docs\security\osv-scanner-postfix-2026-06-13.json
gitleaks dir . --redact --no-banner --report-format json --report-path docs\security\gitleaks-postfix-src-redacted-2026-06-13.json --exit-code 0
gitleaks detect --source . --redact --no-banner --report-format json --report-path docs\security\gitleaks-postfix-redacted-2026-06-13.json --exit-code 0
.\mvnw.cmd -q com.github.spotbugs:spotbugs-maven-plugin:4.9.4.1:spotbugs "-DxmlOutput=true" "-DspotbugsXmlOutput=true"
Copy-Item target\spotbugsXml.xml docs\security\spotbugsXml-postfix-2026-06-13.xml -Force
rg -n "prepareStatement|createStatement|executeQuery|executeUpdate|execute\(|SELECT |INSERT |UPDATE |DELETE |ALTER |DROP |PRAGMA|ProcessBuilder|Runtime\.getRuntime|HttpClient|URI\.create|ServerSocket|Socket\(|WebView|loadContent|ZipInputStream|readAllBytes|BCrypt|password|login|role|permission|resetPassword" src/main/java > docs\security\rg-security-surface-postfix-2026-06-13.txt
& "C:\Users\Gipsy Dávy\.codex-security-tools\semgrep-venv\Scripts\semgrep.exe" scan --config p/java --metrics=off --json --output docs\security\semgrep-postfix-java-2026-06-13.json src/main/java src/test/java 2> docs\security\semgrep-postfix-java-2026-06-13.err
& "C:\Program Files\ClamAV\clamscan.exe" -r --database="C:\Users\GipsyDavy\.codex-security-tools\clamav-db" --log="docs\security\clamscan-postfix-scoped-2026-06-13.log" src docs pom.xml .mvn .gitleaks.toml AGENTS.md GEMINI.md SECURITY.md CLAUDE.md
```

Resultados:

- Compilacion: OK.
- Tests: OK.
- Jackson resuelto: `jackson-databind:2.21.4`, `jackson-core:2.21.4`, `jackson-annotations:2.21`.
- OSV post-fix: `results: []`.
- Gitleaks arbol actual post-fix: `[]`.
- Gitleaks historial Git post-fix: `[]` usando `.gitleaks.toml` para excluir falsos positivos del runtime JDK generado.
- SpotBugs post-fix: 161 avisos totales; 9 categoria `SECURITY` revisados. Corresponden a SQL dinamico conocido en `ClienteDAO`, `DynamicColumnValueDAO` e `ImportBackupService`; el triage actual exige identificadores por allowlist/regex y valores parametrizados donde aplica.
- ClamAV post-fix acotado a codigo, docs y configuracion: 303 ficheros, 0 infecciones.

Artefactos post-fix:

- `dependency-tree-jackson-postfix-2026-06-13.txt`
- `osv-scanner-postfix-2026-06-13.json`
- `gitleaks-postfix-src-redacted-2026-06-13.json`
- `gitleaks-postfix-redacted-2026-06-13.json`
- `spotbugsXml-postfix-2026-06-13.xml`
- `rg-security-surface-postfix-2026-06-13.txt`
- `semgrep-postfix-java-2026-06-13.err`
- `clamscan-postfix-scoped-2026-06-13.log`
- `clamscan-postfix-2026-06-13.log` parcial, generado por un intento completo que se cancelo por timeout.

## Limitaciones

- Semgrep post-fix no pudo descargar reglas remotas (`p/java`) por `SSLCertVerificationError` contra `semgrep.dev` en el entorno Python local. La auditoria original conserva `semgrep-2026-06-13.json`; el error post-fix queda en `semgrep-postfix-java-2026-06-13.err`.
- ClamAV completo sobre todo el repositorio excedio 5 minutos y se cancelo. Se completo un escaneo acotado a `src`, `docs`, `pom.xml`, `.mvn`, `.gitleaks.toml`, `AGENTS.md`, `GEMINI.md`, `SECURITY.md` y `CLAUDE.md` con 0 infecciones.

## Commit final

Todas las correcciones (SEC-2026-06-13-01..10 + NEW-01..03) incluidas en commit `6268479` — `security: auditoría y remediación completa 2026-06-13 (SEC-01..10 + NEW-01..03)`. Validado: 142/142 tests verdes. Fecha: 2026-06-13.
