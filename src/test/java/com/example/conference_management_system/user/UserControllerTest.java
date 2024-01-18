package com.example.conference_management_system.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.conference_management_system.config.SecurityConfig;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.dto.UserDTO;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class
})
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    private static final String USER_PATH = "/api/v1/users";

    //findUserByName()
    @Test
    void shouldReturnUserDTOAnd200OnFindUserByName() throws Exception {
        String responseBody = """
                    {
                        "id": 1,
                        "username": "username",
                        "fullName": "fullName",
                        "roleTypes": [
                            "ROLE_AUTHOR"
                        ]
                    }
                """;

        when(this.userService.findUserByFullName("fullName"))
                .thenReturn(getUserDTO(1L, "username", "fullName", Set.of(RoleType.ROLE_AUTHOR)));

        this.mockMvc.perform(get(USER_PATH + "?name={name}", "fullName")
                .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }


    @Test
    void should404WhenUserIsNotFoundOnFindUserByName() throws Exception {
        String responseBody = String.format("""
                    {
                        "message": "User not found with name: %s"
                    }
                """, "fullName");

        when(this.userService.findUserByFullName("fullName"))
                .thenThrow(new ResourceNotFoundException("User not found with name: fullName"));

        this.mockMvc.perform(get(USER_PATH + "?name={name}", "fullName")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    private UserDTO getUserDTO(Long id, String username, String fullName, Set<RoleType> roleTypes) {
        return new UserDTO(
                id,
                username,
                fullName,
                roleTypes
        );
    }
}
