package com.example.conference_management_system.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.conference_management_system.AbstractIntegrationTest;

/*
    In a @SpringBootTest environment, each test typically runs in isolation with its own application context, which
    includes separate sessions and CSRF tokens for each test. Each test simulates its own client-server interaction,
    meaning a new session and CSRF token are created for each test case where they are required.
 */
@AutoConfigureWebTestClient
class AuthIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String AUTH_PATH = "/api/v1/auth";

    @Test
    void shouldLoginUser() {
        /*
            Getting the csrf token and the cookie of the current session for subsequent requests.

            The CsrfToken is an interface, and we can not specify it as EntityExchangeResult<CsrfToken>. It would result
            in a deserialization error. The default implementation of that interface is the DefaultCsrfToken, so we
            specify that instead.
         */
        EntityExchangeResult<DefaultCsrfToken> result = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectBody(DefaultCsrfToken.class)
                .returnResult();

        String csrfToken = result.getResponseBody().getToken();
        /*
            The cookie in the response Header(SET_COOKIE) is in the form of
            SESSION=OTU2ODllODktYjZhMS00YmUxLTk1NGEtMDk0ZTBmODg0Mzhm; Path=/; HttpOnly; SameSite=Lax

            By splitting with ";" we get the first value which then we set it in the Cookie request header. The expected
            value is SESSION= plus some value.
         */
        String cookieHeader = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
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
                .uri(AUTH_PATH + "/signup?_csrf={csrf}", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
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
                .uri(AUTH_PATH + "/login?_csrf={csrf}", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();
    }
}
