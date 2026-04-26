#define MyAppName "GraficasMulberry"
#define MyAppVersion "3.2.0"
#define MyAppPublisher "Graficas Mulberry"
#define MyAppExeName "GraficasMulberry.exe"
#define MyAppDir "output\GraficasMulberry"

[Setup]
AppId={{EA4500CB-FF1C-32ED-B06A-AF2063329E5C}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
OutputDir=output
OutputBaseFilename=GraficasMulberry-Instalador-v3.2.0
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupIconFile=installer\logo.ico
WizardImageFile=installer\wizard-banner.bmp
WizardSmallImageFile=installer\wizard-small.bmp
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
UninstallDisplayIcon={app}\{#MyAppExeName}

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon";  Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"
Name: "instolarollama"; Description: "Descargar e instalar Ollama (IA local — requiere ~1.8 GB de descarga)"; GroupDescription: "Componentes opcionales:"; Flags: unchecked

[Files]
Source: "{#MyAppDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
; Instalar Ollama si el usuario lo seleccionó (descarga ~1.8 GB via PowerShell)
Filename: "powershell.exe"; \
  Parameters: "-ExecutionPolicy Bypass -WindowStyle Normal -Command ""irm https://ollama.com/install.ps1 | iex"""; \
  Description: "Descargando e instalando Ollama (puede tardar varios minutos)..."; \
  Flags: runhidden waituntilterminated; \
  Tasks: instolarollama

; Descargar el modelo llama3.2 (2 GB) si Ollama se instaló
Filename: "powershell.exe"; \
  Parameters: "-WindowStyle Hidden -Command ""Start-Sleep 5; & '$env:LOCALAPPDATA\Programs\Ollama\ollama.exe' pull llama3.2"""; \
  Description: "Descargando modelo IA llama3.2 (~2 GB, proceso en segundo plano)..."; \
  Flags: runhidden nowait; \
  Tasks: instolarollama

; Arrancar la aplicación al finalizar
Filename: "{app}\{#MyAppExeName}"; \
  Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; \
  Flags: nowait postinstall skipifsilent
