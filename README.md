# ClawHub Java Edition

A Java implementation of ClawHub - A registry for AI skills, agent souls, and packages.

## Overview

This is a complete Java implementation of the ClawHub platform, featuring:
- **Backend**: Spring Boot with PostgreSQL, Redis, MinIO, Elasticsearch
- **Frontend**: React SPA (Vite) + Next.js SSR
- **CLI**: Java command-line tool with Picocli
- **Real-time**: WebSocket for live updates
- **Full-text Search**: Elasticsearch integration
- **Package Registry**: Complete Package/Plugin system with security scanning

## Project Structure

```
clawhub-java/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/clawhub/
│   │   ├── config/            # Configuration (Security, WebSocket, ES, Cache, Async)
│   │   ├── controller/        # REST API + WebSocket handlers
│   │   │   ├── PackageController.java      # Package/Release API
│   │   │   ├── PublisherController.java    # Publisher API
│   │   │   └── PublisherMemberController.java # Member API
│   │   ├── dto/               # Data Transfer Objects
│   │   │   ├── Package*.java               # Package DTOs
│   │   │   └── Publisher*.java             # Publisher DTOs
│   │   ├── entity/            # JPA Entities
│   │   │   ├── Package.java                # Package entity
│   │   │   ├── PackageRelease.java         # Release entity
│   │   │   ├── Publisher.java              # Publisher entity
│   │   │   └── PublisherMember.java        # Member entity
│   │   ├── repository/        # Data access layer
│   │   │   ├── PackageRepository.java
│   │   │   ├── PackageReleaseRepository.java
│   │   │   ├── PublisherRepository.java
│   │   │   └── PublisherMemberRepository.java
│   │   ├── service/           # Business logic
│   │   │   ├── Package*.java               # Package services
│   │   │   ├── Publisher*.java             # Publisher services
│   │   │   ├── CacheService.java           # Cache management
│   │   │   ├── BatchOperationService.java  # Batch operations
│   │   │   ├── PerformanceService.java     # Performance monitoring
│   │   │   └── PackageSecurityScanService.java # Security scanning
│   │   └── websocket/         # WebSocket handlers
│   └── pom.xml
├── frontend/                   # Vue 3 + Nuxt 3 SSR
│   ├── components/            # Vue components
│   ├── composables/           # Composable functions
│   ├── pages/                 # Page components
│   ├── stores/                # Pinia stores
│   └── nuxt.config.ts
├── cli/                        # Java CLI tool
│   └── src/main/java/clawhub/
│       ├── commands/
│       │   └── Packages*.java              # Package CLI commands
│       └── model/
│           ├── Package.java                # CLI Package model
│           └── PackageRelease.java         # CLI Release model
└── docker-compose.yml          # Infrastructure
```

## Quick Start

### Prerequisites

- Java 21
- Node.js 18+
- Docker & Docker Compose
- Maven 3.9+

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- MinIO (port 9000/9001)
- Elasticsearch (port 9200)

### 2. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs on http://localhost:8080

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:3000

### 4. Build & Run CLI

```bash
cd cli
mvn package
java -jar target/clawhub-cli-1.0.0.jar --help
```

## Features

### Core Features
- ✅ User authentication (GitHub OAuth + JWT)
- ✅ Skill/Soul CRUD operations
- ✅ **Package/Plugin Registry** - Complete package management
- ✅ **Publisher Organization** - Multi-user publishing with roles
- ✅ Semantic versioning for releases
- ✅ File storage with MinIO
- ✅ Comments and stars system
- ✅ Moderation tools (hide/remove/report)
- ✅ Badge system (highlighted, verified, trending, etc.)

### Package Registry Features
- ✅ **Package Management** - Create, publish, install packages
- ✅ **Version Control** - Semantic versioning with releases
- ✅ **Publisher System** - Organizations with OWNER/ADMIN/PUBLISHER roles
- ✅ **Security Scanning** - VirusTotal + LLM + Static analysis
- ✅ **Verification Badges** - 5-tier verification system
- ✅ **Channel Support** - Official, Community, Private channels
- ✅ **Family Types** - Skill, Code Plugin, Bundle Plugin

