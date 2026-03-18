# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**FISCO BCOS Supply Chain Finance Platform** - A Spring Boot application integrating with FISCO BCOS blockchain (v3.12.1) to provide enterprise management, warehouse receipts, bill management, receivables, credit management, logistics, and financial services.

## Common Commands

### Build and Run

```bash
# Build the project (skip tests during build)
mvn clean package -DskipTests

# Run locally (requires MySQL and FISCO nodes running)
mvn spring-boot:run

# Run with Docker (full stack: 4 nodes + MySQL + app)
docker compose up -d

# Rebuild and restart app container only
docker compose build app && docker compose up -d app

#Rebuild all container
docker compose down && docker compose up -d --build

# View app logs
docker compose logs -f app
```

### Testing

```bash
# Run all tests
mvn test

# Run single test class
mvn test -Dtest=EnterpriseServiceTest

# Run tests with verbose output
mvn test -X
```

### Database Migrations

```bash
# Check Flyway migration status (in container)
docker compose exec app java -jar app.jar flyway info

# Repair migration (if needed)
docker compose exec app java -jar app.jar flyway repair
```

### Blockchain Operations

```bash
# Check FISCO node consensus status (RPC port 20000)
curl -X POST http://localhost:20000 -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"getConsensusStatus","params":[],"id":1}'

# Get current block number
curl -X POST http://localhost:20000 -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"getBlockNumber","params":[],"id":1}'

# Access FISCO console (interactive)
docker exec -it fisco-console bash -c "cd /data && ./start.sh group0"
```

## Architecture

### Technology Stack

| Component      | Version |
| -------------- | ------- |
| Spring Boot    | 2.7.18  |
| Java           | 11      |
| MyBatis-Plus   | 3.5.3.1 |
| MySQL          | 8.0     |
| FISCO BCOS     | 3.12.1  |
| FISCO Java SDK | 3.8.0   |

### Business Modules (10 total)

```
src/main/java/com/fisco/app/Modules/
├── Auth/           # Authentication & authorization
├── Blockchain/     # Blockchain interaction layer
├── Credit/         # Credit management & scoring
├── Enterprise/     # Enterprise management (27+ APIs)
├── Finance/        # Financial services & receivables
├── Logistics/      # Logistics delegation (DPDO)
├── User/           # Employee/user management
├── Warehouse/      # Warehouse receipts (digital assets)
├── Bill/           # Bill management
└── Test/           # Health & test endpoints
```

### Module Structure Pattern

Each business module follows:

```
ModuleName/
├── Controller/     # REST endpoints
├── Service/       # Business logic (impl/ for implementations)
├── Entity/        # Database entities
├── Mapper/        # MyBatis-Plus mappers
├── DTO/           # Request/Response objects
└── Config/        # Module-specific configuration
```

### REST API Conventions

- **Base path**: `/api/v1/*`
- **Response format**: `{code: int, msg: string, data: object|null}`
- **Authentication**: JWT token in `Authorization: Bearer <token>` header

### Docker Services

| Service       | Description                      | Ports                                |
| ------------- | -------------------------------- | ------------------------------------ |
| fisco-node0-3 | FISCO BCOS blockchain nodes      | 20000-20003 (RPC), 30300-30303 (P2P) |
| fisco-console | FISCO console for blockchain ops | -                                    |
| fisco-dev-db  | MySQL 8.0 database               | 3306                                 |
| fisco-app     | Spring Boot application          | 8080                                 |

### Database

