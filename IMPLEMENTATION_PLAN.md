# ClawHub Java 实现计划

## 项目概述

将 TypeScript/Convex 版的 ClawHub 完整迁移到 Java/Spring Boot 生态，还原度目标 100%。

## 技术栈映射

| 原版 (ClawHub) | Java 版 (ClawHub Java) | 说明 |
|----------------|------------------------|------|
| Convex | Spring Boot + PostgreSQL | 无服务器 → 传统后端 |
| Convex Auth | Spring Security + OAuth2 | GitHub OAuth 登录 |
| Convex Storage | MinIO | 对象存储 |
| Convex Vector Search | Elasticsearch | 向量 + 全文搜索 |
| TanStack Start | React + React Router | 路由框架 |
| TanStack Query | React Query | 数据获取 |
| Convex React | Axios + WebSocket | API 客户端 |

## 阶段规划

### Phase 1: 基础设施 (Week 1)

#### 1.1 数据库设计
- [ ] 创建 PostgreSQL Schema
- [ ] 设计表结构 (users, skills, souls, versions, comments, stars)
- [ ] 创建 Flyway 迁移脚本
- [ ] 设计索引策略

#### 1.2 项目骨架
- [ ] Spring Boot 项目初始化
- [ ] 配置多模块结构 (backend, frontend, cli)
- [ ] Docker Compose 配置
- [ ] CI/CD 流水线

### Phase 2: 核心后端 (Week 2-3)

#### 2.1 用户系统
- [ ] GitHub OAuth 登录
- [ ] JWT Token 管理
- [ ] 用户角色 (admin, moderator, user)
- [ ] 用户 CRUD API

#### 2.2 Skill/Soul 管理
- [ ] Skill 实体和 Repository
- [ ] Soul 实体和 Repository
- [ ] CRUD REST API
- [ ] 权限控制

#### 2.3 版本管理
- [ ] 版本实体设计
- [ ] 语义化版本解析
- [ ] 版本发布 API
- [ ] 版本回滚

### Phase 3: 文件与搜索 (Week 4)

#### 3.1 文件存储
- [ ] MinIO 集成
- [ ] 文件上传 API
- [ ] 文件下载 API
- [ ] 文件校验 (SHA256)

#### 3.2 搜索系统
- [ ] Elasticsearch 集成
- [ ] 全文搜索实现
- [ ] 向量搜索 (pgvector 或 ES)
- [ ] 搜索 API

### Phase 4: 社交功能 (Week 5)

#### 4.1 评论系统
- [ ] 评论实体
- [ ] 评论 CRUD API
- [ ] 嵌套评论支持

#### 4.2 星级系统
- [ ] 星级实体
- [ ] 星级统计
- [ ] 用户星级记录

#### 4.3 统计功能
- [ ] 下载统计
- [ ] 热门排序
- [ ] 实时统计更新

### Phase 5: 管理功能 (Week 6)

#### 5.1 审核系统
- [ ] 内容审核状态
- [ ] 审核 API
- [ ] 自动审核 (VT + LLM)

#### 5.2 徽章系统
- [ ] 徽章实体
- [ ] 徽章管理 API
- [ ] 徽章展示

### Phase 6: 前端开发 (Week 7-8)

#### 6.1 基础架构
- [ ] React + Vite 项目
- [ ] 路由配置
- [ ] 状态管理 (Zustand)
- [ ] API 客户端

#### 6.2 页面实现
- [ ] 首页 (Skills/Souls 双模式)
- [ ] 列表页
- [ ] 详情页
- [ ] 上传页
- [ ] 搜索页
- [ ] 用户页

#### 6.3 组件库
- [ ] SkillCard
- [ ] SoulCard
- [ ] CommentPanel
- [ ] UserBadge
- [ ] Header/Footer

### Phase 7: CLI 工具 (Week 9)

#### 7.1 CLI 架构
- [ ] Picocli 框架
- [ ] 命令设计
- [ ] 配置管理

#### 7.2 命令实现
- [ ] login/logout/whoami
- [ ] search/explore
- [ ] install/uninstall/list
- [ ] publish/sync

### Phase 8: 优化与测试 (Week 10)

#### 8.1 性能优化
- [ ] 缓存策略 (Redis)
- [ ] 数据库优化
- [ ] API 响应优化

#### 8.2 测试覆盖
- [ ] 单元测试
- [ ] 集成测试
- [ ] E2E 测试

#### 8.3 文档完善
- [ ] API 文档 (OpenAPI)
- [ ] 部署文档
- [ ] 用户手册

