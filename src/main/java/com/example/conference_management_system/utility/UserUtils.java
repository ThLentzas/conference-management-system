package com.example.conference_management_system.utility;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import com.example.conference_management_system.security.SecurityUser;

public final class UserUtils {

    private UserUtils() {
        throw new UnsupportedOperationException("UserUtils is a utility class and cannot be instantiated");
    }

    /*
        Non-authenticated user will return a list of ROLE_ANONYMOUS
     */
    public static List<String> getCurrentUserAuthorities () {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public static Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return Optional.of(securityUser.user().getId());
        }

        return Optional.empty();
    }
}
