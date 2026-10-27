@echo off
setlocal

cd /d "%~dp0.."

set TIPO=app-image
if /i "%~1"=="exe" set TIPO=exe

where jpackage >nul 2>nul
if errorlevel 1 (
  if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
)
where jpackage >nul 2>nul
if errorlevel 1 (
  echo.
  echo No se encontro jpackage. Instala el JDK 17 o define JAVA_HOME apuntando a el.
  goto error
)

echo.
echo === 1/3 Compilando RentaPro ===
call mvnw.cmd -q clean package -DskipTests
if errorlevel 1 goto error

echo.
echo === 2/3 Preparando archivos ===
if exist instalador\entrada rmdir /s /q instalador\entrada
mkdir instalador\entrada
copy /y target\alquiler-vehiculos-1.0.0.jar instalador\entrada\ >nul
if errorlevel 1 goto error
if exist instalador\salida rmdir /s /q instalador\salida

echo.
echo === 3/3 Creando %TIPO% con jpackage ===
set MODULOS=java.base,java.desktop,java.logging,java.management,java.naming,java.sql,java.xml,java.instrument,java.prefs,java.security.jgss,java.security.sasl,java.net.http,java.scripting,java.compiler,jdk.unsupported,jdk.crypto.ec,jdk.xml.dom

if /i "%TIPO%"=="exe" (
  jpackage --type exe --name RentaPro --app-version 1.0.0 --vendor "RentaPro" --description "Alquiler de vehiculos y motos" --input instalador\entrada --main-jar alquiler-vehiculos-1.0.0.jar --icon instalador\icono.ico --dest instalador\salida --add-modules %MODULOS% --java-options "-Dfile.encoding=UTF-8" --win-shortcut --win-menu --win-dir-chooser
) else (
  jpackage --type app-image --name RentaPro --app-version 1.0.0 --vendor "RentaPro" --input instalador\entrada --main-jar alquiler-vehiculos-1.0.0.jar --icon instalador\icono.ico --dest instalador\salida --add-modules %MODULOS% --java-options "-Dfile.encoding=UTF-8"
)
if errorlevel 1 goto error

echo.
echo ==================================================================
echo  Listo. El resultado esta en la carpeta:  instalador\salida
if /i "%TIPO%"=="app-image" echo  Abre RentaPro con:  instalador\salida\RentaPro\RentaPro.exe
echo ==================================================================
pause
exit /b 0

:error
echo.
echo *** No se pudo crear el instalador. Revisa el mensaje de arriba. ***
if /i "%TIPO%"=="exe" echo Para el modo "exe" necesitas instalar WiX Toolset 3: https://github.com/wixtoolset/wix3/releases
pause
exit /b 1
