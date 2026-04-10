-- 技能版本表增强（添加安全分析字段）
ALTER TABLE skill_versions
    ADD COLUMN IF NOT EXISTS vt_analysis JSONB,
    ADD COLUMN IF NOT EXISTS llm_analysis JSONB,
    ADD COLUMN IF NOT EXISTS moderation_snapshot JSONB;

-- 技能表增强
ALTER TABLE skills
    ADD COLUMN IF NOT EXISTS moderation_verdict VARCHAR(50),
    ADD COLUMN IF NOT EXISTS moderation_reason TEXT,
    ADD COLUMN IF NOT EXISTS moderation_notes TEXT,
    ADD COLUMN IF NOT EXISTS last_reviewed_at TIMESTAMP WITH TIME ZONE;

-- 技能转移表
CREATE TABLE IF NOT EXISTS skill_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    from_user_id UUID NOT NULL REFERENCES users(id),
    to_user_id UUID NOT NULL REFERENCES users(id),
    request_message TEXT,
    response_message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE,
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_skill_transfers_skill ON skill_transfers(skill_id);
CREATE INDEX IF NOT EXISTS idx_skill_transfers_from_user ON skill_transfers(from_user_id);
CREATE INDEX IF NOT EXISTS idx_skill_transfers_to_user ON skill_transfers(to_user_id);
CREATE INDEX IF NOT EXISTS idx_skill_transfers_status ON skill_transfers(status);
CREATE INDEX IF NOT EXISTS idx_skill_transfers_expires ON skill_transfers(expires_at);

-- 评论举报表
CREATE TABLE IF NOT EXISTS comment_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ai_verdict JSONB,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comment_reports_comment ON comment_reports(comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_reports_reporter ON comment_reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_comment_reports_status ON comment_reports(status);
CREATE INDEX IF NOT EXISTS idx_comment_reports_created ON comment_reports(created_at);

-- 用户表增强（添加信任发布者标志）
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS trusted_publisher BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ban_reason TEXT,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- 下载统计表（用于去重）
CREATE TABLE IF NOT EXISTS download_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    version_id UUID NOT NULL REFERENCES skill_versions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_download_records_skill ON download_records(skill_id);
CREATE INDEX IF NOT EXISTS idx_download_records_version ON download_records(version_id);
CREATE INDEX IF NOT EXISTS idx_download_records_ip_date ON download_records(ip_address, created_at);
CREATE INDEX IF NOT EXISTS idx_download_records_created ON download_records(created_at);

-- 技能统计事件表
CREATE TABLE IF NOT EXISTS skill_stat_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_skill_stat_events_skill ON skill_stat_events(skill_id);
CREATE INDEX IF NOT EXISTS idx_skill_stat_events_processed ON skill_stat_events(processed);
CREATE INDEX IF NOT EXISTS idx_skill_stat_events_created ON skill_stat_events(created_at);

-- 全局统计表
CREATE TABLE IF NOT EXISTS global_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stat_name VARCHAR(100) NOT NULL UNIQUE,
    stat_value BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO global_stats (stat_name, stat_value) VALUES
    ('total_skills', 0),
    ('total_souls', 0),
    ('total_users', 0),
    ('total_downloads', 0)
ON CONFLICT (stat_name) DO NOTHING;

-- 更新触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为新增表添加更新时间触发器
DROP TRIGGER IF EXISTS update_skill_transfers_updated_at ON skill_transfers;
CREATE TRIGGER update_skill_transfers_updated_at
    BEFORE UPDATE ON skill_transfers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_comment_reports_updated_at ON comment_reports;
CREATE TRIGGER update_comment_reports_updated_at
    BEFORE UPDATE ON comment_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