### Advanced Features
- ✅ **WebSocket Real-time Sync** - Live updates for changes
- ✅ **Elasticsearch Full-text Search** - Multi-field weighted search
- ✅ **Redis Caching** - Performance optimization
- ✅ **Batch Operations** - Bulk scan, verify, update
- ✅ **Performance Monitoring** - Metrics and analytics
- ✅ **SSR Support** - Next.js for SEO and performance
- ✅ **Dark Mode** - Theme switching support
- ✅ **CLI Tool** - Command-line interface for power users

## API Documentation

### Authentication
```
GET  /api/auth/whoami
POST /api/auth/logout
```

### Publishers
```
GET    /api/v1/publishers              # List publishers
GET    /api/v1/publishers/:handle      # Get publisher
POST   /api/v1/publishers              # Create publisher (auth)
PUT    /api/v1/publishers/:handle      # Update publisher (auth)
DELETE /api/v1/publishers/:handle      # Delete publisher (auth)
GET    /api/v1/publishers/:handle/members        # List members
POST   /api/v1/publishers/:handle/members        # Add member (auth)
DELETE /api/v1/publishers/:handle/members/:id    # Remove member (auth)
```

### Packages
```
GET    /api/v1/packages                # List packages
GET    /api/v1/packages/:name          # Get package
POST   /api/v1/packages                # Create package (auth)
PUT    /api/v1/packages/:name          # Update package (auth)
DELETE /api/v1/packages/:name          # Delete package (auth)
GET    /api/v1/packages/:name/versions         # List versions
GET    /api/v1/packages/:name/versions/:version # Get version
POST   /api/v1/packages/:name/versions         # Create version (auth)
DELETE /api/v1/packages/:name/versions/:version # Delete version (auth)
GET    /api/v1/packages/:name/versions/latest  # Get latest version
```

### Skills
```
GET    /api/skills              # List skills (paginated)
GET    /api/skills/:slug        # Get skill details
POST   /api/skills              # Create skill (auth)
PATCH  /api/skills/:slug        # Update skill (auth)
DELETE /api/skills/:slug        # Delete skill (auth)
GET    /api/skills/highlighted  # Get highlighted skills
```

### Versions
```
GET    /api/skills/:slug/versions
POST   /api/skills/:slug/versions      # Create version (auth)
POST   /api/skills/:slug/rollback      # Rollback version (auth)
```

### Social
```
GET    /api/skills/:slug/comments
POST   /api/skills/:slug/comments      # Add comment (auth)
POST   /api/skills/:slug/stars         # Star skill (auth)
DELETE /api/skills/:slug/stars         # Unstar skill (auth)
```

### Search
```
GET /api/search?q=&type=       # Search skills/souls/packages
```

### WebSocket
```
WS /ws/skills                  # Real-time updates
```

### Admin
```
GET    /api/admin/moderation/pending
POST   /api/admin/skills/:id/hide
POST   /api/admin/skills/:id/unhide
POST   /api/admin/skills/:id/remove
POST   /api/admin/badges/skills/:id/award
DELETE /api/admin/badges/skills/:id/remove
```

## CLI Commands

### Authentication
```bash
clawhub login                  # Login via GitHub OAuth
clawhub logout                 # Logout
clawhub whoami                 # Show current user
```

### Package Commands
```bash
# Browse packages
clawhub packages explore [query] [-f skill] [--official]

# View package details
clawhub packages inspect <name> [-v version] [--json]

# Publish package
clawhub packages publish <path> -f <family> -v <version> [-n name]

# Install package
clawhub packages install <name> [-v version] [-d directory]

# Uninstall package
clawhub packages uninstall <name>

# List installed packages
clawhub packages list [--json]
```

### Legacy Commands
```bash
# Discovery
clawhub search <query>         # Search skills
clawhub list                   # List installed skills

# Management
clawhub install <slug>         # Install a skill
clawhub publish <path>         # Publish a skill
clawhub sync                   # Check for updates
```

## Verification Badges

Packages can earn verification badges based on their trustworthiness:

| Badge | Tier | Description |
|-------|------|-------------|
| 🟢 | **rebuild-verified** | Build is reproducible from source |
| 🔵 | **provenance-verified** | Publisher identity verified |
| 🟡 | **source-linked** | Source code repository linked |
| ⚪ | **structural** | Package structure validated |
| 🔴 | **none** | No verification performed |

