# ============================================================
# build_exe.ps1 — Compila Graficas Mulberry y genera el .exe
# ============================================================

$ErrorActionPreference = "Stop"

$projectDir  = "C:\Users\GipsyDavy\MAVEN\Graficas Mulberry"
$mvn         = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
$jdk24       = "C:\Program Files\Java\jdk-24"
$jpackage    = "$jdk24\bin\jpackage.exe"
$java        = "$jdk24\bin\java.exe"
$wixDir      = "C:\Users\GipsyDavy\wix3"
$destDir     = [System.IO.Path]::Combine([System.Environment]::GetFolderPath("Desktop"), "graficas_mulberry")
$appName     = "GraficasMulberry"
$appVersion  = "3.1.0"
$jarName     = "GraficasMulberry-3.1.jar"

# Añadir WiX al PATH para que jpackage lo encuentre
$env:PATH = "$wixDir;$env:PATH"

Set-Location $projectDir

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  COMPILACION GRAFICAS MULBERRY" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. Convertir logo JPG → ICO ────────────────────────────
Write-Host "[1/4] Convirtiendo logo a .ico ..." -ForegroundColor Yellow
$icoScript = @"
from PIL import Image
import os
img = Image.open(r'$projectDir\src\main\resources\org\gipsybuho\img\logo.jpg').convert('RGBA')
img.save(r'$projectDir\logo.ico', format='ICO', sizes=[(256,256),(128,128),(64,64),(48,48),(32,32),(16,16)])
print('ICO generado correctamente')
"@
python -c $icoScript
if ($LASTEXITCODE -ne 0) {
    Write-Host "  (Advertencia: no se pudo crear el icono personalizado, se usara el icono por defecto)" -ForegroundColor DarkYellow
    $iconArg = @()
} else {
    Write-Host "  logo.ico creado OK" -ForegroundColor Green
    $iconArg = @("--icon", "$projectDir\logo.ico")
}

# ── 2. Compilar con Maven ──────────────────────────────────
Write-Host ""
Write-Host "[2/4] Compilando con Maven (puede tardar varios minutos)..." -ForegroundColor Yellow
$env:JAVA_HOME = $jdk24
& $mvn -f "$projectDir\pom.xml" clean package "-DskipTests" "-q"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Fallo la compilacion Maven. Revisa los errores anteriores." -ForegroundColor Red
    exit 1
}
Write-Host "  Compilacion Maven completada OK" -ForegroundColor Green

# ── 3. Verificar JAR generado ─────────────────────────────
$jarPath = "$projectDir\target\$jarName"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: No se encontro el JAR en: $jarPath" -ForegroundColor Red
    exit 1
}
$sizeMB = [math]::Round((Get-Item $jarPath).Length / 1MB, 1)
Write-Host "  JAR generado: $jarName ($sizeMB MB)" -ForegroundColor Green

# ── 4. Crear directorio de destino ────────────────────────
if (Test-Path "$destDir\$appName") {
    Write-Host ""
    Write-Host "[3/4] Limpiando instalacion anterior..." -ForegroundColor Yellow
    Remove-Item "$destDir\$appName" -Recurse -Force
}
New-Item -ItemType Directory -Path $destDir -Force | Out-Null

# ── 5. Ejecutar jpackage ──────────────────────────────────
Write-Host ""
Write-Host "[3/4] Creando ejecutable Windows con jpackage..." -ForegroundColor Yellow

$jpackageArgs = @(
    "--type", "exe",
    "--input", "$projectDir\target",
    "--main-jar", $jarName,
    "--main-class", "org.gipsybuho.Main",
    "--name", $appName,
    "--app-version", $appVersion,
    "--vendor", "Graficas Mulberry",
    "--description", "Sistema de gestion para Graficas Mulberry",
    "--dest", $destDir,
    "--java-options", "-Xmx512m",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--win-dir-chooser",
    "--win-shortcut",
    "--win-menu",
    "--win-menu-group", "Graficas Mulberry",
    "--win-shortcut-prompt"
) + $iconArg

& $jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Fallo jpackage." -ForegroundColor Red
    exit 1
}

# ── 6. Resultado ───────────────────────────────────────────
$exePath = "$destDir\${appName}-${appVersion}.exe"
if (Test-Path $exePath) {
    $exeSizeMB = [math]::Round((Get-Item $exePath).Length / 1MB, 1)
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  EXITO - Ejecutable generado correctamente!" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Ruta: $destDir\$appName\" -ForegroundColor White
    Write-Host "  EXE:  $appName.exe ($exeSizeMB MB)" -ForegroundColor White
    Write-Host ""
    Write-Host "[4/4] Abriendo carpeta de destino..." -ForegroundColor Yellow
    Start-Process explorer.exe $destDir
} else {
    Write-Host "ADVERTENCIA: El proceso termino pero no se encontro el .exe en la ruta esperada." -ForegroundColor DarkYellow
    Start-Process explorer.exe $destDir
}
