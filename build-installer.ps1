$ErrorActionPreference = "Stop"

# ── Rutas ──────────────────────────────────────────────────────────────────
$JAVA_HOME = "C:\Program Files\Java\jdk-26"
$MVN       = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
$JPACKAGE  = "$JAVA_HOME\bin\jpackage.exe"
$ISCC_candidatos = @(
    "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe",
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
    "C:\Program Files\Inno Setup 6\ISCC.exe"
)
$ISCC = ($ISCC_candidatos | Where-Object { Test-Path $_ } | Select-Object -First 1)
if (-not $ISCC) { Write-Error "Inno Setup no encontrado. Instálalo con: winget install JRSoftware.InnoSetup"; exit 1 }
$PROJECT   = $PSScriptRoot

# ── 1. Compilar con Maven ───────────────────────────────────────────────────
Write-Host "`n=== 1/3  Compilando con Maven ===" -ForegroundColor Cyan
& $MVN clean package -Ppackage-windows -f "$PROJECT\pom.xml"
if ($LASTEXITCODE -ne 0) { Write-Error "Fallo en la compilacion Maven"; exit 1 }

# ── 2. Preparar estructura para jpackage ────────────────────────────────────
$InputDir = "$PROJECT\target\jpackage-input"
$ModsDir  = "$InputDir\mods"
$AppImDir = "$PROJECT\output\GraficasMulberry"   # app-image destino

Remove-Item "$PROJECT\output" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $InputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item $InputDir, $ModsDir -ItemType Directory | Out-Null

# JAR de la aplicacion (sin dependencias embebidas - el "original" previo al shade)
$OriginalJar = Get-ChildItem "$PROJECT\target\original-GraficasMulberry-*.jar" |
               Sort-Object LastWriteTime -Descending |
               Select-Object -First 1
if (-not $OriginalJar) { Write-Error "No se encontro el JAR original en target\"; exit 1 }
Write-Host "   JAR original: $($OriginalJar.Name)" -ForegroundColor Gray
Copy-Item $OriginalJar.FullName "$InputDir\GraficasMulberry.jar"

Write-Host "   Clasificando dependencias..." -ForegroundColor Gray
Get-ChildItem "$PROJECT\target\lib\*.jar" | ForEach-Object {
    if ($_.Name -match "^javafx-.*-win\.jar$") {
        # JARs JavaFX con DLLs nativas Windows -> module-path (carpeta mods/)
        Copy-Item $_ $ModsDir
    } elseif ($_.Name -notmatch "^javafx-") {
        # Resto (SQLite, OpenPDF, Jackson, Ikonli...) -> classpath
        Copy-Item $_ $InputDir
    }
}

$mods = (Get-ChildItem $ModsDir).Count
$libs = (Get-ChildItem $InputDir -File).Count
Write-Host "   Modulos JavaFX: $mods  |  Librerias classpath: $libs"

# ── 3. Crear app-image con jpackage ─────────────────────────────────────────
Write-Host "`n=== 2/3  Creando app-image con jpackage ===" -ForegroundColor Cyan
& $JPACKAGE `
    --type app-image `
    --name "GraficasMulberry" `
    --app-version "8.5.2" `
    --vendor "Graficas Mulberry S.L." `
    --description "Sistema de gestion para Graficas Mulberry" `
    --copyright "Copyright (C) 2024-2026 Graficas Mulberry S.L." `
    --icon "$PROJECT\installer\logo.ico" `
    --input $InputDir `
    --main-jar "GraficasMulberry.jar" `
    --main-class "org.gipsybuho.Main" `
    --java-options "--module-path `$APPDIR/mods" `
    --java-options "--add-modules javafx.controls,javafx.fxml,javafx.swing,javafx.base,javafx.graphics" `
    --dest "$PROJECT\output"

if ($LASTEXITCODE -ne 0) { Write-Error "jpackage fallo"; exit 1 }

# ── 4. Compilar instalador con Inno Setup ───────────────────────────────────
Write-Host "`n=== 3/3  Creando instalador .exe con Inno Setup ===" -ForegroundColor Cyan
Set-Location $PROJECT
& $ISCC "installer.iss"
if ($LASTEXITCODE -ne 0) { Write-Error "Inno Setup fallo"; exit 1 }

Write-Host "`n=== Instalador listo ===" -ForegroundColor Green
Get-ChildItem "$PROJECT\output\*.exe" | ForEach-Object {
    Write-Host "   $($_.Name)  ($([math]::Round($_.Length/1MB,1)) MB)"
}
Write-Host ""
Write-Host "Ruta: $PROJECT\output\" -ForegroundColor Yellow

# ── 5. Firma de codigo (opcional — requiere certificado .pfx) ───────────────
# Para eliminar falsos positivos de antivirus y Windows SmartScreen, descomenta
# este bloque y proporciona tu certificado de firma de codigo (OV o EV).
#
# NUNCA hardcodees la contrasena — pásala como variable de entorno:
#   $env:SIGN_CERT_PASSWORD = "tu_contrasena"
#
# $SIGNTOOL  = "C:\Program Files (x86)\Windows Kits\10\bin\10.0.22621.0\x64\signtool.exe"
# $CERT_PFX  = "C:\ruta\certificado_firma.pfx"
# $TIMESTAMP = "http://timestamp.sectigo.com"
#
# if (Test-Path $CERT_PFX) {
#     Write-Host "`n=== Firmando ejecutables ===" -ForegroundColor Cyan
#     $signArgs = @("sign", "/fd", "SHA256", "/tr", $TIMESTAMP, "/td", "SHA256",
#                   "/f", $CERT_PFX, "/p", $env:SIGN_CERT_PASSWORD)
#
#     # Firmar el launcher principal
#     & $SIGNTOOL @signArgs "$PROJECT\output\GraficasMulberry\GraficasMulberry.exe"
#
#     # Firmar el instalador
#     Get-ChildItem "$PROJECT\output\*.exe" | ForEach-Object {
#         & $SIGNTOOL @signArgs $_.FullName
#         Write-Host "   Firmado: $($_.Name)" -ForegroundColor Green
#     }
# } else {
#     Write-Host "`n[AVISO] Certificado no encontrado — ejecutables sin firmar" -ForegroundColor Yellow
# }
