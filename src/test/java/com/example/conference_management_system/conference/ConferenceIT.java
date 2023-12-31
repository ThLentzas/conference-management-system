package com.example.conference_management_system.conference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.conference_management_system.AbstractIntegrationTest;
import com.example.conference_management_system.role.RoleType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
class ConferenceIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String USER_PATH = "/api/v1/users";
    private static final String CONFERENCE_PATH = "/api/v1/conferences";

    @Test
    void shouldCreateConference() {
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
                    "fullName": "Full Name",
                    "roleTypes": [
                        "ROLE_AUTHOR"
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
                    "name": "name",
                    "description": "description"
                }
                """;

        EntityExchangeResult<byte[]> response = this.webTestClient.post()
                .uri(CONFERENCE_PATH + "?_csrf={csrf}", csrfToken)
                .header("Cookie", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .returnResult();

        String location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        UUID conferenceId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        /*
            At this point the user was assigned a new Role(ROLE_PC_CHAIR) and the previous session is invalid, so we
            have to request a new csrf and a cookie to assert that

            1) The GET request to /conferences/{id} returns the values of the newly created conference
            2) The user now has a new Role
         */
        result = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectBody(DefaultCsrfToken.class)
                .returnResult();

        csrfToken = result.getResponseBody().getToken();
        cookieHeader = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        sessionId = cookieHeader.split(";")[0];

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

        /*
            GET: /api/v1/conferences/{id}

            Since the user that made the request has the role PC_CHAIR for the requested conference, they also have
            access to the conference's state and papers
         */
        this.webTestClient.get()
                .uri(CONFERENCE_PATH + "/{id}", conferenceId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectBody()
                .jsonPath("$.id").isEqualTo(conferenceId.toString())
                .jsonPath("$.name").isEqualTo("name")
                .jsonPath("$.description").isEqualTo("description")
                .jsonPath("$.conferenceState").isEqualTo("CREATED")
                .jsonPath("$.users[0].username").isEqualTo("username")
                .jsonPath("$.users[0].fullName").isEqualTo("Full Name")
                .jsonPath("$.users[0].roles[0]").isEqualTo("ROLE_AUTHOR")
                .jsonPath("$.users[0].roles[1]").isEqualTo("ROLE_PC_CHAIR")
                .jsonPath("$.papers").isEmpty();

        /*
            GET: /api/v1/user

            Asserting that the user has the new role of ROLE_PC_CHAIR after successfully creating a conference.
         */
        this.webTestClient.get()
                .uri(USER_PATH + "?username={username}", "username")
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectBody()
                /*
                    Since the object is serialized we check for String values and not RoleType that's the user property.
                 */
                .jsonPath("$.roles").value(roles ->
                        assertThat((List<String>) roles).contains(RoleType.ROLE_PC_CHAIR.name()));

    }

    @Test
    void shouldStartSubmission() {
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
                    "fullName": "Full Name",
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
                    "name": "name",
                    "description": "description"
                }
                """;

        EntityExchangeResult<byte[]> response = this.webTestClient.post()
                .uri(CONFERENCE_PATH + "?_csrf={csrf}", csrfToken)
                .header("Cookie", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .returnResult();

        String location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        UUID conferenceId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        /*
            The user initially had the role PC_CHAIR, so we don't have to invalidate the session and request a new csrf
            token and cookie
         */
        this.webTestClient.put()
                .uri(CONFERENCE_PATH + "/{id}/start-submission?_csrf={csrf}", conferenceId, csrfToken)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isNoContent();

        /*
            GET: /api/v1/conferences/{id}

            We are only interested that the state of the Conference has changed to SUBMISSION
         */
        this.webTestClient.get()
                .uri(CONFERENCE_PATH + "/{id}", conferenceId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectBody()
                .jsonPath("$.conferenceState").isEqualTo("SUBMISSION");
    }
}
