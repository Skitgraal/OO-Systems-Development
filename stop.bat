@echo off
rem Stops every Portfolio Desk process started by start.bat (the two jars and the Vite dev server).
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe' or Name='node.exe'\" | Where-Object { $_.CommandLine -match 'finance-portfolio' -and $_.CommandLine -match 'quoteServer-|portfolioService-|vite' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"
if /i not "%~1"=="/quiet" echo Portfolio Desk arrete.
