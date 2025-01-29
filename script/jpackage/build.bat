@echo off
:: Check command line arguments and set version
if "%1"=="" (
    :: Set default version if no argument provided
    set VERSION=0.0.0
) else (
    :: Use provided version argument
    set VERSION=%1
)
echo BUILD VERSION: %1

:: Configure directory paths
:: %~dp0 = batch file's directory
set DIR=%~dp0..\..  :: Go up two directories from script location

:: Clean previous build output
rd /S /Q ".\LabelPlusFX"  :: Force delete existing directory (/S=recursive, /Q=quiet)

:: Set build parameters
set MODULES="%DIR%\target\build"  :: Path to Java module dependencies
set ICON="%DIR%\images\icons\cat.ico"  :: Application icon path
set SCRIPT_DIR=%~dp0  :: Store original script directory

:: Package application using jpackage
jpackage --verbose --type app-image --app-version %VERSION% --copyright "Meodinger Tech (C) 2024" --name LabelPlusFX --icon %ICON% --dest %SCRIPT_DIR% --module-path %MODULES% --add-modules lpfx,jdk.crypto.cryptoki --module lpfx/ink.meodinger.lpfx.LauncherKt  --java-options "-Dprism.maxvram=2G"

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
echo:
echo All completed!

