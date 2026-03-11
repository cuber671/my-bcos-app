# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **FISCO BCOS Supply Chain Finance Platform** built with Spring Boot. It integrates with FISCO BCOS blockchain (version 3.12.1) to provide enterprise management, warehouse receipts, bill management, receivables, credit management, logistics, and financial services.

## Common Commands

### Build and Run
```bash
# Build the project
mvn clean package -DskipTests

# Run locally (requires MySQL and FISCO nodes running)
mvn spring-boot:run

# Run with Docker
docker compose up -d

# Rebuild and restart app container
docker compose build app && docker compose up -d app
```

### Testing
```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=EnterpriseServiceTest

# Run tests with verbose output
mvn test -X
```

### Database
```bash
# Database migrations are managed via Flyway
# Migration scripts: src/main/resources/db/migration/

# Check migration status (in container)
docker compose exec app java -jar app.jar flyway info
```

### Blockchain Operations
```bash
# Check FISCO node status (via RPC)
curl -X POST http://localhost:20000 -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"getConsensusStatus","params":[],"id":1}'

# Get block number
curl -X POST http://localhost:20000 -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"getBlockNumber","params":[],"id":1}'

# Access FISCO console
docker exec -it fisco-console bash -c "cd /data && ./start.sh group0"
```

## Architecture

### Technology Stack
- **Backend**: Spring Boot 2.7.18, Java 11
- **ORM**: MyBatis-Plus 3.5.3
- **Database**: MySQL 8.0 (via Docker)
- **Blockchain**: FISCO BCOS 3.12.1 (4 nodes)
- **Security**: Spring Security + JWT
- **Caching**: Caffeine

### Project Structure
```
src/main/java/com/fisco/app/
├── FiscoApplication.java          # Spring Boot entry point
├── Common/                         # Shared components
│   ├── Config/                    # SecurityConfig, etc.
│   ├── Utils/                     # Result, JwtUtil
│   └── Enums/                     # ResultCodeEnum
└── Modules/                        # Business modules
    ├── Enterprise/                # Enterprise management
    ├── User/                      # User/employee management
    ├── Warehouse/                 # Warehouse receipts
    ├── Bill/                      # Bill management
    ├── Finance/                   # Financial services
    ├── Logistics/                 # Logistics
    └── Blockchain/                # Blockchain interaction
```

### Docker Services (docker-compose.yml)
| Service | Description | Ports |
|---------|-------------|-------|
| fisco-node0-3 | FISCO BCOS blockchain nodes | 20000-20003, 30300-30303 |
| fisco-console | FISCO console for blockchain interaction | - |
| fisco-dev-db | MySQL database | 3306 |
| fisco-app | Spring Boot application | 8080 |

### Database Migrations
- Managed by **Flyway** (not JPA auto-create)
- Scripts location: `src/main/resources/db/migration/`
- Naming convention: `V{version}__{description}.sql`

### Blockchain Integration
- Uses **fisco-bcos-java-sdk** for blockchain interaction
- SDK config: `docker/config/config.toml`
- Nodes use static IPs (172.25.0.10-13) on Docker network
- P2P ports: 30000-30003 (internal)

## Key Files

- `pom.xml` - Maven dependencies and build config
- `docker-compose.yml` - Container orchestration
- `.env` - Environment variables (DB credentials, JWT secret, etc.)
- `src/main/resources/application.yml` - Spring Boot config (includes Flyway settings)
- `contracts/` - Solidity smart contracts
- `console/` - FISCO BCOS console for blockchain operations

## Important Notes

- JWT secret and encryption key must be set in `.env` before running
- Database is persisted via Docker volume (`mysql-data`)
- FISCO nodes require proper P2P configuration in `fisco/nodes/127.0.0.1/node*/nodes.json` for consensus
- Control plane network uses static subnet `172.25.0.0/16`
