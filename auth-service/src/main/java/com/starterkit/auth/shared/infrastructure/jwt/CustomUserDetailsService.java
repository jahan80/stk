package com.starterkit.auth.shared.infrastructure.jwt;

import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsernameWithRole(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        // Reject users whose role has been soft-deleted.
        if (user.getRole() != null && user.getRole().getDeletedAt() != null) {
            throw new UsernameNotFoundException(
                    "User's role has been deleted: " + username);
        }

        return new UserPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + userId));

        // Reject users whose role has been soft-deleted.
        if (user.getRole() != null && user.getRole().getDeletedAt() != null) {
            throw new UsernameNotFoundException(
                    "User's role has been deleted: " + userId);
        }

        return new UserPrincipal(user);
    }
}