- **Flyway** manages migrations (not JPA auto-create)
- Location: `src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- Volume: `mysql-data` persists database

### Blockchain Integration

- **fisco-bcos-java-sdk** connects to 4 nodes
- SDK config: `docker/config/config.toml` or `/app/sdk/config.toml`
- Node IPs: 172.25.0.10-13 (Docker network)
- Contract addresses loaded from environment variables

## Key Files

| File                                   | Purpose                                         |
| -------------------------------------- | ----------------------------------------------- |
| `pom.xml`                            | Maven dependencies                              |
| `docker-compose.yml`                 | Full stack orchestration                        |
| `.env`                               | Environment variables (required before running) |
| `src/main/resources/application.yml` | Spring Boot config                              |
| `contracts/`                         | Solidity smart contracts                        |
| `console/`                           | FISCO BCOS console                              |
| `docs/*.md`                          | API documentation                               |

## Required Environment Variables (.env)

Before running, set these in `.env`:

```
DB_NAME=fisco_data
DB_USERNAME=fisco_user
DB_PASSWORD=123456
DB_ROOT_PASSWORD=root_pwd
JWT_SECRET=<your-secret-key>
ENCRYPTION_KEY=<your-encryption-key>
SERVER_PORT=8080
FISCO_ENABLED=true
```

## Important Notes

- JWT secret and encryption key **must** be set in `.env` before running
- FISCO nodes require P2P configuration in `fisco/nodes/127.0.0.1/node*/nodes.json` for consensus
- Test documentation: see `测试计划.md` for 113 API testing scope
- API documentation: see `docs/` directory for module-specific APIs
- Testing standards: see `测试流程标准.md` for test engineer workflow
- Program repair: see `程序修复规范.md` for bugfix guidelines

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Testing Standards

### Functional Testing (curl)

- Use `curl` for interface functional testing
- Before testing, read the actual source code to confirm the interface path, request method, parameter names, and parameter types are all correct
- Must validate the following scenarios:
  - Normal requests: valid parameters, expected data returned correctly
  - Abnormal requests: missing required parameters, wrong parameter types, unauthorized access, and other edge cases
- After each `curl` command, record the actual response and compare it against the expected result
- Authenticated interfaces must include the correct token/cookie — bypassing authentication is not allowed

### Unit Testing (JUnit 5 / Jupiter)

- Use JUnit 5 (Jupiter) for unit testing
- Use standard annotations such as `@Test`, `@BeforeEach`, `@AfterEach` to organize tests
- Use `@ParameterizedTest` for multiple input scenarios to avoid duplicated test code
- Test method names must clearly express intent, e.g. `getUserById_shouldReturnUser_whenExists`
- Each test must validate only one behavior — do not stack multiple assertion targets in a single test
- Use `assertThrows` to validate exception scenarios
- Do not depend on external services (database, third-party APIs) — use Mocks to isolate dependencies

### Test Code Structure

- All test code is stored under `src/test/java/com/fisco/app/Modules/`
- Organized by module, each module has its own subdirectory, e.g.:
  - `src/test/java/com/fisco/app/Modules/User/`
  - `src/test/java/com/fisco/app/Modules/Enterprise/`
- When adding tests for a new module, create the corresponding directory under `Modules/` — do not mix tests across directories
- Test class names must mirror the class under test, e.g. `UserService` → `UserServiceTest`

### Testing Boundaries

- If any unexpected issue is found, immediately stop all subsequent tests, report the problem, and wait for manual confirmation before proceeding
- Absolutely forbidden to modify any code, configuration, or database data during testing
- Forbidden to skip assertions or alter expected values to force tests to pass
- Test environment must be strictly isolated from production — no testing against production is allowed
- Tests are for verification only and must not produce any non-rollbackable side effects

### Testing Workflow

1. Read the code and understand the design intent of the interface or method
2. Define test cases covering both normal and abnormal scenarios
3. Execute tests one by one and record results
4. Stop immediately upon any anomaly and output a clear problem description with reproduction steps

### Test Execution Order
- Tests must be executed strictly in business process order, not in arbitrary or alphabetical order
- Use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` on the test class and `@Order(n)` on each test method to enforce execution sequence
- The order must reflect the actual business flow, for example:
  - User module: Register → Login → Query → Update → Delete
  - Enterprise module: Create Enterprise → Associate User → Query → Update → Dissolve
- Tests that depend on the result of a previous step must not be executed independently out of order
- If a preceding step fails, all subsequent dependent steps must be skipped immediately — do not continue with an invalid state

### Test Data Preparation
- Before executing any test, identify and prepare all required prerequisite data
- Use `@BeforeAll` to prepare data that is shared across the entire test class (e.g. database records, test accounts)
- Use `@BeforeEach` to prepare data that needs to be reset before each individual test
- Prerequisite data must be explicit and documented — never rely on existing data in the environment that may be inconsistent or missing
- If a test requires data produced by a previous step (e.g. a created user ID, a generated token), that data must be stored and passed explicitly between tests
- After all tests complete, use `@AfterAll` to clean up any data created during the test run, ensuring the environment is restored to its original state
- Absolutely forbidden to use hardcoded IDs or assume any pre-existing state in the database