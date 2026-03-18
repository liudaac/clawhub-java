-- Add indexes for performance optimization

-- Skills table indexes
CREATE INDEX IF NOT EXISTS idx_skills_slug ON skills(slug);
CREATE INDEX IF NOT EXISTS idx_skills_owner_id ON skills(owner_id);
CREATE INDEX IF NOT EXISTS idx_skills_moderation_status ON skills(moderation_status);
CREATE INDEX IF NOT EXISTS idx_skills_created_at ON skills(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_skills_stats_downloads ON skills(stats_downloads DESC);
CREATE INDEX IF NOT EXISTS idx_skills_stats_stars ON skills(stats_stars DESC);
CREATE INDEX IF NOT EXISTS idx_skills_search ON skills USING gin(to_tsvector('english', display_name || ' ' || coalesce(summary, '')));

-- Souls table indexes
CREATE INDEX IF NOT EXISTS idx_souls_slug ON souls(slug);
CREATE INDEX IF NOT EXISTS idx_souls_owner_id ON souls(owner_id);
CREATE INDEX IF NOT EXISTS idx_souls_created_at ON souls(created_at DESC);

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_handle ON users(handle);
CREATE INDEX IF NOT EXISTS idx_users_github_id ON users(github_id);

-- Comments table indexes
CREATE INDEX IF NOT EXISTS idx_comments_skill_id ON comments(skill_id);
CREATE INDEX IF NOT EXISTS idx_comments_soul_id ON comments(soul_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at DESC);

-- Stars table indexes
CREATE INDEX IF NOT EXISTS idx_stars_skill_id ON stars(skill_id);
CREATE INDEX IF NOT EXISTS idx_stars_soul_id ON stars(soul_id);
CREATE INDEX IF NOT EXISTS idx_stars_user_id ON stars(user_id);

-- Skill versions table indexes
CREATE INDEX IF NOT EXISTS idx_skill_versions_skill_id ON skill_versions(skill_id);
CREATE INDEX IF NOT EXISTS idx_skill_versions_version ON skill_versions(skill_id, version);

-- Soul versions table indexes
CREATE INDEX IF NOT EXISTS idx_soul_versions_soul_id ON soul_versions(soul_id);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_skills_status_created ON skills(moderation_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_skills_owner_status ON skills(owner_id, moderation_status);
