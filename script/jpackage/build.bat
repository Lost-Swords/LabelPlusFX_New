@echo off
if "%1"=="" (
    set VERSION=0.0.0
) else (
    set VERSION=%1
)
echo VERSION: %1
set DIR=%~dp0..\..
rd /S /Q ".\LabelPlusFX"
set MODULES="%DIR%\target\build"
set ICON="%DIR%\images\icons\cat.ico"
set SCRIPT_DIR=%~dp0

jpackage --verbose --type app-image --app-version %VERSION% --copyright "Meodinger Tech (C) 2024" --name LabelPlusFX --icon %ICON% --dest %SCRIPT_DIR% --module-path %MODULES% --add-modules lpfx,jdk.crypto.cryptoki --module lpfx/ink.meodinger.lpfx.LauncherKt  --java-options "-Dprism.maxvram=2G"

if exist "%SCRIPT_DIR%\IMEInterface.dll" (
 copy "%SCRIPT_DIR%\IMEInterface.dll" "%SCRIPT_DIR%\LabelPlusFX
)
if exist "%SCRIPT_DIR%\IMEWrapper.dll" (
 copy "%SCRIPT_DIR%\IMEWrapper.dll" "%SCRIPT_DIR%\LabelPlusFX
)
if exist "%SCRIPT_DIR%\LabelPlusFXDict.lnk" (
 copy "%SCRIPT_DIR%\LabelPlusFXDict.lnk" "%SCRIPT_DIR%\LabelPlusFX
)
echo:
echo All completed, remember to copy dlls!

