package clawhub.security;

import clawhub.entity.User;
import clawhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByHandle(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = "ROLE_" + user.getRole().name();
        
        return new org.springframework.security.core.userdetails.User(
                user.getHandle(),
                "", // No password for OAuth2 users
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(java.util.UUID userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        String role = "ROLE_" + user.getRole().name();
        
        return new org.springframework.security.core.userdetails.User(
                user.getHandle(),
                "", // No password for OAuth2 users
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
