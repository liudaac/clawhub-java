package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final SkillRepository skillRepository;

    // Predefined badge types
    public static final String BADGE_HIGHLIGHTED = "highlighted";
    public static final String BADGE_VERIFIED = "verified";
    public static final String BADGE_TRENDING = "trending";
    public static final String BADGE_STAFF_PICK = "staff_pick";
    public static final String BADGE_COMMUNITY_FAVORITE = "community_favorite";
    public static final String BADGE_NEW = "new";

    @Transactional
    public Skill addBadge(UUID skillId, String badgeType, Map<String, Object> badgeData) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        Map<String, Object> badges = skill.getBadges();
        if (badges == null) {
            badges = new HashMap<>();
        }

        badges.put(badgeType, badgeData != null ? badgeData : Map.of("awardedAt", System.currentTimeMillis()));
        skill.setBadges(badges);

        log.info("Added badge {} to skill {}", badgeType, skillId);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill addBadge(UUID skillId, String badgeType) {
        return addBadge(skillId, badgeType, null);
    }

    @Transactional
    public Skill removeBadge(UUID skillId, String badgeType) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        Map<String, Object> badges = skill.getBadges();
        if (badges != null) {
            badges.remove(badgeType);
            skill.setBadges(badges);
        }

        log.info("Removed badge {} from skill {}", badgeType, skillId);
        return skillRepository.save(skill);
    }

    @Transactional(readOnly = true)
    public boolean hasBadge(UUID skillId, String badgeType) {
        return skillRepository.findById(skillId)
                .map(skill -> {
                    Map<String, Object> badges = skill.getBadges();
                    return badges != null && badges.containsKey(badgeType);
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBadges(UUID skillId) {
        return skillRepository.findById(skillId)
                .map(Skill::getBadges)
                .orElse(new HashMap<>());
    }

    @Transactional
    public Skill awardHighlightedBadge(UUID skillId, User awardedBy) {
        Map<String, Object> badgeData = Map.of(
                "awardedAt", System.currentTimeMillis(),
                "awardedBy", awardedBy.getHandle()
        );
        return addBadge(skillId, BADGE_HIGHLIGHTED, badgeData);
    }

    @Transactional
    public Skill awardVerifiedBadge(UUID skillId) {
        return addBadge(skillId, BADGE_VERIFIED, Map.of(
                "awardedAt", System.currentTimeMillis(),
                "verified", true
        ));
    }

    @Transactional
    public Skill awardTrendingBadge(UUID skillId) {
        return addBadge(skillId, BADGE_TRENDING, Map.of(
                "awardedAt", System.currentTimeMillis()
        ));
    }

    @Transactional
    public Skill awardStaffPickBadge(UUID skillId, User pickedBy) {
        return addBadge(skillId, BADGE_STAFF_PICK, Map.of(
                "awardedAt", System.currentTimeMillis(),
                "pickedBy", pickedBy.getHandle()
        ));
    }

    @Transactional
    public Skill awardCommunityFavoriteBadge(UUID skillId) {
        return addBadge(skillId, BADGE_COMMUNITY_FAVORITE, Map.of(
                "awardedAt", System.currentTimeMillis()
        ));
    }

    @Transactional
    public Skill awardNewBadge(UUID skillId) {
        return addBadge(skillId, BADGE_NEW, Map.of(
                "awardedAt", System.currentTimeMillis()
        ));
    }

    @Transactional
    public void checkAndAwardBadges(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        // Check for community favorite (100+ stars)
        if (skill.getStatsStars() >= 100 && !hasBadge(skillId, BADGE_COMMUNITY_FAVORITE)) {
            awardCommunityFavoriteBadge(skillId);
        }

        // Check for trending (high download rate - simplified logic)
        if (skill.getStatsDownloads() >= 1000 && !hasBadge(skillId, BADGE_TRENDING)) {
            awardTrendingBadge(skillId);
        }

        // Remove "new" badge after 30 days (simplified - should check creation date)
        if (hasBadge(skillId, BADGE_NEW)) {
            long daysSinceCreation = (System.currentTimeMillis() - skill.getCreatedAt().toEpochMilli()) / (1000 * 60 * 60 * 24);
            if (daysSinceCreation > 30) {
                removeBadge(skillId, BADGE_NEW);
            }
        }
    }
}
