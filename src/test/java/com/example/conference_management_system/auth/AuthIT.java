package com.example.conference_management_system.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.conference_management_system.utils.WebUtils;
import com.example.conference_management_system.AbstractIntegrationTest;

import java.util.Map;

/*
    In a @SpringBootTest environment, each test typically runs in isolation with its own application context, which
    includes separate sessions and CSRF tokens for each test. Each test simulates its own client-server interaction,
    meaning a new session and CSRF token are created for each test case where they are required.

    The Csrf token is recommended by OWASP to be included as a request header

    https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#synchronizer-token-pattern
 */
@AutoConfigureWebTestClient
class AuthIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String AUTH_PATH = "/api/v1/auth";

    @Test
    void shouldLoginUser() {
        EntityExchangeResult<byte[]> response = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody()
                .returnResult();

        Map<String, String> csrf = WebUtils.getCsrfToken(response.getResponseHeaders());

        String requestBody = """
                {
                    "username": "username",
                    "password": "CyN549!@o2Cr",
                    "fullName": "TestUser",
                    "roleTypes": [
                        "ROLE_PC_CHAIR"
                    ]
                }
                """;

        response = this.webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", csrf.get("csrfCookie"))
                .header("X-XSRF-TOKEN", csrf.get("csrfToken"))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult();

        String sessionId = WebUtils.getSessionId(response.getResponseHeaders());

        requestBody = """
                {
                    "username": "username",
                    "password": "CyN549!@o2Cr"
                }
                """;

        this.webTestClient.post()
                .uri(AUTH_PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .header("Cookie", csrf.get("csrfCookie"))
                .header("X-XSRF-TOKEN", csrf.get("csrfToken"))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();
    }
}
