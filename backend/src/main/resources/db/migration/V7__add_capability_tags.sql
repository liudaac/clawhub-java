-- Add capability tags support for skills

-- Create table for skill capability tags
CREATE TABLE IF NOT EXISTS skill_capability_tags (
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (skill_id, tag)
);

-- Create index for tag lookups
CREATE INDEX IF NOT EXISTS idx_skill_capability_tags_tag ON skill_capability_tags(tag);

-- Add comment
COMMENT ON TABLE skill_capability_tags IS 'Stores capability tags for skills';
