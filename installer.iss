#define MyAppName "GraficasMulberry"
#define MyAppVersion "5.0.0"
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
OutputBaseFilename=GraficasMulberry-Instalador-v5.0.0
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

[Files]
Source: "{#MyAppDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
; Arrancar la aplicación al finalizar
Filename: "{app}\{#MyAppExeName}"; \
  Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; \
  Flags: nowait postinstall skipifsilent
