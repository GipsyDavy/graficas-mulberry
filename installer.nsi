; ============================================================
; installer.nsi — Instalador NSIS moderno para Graficas Mulberry
; NSIS 3.x + Modern UI 2
; ============================================================

!include "MUI2.nsh"
!include "FileFunc.nsh"
!include "LogicLib.nsh"

; ── Configuracion general ────────────────────────────────────
Name              "Graficas Mulberry"
OutFile           "output\GraficasMulberry-Instalador-v13.5.0.exe"
InstallDir        "$LOCALAPPDATA\GraficasMulberry"
InstallDirRegKey  HKCU "Software\GraficasMulberry" "InstallDir"
RequestExecutionLevel user
Unicode True
SetCompressor /SOLID lzma
SetCompressorDictSize 32

; ── MUI2: iconos ─────────────────────────────────────────────
!define MUI_ICON   "installer\logo.ico"
!define MUI_UNICON "installer\logo.ico"

; ── MUI2: panel izquierdo (164x314) ─────────────────────────
!define MUI_WELCOMEFINISHPAGE_BITMAP   "installer\nsis-welcome.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "installer\nsis-welcome.bmp"

; ── MUI2: cabecera paginas interiores (150x57) ───────────────
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP         "installer\nsis-header.bmp"
!define MUI_HEADERIMAGE_BITMAP_NOSTRETCH
!define MUI_HEADERIMAGE_RIGHT

; ── MUI2: textos pagina de bienvenida ────────────────────────
!define MUI_WELCOMEPAGE_TITLE "Graficas Mulberry 13.5.0"
!define MUI_WELCOMEPAGE_TEXT "Bienvenido al instalador de Graficas Mulberry.$\r$\n$\r$\nEste sistema de gestion para serigrafía incluye su propio entorno Java. No necesitas instalar ningun software adicional.$\r$\n$\r$\nHaz clic en Siguiente para continuar."

; ── MUI2: textos pagina final ────────────────────────────────
!define MUI_FINISHPAGE_TITLE "Instalacion completada"
!define MUI_FINISHPAGE_TEXT "Graficas Mulberry v13.5.0 se ha instalado correctamente.$\r$\n$\r$\nSe ha creado un acceso directo en el escritorio.$\r$\n$\r$\nHaz clic en Finalizar para cerrar el instalador."
!define MUI_FINISHPAGE_RUN      "$INSTDIR\GraficasMulberry.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Abrir Graficas Mulberry ahora"

; ── MUI2: advertencia al cancelar ────────────────────────────
!define MUI_ABORTWARNING
!define MUI_ABORTWARNING_TEXT "La instalacion no ha terminado. Si cierras ahora, el programa no quedara instalado.$\r$\n$\r$\n¿Deseas salir del instalador?"

; ── Paginas de instalacion ───────────────────────────────────
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

; ── Paginas de desinstalacion ────────────────────────────────
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; ── Idioma ───────────────────────────────────────────────────
!insertmacro MUI_LANGUAGE "Spanish"

; ── Version info embebida en el .exe ─────────────────────────
VIProductVersion "13.5.0.0"
VIAddVersionKey /LANG=0 "ProductName"     "Graficas Mulberry"
VIAddVersionKey /LANG=0 "ProductVersion"  "13.5.0"
VIAddVersionKey /LANG=0 "CompanyName"     "Graficas Mulberry S.L."
VIAddVersionKey /LANG=0 "FileDescription" "Instalador de Graficas Mulberry"
VIAddVersionKey /LANG=0 "FileVersion"     "13.5.0.0"
VIAddVersionKey /LANG=0 "LegalCopyright"  "(C) 2024-2026 Graficas Mulberry S.L."

; ── Detectar version anterior y ofrecer desinstalacion ───────
Function .onInit
    ReadRegStr $R0 HKCU "Software\GraficasMulberry" "InstallDir"
    ${If} $R0 != ""
    ${AndIf} ${FileExists} "$R0\Desinstalar.exe"
        MessageBox MB_YESNO|MB_ICONQUESTION \
            "Se ha detectado una version anterior de Graficas Mulberry.$\r$\n$\r$\nSe desinstalara automaticamente antes de instalar la nueva version.$\r$\n$\r$\n¿Deseas continuar?" \
            IDNO abort_install
        ExecWait '"$R0\Desinstalar.exe" /S'
        Goto done_check
        abort_install:
            Abort
        done_check:
    ${EndIf}
FunctionEnd

; ── Seccion de instalacion ────────────────────────────────────
Section "Aplicacion" SecMain
    SectionIn RO

    SetOutPath "$INSTDIR"
    SetOverwrite on

    ; Copiar toda la app-image de jpackage
    File /r "output\GraficasMulberry\*.*"

    ; Guardar directorio de instalacion
    WriteRegStr HKCU "Software\GraficasMulberry" "InstallDir" "$INSTDIR"
    WriteRegStr HKCU "Software\GraficasMulberry" "Version"    "13.5.0"

    ; Desinstalador
    WriteUninstaller "$INSTDIR\Desinstalar.exe"

    ; Entrada en Agregar/Quitar programas
    WriteRegStr HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "DisplayName" "Graficas Mulberry"
    WriteRegStr HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "DisplayVersion" "13.5.0"
    WriteRegStr HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "Publisher" "Graficas Mulberry S.L."
    WriteRegStr HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "UninstallString" '"$INSTDIR\Desinstalar.exe"'
    WriteRegStr HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "DisplayIcon" "$INSTDIR\GraficasMulberry.exe,0"
    WriteRegStr HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "InstallLocation" "$INSTDIR"
    WriteRegDWORD HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "NoModify" 1
    WriteRegDWORD HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "NoRepair" 1

    ; Calcular tamano estimado
    ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
    IntFmt $0 "0x%08X" $0
    WriteRegDWORD HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry" \
        "EstimatedSize" "$0"

    ; Menu de inicio
    CreateDirectory "$SMPROGRAMS\Graficas Mulberry"
    CreateShortcut \
        "$SMPROGRAMS\Graficas Mulberry\Graficas Mulberry.lnk" \
        "$INSTDIR\GraficasMulberry.exe" "" "$INSTDIR\GraficasMulberry.exe" 0
    CreateShortcut \
        "$SMPROGRAMS\Graficas Mulberry\Desinstalar.lnk" \
        "$INSTDIR\Desinstalar.exe"

    ; Acceso directo en escritorio
    CreateShortcut \
        "$DESKTOP\Graficas Mulberry.lnk" \
        "$INSTDIR\GraficasMulberry.exe" "" "$INSTDIR\GraficasMulberry.exe" 0
SectionEnd

; ── Seccion de desinstalacion ─────────────────────────────────
Section "Uninstall"
    ; Eliminar archivos de la aplicacion
    RMDir /r "$INSTDIR"

    ; Eliminar accesos directos
    Delete "$SMPROGRAMS\Graficas Mulberry\Graficas Mulberry.lnk"
    Delete "$SMPROGRAMS\Graficas Mulberry\Desinstalar.lnk"
    RMDir  "$SMPROGRAMS\Graficas Mulberry"
    Delete "$DESKTOP\Graficas Mulberry.lnk"

    ; Limpiar registro
    DeleteRegKey HKCU \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\GraficasMulberry"
    DeleteRegKey HKCU "Software\GraficasMulberry"
SectionEnd
