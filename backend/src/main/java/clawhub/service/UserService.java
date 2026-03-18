package clawhub.service;

import clawhub.entity.User;
import clawhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByHandle(String handle) {
        return userRepository.findByHandle(handle);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByGithubId(Long githubId) {
        return userRepository.findByGithubId(githubId);
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User findOrCreateUser(Long githubId, String handle, String name, String avatarUrl, 
                                  String bio, Instant githubCreatedAt) {
        Optional<User> existingUser = userRepository.findByGithubId(githubId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update user info if changed
            boolean updated = false;
            
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
                updated = true;
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                updated = true;
            }
            if (bio != null && !bio.equals(user.getBio())) {
                user.setBio(bio);
                updated = true;
            }
            
            if (updated) {
                user = userRepository.save(user);
                log.info("Updated user: {}", handle);
            }
            
            return user;
        }
        
        // Create new user
        User newUser = User.builder()
                .githubId(githubId)
                .handle(handle)
                .name(name)
                .avatarUrl(avatarUrl)
                .bio(bio)
                .githubCreatedAt(githubCreatedAt)
                .role(User.Role.USER)
                .build();
        
        User savedUser = userRepository.save(newUser);
        log.info("Created new user: {}", handle);
        
        return savedUser;
    }

    @Transactional
    public User updateUser(UUID userId, String name, String bio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        if (name != null) {
            user.setName(name);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
        log.info("Deleted user: {}", userId);
    }

    @Transactional
    public User updateUserRole(UUID userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByHandle(String handle) {
        return userRepository.existsByHandle(handle);
    }
}
