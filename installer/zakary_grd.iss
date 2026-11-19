#define MyAppName "ZAKARY GRD"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "ZAKARY GRD"
#define BuildDir "..\build\windows\x64\runner\Release"

[Setup]
AppId={{8D12D991-9A8B-4D38-BEA1-1B3DAF3F4D90}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\ZAKARY GRD
DefaultGroupName=ZAKARY GRD
OutputDir=output
OutputBaseFilename=ZAKARY-GRD-Setup
Compression=lzma
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64
WizardStyle=modern
UninstallDisplayIcon={app}\zakary_grd.exe

[Files]
Source: "{#BuildDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\ZAKARY GRD"; Filename: "{app}\zakary_grd.exe"
Name: "{autodesktop}\ZAKARY GRD"; Filename: "{app}\zakary_grd.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"
