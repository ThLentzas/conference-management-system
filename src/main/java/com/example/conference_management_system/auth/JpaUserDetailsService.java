package com.example.conference_management_system.auth;

import com.example.conference_management_system.security.SecurityUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.conference_management_system.user.UserRepository;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return this.userRepository.findUserByUsernameFetchingRoles(username)
                .map(SecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Username or password is incorrect"));
    }
}