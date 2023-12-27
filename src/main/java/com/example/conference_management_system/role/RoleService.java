package com.example.conference_management_system.role;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.exception.ServerErrorException;
import com.example.conference_management_system.security.SecurityUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later";

    public void assignRole(SecurityUser securityUser, RoleType roleType, HttpServletRequest request) {
        this.roleRepository.findByType(roleType).ifPresentOrElse(role -> {
            List<String> authorities = securityUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            /*
                If the user is assigned a new role, for example ROLE_AUTHOR it means that now they have access to AUTHOR
                endpoints in subsequent requests but making one request to an AUTHOR access endpoint would result in 403
                despite them having the role. The reason is the token/cookie was generated upon the user logging in/
                signing up and had the roles at that time. In order to give the user access to new endpoints either we
                invalidate the session or we revoke the jwt and we force to log in again.
             */
            if (!authorities.contains(role.getType().name())) {
                securityUser.user().getRoles().add(role);
                this.authService.invalidateSession(request);
            }
        }, () -> {
            logger.error("Role was not found with type: {}", roleType);
            throw new ServerErrorException(SERVER_ERROR_MSG);
        });
    }
}
