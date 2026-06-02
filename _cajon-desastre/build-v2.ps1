$ErrorActionPreference = "Stop"

$JAVA_HOME  = "C:\Program Files\Java\jdk-26.0.1"
$MVN        = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
$JPACKAGE   = "$JAVA_HOME\bin\jpackage.exe"
$ISCC       = "C:\Users\david\AppData\Local\Programs\Inno Setup 6\ISCC.exe"
$PROJECT    = $PSScriptRoot
$VERSION    = "2.0.0"
$JAR_ORIG   = "original-GraficasMulberry-1.0-SNAPSHOT.jar"
$INPUT_DIR  = "$PROJECT\target\jpackage-input-v2"
$MODS_DIR   = "$INPUT_DIR\mods"
$APP_IMAGE  = "$PROJECT\output\GraficasMulberry"

$env:JAVA_HOME = $JAVA_HOME

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  GRAFICAS MULBERRY  v$VERSION  -- Generando instalador .exe" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# --- 1. Compilar con Maven ---------------------------------------------------
Write-Host "[1/4] Compilando con Maven (perfil Windows)..." -ForegroundColor Yellow
& $MVN package -Ppackage-windows -f "$PROJECT\pom.xml" -q
if ($LASTEXITCODE -ne 0) { Write-Error "ERROR: Fallo la compilacion Maven"; exit 1 }

$jarPath = "$PROJECT\target\$JAR_ORIG"
if (-not (Test-Path $jarPath)) { Write-Error "ERROR: No se encontro $JAR_ORIG en target\"; exit 1 }
$sizeMB = [math]::Round((Get-Item $jarPath).Length / 1MB, 1)
Write-Host ("  JAR original: " + $JAR_ORIG + "  (" + $sizeMB + " MB)") -ForegroundColor Green

# --- 2. Preparar directorio de entrada para jpackage ------------------------
Write-Host ""
Write-Host "[2/4] Preparando dependencias para jpackage..." -ForegroundColor Yellow

# Solo borramos la app-image y el directorio temporal de input.
# NO tocamos output\GraficasMulberry-Instalador-v1.0.0.exe
Remove-Item $INPUT_DIR  -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $APP_IMAGE  -Recurse -Force -ErrorAction SilentlyContinue
New-Item $INPUT_DIR, $MODS_DIR -ItemType Directory | Out-Null

Copy-Item $jarPath "$INPUT_DIR\GraficasMulberry.jar"

$mods = 0
$libs = 0
Get-ChildItem "$PROJECT\target\lib\*.jar" | ForEach-Object {
    if ($_.Name -match "^javafx-.*-win\.jar$") {
        Copy-Item $_ $MODS_DIR; $mods++
    } elseif ($_.Name -notmatch "^javafx-") {
        Copy-Item $_ $INPUT_DIR; $libs++
    }
}
Write-Host ("  Modulos JavaFX win: " + $mods + "  |  Libs classpath: " + $libs) -ForegroundColor Green

# --- 3. Crear app-image con jpackage ----------------------------------------
Write-Host ""
Write-Host "[3/4] Creando app-image con jpackage v$VERSION..." -ForegroundColor Yellow

& $JPACKAGE `
    --type          app-image `
    --name          "GraficasMulberry" `
    --app-version   $VERSION `
    --vendor        "Graficas Mulberry" `
    --description   "Sistema de gestion para Graficas Mulberry" `
    --icon          "$PROJECT\installer\logo.ico" `
    --input         $INPUT_DIR `
    --main-jar      "GraficasMulberry.jar" `
    --main-class    "org.gipsybuho.Main" `
    --java-options  "--module-path `$APPDIR/mods" `
    --java-options  "--add-modules javafx.controls,javafx.fxml,javafx.swing,javafx.base,javafx.graphics,javafx.media" `
    --java-options  "-Xmx512m" `
    --java-options  "-Dfile.encoding=UTF-8" `
    --dest          "$PROJECT\output"

if ($LASTEXITCODE -ne 0) { Write-Error "ERROR: jpackage fallo"; exit 1 }
Write-Host "  App-image creada en: output\GraficasMulberry\" -ForegroundColor Green

# --- 4. Compilar instalador con Inno Setup ----------------------------------
Write-Host ""
Write-Host "[4/4] Generando instalador .exe con Inno Setup..." -ForegroundColor Yellow
Set-Location $PROJECT
& $ISCC "installer_v2.iss"
if ($LASTEXITCODE -ne 0) { Write-Error "ERROR: Inno Setup fallo"; exit 1 }

# --- Resultado ---------------------------------------------------------------
$exeV2 = "$PROJECT\output\GraficasMulberry-Instalador-v$VERSION.exe"
$exeV1 = "$PROJECT\output\GraficasMulberry-Instalador-v1.0.0.exe"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  EXITO -- Instalador v$VERSION generado correctamente" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""

if (Test-Path $exeV1) {
    $s1 = [math]::Round((Get-Item $exeV1).Length / 1MB, 1)
    Write-Host ("  v1.0.0 (anterior): GraficasMulberry-Instalador-v1.0.0.exe  (" + $s1 + " MB)") -ForegroundColor Gray
}
if (Test-Path $exeV2) {
    $s2 = [math]::Round((Get-Item $exeV2).Length / 1MB, 1)
    Write-Host ("  v2.0.0 (nuevo):    GraficasMulberry-Instalador-v" + $VERSION + ".exe  (" + $s2 + " MB)") -ForegroundColor White
}

Write-Host ""
Write-Host ("  Carpeta: " + $PROJECT + "\output\") -ForegroundColor Yellow
Write-Host ""

Start-Process explorer.exe "$PROJECT\output"
