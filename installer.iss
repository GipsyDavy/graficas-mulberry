#define MyAppName "GraficasMulberry"
#define MyAppVersion "12.0.0"
#define MyAppPublisher "Graficas Mulberry"
#define MyAppExeName "GraficasMulberry.exe"
#define MyAppDir "output\GraficasMulberry"
#define MyAppGuid "EA4500CB-FF1C-32ED-B06A-AF2063329E5C"

[Setup]
AppId={{EA4500CB-FF1C-32ED-B06A-AF2063329E5C}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
OutputDir=output
OutputBaseFilename=GraficasMulberry-Instalador-v{#MyAppVersion}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupIconFile=installer\logo.ico
WizardImageFile=installer\wizard-banner.bmp
WizardSmallImageFile=installer\wizard-small.bmp
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
UninstallDisplayIcon={app}\{#MyAppExeName}
; Cierra la aplicacion si esta en ejecucion antes de instalar
CloseApplications=yes
CloseApplicationsFilter={#MyAppExeName}
RestartApplications=no

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#MyAppDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; \
  Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; \
  Flags: nowait postinstall skipifsilent

[Code]
// ── Busca la cadena de desinstalacion de la version anterior ────────────
function GetUninstallString(): String;
var
  sUnInstPath: String;
  sUnInstallString: String;
begin
  // Inno Setup crea la clave con el GUID del AppId + sufijo _is1
  sUnInstPath := 'Software\Microsoft\Windows\CurrentVersion\Uninstall\' +
                 '{#MyAppGuid}_is1';
  sUnInstallString := '';
  // Primero buscamos instalacion por maquina (admin), luego por usuario
  if not RegQueryStringValue(HKLM, sUnInstPath, 'UninstallString', sUnInstallString) then
    RegQueryStringValue(HKCU, sUnInstPath, 'UninstallString', sUnInstallString);
  Result := sUnInstallString;
end;

// ── True si existe una version anterior instalada ────────────────────────
function IsUpgrade(): Boolean;
begin
  Result := (GetUninstallString() <> '');
end;

// ── Ejecuta el desinstalador anterior en silencio y espera a que termine ─
function UnInstallOldVersion(): Integer;
var
  sUnInstallString: String;
  iResultCode: Integer;
begin
  Result := 0;
  sUnInstallString := GetUninstallString();
  if sUnInstallString <> '' then begin
    // RemoveQuotes elimina las comillas que Inno Setup pone alrededor de la ruta
    sUnInstallString := RemoveQuotes(sUnInstallString);
    if Exec(sUnInstallString,
            '/SILENT /NORESTART /SUPPRESSMSGBOXES',
            '', SW_HIDE, ewWaitUntilTerminated, iResultCode) then
      Result := iResultCode;
  end;
end;

// ── Personaliza el mensaje de bienvenida segun si es instalacion nueva
//    o actualizacion de una version existente ─────────────────────────────
procedure InitializeWizard();
begin
  if IsUpgrade() then begin
    WizardForm.WelcomeLabel2.Caption :=
      'Se ha detectado una versión anterior de Gráficas Mulberry instalada en ' +
      'este equipo.' + #13#10 + #13#10 +
      'El asistente desinstalará automáticamente la versión anterior e ' +
      'instalará una copia limpia de la versión {#MyAppVersion}.' + #13#10 + #13#10 +
      'El modelo de IA (Ollama) y la base de datos existente no se verán ' +
      'afectados.' + #13#10 + #13#10 +
      'Haz clic en Siguiente para continuar con la actualización.';
  end;
end;

// ── Desinstala la version anterior justo antes de copiar los archivos nuevos
procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then
    if IsUpgrade() then
      UnInstallOldVersion();
end;
