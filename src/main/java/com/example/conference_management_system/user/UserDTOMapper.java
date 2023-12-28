package com.example.conference_management_system.user;

import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.role.RoleType;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class UserDTOMapper implements Function<User, UserDTO> {

    @Override
    public UserDTO apply(User user) {
        Set<RoleType> roles = user.getRoles().stream()
                .map(Role::getType)
                .collect(Collectors.toSet());

        return new UserDTO(
                user.getUsername(),
                user.getFullName(),
                roles
        );
    }
}
