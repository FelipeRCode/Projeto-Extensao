@echo off
SET MAVEN_PROJECTBASEDIR=%~dp0
SET MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
SET MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

IF EXIST "%MAVEN_WRAPPER_JAR%" GOTO execute

echo Baixando Maven Wrapper...
FOR /F "tokens=2 delims==" %%G IN ('findstr /i "wrapperUrl" "%MAVEN_WRAPPER_PROPERTIES%"') DO SET DOWNLOAD_URL=%%G

powershell -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%MAVEN_WRAPPER_JAR%'"

IF ERRORLEVEL 1 (
  echo ERRO: Nao foi possivel baixar o maven-wrapper.jar
  echo Verifique sua conexao com a internet.
  exit /B 1
)

:execute
IF NOT "%JAVA_HOME%"=="" SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
IF "%JAVA_EXE%"=="" SET JAVA_EXE=java

"%JAVA_EXE%" -classpath "%MAVEN_WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
