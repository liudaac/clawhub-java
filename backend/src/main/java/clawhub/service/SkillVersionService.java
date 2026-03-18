package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import clawhub.repository.SkillVersionRepository;
import clawhub.websocket.SkillWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillVersionService {

    private final SkillVersionRepository skillVersionRepository;
    private final SkillRepository skillRepository;
    private final SkillService skillService;
    private final ObjectMapper objectMapper;
    private final SkillWebSocketHandler webSocketHandler;

    // Semantic versioning pattern: major.minor.patch[-prerelease][+build]
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
            "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
            "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
    );

    @Transactional(readOnly = true)
    public Page<SkillVersion> findBySkill(Skill skill, Pageable pageable) {
        return skillVersionRepository.findBySkill(skill, pageable);
    }

    @Transactional(readOnly = true)
    public List<SkillVersion> findBySkillOrderByCreatedAtDesc(Skill skill) {
        return skillVersionRepository.findBySkillOrderByCreatedAtDesc(skill);
    }

    @Transactional(readOnly = true)
    public Optional<SkillVersion> findById(UUID id) {
        return skillVersionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<SkillVersion> findBySkillAndVersion(Skill skill, String version) {
        return skillVersionRepository.findBySkillAndVersion(skill, version);
    }

    @Transactional(readOnly = true)
    public Optional<SkillVersion> findLatestVersion(Skill skill) {
        return skillVersionRepository.findTopBySkillOrderByCreatedAtDesc(skill);
    }

    @Transactional
    public SkillVersion createVersion(UUID skillId, String version, String tag, String changelog, 
                                       List<SkillVersion.SkillFile> files, Object parsed, 
                                       User createdBy) {
        // Validate semantic version
        if (!isValidSemver(version)) {
            throw new RuntimeException("Invalid semantic version format: " + version);
        }

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        // Check ownership
        if (!skill.getOwner().getId().equals(createdBy.getId()) && 
            !createdBy.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to create version for this skill");
        }

        // Check if version already exists
        if (skillVersionRepository.existsBySkillAndVersion(skill, version)) {
            throw new RuntimeException("Version " + version + " already exists for skill " + skill.getSlug());
        }

        SkillVersion skillVersion = SkillVersion.builder()
                .skill(skill)
                .version(version)
                .tag(tag)
                .changelog(changelog)
                .files(files)
                .parsed(parsed)
                .createdBy(createdBy)
                .build();

        SkillVersion savedVersion = skillVersionRepository.save(skillVersion);

        // Update skill's latest version
        skill.setLatestVersion(savedVersion);
        skill.setStatsVersions(skill.getStatsVersions() + 1);
        Skill updatedSkill = skillRepository.save(skill);

        log.info("Created version {} for skill {}", version, skill.getSlug());
        
        // Broadcast update
        webSocketHandler.broadcastSkillUpdate(updatedSkill);

        return savedVersion;
    }

    @Transactional
    public SkillVersion createVersion(String skillSlug, String version, String tag, String changelog,
                                       List<SkillVersion.SkillFile> files, Object parsed,
                                       User createdBy) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));
        return createVersion(skill.getId(), version, tag, changelog, files, parsed, createdBy);
    }

    @Transactional
    public Skill rollbackToVersion(String skillSlug, String version, User currentUser) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));

        // Check ownership
        if (!skill.getOwner().getId().equals(currentUser.getId()) && 
            !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to rollback this skill");
        }

        SkillVersion targetVersion = skillVersionRepository.findBySkillAndVersion(skill, version)
                .orElseThrow(() -> new RuntimeException("Version not found: " + version));

        skill.setLatestVersion(targetVersion);
        skillRepository.save(skill);

        log.info("Rolled back skill {} to version {}", skillSlug, version);

        return skill;
    }

    @Transactional
    public void softDeleteVersion(UUID versionId, User currentUser) {
        SkillVersion version = skillVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

        Skill skill = version.getSkill();

        // Check ownership
        if (!skill.getOwner().getId().equals(currentUser.getId()) && 
            !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to delete this version");
        }

        version.setSoftDeletedAt(java.time.Instant.now());
        skillVersionRepository.save(version);

        // If this was the latest version, update to the previous one
        if (skill.getLatestVersion() != null && skill.getLatestVersion().getId().equals(versionId)) {
            List<SkillVersion> versions = skillVersionRepository.findBySkillOrderByCreatedAtDesc(skill);
            SkillVersion newLatest = versions.stream()
                    .filter(v -> !v.getId().equals(versionId) && v.getSoftDeletedAt() == null)
                    .findFirst()
                    .orElse(null);
            skill.setLatestVersion(newLatest);
            skillRepository.save(skill);
        }

        log.info("Soft deleted version {} of skill {}", version.getVersion(), skill.getSlug());
    }

    @Transactional(readOnly = true)
    public List<SkillVersion> getVersionHistory(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
        
        return skillVersionRepository.findBySkillOrderByCreatedAtDesc(skill);
    }

    @Transactional(readOnly = true)
    public int compareVersions(String v1, String v2) {
        return parseSemver(v1).compareTo(parseSemver(v2));
    }

    @Transactional(readOnly = true)
    public boolean isValidSemver(String version) {
        return SEMVER_PATTERN.matcher(version).matches();
    }

    private Semver parseSemver(String version) {
        Matcher matcher = SEMVER_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid semantic version: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String prerelease = matcher.group(4);
        String build = matcher.group(5);

        return new Semver(major, minor, patch, prerelease, build);
    }

    private record Semver(int major, int minor, int patch, String prerelease, String build) 
            implements Comparable<Semver> {
        
        @Override
        public int compareTo(Semver other) {
            int result = Integer.compare(this.major, other.major);
            if (result != 0) return result;
            
            result = Integer.compare(this.minor, other.minor);
            if (result != 0) return result;
            
            result = Integer.compare(this.patch, other.patch);
            if (result != 0) return result;
            
            // Pre-release versions have lower precedence
            if (this.prerelease == null && other.prerelease != null) return 1;
            if (this.prerelease != null && other.prerelease == null) return -1;
            if (this.prerelease != null && other.prerelease != null) {
                return this.prerelease.compareTo(other.prerelease);
            }
            
            return 0;
        }
    }
}