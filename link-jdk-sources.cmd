@echo off
rem # mvn clean
rem # link-jdk-sources
rem # mvn package -P apidoc javadoc:aggregate-jar -DskipTests
rem # mvn verify -P release -Dgpg.skip=true

setlocal
set apidocs_src=target\apidocs\src
set JDK_HOME=%JAVA_HOME%

for %%M in (. xbrz-core xbrz-awt xbrz-tool) do (
  mkdir %%M\%apidocs_src%
)

rem # src.zip needs to be extracted as "src" directory
for /d %%D in (%JDK_HOME%\lib\src\*) do (
  for %%M in (. xbrz-core xbrz-awt xbrz-tool) do (
    mklink /d %%M\%apidocs_src%\%%~nxD %%D
  )
)
