@echo off
rem Compila la APK de "Reglamento Copa CD" SIN internet (igual que generador-licencias).
rem ANTES: ejecutar `cordova prepare android` (copia www/, PDF e iconos al proyecto de Android).
rem Salida: platforms\android\app\build\outputs\apk\debug\app-debug.apk  (log en build-reglamento.log)
setlocal

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set "KIT_GRADLE_HOME=%~dp0..\mitienda-pos\build-offline\gradle-home"
if exist "%KIT_GRADLE_HOME%\caches\modules-2\files-2.1\com.android.tools.build\gradle\8.0.0" (
    set "GRADLE_USER_HOME=%KIT_GRADLE_HOME%"
) else (
    set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
)
cd /d "%~dp0"
pushd "%~dp0platforms\android"

call gradlew.bat --offline --console=plain cdvBuildDebug > "%~dp0build-reglamento.log" 2>&1
set "RESULT=%ERRORLEVEL%"
popd

endlocal & exit /b %RESULT%