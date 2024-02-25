package com.example.conference_management_system.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.conference_management_system.AbstractIntegrationTest;

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
        //Getting the csrf token and the cookie of the current session for subsequent requests.
        EntityExchangeResult<byte[]> response = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody()
                .returnResult();

        String csrfToken = response.getResponseHeaders().getFirst("X-CSRF-TOKEN");
        /*
            The cookie in the response Header(SET_COOKIE) is in the form of
            SESSION=OTU2ODllODktYjZhMS00YmUxLTk1NGEtMDk0ZTBmODg0Mzhm; Path=/; HttpOnly; SameSite=Lax

            By splitting with ";" we get the first value which then we set it in the Cookie request header. The expected
            value is SESSION= plus some value.
         */
        String cookieHeader = response.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String sessionId = cookieHeader.split(";")[0];
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

        this.webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated();

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
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();
    }
}
