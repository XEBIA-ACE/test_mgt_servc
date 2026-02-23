package com.example.usermanagement.security;

import com.example.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads user details by username or email during authentication.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Support login with either username or email
        return userRepository.findByEmail(usernameOrEmail)
            .or(() -> userRepository.findByUsername(usernameOrEmail))
            .map(UserPrincipal::of)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + usernameOrEmail));
    }
}
