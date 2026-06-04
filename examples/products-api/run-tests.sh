#!/bin/bash

set -e

echo "=========================================="
echo "Products API - Local Test Runner"
echo "=========================================="

cd "$(dirname "$0")"

chmod +x gradlew

echo ""
echo "=========================================="
echo "Running Unit Tests..."
echo "=========================================="
./gradlew unitTest --console=plain

echo ""
echo "=========================================="
echo "Running Integration Tests..."
echo "=========================================="
./gradlew integrationTest --console=plain

echo ""
echo "=========================================="
echo "Running E2E Tests..."
echo "=========================================="
./gradlew e2eTest --console=plain

echo ""
echo "=========================================="
echo "Generating Code Coverage Report..."
echo "=========================================="
./gradlew jacocoTestReport --console=plain

echo ""
echo "=========================================="
echo "All tests passed!"
echo "=========================================="
echo ""
echo "Coverage report: build/reports/jacoco/test/html/index.html"
echo ""
