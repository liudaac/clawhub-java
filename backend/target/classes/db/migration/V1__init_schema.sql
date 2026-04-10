-- ClawHub Java Database Schema
-- Initial migration

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    github_id BIGINT UNIQUE NOT NULL,
    handle VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    bio TEXT,
    avatar_url VARCHAR(1024),
    role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('admin', 'moderator', 'user')),
    github_created_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_users_github_id ON users(github_id);
CREATE INDEX idx_users_handle ON users(handle);
CREATE INDEX idx_users_role ON users(role);

-- Skills table
CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary TEXT,
    latest_version_id UUID,
    resource_id VARCHAR(255),
    canonical_skill_id UUID REFERENCES skills(id),
    fork_of JSONB,
    badges JSONB DEFAULT '{}',
    moderation_status VARCHAR(20) DEFAULT 'active' CHECK (moderation_status IN ('active', 'hidden', 'removed')),
    moderation_flags JSONB DEFAULT '[]',
    moderation_verdict VARCHAR(50),
    moderation_notes TEXT,
    moderation_reason TEXT,
    hidden_at TIMESTAMP WITH TIME ZONE,
    hidden_by UUID REFERENCES users(id),
    last_reviewed_at TIMESTAMP WITH TIME ZONE,
    report_count INT DEFAULT 0,
    stats_downloads BIGINT DEFAULT 0,
    stats_stars INT DEFAULT 0,
    stats_versions INT DEFAULT 0,
    stats_comments INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_skills_slug ON skills(slug);
CREATE INDEX idx_skills_owner ON skills(owner_user_id);
CREATE INDEX idx_skills_status ON skills(moderation_status);
CREATE INDEX idx_skills_canonical ON skills(canonical_skill_id);
CREATE INDEX idx_skills_stats ON skills(stats_downloads DESC, stats_stars DESC);

-- Skill versions table
CREATE TABLE skill_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    tag VARCHAR(50),
    changelog TEXT NOT NULL,
    files JSONB NOT NULL DEFAULT '[]',
    parsed JSONB,
    embedding_id VARCHAR(255),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    soft_deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(skill_id, version)
);

CREATE INDEX idx_skill_versions_skill ON skill_versions(skill_id);
CREATE INDEX idx_skill_versions_created ON skill_versions(created_at DESC);

-- Update skills foreign key after skill_versions table is created
ALTER TABLE skills ADD CONSTRAINT fk_skills_latest_version 
    FOREIGN KEY (latest_version_id) REFERENCES skill_versions(id) DEFERRABLE INITIALLY DEFERRED;

-- Souls table (simplified version of skills)
CREATE TABLE souls (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary TEXT,
    latest_version_id UUID,
    tags JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active')),
    stats_downloads BIGINT DEFAULT 0,
    stats_stars INT DEFAULT 0,
    stats_versions INT DEFAULT 0,
    stats_comments INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_souls_slug ON souls(slug);
CREATE INDEX idx_souls_owner ON souls(owner_user_id);
CREATE INDEX idx_souls_stats ON souls(stats_downloads DESC, stats_stars DESC);

-- Soul versions table
CREATE TABLE soul_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    soul_id UUID NOT NULL REFERENCES souls(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    tag VARCHAR(50),
    changelog TEXT NOT NULL,
    files JSONB NOT NULL DEFAULT '[]',
    parsed JSONB,
    embedding_id VARCHAR(255),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    soft_deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(soul_id, version)
);

CREATE INDEX idx_soul_versions_soul ON soul_versions(soul_id);
CREATE INDEX idx_soul_versions_created ON soul_versions(created_at DESC);

-- Update souls foreign key
ALTER TABLE souls ADD CONSTRAINT fk_souls_latest_version 
    FOREIGN KEY (latest_version_id) REFERENCES soul_versions(id) DEFERRABLE INITIALLY DEFERRED;

-- Comments table (polymorphic for skills and souls)
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    skill_id UUID REFERENCES skills(id) ON DELETE CASCADE,
    soul_id UUID REFERENCES souls(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_comment_target CHECK (
        (skill_id IS NOT NULL AND soul_id IS NULL) OR 
        (skill_id IS NULL AND soul_id IS NOT NULL)
    )
);

CREATE INDEX idx_comments_skill ON comments(skill_id) WHERE skill_id IS NOT NULL;
CREATE INDEX idx_comments_soul ON comments(soul_id) WHERE soul_id IS NOT NULL;
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_created ON comments(created_at DESC);

-- Stars table (polymorphic for skills and souls)
CREATE TABLE stars (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    skill_id UUID REFERENCES skills(id) ON DELETE CASCADE,
    soul_id UUID REFERENCES souls(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(skill_id, user_id),
    UNIQUE(soul_id, user_id),
    CONSTRAINT chk_star_target CHECK (
        (skill_id IS NOT NULL AND soul_id IS NULL) OR 
        (skill_id IS NULL AND soul_id IS NOT NULL)
    )
);

CREATE INDEX idx_stars_skill ON stars(skill_id) WHERE skill_id IS NOT NULL;
CREATE INDEX idx_stars_soul ON stars(soul_id) WHERE soul_id IS NOT NULL;
CREATE INDEX idx_stars_user ON stars(user_id);

-- Skill search digests (for vector search)
CREATE TABLE skill_search_digests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    embedding_id VARCHAR(255),
    search_text TEXT,
    visibility VARCHAR(20) DEFAULT 'public',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_skill_search_skill ON skill_search_digests(skill_id);
CREATE INDEX idx_skill_search_visibility ON skill_search_digests(visibility);

-- Soul search digests
CREATE TABLE soul_search_digests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    soul_id UUID NOT NULL REFERENCES souls(id) ON DELETE CASCADE,
    embedding_id VARCHAR(255),
    search_text TEXT,
    visibility VARCHAR(20) DEFAULT 'public',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_soul_search_soul ON soul_search_digests(soul_id);
CREATE INDEX idx_soul_search_visibility ON soul_search_digests(visibility);

-- Audit logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);

-- File storage metadata (for MinIO integration)
CREATE TABLE file_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    storage_id VARCHAR(255) UNIQUE NOT NULL,
    original_name VARCHAR(1024),
    content_type VARCHAR(255),
    size BIGINT NOT NULL,
    sha256 VARCHAR(64),
    bucket VARCHAR(255) NOT NULL,
    path VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_file_metadata_storage ON file_metadata(storage_id);
CREATE INDEX idx_file_metadata_sha256 ON file_metadata(sha256);

-- Update timestamps function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Add update triggers
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_skills_updated_at BEFORE UPDATE ON skills
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_souls_updated_at BEFORE UPDATE ON souls
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_comments_updated_at BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_skill_search_digests_updated_at BEFORE UPDATE ON skill_search_digests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_soul_search_digests_updated_at BEFORE UPDATE ON soul_search_digests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();