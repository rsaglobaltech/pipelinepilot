#!/bin/bash

set -e

echo "=========================================="
echo "Products API - Docker Deployment"
echo "=========================================="

cd "$(dirname "$0")"

echo ""
echo "Building Docker image..."
docker build -t products-api:latest .

echo ""
echo "Stopping existing container (if any)..."
docker stop products-api 2>/dev/null || true
docker rm products-api 2>/dev/null || true

echo ""
echo "Starting application..."
docker run -d \
    --name products-api \
    -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -e JAVA_OPTS="-Xmx512m -Xms256m" \
    --restart unless-stopped \
    --health-cmd="wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1" \
    --health-interval=30s \
    --health-timeout=3s \
    --health-start-period=30s \
    --health-retries=3 \
    products-api:latest

echo ""
echo "Waiting for application to start..."
sleep 10

echo ""
echo "Checking application health..."
for i in {1..30}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "Application is healthy!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

echo ""
echo "=========================================="
echo "Application is running!"
echo "=========================================="
echo ""
echo "API: http://localhost:8080/api/products"
echo "Health: http://localhost:8080/actuator/health"
echo ""
echo "To stop: docker stop products-api"
echo "To remove: docker rm products-api"
echo ""
