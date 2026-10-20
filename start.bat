@echo off
rem Portfolio Desk launcher: builds and starts quoteServer, portfolioService and the frontend,
rem then opens the browser. Double-click it. Closing this window stops everything.
setlocal
chcp 65001 >nul
title Portfolio Desk
cd /d "%~dp0"

echo ================================
echo          Portfolio Desk
echo ================================
echo.

where java >nul 2>&1 || (echo [ERREUR] Java 17+ introuvable. Installe un JDK : https://adoptium.net & pause & exit /b 1)
where npm >nul 2>&1 || (echo [ERREUR] Node.js introuvable. Installe-le : https://nodejs.org & pause & exit /b 1)

rem Clean up a previous run that was not stopped properly
call "%~dp0stop.bat" /quiet

set "LOGS=%TEMP%\portfolio-desk-logs"
if not exist "%LOGS%" mkdir "%LOGS%"

rem Same rule as build.gradle: under OneDrive, the build output lives in %LOCALAPPDATA%
set "QLIBS=%~dp0quoteServer\build\libs"
set "PLIBS=%~dp0portfolioService\build\libs"
echo %~dp0| findstr /i onedrive >nul
if not errorlevel 1 if defined LOCALAPPDATA (
    set "QLIBS=%LOCALAPPDATA%\finance-portfolio-build\quoteServer\libs"
    set "PLIBS=%LOCALAPPDATA%\finance-portfolio-build\portfolioService\libs"
)

echo [1/4] Compilation des services Java...
call "%~dp0gradlew.bat" :quoteServer:bootJar :portfolioService:bootJar -q
if errorlevel 1 (echo [ERREUR] La compilation a echoue. & pause & exit /b 1)
for /f "delims=" %%f in ('dir /b "%QLIBS%\*.jar" ^| findstr /v plain') do set "QJAR=%QLIBS%\%%f"
for /f "delims=" %%f in ('dir /b "%PLIBS%\*.jar" ^| findstr /v plain') do set "PJAR=%PLIBS%\%%f"

echo [2/4] Dependances du frontend...
if not exist "%~dp0portfolio-frontend\node_modules" (
    pushd "%~dp0portfolio-frontend"
    call npm install --no-audit --no-fund
    popd
)

rem Default ports, or the next free one if something else already uses it
call :freeport 9090 GRPC
call :freeport 8080 API
call :freeport 5173 WEB

echo [3/4] Demarrage des services...
echo       quoteServer (gRPC)      port %GRPC%
start "" /b cmd /c java -jar "%QJAR%" --grpc.server.port=%GRPC% ^> "%LOGS%\quoteServer.log" 2^>^&1
echo       portfolioService (REST) port %API%
start "" /b cmd /c java -jar "%PJAR%" --spring.profiles.active=dev --server.port=%API% --quote-service.port=%GRPC% ^> "%LOGS%\portfolioService.log" 2^>^&1
echo       frontend (Vue)          port %WEB%
set "API_PORT=%API%"
start "" /b /d "%~dp0portfolio-frontend" cmd /c npx vite --port %WEB% --strictPort ^> "%LOGS%\frontend.log" 2^>^&1

echo [4/4] Attente du demarrage (jusqu'a 2 min)...
set /a tries=0
:wait_api
set /a tries+=1
if %tries% gtr 60 (
    echo [ERREUR] portfolioService ne repond pas. Logs : %LOGS%
    goto shutdown
)
curl.exe -s http://localhost:%API%/actuator/health | findstr UP >nul || (ping -n 3 127.0.0.1 >nul & goto wait_api)
:wait_web
set /a tries+=1
if %tries% gtr 70 (
    echo [ERREUR] Le frontend ne repond pas. Logs : %LOGS%
    goto shutdown
)
curl.exe -s http://localhost:%WEB% | findstr /i "Portfolio" >nul || (ping -n 2 127.0.0.1 >nul & goto wait_web)

start "" http://localhost:%WEB%
echo.
echo ==================================================
echo   Application prete : http://localhost:%WEB%
echo   API REST          : http://localhost:%API%/api
echo   Logs              : %LOGS%
echo.
echo   Appuie sur une touche (ou ferme cette fenetre)
echo   pour tout arreter.
echo ==================================================
pause >nul

:shutdown
echo Arret des services...
call "%~dp0stop.bat" /quiet
exit /b 0

:freeport
set /a "p=%1"
:freeport_next
netstat -ano | findstr /r /c:":%p%  *[^ ]*  *LISTENING" >nul
if not errorlevel 1 (
    set /a p+=1
    goto freeport_next
)
set "%2=%p%"
exit /b 0
