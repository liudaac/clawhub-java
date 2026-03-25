package clawhub.repository;

import clawhub.entity.PublisherMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublisherMemberRepository extends JpaRepository<PublisherMember, UUID> {

    List<PublisherMember> findByPublisherId(UUID publisherId);

    List<PublisherMember> findByUserId(UUID userId);

    Optional<PublisherMember> findByPublisherIdAndUserId(UUID publisherId, UUID userId);

    boolean existsByPublisherIdAndUserId(UUID publisherId, UUID userId);

    boolean existsByPublisherIdAndUserIdAndRoleIn(UUID publisherId, UUID userId, List<PublisherMember.Role> roles);

    @Query("SELECT pm FROM PublisherMember pm WHERE pm.publisher.id = :publisherId AND pm.user.id = :userId")
    Optional<PublisherMember> findByPublisherIdAndUserIdWithEntities(@Param("publisherId") UUID publisherId, @Param("userId") UUID userId);

    @Query("SELECT pm FROM PublisherMember pm JOIN FETCH pm.user WHERE pm.publisher.id = :publisherId")
    List<PublisherMember> findByPublisherIdWithUsers(@Param("publisherId") UUID publisherId);

    long countByPublisherId(UUID publisherId);

    void deleteByPublisherIdAndUserId(UUID publisherId, UUID userId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PublisherMember pm " +
           "WHERE pm.publisher.id = :publisherId AND pm.user.id = :userId AND pm.role = 'OWNER'")
    boolean isOwner(@Param("publisherId") UUID publisherId, @Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PublisherMember pm " +
           "WHERE pm.publisher.id = :publisherId AND pm.user.id = :userId AND pm.role IN ('OWNER', 'ADMIN')")
    boolean isAdminOrOwner(@Param("publisherId") UUID publisherId, @Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PublisherMember pm " +
           "WHERE pm.publisher.id = :publisherId AND pm.user.id = :userId AND pm.role IN ('OWNER', 'ADMIN', 'PUBLISHER')")
    boolean canPublish(@Param("publisherId") UUID publisherId, @Param("userId") UUID userId);
}
