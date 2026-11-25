; Inno Setup script for LabelPlusFX (Windows x64)
;
; Prerequisite: run build.bat <version> first, which produces the app-image in
;               .\LabelPlusFX (jpackage --type app-image).
; Build:        ISCC.exe "inno installer script.iss"
; Output:       .\Output\LabelPlusFX-setup-<version>-Win64.exe

#define MyAppName "LabelPlusFx"
#define MyAppVersion "2.4.1"
#define MyAppPublisher "MoeDinger,Yeding"
#define MyAppURL "https://github.com/Lost-Swords/LabelPlusFX"
#define MyAppExeName "LabelPlusFX.exe"
#define MyAppAssocName MyAppName + " File"
#define MyAppAssocExt ".myp"
#define MyAppAssocKey StringChange(MyAppAssocName, " ", "") + MyAppAssocExt

; jpackage app-image directory (relative to this script)
#define AppImageDir "LabelPlusFX"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the
; same AppId value in installers for other applications.
AppId={{0962D110-5B79-4ACC-8FA5-F7F36248CE35}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
UninstallDisplayName={#MyAppName} {#MyAppVersion}
; Per-user installation into %APPDATA%\LabelPlusFx (no elevation required).
DefaultDirName={userappdata}\{#MyAppName}
PrivilegesRequired=lowest
ChangesAssociations=yes
DisableProgramGroupPage=yes
; "ArchitecturesAllowed=x64compatible" restricts Setup to x64 (and Windows 11 on Arm).
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
LicenseFile=..\..\LICENSE
OutputDir=Output
OutputBaseFilename=LabelPlusFX-setup-{#MyAppVersion}-Win64
SetupIconFile=..\..\images\icons\cat.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
VersionInfoVersion={#MyAppVersion}.0
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription={#MyAppName} Setup
VersionInfoProductName={#MyAppName}
VersionInfoProductVersion={#MyAppVersion}
VersionInfoCopyright=Meodinger Tech (C) 2025

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "chinesesimplified"; MessagesFile: "ChineseSimplified.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; The whole jpackage app-image (launcher, .cfg, bundled 117 MB runtime, IME DLLs).
Source: "{#AppImageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; Ship the AGPL license text with the installation.
Source: "..\..\LICENSE"; DestDir: "{app}"; DestName: "LICENSE.txt"; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Registry]
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocExt}\OpenWithProgids"; ValueType: string; ValueName: "{#MyAppAssocKey}"; ValueData: ""; Flags: uninsdeletevalue
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}"; ValueType: string; ValueName: ""; ValueData: "{#MyAppAssocName}"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\{#MyAppExeName},0"
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""
Root: HKA; Subkey: "Software\Classes\Applications\{#MyAppExeName}\SupportedTypes"; ValueType: string; ValueName: ".myp"; ValueData: ""

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
