@echo off
setlocal EnableExtensions
title CramLess
cd /d "%~dp0"

rem  CramLess launcher.
rem    start.bat        start the app
rem    start.bat test   run the logic tests
rem  If Java 17+ or Maven are missing, a private copy is downloaded into .tools (one time only).

set "TOOLS=%~dp0.tools"
set "MVN_VER=3.9.9"
set "JDK_URL=https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
set "MVN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VER%/apache-maven-%MVN_VER%-bin.zip"

rem ------------------------------------------------------------ Java 17 or newer
if exist "%TOOLS%\jdk\bin\java.exe" (
    set "JAVA_HOME=%TOOLS%\jdk"
    goto java_ok
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /r /c:"version \"1[7-9]" /c:"version \"2[0-9]" >nul && goto java_ok
)
set "JAVA_HOME="
where java >nul 2>nul || goto get_java
java -version 2>&1 | findstr /r /c:"version \"1[7-9]" /c:"version \"2[0-9]" >nul && goto java_ok

:get_java
echo Java 17 or newer was not found.
echo Downloading a private copy of Java 21 into .tools (about 190 MB, one time only)...
if not exist "%TOOLS%" mkdir "%TOOLS%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol='Tls12'; Invoke-WebRequest '%JDK_URL%' -OutFile '%TOOLS%\jdk.zip'"
if errorlevel 1 goto fail_java
if exist "%TOOLS%\jdk-tmp" rmdir /s /q "%TOOLS%\jdk-tmp"
mkdir "%TOOLS%\jdk-tmp"
tar -xf "%TOOLS%\jdk.zip" -C "%TOOLS%\jdk-tmp"
if errorlevel 1 goto fail_java
for /d %%D in ("%TOOLS%\jdk-tmp\*") do move "%%D" "%TOOLS%\jdk" >nul
rmdir /s /q "%TOOLS%\jdk-tmp"
del "%TOOLS%\jdk.zip"
if not exist "%TOOLS%\jdk\bin\java.exe" goto fail_java
set "JAVA_HOME=%TOOLS%\jdk"

:java_ok
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ------------------------------------------------------------ Maven
where mvn >nul 2>nul && (
    set "MVN=mvn"
    goto mvn_ok
)
set MVN="%TOOLS%\apache-maven-%MVN_VER%\bin\mvn.cmd"
if exist %MVN% goto mvn_ok
echo Maven was not found. Downloading Maven %MVN_VER% into .tools (about 10 MB, one time only)...
if not exist "%TOOLS%" mkdir "%TOOLS%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol='Tls12'; Invoke-WebRequest '%MVN_URL%' -OutFile '%TOOLS%\maven.zip'"
if errorlevel 1 goto fail_mvn
tar -xf "%TOOLS%\maven.zip" -C "%TOOLS%"
if errorlevel 1 goto fail_mvn
del "%TOOLS%\maven.zip"
if not exist %MVN% goto fail_mvn

:mvn_ok
if /i "%~1"=="test" (
    echo Running tests...
    call %MVN% -B -ntp test
    goto done
)

if exist "target\classes" (
    echo Starting CramLess...
    call %MVN% -q javafx:run
) else (
    echo Starting CramLess. The first start downloads the libraries and can take a few minutes...
    call %MVN% -B -ntp javafx:run
)

:done
if errorlevel 1 (
    echo.
    echo CramLess stopped with an error. The messages above say what went wrong.
    pause
    exit /b 1
)
if /i "%~1"=="test" pause
exit /b 0

:fail_java
echo.
echo Could not download Java. Check the internet connection, or install JDK 17+ from https://adoptium.net and run this again.
pause
exit /b 1

:fail_mvn
echo.
echo Could not download Maven. Check the internet connection, or install Maven from https://maven.apache.org and run this again.
pause
exit /b 1
