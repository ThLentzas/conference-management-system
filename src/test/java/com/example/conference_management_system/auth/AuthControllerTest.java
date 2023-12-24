package com.example.conference_management_system.auth;

import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.role.RoleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.conference_management_system.config.SecurityConfig;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.security.SecurityUser;

import java.util.Set;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class
})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AuthService authService;
    private static final String AUTH_PATH = "/api/v1/auth";

    @Test
    void shouldReturnHTTP201WhenUserIsRegisteredSuccessfully() throws Exception {
        String requestBody = """
                {
                    "username": "username",
                    "password": "password",
                    "fullName": "Test User"
                }
                """;
        Authentication authentication = getAuthentication();
        when(this.authService.registerUser(any(RegisterRequest.class))).thenReturn(authentication);

        this.mockMvc.perform(post(AUTH_PATH + "/signup").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterUsernameIsNullOrEmpty(String username) throws Exception {
        String usernameValue = username == null ? "null" : "\"" + username + "\"";
        String requestBody = String.format("""
                {
                    "username": %s,
                    "password": "password",
                    "fullName": "Test User"
                }
                """, usernameValue);
        String responseBody = """
                {
                    "message": "The username field is required"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterPasswordIsNullOrEmpty(String password) throws Exception{
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "username": "username",
                    "password": %s,
                    "fullName": "Test User"
                }
                """, passwordValue);
        String responseBody = """
                {
                    "message": "The password field is required"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterFullNameIsNullOrEmpty(String fullName) throws Exception {
        String fullNameValue = fullName == null ? "null" : "\"" + fullName + "\"";
        String requestBody = String.format("""
                {
                    "username": "username",
                    "password": "password",
                    "fullName": %s
                }
                """, fullNameValue);
        String responseBody = """
                {
                    "message": "The full name field is required"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenRegisterUserIsCalledWithNoCsrfToken() throws Exception{
        String requestBody = """
                {
                    "username": "username",
                    "password": "password",
                    "fullName": "Test User"
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenRegisterUserIsCalledWithInvalidCsrfToken() throws Exception{
        String requestBody = """
                {
                    "username": "username",
                    "password": "password",
                    "fullName": "Test User"
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/signup").with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP200WhenUserLogsInSuccessfully() throws Exception {
        String requestBody = """
                {
                    "username": "username",
                    "password": "password"
                }
                """;
        Authentication authentication = getAuthentication();
        when(this.authService.loginUser(any(LoginRequest.class))).thenReturn(authentication);

        this.mockMvc.perform(post(AUTH_PATH + "/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenLoginUsernameIsNullOrEmpty(String username) throws Exception {
        String usernameValue = username == null ? "null" : "\"" + username + "\"";
        String requestBody = String.format("""
                {
                    "username": %s,
                    "password": "password"
                }
                """, usernameValue);
        String responseBody = """
                {
                    "message": "The username field is required"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenLoginPasswordIsNullOrEmpty(String password) throws Exception{
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "username": "username",
                    "password": %s
                }
                """, passwordValue);
        String responseBody = """
                {
                    "message": "The password field is required"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenLoginUserIsCalledWithNoCsrfToken() throws Exception{
        String requestBody = """
                {
                    "username": "username",
                    "password": "password"
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenLoginUserIsCalledWithInvalidCsrfToken() throws Exception{
        String requestBody = """
                {
                    "username": "username",
                    "password": "password"
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/login").with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    private Authentication getAuthentication() {
        User user = new User("username", "password", "Test User", Set.of(new Role(RoleType.ROLE_PC_MEMBER)));
        SecurityUser securityUser = new SecurityUser(user);

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }
}
