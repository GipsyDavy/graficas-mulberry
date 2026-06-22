$ErrorActionPreference = "Stop"

# ── Rutas ──────────────────────────────────────────────────────────
$PROJECT     = "C:\Users\GipsyDavy\MAVEN\Graficas Mulberry"
$JAVA_HOME   = "C:\Program Files\Java\jdk-26"
$MVN         = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
$JPACKAGE    = "$JAVA_HOME\bin\jpackage.exe"
$APP_VERSION = "14.0.0"

$MAKENSIS = @(
    "C:\Program Files (x86)\NSIS\makensis.exe",
    "C:\Program Files\NSIS\makensis.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $MAKENSIS) {
    Write-Error "NSIS no encontrado. Instala con: winget install NSIS.NSIS --source winget"
    exit 1
}

Set-Location $PROJECT

Write-Host ""
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  GRAFICAS MULBERRY — Build NSIS v$APP_VERSION" -ForegroundColor Magenta
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host ""

# ── 1. Maven clean package ─────────────────────────────────────────
Write-Host "[1/4] Compilando con Maven..." -ForegroundColor Yellow
$env:JAVA_HOME = $JAVA_HOME
& $MVN clean package -Ppackage-windows -f "$PROJECT\pom.xml" -q
if ($LASTEXITCODE -ne 0) { Write-Error "Fallo en Maven"; exit 1 }
Write-Host "  Maven OK" -ForegroundColor Green

# ── 2. jpackage app-image ──────────────────────────────────────────
Write-Host ""
Write-Host "[2/4] Creando app-image con jpackage..." -ForegroundColor Yellow

$InputDir = "$PROJECT\target\jpackage-input"
$ModsDir  = "$InputDir\mods"

Remove-Item "$PROJECT\output" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $InputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item $InputDir, $ModsDir -ItemType Directory | Out-Null

$OriginalJar = Get-ChildItem "$PROJECT\target\original-GraficasMulberry-*.jar" `
    | Sort-Object LastWriteTime -Descending `
    | Select-Object -First 1
if (-not $OriginalJar) { Write-Error "JAR original no encontrado en target\"; exit 1 }
Copy-Item $OriginalJar.FullName "$InputDir\GraficasMulberry.jar"

$mods = 0; $libs = 0
Get-ChildItem "$PROJECT\target\lib\*.jar" | ForEach-Object {
    if ($_.Name -match "^javafx-.*-win\.jar$") {
        Copy-Item $_ $ModsDir; $mods++
    } elseif ($_.Name -notmatch "^javafx-") {
        Copy-Item $_ $InputDir; $libs++
    }
}
Write-Host "  Modulos JavaFX: $mods  |  Librerias classpath: $libs" -ForegroundColor Gray

& $JPACKAGE `
    --type app-image `
    --name "GraficasMulberry" `
    --app-version $APP_VERSION `
    --vendor "Graficas Mulberry S.L." `
    --description "Sistema de gestion para Graficas Mulberry" `
    --copyright "(C) 2024-2026 Graficas Mulberry S.L." `
    --icon "$PROJECT\installer\logo.ico" `
    --input $InputDir `
    --main-jar "GraficasMulberry.jar" `
    --main-class "org.gipsybuho.Main" `
    --java-options "--module-path `$APPDIR/mods" `
    --java-options "--add-modules javafx.controls,javafx.fxml,javafx.swing,javafx.base,javafx.graphics" `
    --dest "$PROJECT\output"

if ($LASTEXITCODE -ne 0) { Write-Error "jpackage fallo"; exit 1 }
Write-Host "  App-image OK" -ForegroundColor Green

# ── 3. Graficos del instalador ─────────────────────────────────────
Write-Host ""
Write-Host "[3/4] Generando graficos del instalador..." -ForegroundColor Yellow
python "$PROJECT\installer\gen_graphics.py" "$PROJECT"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  (Advertencia: graficos no generados - se usaran los existentes)" -ForegroundColor DarkYellow
} else {
    Write-Host "  Graficos OK" -ForegroundColor Green
}

# ── 4. NSIS ────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[4/4] Compilando instalador NSIS..." -ForegroundColor Yellow
& $MAKENSIS /V2 /INPUTCHARSET UTF8 "$PROJECT\installer.nsi"
if ($LASTEXITCODE -ne 0) { Write-Error "NSIS fallo"; exit 1 }

# Guardar copia en historial
$histDir = "$PROJECT\installer\v$APP_VERSION-nsis"
if (-not (Test-Path $histDir)) { New-Item $histDir -ItemType Directory | Out-Null }
$exeOut = "$PROJECT\output\GraficasMulberry-Instalador-v$APP_VERSION.exe"
if (Test-Path $exeOut) {
    Copy-Item $exeOut "$histDir\GraficasMulberry-Instalador-v$APP_VERSION-nsis.exe"
}

$exe     = Get-Item $exeOut -ErrorAction SilentlyContinue
$sizeMB  = if ($exe) { [math]::Round($exe.Length / 1MB, 1) } else { "?" }

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  INSTALADOR NSIS GENERADO CORRECTAMENTE" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Archivo : GraficasMulberry-Instalador-v$APP_VERSION.exe" -ForegroundColor White
Write-Host "  Tamano  : $sizeMB MB" -ForegroundColor White
Write-Host "  Ruta    : $PROJECT\output\" -ForegroundColor Yellow
Write-Host ""

Start-Process explorer.exe "$PROJECT\output"
