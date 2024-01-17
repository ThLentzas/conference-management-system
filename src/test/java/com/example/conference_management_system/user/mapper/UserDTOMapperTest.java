package com.example.conference_management_system.user.mapper;

import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserDTOMapperTest {
    private UserDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new UserDTOMapper();
    }

    @Test
    void shouldMapUserToUserDTO() {
        User user = new User();
        user.setId(1L);
        user.setUsername("username");
        user.setFullName("full name");
        user.setRoles(Set.of(new Role(RoleType.ROLE_REVIEWER)));

        UserDTO expected = new UserDTO(1L, "username", "full name", Set.of(RoleType.ROLE_REVIEWER));

        UserDTO actual = this.underTest.apply(user);

        assertThat(actual).isEqualTo(expected);
    }
}
