package clawhub.service;

import clawhub.dto.PublisherMemberRequest;
import clawhub.dto.PublisherMemberResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublisherMemberService {

    private final PublisherMemberRepository publisherMemberRepository;
    private final PublisherRepository publisherRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PublisherMemberResponse> getMembers(String publisherHandle) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(publisherHandle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + publisherHandle));

        List<PublisherMember> members = publisherMemberRepository.findByPublisherIdWithUsers(publisher.getId());
        return members.stream()
                .map(PublisherMemberResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PublisherMemberResponse addMember(String publisherHandle, PublisherMemberRequest request, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(publisherHandle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + publisherHandle));

        // Only owner or admin can add members
        if (!publisherMemberRepository.isAdminOrOwner(publisher.getId(), currentUserId)) {
            throw new ForbiddenException("You don't have permission to add members");
        }

        // Cannot add owner through this endpoint
        if (request.getRole() == PublisherMemberRequest.Role.ADMIN && 
            !publisherMemberRepository.isOwner(publisher.getId(), currentUserId)) {
            throw new ForbiddenException("Only owner can add admins");
        }

        User userToAdd = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        // Check if already a member
        if (publisherMemberRepository.existsByPublisherIdAndUserId(publisher.getId(), request.getUserId())) {
            throw new BadRequestException("User is already a member of this publisher");
        }

        PublisherMember.Role role = request.getRole() == PublisherMemberRequest.Role.ADMIN 
                ? PublisherMember.Role.ADMIN 
                : PublisherMember.Role.PUBLISHER;

        PublisherMember member = PublisherMember.builder()
                .publisher(publisher)
                .user(userToAdd)
                .role(role)
                .build();

        PublisherMember saved = publisherMemberRepository.save(member);
        log.info("Added member {} with role {} to publisher {} by user {}", 
                request.getUserId(), role, publisherHandle, currentUserId);

        return PublisherMemberResponse.fromEntity(saved);
    }

    @Transactional
    public PublisherMemberResponse updateMember(String publisherHandle, UUID memberId, PublisherMemberRequest request, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(publisherHandle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + publisherHandle));

        PublisherMember member = publisherMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        if (!member.getPublisher().getId().equals(publisher.getId())) {
            throw new BadRequestException("Member does not belong to this publisher");
        }

        // Owner can update any member, admin can only update publishers (not other admins or owner)
        boolean isOwner = publisherMemberRepository.isOwner(publisher.getId(), currentUserId);
        boolean isAdmin = publisherMemberRepository.isAdminOrOwner(publisher.getId(), currentUserId);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You don't have permission to update members");
        }

        // Admin cannot modify owner or other admins
        if (!isOwner && member.getRole() != PublisherMember.Role.PUBLISHER) {
            throw new ForbiddenException("Only owner can modify admins or owner");
        }

        // Cannot change role to owner through this endpoint
        if (request.getRole() == null || request.getRole() == PublisherMemberRequest.Role.ADMIN) {
            if (!isOwner) {
                throw new ForbiddenException("Only owner can assign admin role");
            }
            member.setRole(PublisherMember.Role.ADMIN);
        } else {
            member.setRole(PublisherMember.Role.PUBLISHER);
        }

        PublisherMember updated = publisherMemberRepository.save(member);
        log.info("Updated member {} to role {} in publisher {} by user {}", 
                memberId, member.getRole(), publisherHandle, currentUserId);

        return PublisherMemberResponse.fromEntity(updated);
    }

    @Transactional
    public void removeMember(String publisherHandle, UUID memberId, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(publisherHandle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + publisherHandle));

        PublisherMember member = publisherMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        if (!member.getPublisher().getId().equals(publisher.getId())) {
            throw new BadRequestException("Member does not belong to this publisher");
        }

        // Cannot remove owner
        if (member.getRole() == PublisherMember.Role.OWNER) {
            throw new ForbiddenException("Cannot remove owner from publisher");
        }

        // Owner can remove anyone, admin can only remove publishers
        boolean isOwner = publisherMemberRepository.isOwner(publisher.getId(), currentUserId);
        boolean isAdmin = publisherMemberRepository.isAdminOrOwner(publisher.getId(), currentUserId);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You don't have permission to remove members");
        }

        if (!isOwner && member.getRole() == PublisherMember.Role.ADMIN) {
            throw new ForbiddenException("Only owner can remove admins");
        }

        publisherMemberRepository.delete(member);
        log.info("Removed member {} from publisher {} by user {}", memberId, publisherHandle, currentUserId);
    }

    @Transactional
    public void leavePublisher(String publisherHandle, UUID currentUserId) {
        Publisher publisher = publisherRepository.findByHandleAndDeletedAtIsNull(publisherHandle)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + publisherHandle));

        PublisherMember member = publisherMemberRepository
                .findByPublisherIdAndUserId(publisher.getId(), currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this publisher"));

        // Owner cannot leave (must transfer ownership or delete publisher)
        if (member.getRole() == PublisherMember.Role.OWNER) {
            throw new ForbiddenException("Owner cannot leave publisher. Transfer ownership or delete the publisher instead.");
        }

        publisherMemberRepository.delete(member);
        log.info("User {} left publisher {}", currentUserId, publisherHandle);
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID publisherId, UUID userId) {
        return publisherMemberRepository.existsByPublisherIdAndUserId(publisherId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(UUID publisherId, UUID userId, PublisherMember.Role... roles) {
        return publisherMemberRepository.existsByPublisherIdAndUserIdAndRoleIn(
                publisherId, userId, List.of(roles));
    }
}
