-- Phase 1: Publisher Organization System
-- Migration for adding Publisher and PublisherMember tables

-- Publishers table
CREATE TABLE publishers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('USER', 'ORG')),
    handle VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    bio TEXT,
    image VARCHAR(1024),
    linked_user_id UUID REFERENCES users(id),
    trusted_publisher BOOLEAN DEFAULT FALSE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for publishers
CREATE INDEX idx_publishers_handle ON publishers(handle);
CREATE INDEX idx_publishers_linked_user ON publishers(linked_user_id);
CREATE INDEX idx_publishers_kind ON publishers(kind);
CREATE INDEX idx_publishers_kind_handle ON publishers(kind, handle);
CREATE INDEX idx_publishers_trusted ON publishers(trusted_publisher) WHERE trusted_publisher = TRUE;
CREATE INDEX idx_publishers_active ON publishers(deleted_at, deactivated_at) WHERE deleted_at IS NULL AND deactivated_at IS NULL;

-- Publisher members table
CREATE TABLE publisher_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    publisher_id UUID NOT NULL REFERENCES publishers(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'PUBLISHER')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(publisher_id, user_id)
);

-- Indexes for publisher_members
CREATE INDEX idx_publisher_members_publisher ON publisher_members(publisher_id);
CREATE INDEX idx_publisher_members_user ON publisher_members(user_id);
CREATE INDEX idx_publisher_members_role ON publisher_members(role);
CREATE INDEX idx_publisher_members_publisher_role ON publisher_members(publisher_id, role);

-- Add personal_publisher_id to users table
ALTER TABLE users ADD COLUMN personal_publisher_id UUID REFERENCES publishers(id);
CREATE INDEX idx_users_personal_publisher ON users(personal_publisher_id);

-- Add owner_publisher_id to skills table
ALTER TABLE skills ADD COLUMN owner_publisher_id UUID REFERENCES publishers(id);
CREATE INDEX idx_skills_owner_publisher ON skills(owner_publisher_id);

-- Add owner_publisher_id to souls table
ALTER TABLE souls ADD COLUMN owner_publisher_id UUID REFERENCES publishers(id);
CREATE INDEX idx_souls_owner_publisher ON souls(owner_publisher_id);

-- Add update triggers for new tables
CREATE TRIGGER update_publishers_updated_at BEFORE UPDATE ON publishers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_publisher_members_updated_at BEFORE UPDATE ON publisher_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add stats columns to publishers (for future use)
ALTER TABLE publishers ADD COLUMN stats_downloads BIGINT DEFAULT 0;
ALTER TABLE publishers ADD COLUMN stats_stars INTEGER DEFAULT 0;
ALTER TABLE publishers ADD COLUMN stats_versions INTEGER DEFAULT 0;

CREATE INDEX idx_publishers_stats ON publishers(stats_downloads DESC, stats_stars DESC);
