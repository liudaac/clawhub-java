package clawhub.repository;

import clawhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGithubId(Long githubId);

    Optional<User> findByHandle(String handle);

    boolean existsByHandle(String handle);

    boolean existsByGithubId(Long githubId);
}
