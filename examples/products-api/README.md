# Products API — Spring Boot Demo

A production-ready Spring Boot REST API for a product catalog, featuring comprehensive testing (unit, integration, E2E) with Testcontainers and Docker deployment.

## Endpoints

| Method | Path | Body | Result |
|--------|------|------|--------|
| GET | `/api/products` | — | list all |
| GET | `/api/products/{id}` | — | one, or 404 |
| POST | `/api/products` | `{name, price, quantity}` | 201 + created |
| PUT | `/api/products/{id}` | `{name, price, quantity}` | updated, or 404 |
| DELETE | `/api/products/{id}` | — | 204, or 404 |
| GET | `/actuator/health` | — | health check |

Storage is in-memory (`ConcurrentHashMap`) — no database required.

## Tech Stack

- **Java 17** with Spring Boot 3.3.5
- **Gradle 8.10.2** for build automation
- **JUnit 5** for testing
- **Testcontainers** for integration tests
- **REST Assured** for API testing
- **JaCoCo** for code coverage
- **Docker** for containerization

## Quick Start

### Prerequisites

- Java 17+
- Docker (for containerized deployment)
- Gradle (or use included wrapper)

### Build & Test Locally

```bash
cd examples/products-api

# Run all tests
./gradlew test

# Run specific test suites
./gradlew unitTest
./gradlew integrationTest
./gradlew e2eTest

# Build JAR
./gradlew bootJar

# Run locally
java -jar build/libs/products-api-1.0.0.jar
```

### Using Test Scripts

**Linux/Mac:**
```bash
chmod +x run-tests.sh
./run-tests.sh
```

**Windows:**
```cmd
run-tests.bat
```

### Docker Deployment

**Quick deploy:**
```bash
# Linux/Mac
chmod +x deploy-local.sh
./deploy-local.sh

# Windows
deploy-local.bat
```

**Manual Docker commands:**
```bash
# Build image
docker build -t products-api:latest .

# Run container
docker run -d \
    --name products-api \
    -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=docker \
    products-api:latest

# Check health
curl http://localhost:8080/actuator/health

# Stop container
docker stop products-api
docker rm products-api
```

**Using Docker Compose:**
```bash
docker-compose up -d
docker-compose down
```

## Testing Strategy

### Unit Tests (`unitTest`)
- Tests individual components in isolation
- No Spring context loaded
- Fast execution
- Files: `ProductServiceTest.java`, `ProductControllerTest.java`

### Integration Tests (`integrationTest`)
- Tests with Spring Boot context
- Uses Testcontainers for Redis
- Tests REST API with MockMvc
- File: `ProductIntegrationTest.java`

### End-to-End Tests (`e2eTest`)
- Full application lifecycle testing
- Tests complete CRUD operations
- Concurrent operations testing
- Health endpoint verification
- File: `ProductE2ETest.java`

## Code Coverage

Generate and view coverage report:

```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

Minimum coverage thresholds:
- Line coverage: 50%
- Branch coverage: 50%

## Jenkins Pipeline

The `Jenkinsfile` includes the following stages:

1. **Checkout** - Source code checkout
2. **Compile** - Java compilation
3. **Unit Tests** - Run unit tests with JUnit
4. **Integration Tests** - Run integration tests with Testcontainers
5. **E2E Tests** - Run end-to-end tests
6. **Code Coverage** - Generate and verify JaCoCo coverage
7. **Build JAR** - Create executable JAR
8. **Docker Build** - Build Docker image
9. **Docker Deploy** - Deploy container locally
10. **Health Check** - Verify application health
11. **Smoke Test** - Validate API endpoints

### Running Pipeline

```bash
# Copy to Jenkins container
docker cp examples/products-api pipelinepilot-jenkins:/var/jenkins_home/products-api

# Warm Gradle cache
docker cp ~/.gradle pipelinepilot-jenkins:/var/jenkins_home/.gradle
```

Then trigger the pipeline from Jenkins UI.

## API Usage Examples

### Create Product
```bash
curl -X POST http://localhost:8080/api/products \
    -H "Content-Type: application/json" \
    -d '{"name":"Laptop","price":999.99,"quantity":10}'
```

### List Products
```bash
curl http://localhost:8080/api/products
```

### Get Product
```bash
curl http://localhost:8080/api/products/1
```

### Update Product
```bash
curl -X PUT http://localhost:8080/api/products/1 \
    -H "Content-Type: application/json" \
    -d '{"name":"Gaming Laptop","price":1499.99,"quantity":5}'
```

### Delete Product
```bash
curl -X DELETE http://localhost:8080/api/products/1
```

## Project Structure

```
products-api/
├── src/
│   ├── main/
│   │   ├── java/com/example/products/
│   │   │   ├── ProductsApiApplication.java
│   │   │   ├── Product.java
│   │   │   ├── ProductController.java
│   │   │   └── ProductService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-docker.properties
│   └── test/
│       └── java/com/example/products/
│           ├── ProductServiceTest.java
│           ├── ProductControllerTest.java
│           ├── ProductIntegrationTest.java
│           └── ProductE2ETest.java
├── build.gradle
├── settings.gradle
├── Dockerfile
├── docker-compose.yml
├── Jenkinsfile
├── run-tests.sh
├── run-tests.bat
├── deploy-local.sh
└── deploy-local.bat
```

## License

This project is part of the PipelinePilot demo suite.
