package clawhub.service;

import clawhub.dto.*;
import clawhub.entity.Publisher;
import clawhub.entity.PublisherMember;
import clawhub.entity.User;
import clawhub.exception.BadRequestException;
import clawhub.exception.ForbiddenException;
import clawhub.exception.ResourceNotFoundException;
import clawhub.repository.PublisherMemberRepository;
import clawhub.repository.PublisherRepository;
import clawhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublisherService {

    private final PublisherRepository publisherRepository;
    private final PublisherMemberRepository publisherMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public PublisherResponse createPublisher(PublisherCreateRequest request, UUID currentUserId) {
        // Validate handle uniqueness
        if (publisherRepository.existsByHandleAndDeletedAtIsNull(request.getHandle())) {
            throw new BadRequestException("Handle '" + request.getHandle() + "' is already taken");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // For USER kind, check if user already has a personal publisher
        if (request.getKind() == PublisherCreateRequest.Kind.USER) {
            Optional<Publisher> existingPersonal = publisherRepository
                    .findByLinkedUserIdAndKindAndDeletedAtIsNull(currentUserId, Publisher.Kind.USER);
            if (existingPersonal.isPresent()) {
                throw new BadRequestException("User already has a personal publisher");
            }
        }

        // Create publisher
        Publisher publisher = Publisher.builder()
                .kind(request.getKind() == PublisherCreateRequest.Kind.USER ? Publisher.Kind.USER : Publisher.Kind.ORG)
                .handle(request.getHandle())
                .displayName(request.getDisplayName())
                .bio(request.getBio())
                .image(request.getImage())
                .linkedUserId(request.getKind() == PublisherCreateRequest.Kind.USER ? currentUserId : null)
                .trustedPublisher(false)
                .build();

        Publisher savedPublisher = publisherRepository.save(publisher);

        // Create owner membership
        PublisherMember member = PublisherMember.builder()
                .publisher(savedPublisher)
                .user(currentUser)
                .role(PublisherMember.Role.OWNER)
                .build();
        publisherMemberRepository.save(member);

        // Update user's personal publisher ID if it's a USER kind publisher
        if (request.getKind() == PublisherCreateRequest.Kind.USER) {
            currentUser.setPersonalPublisherId(savedPublisher.getId());
            userRepository.save(currentUser);
        }

        log.info("Created publisher: {} (id: {}) by user: {}", savedPublisher.getHandle(), savedPublisher.getId(), currentUserId);
        return PublisherResponse.fromEntity(savedPublisher);
    }

    @Transactional(readOnly = true)
    public PublisherResponse getPublisher(String handle) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(handle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + handle));
        return PublisherResponse.fromEntity(publisher);
    }

    @Transactional(readOnly = true)
    public PublisherResponse getPublisherById(UUID id) {
        Publisher publisher = publisherRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + id));
        return PublisherResponse.fromEntity(publisher);
    }

    @Transactional(readOnly = true)
    public PublisherListResponse listPublishers(String search, String kind, Boolean trusted, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Publisher.Kind kindFilter = null;
        if (kind != null && !kind.isEmpty()) {
            try {
                kindFilter = Publisher.Kind.valueOf(kind.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid kind filter, ignore
            }
        }

        Page<Publisher> publishers;
        if (search != null && !search.isEmpty()) {
            publishers = publisherRepository.searchPublishers(search, kindFilter, trusted, pageable);
        } else {
            publishers = publisherRepository.findAllActive(pageable);
        }

        List<PublisherResponse> responses = publishers.getContent().stream()
                .map(PublisherResponse::fromEntity)
                .toList();

        return PublisherListResponse.builder()
                .publishers(responses)
                .total(publishers.getTotalElements())
                .page(page)
                .size(size)
                .hasMore(publishers.hasNext())
                .build();
    }

    @Transactional
    public PublisherResponse updatePublisher(String handle, PublisherUpdateRequest request, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(handle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + handle));

        // Check permission (owner or admin)
        if (!isAdminOrOwner(publisher.getId(), currentUserId)) {
            throw new ForbiddenException("You don't have permission to update this publisher");
        }

        if (request.getDisplayName() != null) {
            publisher.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            publisher.setBio(request.getBio());
        }
        if (request.getImage() != null) {
            publisher.setImage(request.getImage());
        }

        Publisher updated = publisherRepository.save(publisher);
        log.info("Updated publisher: {} by user: {}", handle, currentUserId);
        return PublisherResponse.fromEntity(updated);
    }

    @Transactional
    public void deletePublisher(String handle, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(handle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + handle));

        // Only owner can delete
        if (!isOwner(publisher.getId(), currentUserId)) {
            throw new ForbiddenException("Only owner can delete this publisher");
        }

        publisher.setDeletedAt(Instant.now());
        publisherRepository.save(publisher);

        // Clear user's personal publisher ID if applicable
        if (publisher.getLinkedUserId() != null) {
            User user = userRepository.findById(publisher.getLinkedUserId()).orElse(null);
            if (user != null && user.getPersonalPublisherId() != null 
                    && user.getPersonalPublisherId().equals(publisher.getId())) {
                user.setPersonalPublisherId(null);
                userRepository.save(user);
            }
        }

        log.info("Deleted publisher: {} by user: {}", handle, currentUserId);
    }

    @Transactional
    public void deactivatePublisher(String handle, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(handle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + handle));

        // Only owner or admin can deactivate
        if (!isAdminOrOwner(publisher.getId(), currentUserId)) {
            throw new ForbiddenException("You don't have permission to deactivate this publisher");
        }

        publisher.setDeactivatedAt(Instant.now());
        publisherRepository.save(publisher);
        log.info("Deactivated publisher: {} by user: {}", handle, currentUserId);
    }

    @Transactional
    public void reactivatePublisher(String handle, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(handle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + handle));

        // Only owner or admin can reactivate
        if (!isAdminOrOwner(publisher.getId(), currentUserId)) {
            throw new ForbiddenException("You don't have permission to reactivate this publisher");
        }

        publisher.setDeactivatedAt(null);
        publisherRepository.save(publisher);
        log.info("Reactivated publisher: {} by user: {}", handle, currentUserId);
    }

    @Transactional(readOnly = true)
    public Publisher getPublisherEntity(String handle) {
        return publisherRepository.findByHandleAndDeletedAtIsNull(handle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + handle));
    }

    @Transactional(readOnly = true)
    public Publisher getPublisherEntityById(UUID id) {
        return publisherRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + id));
    }

    // Permission helpers
    public boolean isOwner(UUID publisherId, UUID userId) {
        return publisherMemberRepository.isOwner(publisherId, userId);
    }

    public boolean isAdminOrOwner(UUID publisherId, UUID userId) {
        return publisherMemberRepository.isAdminOrOwner(publisherId, userId);
    }

    public boolean canPublish(UUID publisherId, UUID userId) {
        return publisherMemberRepository.canPublish(publisherId, userId);
    }

    @Transactional(readOnly = true)
    public PublisherResponse getPersonalPublisher(UUID userId) {
        Publisher publisher = publisherRepository.findPersonalPublisherByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal publisher not found for user: " + userId));
        return PublisherResponse.fromEntity(publisher);
    }
}