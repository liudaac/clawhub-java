# ClawHub Java Edition

A Java implementation of ClawHub - A registry for AI skills and agent souls.

## Overview

This is a complete Java implementation of the ClawHub platform, featuring:
- **Backend**: Spring Boot with PostgreSQL, Redis, MinIO, Elasticsearch
- **Frontend**: React SPA (Vite) + Next.js SSR
- **CLI**: Java command-line tool with Picocli
- **Real-time**: WebSocket for live updates
- **Full-text Search**: Elasticsearch integration

## Project Structure

```
clawhub-java/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/clawhub/
│   │   ├── config/            # Configuration (Security, WebSocket, ES)
│   │   ├── controller/        # REST API + WebSocket handlers
│   │   ├── dto/               # Data Transfer Objects
│   │   ├── entity/            # JPA Entities
│   │   ├── exception/         # Exception handling
│   │   ├── repository/        # Data access layer
│   │   ├── security/          # JWT + OAuth2
│   │   ├── service/           # Business logic
│   │   └── websocket/         # WebSocket handlers
│   └── pom.xml
├── frontend/                   # Vue 3 + Nuxt 3 SSR
│   ├── components/            # Vue components
│   ├── composables/           # Composable functions
│   ├── pages/                 # Page components
│   ├── stores/                # Pinia stores
│   └── nuxt.config.ts
├── cli/                        # Java CLI tool
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

### 5. Build & Run CLI

```bash
cd cli
mvn package
java -jar target/clawhub-cli-1.0.0.jar --help
```

## Features

### Core Features
- ✅ User authentication (GitHub OAuth + JWT)
- ✅ Skill/Soul CRUD operations
- ✅ Semantic versioning for releases
- ✅ File storage with MinIO
- ✅ Comments and stars system
- ✅ Moderation tools (hide/remove/report)
- ✅ Badge system (highlighted, verified, trending, etc.)

### Advanced Features
- ✅ **WebSocket Real-time Sync** - Live updates for skill changes
- ✅ **Elasticsearch Full-text Search** - Multi-field weighted search
- ✅ **SSR Support** - Next.js for SEO and performance
- ✅ **Dark Mode** - Theme switching support
- ✅ **CLI Tool** - Command-line interface for power users

## API Documentation

### Authentication
```
GET  /api/auth/whoami
POST /api/auth/logout
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
GET /api/search?q=&type=       # Search skills/souls
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

```bash
# Authentication
clawhub login                  # Login via GitHub OAuth
clawhub logout                 # Logout
clawhub whoami                 # Show current user

# Discovery
clawhub search <query>         # Search skills
clawhub list                   # List installed skills

# Management
clawhub install <slug>         # Install a skill
clawhub publish <path>         # Publish a skill
clawhub sync                   # Check for updates
```

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

## Comparison with Original

| Feature | Original (Convex) | Java Edition |
|---------|------------------|--------------|
| Architecture | Serverless | Spring Boot |
| Real-time | Auto-sync | WebSocket |
| Search | Vector Search | Elasticsearch |
| SSR | TanStack Start | Nuxt 3 |
| Storage | Convex Storage | MinIO |
| Auth | Convex Auth | Spring Security |

**Advantages of Java Edition:**
- Full control over infrastructure
- Better performance tuning
- Enterprise-grade ecosystem
- No vendor lock-in
- Flexible deployment options

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