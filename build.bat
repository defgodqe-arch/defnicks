@echo off
echo Building KrystalDisplayName...
mvn clean package
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD FAILED.
    pause
    exit /b %ERRORLEVEL%
)
echo.
echo BUILD COMPLETE.
echo JAR: target\krystal-display-name-1.0.0.jar
pause