## 数据库 Schema

### 核心表

```sql
-- 用户表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_id BIGINT UNIQUE NOT NULL,
    handle VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    bio TEXT,
    avatar_url VARCHAR(1024),
    role VARCHAR(20) DEFAULT 'user', -- admin, moderator, user
    github_created_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Skill 表
CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    summary TEXT,
    latest_version_id UUID,
    resource_id VARCHAR(255),
    canonical_skill_id UUID REFERENCES skills(id),
    fork_of JSONB,
    badges JSONB DEFAULT '{}',
    moderation_status VARCHAR(20) DEFAULT 'active', -- active, hidden, removed
    moderation_flags JSONB DEFAULT '[]',
    moderation_verdict VARCHAR(50),
    moderation_notes TEXT,
    moderation_reason TEXT,
    hidden_at TIMESTAMP,
    hidden_by UUID REFERENCES users(id),
    last_reviewed_at TIMESTAMP,
    report_count INT DEFAULT 0,
    stats_downloads BIGINT DEFAULT 0,
    stats_stars INT DEFAULT 0,
    stats_versions INT DEFAULT 0,
    stats_comments INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Skill 版本表
CREATE TABLE skill_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID NOT NULL REFERENCES skills(id),
    version VARCHAR(50) NOT NULL,
    tag VARCHAR(50),
    changelog TEXT NOT NULL,
    files JSONB NOT NULL DEFAULT '[]',
    parsed JSONB,
    embedding_id VARCHAR(255),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    soft_deleted_at TIMESTAMP,
    UNIQUE(skill_id, version)
);

-- Soul 表 (简化版)
CREATE TABLE souls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    summary TEXT,
    latest_version_id UUID,
    tags JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'active',
    stats_downloads BIGINT DEFAULT 0,
    stats_stars INT DEFAULT 0,
    stats_versions INT DEFAULT 0,
    stats_comments INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Soul 版本表
CREATE TABLE soul_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    soul_id UUID NOT NULL REFERENCES souls(id),
    version VARCHAR(50) NOT NULL,
    tag VARCHAR(50),
    changelog TEXT NOT NULL,
    files JSONB NOT NULL DEFAULT '[]',
    parsed JSONB,
    embedding_id VARCHAR(255),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    soft_deleted_at TIMESTAMP,
    UNIQUE(soul_id, version)
);

-- 评论表
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID REFERENCES skills(id),
    soul_id UUID REFERENCES souls(id),
    user_id UUID NOT NULL REFERENCES users(id),
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CHECK (skill_id IS NOT NULL OR soul_id IS NOT NULL)
);

-- 星级表
CREATE TABLE stars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID REFERENCES skills(id),
    soul_id UUID REFERENCES souls(id),
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(skill_id, user_id),
    UNIQUE(soul_id, user_id),
    CHECK (skill_id IS NOT NULL OR soul_id IS NOT NULL)
);

-- 搜索索引表
CREATE TABLE skill_search_digests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID NOT NULL REFERENCES skills(id),
    embedding_id VARCHAR(255),
    search_text TEXT,
    visibility VARCHAR(20) DEFAULT 'public',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 审计日志表
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## API 设计

### REST API 端点

```
# 认证
POST   /api/auth/github/callback
POST   /api/auth/logout
GET    /api/auth/whoami

# Skills
GET    /api/skills
GET    /api/skills/{slug}
POST   /api/skills
PATCH  /api/skills/{slug}
DELETE /api/skills/{slug}

# Skill Versions
GET    /api/skills/{slug}/versions
GET    /api/skills/{slug}/versions/{version}
POST   /api/skills/{slug}/versions
POST   /api/skills/{slug}/rollback

# Souls
GET    /api/souls
GET    /api/souls/{slug}
POST   /api/souls
PATCH  /api/souls/{slug}
DELETE /api/souls/{slug}

# Search
GET    /api/search?q={query}

# Comments
GET    /api/skills/{slug}/comments
POST   /api/skills/{slug}/comments
DELETE /api/comments/{id}

# Stars
POST   /api/skills/{slug}/stars
DELETE /api/skills/{slug}/stars
```

## 双模式实现

### 前端模式检测

```typescript
// src/lib/site.ts
export type SiteMode = 'skills' | 'souls';

export function detectSiteMode(host: string): SiteMode {
  const onlyCrabsHost = 'onlycrabs.ai';
  if (host.toLowerCase().includes(onlyCrabsHost)) {
    return 'souls';
  }
  return '