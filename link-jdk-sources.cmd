@echo off
rem # mvn clean
rem # link-jdk-sources
rem # mvn package -P apidoc javadoc:aggregate-jar -DskipTests
rem # mvn verify -P release -Dgpg.skip=true

setlocal
set BASE_DIR=%~dp0
if "%BASE_DIR%" == "%CD%\" set BASE_DIR=

set apidocs_src=target\apidocs\src

if "%JDK_HOME%" == "" set JDK_HOME=%JAVA_HOME%
if "%JDK_HOME%" == "" (
  echo Set JDK_HOME or JAVA_HOME to the JDK home directory
  exit /b 2
)

if not exist "%JDK_HOME%\lib\src\" (
  if exist "%JDK_HOME%\lib\src.zip" (
    echo Unzip "%JDK_HOME%\lib\src.zip" as "%JDK_HOME%\lib\src" directory
    exit /b 3
  )
  echo JDK sources not found: %JDK_HOME%\lib\src\
  exit /b 3
)

for %%M in (. xbrz-core xbrz-awt xbrz-tool) do (
  mkdir %BASE_DIR%%%M\%apidocs_src%
)

for /d %%D in (%JDK_HOME%\lib\src\*) do (
  for %%M in (. xbrz-core xbrz-awt xbrz-tool) do (
    mklink /d "%BASE_DIR%%%M\%apidocs_src%\%%~nxD" %%D
  )
)
