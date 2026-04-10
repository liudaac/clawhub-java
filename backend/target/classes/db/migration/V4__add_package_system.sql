-- Phase 2: Package/Plugin System
-- Migration for adding Package and PackageRelease tables

-- Packages table
CREATE TABLE packages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    summary TEXT,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    owner_publisher_id UUID REFERENCES publishers(id),
    family VARCHAR(20) NOT NULL CHECK (family IN ('SKILL', 'CODE_PLUGIN', 'BUNDLE_PLUGIN')),
    channel VARCHAR(20) NOT NULL DEFAULT 'COMMUNITY' CHECK (channel IN ('OFFICIAL', 'COMMUNITY', 'PRIVATE')),
    is_official BOOLEAN DEFAULT FALSE,
    runtime_id VARCHAR(100),
    compatibility JSONB,
    capabilities JSONB,
    verification JSONB,
    scan_status VARCHAR(20) DEFAULT 'NOT_RUN' CHECK (scan_status IN ('NOT_RUN', 'PENDING', 'CLEAN', 'SUSPICIOUS', 'MALICIOUS')),
    stats_downloads BIGINT DEFAULT 0,
    stats_installs BIGINT DEFAULT 0,
    stats_stars INTEGER DEFAULT 0,
    stats_versions INTEGER DEFAULT 0,
    soft_deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for packages
CREATE INDEX idx_packages_name ON packages(normalized_name);
CREATE INDEX idx_packages_owner ON packages(owner_user_id);
CREATE INDEX idx_packages_owner_publisher ON packages(owner_publisher_id);
CREATE INDEX idx_packages_family ON packages(family);
CREATE INDEX idx_packages_channel ON packages(channel);
CREATE INDEX idx_packages_is_official ON packages(is_official);
CREATE INDEX idx_packages_family_updated ON packages(family, updated_at);
CREATE INDEX idx_packages_family_channel ON packages(family, channel);
CREATE INDEX idx_packages_stats ON packages(stats_downloads DESC, stats_stars DESC);
CREATE INDEX idx_packages_soft_deleted ON packages(soft_deleted_at) WHERE soft_deleted_at IS NULL;
CREATE INDEX idx_packages_runtime ON packages(runtime_id);

-- Package releases table
CREATE TABLE package_releases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    package_id UUID NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    changelog TEXT,
    changelog_source VARCHAR(10) DEFAULT 'AUTO' CHECK (changelog_source IN ('AUTO', 'USER')),
    files JSONB NOT NULL DEFAULT '[]',
    integrity_sha256 VARCHAR(64),
    compatibility JSONB,
    capabilities JSONB,
    verification JSONB,
    vt_analysis JSONB,
    llm_analysis JSONB,
    static_scan JSONB,
    published_by UUID REFERENCES users(id),
    soft_deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(package_id, version)
);

-- Indexes for package_releases
CREATE INDEX idx_package_releases_package ON package_releases(package_id);
CREATE INDEX idx_package_releases_version ON package_releases(version);
CREATE INDEX idx_package_releases_created ON package_releases(created_at DESC);
CREATE INDEX idx_package_releases_published_by ON package_releases(published_by);
CREATE INDEX idx_package_releases_soft_deleted ON package_releases(soft_deleted_at) WHERE soft_deleted_at IS NULL;

-- Add update triggers
CREATE TRIGGER update_packages_updated_at BEFORE UPDATE ON packages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_package_releases_updated_at BEFORE UPDATE ON package_releases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- GIN indexes for JSONB columns (for efficient querying)
CREATE INDEX idx_packages_capabilities ON packages USING GIN (capabilities);
CREATE INDEX idx_packages_compatibility ON packages USING GIN (compatibility);
CREATE INDEX idx_packages_verification ON packages USING GIN (verification);
CREATE INDEX idx_package_releases_capabilities ON package_releases USING GIN (capabilities);
CREATE INDEX idx_package_releases_compatibility ON package_releases USING GIN (compatibility);
