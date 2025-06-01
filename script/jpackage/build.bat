@echo off
if "%1"=="" (
    set /p VERSION=Please enter the version number:
) else (
    set VERSION=%1
)
echo VERSION: %VERSION%
set DIR=%~dp0..\..
rd /S /Q ".\LabelPlusFX"
set MODULES="%DIR%\target\build"
set ICON="%DIR%\images\icons\cat.ico"
set SCRIPT_DIR=%~dp0

jpackage --verbose --type app-image --app-version %VERSION% --copyright "Meodinger Tech (C) 2025" --name LabelPlusFX --icon %ICON% --dest %SCRIPT_DIR% --module-path %MODULES% --add-modules lpfx,jdk.crypto.cryptoki --module lpfx/ink.meodinger.lpfx.LauncherKt

setlocal enabledelayedexpansion

:: Target directory handling
set "TARGET_DIR=%SCRIPT_DIR%\LabelPlusFX"
if not exist "%TARGET_DIR%\" mkdir "%TARGET_DIR%"

:: File list to process
set FILE_LIST=IMEInterface.dll IMEWrapper.dll LabelPlusFXDict.lnk

:: Process files in loop
for %%F in (%FILE_LIST%) do (
    if exist "%SCRIPT_DIR%\%%F" (
        copy /Y "%SCRIPT_DIR%\%%F" "%TARGET_DIR%\" >nul
        echo [Success] Copied: %%F
    ) else (
        echo [Warning] Missing: %%F (skipped)
    )
)

endlocal

echo:
echo All completed
