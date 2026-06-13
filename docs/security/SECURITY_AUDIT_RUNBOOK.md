# Runbook de auditoria de seguridad

Este procedimiento permite repetir una auditoria similar en Windows desde la raiz del proyecto.

## Preparacion

```powershell
Set-Location "C:\Users\GipsyDavy\MAVEN\Graficas Mulberry"
New-Item -ItemType Directory -Force -Path docs\security | Out-Null
$DATE = Get-Date -Format "yyyy-MM-dd"
```

Herramientas usadas en la auditoria 2026-06-13:

```powershell
winget install --id Gitleaks.Gitleaks --source winget --accept-source-agreements --accept-package-agreements --disable-interactivity
winget install --id Google.OSVScanner --source winget --accept-source-agreements --accept-package-agreements --disable-interactivity
winget install --id Cisco.ClamAV --source winget --accept-source-agreements --accept-package-agreements --disable-interactivity

python -m venv "C:\Users\Gipsy Dávy\.codex-security-tools\semgrep-venv"
& "C:\Users\Gipsy Dávy\.codex-security-tools\semgrep-venv\Scripts\pip.exe" install semgrep
```

## Baseline de compilacion y dependencias

```powershell
mvn -q -DskipTests compile
mvn -q dependency:tree "-DoutputFile=docs/security/dependency-tree-$DATE.txt"
mvn -q dependency:tree "-DoutputType=json" "-DoutputFile=docs/security/dependency-tree-$DATE.json"
```

## SCA con OSV

```powershell
osv-scanner scan source -r . --format json --output "docs/security/osv-scanner-$DATE.json"
```

Triage:

- Revisar `groups[].max_severity`.
- Confirmar paquete/version exacta.
- Distinguir dependencia directa de transitiva.
- Verificar si el API vulnerable se usa en el proyecto.

## Secret scanning

```powershell
gitleaks dir . --redact --no-banner --report-format json --report-path "docs/security/gitleaks-src-redacted-$DATE.json" --exit-code 0
gitleaks detect --source . --redact --no-banner --report-format json --report-path "docs/security/gitleaks-redacted-$DATE.json" --exit-code 0
```

Triage:

- Este repo incluye `.gitleaks.toml` para permitir falsos positivos concretos del runtime JDK generado.
- Tratar `output/`, runtimes generados y docs como posibles falsos positivos.
- El escaneo `src` pesa mas para secretos reales de codigo.
- Revisar `.env.example`; placeholders no son secretos.

## SAST con Semgrep

```powershell
& "C:\Users\Gipsy Dávy\.codex-security-tools\semgrep-venv\Scripts\semgrep.exe" `
  --config p/java `
  --config p/security-audit `
  --metrics=off `
  --json `
  --output "docs/security/semgrep-$DATE.json" `
  src/main/java src/test/java
```

Triage:

- Si la descarga de reglas remotas falla por certificados TLS, guardar `stderr` en `docs/security/semgrep-*-*.err` y documentarlo como limitacion.
- SQL dinamico: confirmar si los identificadores tienen allowlist/regex y los valores usan parametros.
- ProcessBuilder/Runtime: confirmar si hay concatenacion de input de usuario.
- HTTP: confirmar destino, timeout, TLS y verificacion de binarios descargados.

## SpotBugs

```powershell
mvn -q com.github.spotbugs:spotbugs-maven-plugin:4.9.4.1:spotbugs "-DxmlOutput=true" "-DspotbugsXmlOutput=true"
Copy-Item target\spotbugsXml.xml "docs/security/spotbugsXml-$DATE.xml" -Force
```

Resumen rapido:

```powershell
[xml]$x = Get-Content target\spotbugsXml.xml
$bugs = @($x.BugCollection.BugInstance)
$bugs | Group-Object category | Sort-Object Count -Descending
$bugs | Where-Object {
  $_.category -match 'SECURITY|MALICIOUS|BAD_PRACTICE' -or
  $_.type -match 'SQL|PATH|COMMAND|SEC|XSS|XXE|PREDICT|HARD|WEAK|ENCRYPT|INJECTION|RANDOM'
} | Sort-Object @{Expression={[int]$_.priority}}, @{Expression={[int]$_.rank}}
```

