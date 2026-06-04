@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo Products API - Docker Deployment
echo ==========================================

cd /d "%~dp0"

echo.
echo Building Docker image...
docker build -t products-api:latest .
if errorlevel 1 (
    echo Docker build failed!
    exit /b 1
)

echo.
echo Stopping existing container (if any)...
docker stop products-api 2>nul
docker rm products-api 2>nul

echo.
echo Starting application...
docker run -d ^
    --name products-api ^
    -p 8080:8080 ^
    -e SPRING_PROFILES_ACTIVE=docker ^
    -e JAVA_OPTS="-Xmx512m -Xms256m" ^
    --restart unless-stopped ^
    products-api:latest
if errorlevel 1 (
    echo Failed to start container!
    exit /b 1
)

echo.
echo Waiting for application to start...
timeout /t 15 /nobreak >nul

echo.
echo Checking application health...
set /a "attempts=0"
:health_check
set /a "attempts+=1"
if %attempts% gtr 30 (
    echo Application failed to start after 30 attempts!
    exit /b 1
)
curl -sf http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo Waiting... (%attempts%/30)
    timeout /t 2 /nobreak >nul
    goto health_check
)
echo Application is healthy!

echo.
echo ==========================================
echo Application is running!
echo ==========================================
echo.
echo API: http://localhost:8080/api/products
echo Health: http://localhost:8080/actuator/health
echo.
echo To stop: docker stop products-api
echo To remove: docker rm products-api
echo.
