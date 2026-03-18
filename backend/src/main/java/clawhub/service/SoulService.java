package clawhub.service;

import clawhub.entity.Soul;
import clawhub.entity.SoulVersion;
import clawhub.entity.User;
import clawhub.repository.SoulRepository;
import clawhub.repository.SoulVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoulService {

    private final SoulRepository soulRepository;
    private final SoulVersionRepository soulVersionRepository;

    @Transactional(readOnly = true)
    public Page<Soul> findAll(Pageable pageable) {
        return soulRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Soul> findBySlug(String slug) {
        return soulRepository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public Optional<Soul> findById(UUID id) {
        return soulRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Soul> findByOwner(User owner, Pageable pageable) {
        return soulRepository.findByOwner(owner, pageable);
    }

    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return soulRepository.existsBySlug(slug);
    }

    @Transactional
    public Soul createSoul(String slug, String displayName, String summary, User owner) {
        if (soulRepository.existsBySlug(slug)) {
            throw new RuntimeException("Soul with slug '" + slug + "' already exists");
        }

        Soul soul = Soul.builder()
                .slug(slug)
                .displayName(displayName)
                .summary(summary)
                .owner(owner)
                .statsDownloads(0L)
                .statsStars(0)
                .statsVersions(0)
                .statsComments(0)
                .build();

        Soul savedSoul = soulRepository.save(soul);
        log.info("Created soul: {} by {}", slug, owner.getHandle());
        
        return savedSoul;
    }

    @Transactional
    public Soul updateSoul(String slug, String displayName, String summary, UUID currentUserId) {
        Soul soul = soulRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Soul not found: " + slug));
        
        // Check ownership
        if (!soul.getOwner().getId().equals(currentUserId) && 
            !soul.getOwner().getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to update this soul");
        }
        
        if (displayName != null) {
            soul.setDisplayName(displayName);
        }
        if (summary != null) {
            soul.setSummary(summary);
        }
        
        Soul savedSoul = soulRepository.save(soul);
        log.info("Updated soul: {}", slug);
        
        return savedSoul;
    }

    @Transactional
    public void deleteSoul(String slug, UUID currentUserId) {
        Soul soul = soulRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Soul not found: " + slug));
        
        // Check ownership or admin
        if (!soul.getOwner().getId().equals(currentUserId) && 
            !soul.getOwner().getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to delete this soul");
        }
        
        soulRepository.delete(soul);
        log.info("Deleted soul: {}", slug);
    }

    @Transactional
    public void incrementDownloads(UUID soulId) {
        soulRepository.incrementDownloads(soulId);
    }

    @Transactional
    public void incrementStars(UUID soulId) {
        soulRepository.incrementStars(soulId);
    }

    @Transactional
    public void decrementStars(UUID soulId) {
        soulRepository.decrementStars(soulId);
    }

    @Transactional
    public void incrementComments(UUID soulId) {
        soulRepository.incrementComments(soulId);
    }

    @Transactional
    public void decrementComments(UUID soulId) {
        soulRepository.decrementComments(soulId);
    }
}
