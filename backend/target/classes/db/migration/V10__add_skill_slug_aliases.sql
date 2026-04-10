-- Migration V10: Add skill_slug_aliases table for redirects
-- This table stores slug aliases for skills, enabling rename and merge functionality

CREATE TABLE skill_slug_aliases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug VARCHAR(255) UNIQUE NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for efficient lookups
CREATE INDEX idx_skill_slug_aliases_slug ON skill_slug_aliases(slug);
CREATE INDEX idx_skill_slug_aliases_skill ON skill_slug_aliases(skill_id);
CREATE INDEX idx_skill_slug_aliases_owner ON skill_slug_aliases(owner_user_id);

-- Add update trigger
CREATE TRIGGER update_skill_slug_aliases_updated_at BEFORE UPDATE ON skill_slug_aliases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add soft_deleted_at to skills table for merge functionality
-- (marking merged skills as soft deleted)
ALTER TABLE skills ADD COLUMN IF NOT EXISTS soft_deleted_at TIMESTAMP WITH TIME ZONE;

-- Add index for soft deleted filtering
CREATE INDEX idx_skills_soft_deleted ON skills(soft_deleted_at) WHERE soft_deleted_at IS NULL;
