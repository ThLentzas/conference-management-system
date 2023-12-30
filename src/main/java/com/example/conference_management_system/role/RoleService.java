package com.example.conference_management_system.role;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
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

    public boolean assignRole(User user, RoleType roleType) {
        Role role = this.roleRepository.findByType(roleType).orElseThrow(() -> {
                    logger.error("Role was not found with type: {}", roleType);
                    return new ServerErrorException(SERVER_ERROR_MSG);
                });

        List<RoleType> roleTypes = user.getRoles().stream()
                .map(Role::getType)
                .toList();
        if (!roleTypes.contains(role.getType())) {
            user.getRoles().add(role);
            logger.info("User with id: {} was assigned a new role type: {}", user.getId(), role.getType());

            return true;
        }

        logger.info("User with id: {} already had role type: {}. No new role was assigned",
                user.getId(),
                role.getType());

        return false;
    }
}