## Security Scanning

All packages undergo automated security scanning:

1. **Static Analysis** - Code pattern detection for:
   - Raw IP URLs
   - Shell command injection
   - Base64 encoded execution
   - Dynamic code execution
   - Environment variable access

2. **VirusTotal Scan** - Multi-engine malware detection

3. **LLM Security Analysis** - AI-powered security assessment

Scan results are displayed in the package details and CLI output.

## Architecture

### Backend Stack
- **Java 21** - Modern Java features
- **Spring Boot 3.2** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Data access
- **PostgreSQL** - Primary database
- **Redis** - Caching & sessions
- **MinIO** - Object storage
- **Elasticsearch** - Full-text search
- **WebSocket** - Real-time communication

### Frontend Stack
- **Vue 3** - Progressive framework
- **Nuxt 3** - Vue framework with SSR
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **Pinia** - State management
- **VueUse** - Composition utilities

### CLI Stack
- **Java 21** - Language
- **Picocli** - Command framework
- **OkHttp** - HTTP client
- **Jackson** - JSON processing

## Configuration

### Backend (`application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/clawhub
    username: clawhub
    password: clawhub
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  redis:
    host: localhost
    port: 6379
  
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin

elasticsearch:
  uris: http://localhost:9200

# Security scanning
virustotal:
  api-key: ${VT_API_KEY}

openai:
  api:
    key: ${OPENAI_API_KEY}
```

### Frontend SSR (`.env`)
```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Development

### Backend Development
```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

### Database Migrations
```bash
./mvnw flyway:migrate
```

### Frontend Development
```bash
cd frontend
npm install
npm run dev
```

### CLI Development
```bash
cd cli
mvn clean package
java -jar target/clawhub-cli-1.0.0.jar
```

## Testing

### Backend Tests
```bash
cd backend
./mvnw test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## Deployment

### Docker Build
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Manual Deployment
1. Build backend: `./mvnw clean package`
2. Build frontend: `npm run generate`
3. Deploy to server

## Performance Optimization

### Caching Strategy
- **Package details**: 5 minutes
- **Package lists**: 2 minutes
- **Publisher info**: 10 minutes
- **User sessions**: 30 minutes
- **Verification badges**: 1 hour

### Batch Operations
- Batch size: 100
- Async processing for security scans
- Connection pool: 20 max connections

### Monitoring
- Micrometer metrics
- Performance timers for API calls
- Cache hit/miss tracking
- Download statistics

## Comparison with Original

| Feature | Original (Convex) | Java Edition |
|---------|------------------|--------------|
| Architecture | Serverless | Spring Boot |
| Real-time | Auto-sync | WebSocket |
| Search | Vector Search | Elasticsearch |
| SSR | TanStack Start | Nuxt 3 |
| Storage | Convex Storage | MinIO |
| Auth | Convex Auth | Spring Security |
| **Package Registry** | ❌ | ✅ |
| **Publisher System** | ❌ | ✅ |
| **Security Scanning** | ❌ | ✅ |
| **Verification Badges** | ❌ | ✅ |
| **Redis Caching** | ❌ | ✅ |
| **Batch Operations** | ❌ | ✅ |

**Advantages of Java Edition:**
- Full control over infrastructure
- Better performance tuning
- Enterprise-grade ecosystem
- No vendor lock-in
- Flexible deployment options
- Complete package registry
- Multi-level security scanning
- Verification badge system

## Documentation

- `IMPLEMENTATION_PLAN.md` - Implementation phases
- `PHASE1_SUMMARY.md` - Infrastructure setup
- `PHASE2_SUMMARY.md` - Backend core
- `PHASE3_SUMMARY.md` - File/search/social
- `PHASE4_SUMMARY.md` - Moderation features
- `PHASE5_SUMMARY.md` - Frontend & CLI
- `COMPARISON_ANALYSIS.md` - Detailed comparison
- `ITERATION_SUMMARY.md` - WebSocket & ES additions
- `FINAL_SUMMARY.md` - Complete project summary

## License

MIT

## Contributing

Contributions welcome! Please read the contribution guidelines first.

## Acknowledgments

- Original ClawHub project
- Spring Boot team
- Open source community