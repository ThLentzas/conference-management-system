package com.example.conference_management_system.user.mapper;

import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.dto.UserDTO;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;

public class UserDTOMapper implements Converter<User, UserDTO> {

    @Override
    public UserDTO convert(User user) {
        Set<RoleType> roleTypes = user.getRoles().stream()
                .map(Role::getType)
                .collect(Collectors.toSet());

        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roleTypes
        );
    }
}
