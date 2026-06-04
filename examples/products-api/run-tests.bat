@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo Products API - Local Test Runner
echo ==========================================

cd /d "%~dp0"

echo.
echo ==========================================
echo Running Unit Tests...
echo ==========================================
call gradlew.bat unitTest --console=plain
if errorlevel 1 (
    echo Unit tests failed!
    exit /b 1
)

echo.
echo ==========================================
echo Running Integration Tests...
echo ==========================================
call gradlew.bat integrationTest --console=plain
if errorlevel 1 (
    echo Integration tests failed!
    exit /b 1
)

echo.
echo ==========================================
echo Running E2E Tests...
echo ==========================================
call gradlew.bat e2eTest --console=plain
if errorlevel 1 (
    echo E2E tests failed!
    exit /b 1
)

echo.
echo ==========================================
echo Generating Code Coverage Report...
echo ==========================================
call gradlew.bat jacocoTestReport --console=plain
if errorlevel 1 (
    echo Coverage report generation failed!
    exit /b 1
)

echo.
echo ==========================================
echo All tests passed!
echo ==========================================
echo.
echo Coverage report: build\reports\jacoco\test\html\index.html
echo.