## Superficies por `rg`

```powershell
rg -n "prepareStatement|createStatement|executeQuery|executeUpdate|execute\(|SELECT |INSERT |UPDATE |DELETE |ALTER |DROP |PRAGMA" src/main/java > "docs/security/rg-sql-surface-$DATE.txt"
rg -n "BCrypt|password|login|role|permission|security_question|resetPassword|hasPermission|isAdmin" src/main/java > "docs/security/rg-auth-surface-$DATE.txt"
rg -n "ProcessBuilder|Runtime\.getRuntime|HttpClient|URI\.create|ServerSocket|Socket\(|WebView|loadContent|Files\.readAllBytes|readAllBytes|ZipInputStream|FileChooser" src/main/java > "docs/security/rg-exec-network-surface-$DATE.txt"
rg -n "FileChooser|DirectoryChooser|Files\.|Path\.|new File|ZipInputStream|PDDocument\.load|WorkbookFactory\.create|OPCPackage\.open|readString|readAllBytes" src/main/java > "docs/security/rg-file-surface-$DATE.txt"
```

## Malware scan con ClamAV

No guardar la base de firmas en el repo. Usar una ruta externa:

```powershell
$db = "C:\Users\GipsyDavy\.codex-security-tools\clamav-db"
New-Item -ItemType Directory -Force -Path $db | Out-Null
$conf = Join-Path $env:TEMP "freshclam-graficas-mulberry.conf"
$freshLog = (Resolve-Path "docs\security").Path + "\freshclam-$DATE.log"
@"
DatabaseDirectory "$db"
UpdateLogFile "$freshLog"
LogTime yes
DatabaseMirror database.clamav.net
"@ | Set-Content -LiteralPath $conf -Encoding ASCII

& "C:\Program Files\ClamAV\freshclam.exe" --config-file=$conf

$scanLog = (Resolve-Path "docs\security").Path + "\clamscan-$DATE.log"
& "C:\Program Files\ClamAV\clamscan.exe" `
  --recursive `
  --infected `
  --database=$db `
  --log=$scanLog `
  --exclude-dir='[\\/]\\.git([\\/]|$)' `
  (Resolve-Path .).Path
```

Si el escaneo completo excede el tiempo disponible, hacer tambien un escaneo acotado de los ficheros fuente y de configuracion:

```powershell
& "C:\Program Files\ClamAV\clamscan.exe" `
  -r `
  --database=$db `
  --log="docs\security\clamscan-scoped-$DATE.log" `
  src docs pom.xml .mvn .gitleaks.toml AGENTS.md GEMINI.md SECURITY.md CLAUDE.md
```

Si Microsoft Defender esta activo, se puede anadir:

```powershell
Start-MpScan -ScanType CustomScan -ScanPath (Resolve-Path .).Path
Get-MpThreat
```

## Revision manual obligatoria

Revisar y documentar con `archivo:linea`:

- SQL dinamico y restauracion/importacion SQL.
- Importacion de CSV/Excel/JSON/PDF/ZIP y limites de tamano.
- Descargas y ejecucion de binarios externos.
- Autenticacion, recuperacion de contraseña y permisos.
- WebView/carga HTML.
- Scripts de instalacion, wrapper Maven y workflows.
- Puertos/sockets locales.
- Dependencias vulnerables.

## Criterios de severidad

- Alto: ejecucion remota, robo de credenciales, bypass de admin o escritura arbitraria sin interaccion clara del usuario.
- Medio: supply chain ejecutable, DoS local fuerte, importacion SQL peligrosa, dependencia vulnerable alcanzable.
- Bajo: hardening, deuda de defensa en profundidad, riesgo solo con acceso local o scripts archivados.

## Cierre

Antes de cerrar una auditoria:

```powershell
git status --short
Get-ChildItem docs\security | Sort-Object Name | Select-Object Name,Length,LastWriteTime
```

El informe debe incluir:

- Herramientas y versiones.
- Comandos ejecutados.
- Hallazgos con severidad y `archivo:linea`.
- Falsos positivos descartados.
- Limitaciones de la auditoria.
- Acciones recomendadas.
