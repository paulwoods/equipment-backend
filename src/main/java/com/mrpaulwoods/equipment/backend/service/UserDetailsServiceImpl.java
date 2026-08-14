package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        com.mrpaulwoods.equipment.backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return toUserDetails(user);
    }

    public UserDetails toUserDetails(com.mrpaulwoods.equipment.backend.entity.User user) {
        List<SimpleGrantedAuthority> authorities = user.getUserRoles().stream()
                .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRole().getName()))
                .toList();

        // Google-provisioned accounts have no password hash, and Spring's User rejects
        // a null one. The empty string can never match a bcrypt comparison, and
        // AuthService.login rejects these accounts before reaching the encoder anyway.
        return new User(
                user.getEmail(),
                user.getPassword() == null ? "" : user.getPassword(),
                authorities
        );
    }
}
