package clawhub.service;

import clawhub.dto.TrustedPublisherRequest;
import clawhub.dto.TrustedPublisherResponse;
import clawhub.entity.Skill;
import clawhub.entity.SkillTrustedPublisher;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import clawhub.repository.SkillTrustedPublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustedPublisherService {

    private final SkillTrustedPublisherRepository trustedPublisherRepository;
    private final SkillRepository skillRepository;

    /**
     * Get all trusted publishers for a skill
     */
    public List<TrustedPublisherResponse> getTrustedPublishers(String skillSlug) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));

        return trustedPublisherRepository.findBySkill(skill).stream()
                .map(TrustedPublisherResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific trusted publisher by ID
     */
    public Optional<TrustedPublisherResponse> getTrustedPublisher(Long id) {
        return trustedPublisherRepository.findById(id)
                .map(TrustedPublisherResponse::fromEntity);
    }

    /**
     * Create a new trusted publisher
     */
    @Transactional
    public TrustedPublisherResponse createTrustedPublisher(
            String skillSlug,
            TrustedPublisherRequest request,
            User currentUser) {

        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));

        // Check if user is the owner
        if (!skill.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to manage trusted publishers for this skill");
        }

        // Check if already exists
        if (trustedPublisherRepository.existsBySkillAndRepositoryAndEnvironment(
                skill, request.getRepository(), request.getEnvironment())) {
            throw new RuntimeException("Trusted publisher already exists for this repository and environment");
        }

        SkillTrustedPublisher publisher = SkillTrustedPublisher.builder()
                .skill(skill)
                .repository(request.getRepository())
                .repositoryId(request.getRepositoryId())
                .repositoryOwner(request.getRepositoryOwner())
                .repositoryOwnerId(request.getRepositoryOwnerId())
                .workflowFilename(request.getWorkflowFilename())
                .environment(request.getEnvironment())
                .createdBy(currentUser)
                .build();

        SkillTrustedPublisher saved = trustedPublisherRepository.save(publisher);
        log.info("Created trusted publisher {} for skill {} by user {}",
                saved.getId(), skillSlug, currentUser.getHandle());

        return TrustedPublisherResponse.fromEntity(saved);
    }

    /**
     * Delete a trusted publisher
     */
    @Transactional
    public void deleteTrustedPublisher(String skillSlug, Long id, User currentUser) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));

        // Check if user is the owner
        if (!skill.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to manage trusted publishers for this skill");
        }

        SkillTrustedPublisher publisher = trustedPublisherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trusted publisher not found: " + id));

        if (!publisher.getSkill().getId().equals(skill.getId())) {
            throw new RuntimeException("Trusted publisher does not belong to this skill");
        }

        trustedPublisherRepository.delete(publisher);
        log.info("Deleted trusted publisher {} for skill {} by user {}",
                id, skillSlug, currentUser.getHandle());
    }

    /**
     * Find a matching trusted publisher for the given repository and environment
     */
    public Optional<SkillTrustedPublisher> findMatchingPublisher(
            Skill skill,
            String repository,
            String environment) {
        return trustedPublisherRepository.findBySkillAndRepositoryAndEnvironment(
                skill, repository, environment);
    }

    /**
     * Check if a trusted publisher exists for the given criteria
     */
    public boolean hasTrustedPublisher(Skill skill, String repository, String environment) {
        return trustedPublisherRepository.existsBySkillAndRepositoryAndEnvironment(
                skill, repository, environment);
    }
}
