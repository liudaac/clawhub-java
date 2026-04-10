CREATE TABLE skill_trusted_publishers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_id VARCHAR(36) NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'github-actions',
    repository VARCHAR(255) NOT NULL,
    repository_id VARCHAR(50) NOT NULL,
    repository_owner VARCHAR(100) NOT NULL,
    repository_owner_id VARCHAR(50) NOT NULL,
    workflow_filename VARCHAR(255) NOT NULL,
    environment VARCHAR(100) NOT NULL DEFAULT 'production',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NOT NULL,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE KEY uk_skill_repo_env (skill_id, repository, environment)
);

CREATE INDEX idx_trusted_publisher_skill ON skill_trusted_publishers(skill_id);
CREATE INDEX idx_trusted_publisher_repo ON skill_trusted_publishers(repository);
