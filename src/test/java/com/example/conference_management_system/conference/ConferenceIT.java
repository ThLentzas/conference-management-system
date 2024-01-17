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
import com.example.conference_management_system.user.dto.UserDTO;
import com.example.conference_management_system.conference.dto.ConferenceDTO;

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
                .jsonPath("$.users[0].roleTypes").value(roleTypes -> assertThat((List<String>) roleTypes).contains(
                        RoleType.ROLE_PC_CHAIR.name(), RoleType.ROLE_AUTHOR.name()))
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
                .jsonPath("$.roleTypes").value(roleTypes -> assertThat((List<String>) roleTypes).contains(
                        RoleType.ROLE_PC_CHAIR.name()));

    }

    @Test
    void shouldUpdateConference() {
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

        requestBody = """
                {
                    "name": "test name",
                    "description": "test description"
                }
                """;

        this.webTestClient.put()
                .uri(CONFERENCE_PATH + "/{id}?_csrf={csrf}", conferenceId, csrfToken)
                .header("Cookie", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

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
                .jsonPath("$.name").isEqualTo("test name")
                .jsonPath("$.description").isEqualTo("test description")
                .jsonPath("$.conferenceState").isEqualTo("CREATED")
                .jsonPath("$.users[0].username").isEqualTo("username")
                .jsonPath("$.users[0].fullName").isEqualTo("Full Name")
                .jsonPath("$.users[0].roleTypes").value(roleTypes -> assertThat((List<String>) roleTypes).contains(
                        RoleType.ROLE_PC_CHAIR.name()))
                .jsonPath("$.papers").isEmpty();
    }

    @Test
    void shouldAddPCChair() {
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
                    "fullName": "Full Name"
                }
                """;

        this.webTestClient.post()
                .uri(AUTH_PATH + "/signup?_csrf={csrf}", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated();

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
                    "username": "another username",
                    "password": "CyN549!@o2Cr",
                    "fullName": "Test",
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

        UserDTO user = this.webTestClient.get()
                .uri(USER_PATH + "?username={username}", "username")
                .header("Cookie", sessionId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDTO.class)
                .returnResult()
                .getResponseBody();

        requestBody = String.format("""
                {
                    "userId": %d
                }
                """, user.id());

        this.webTestClient.put()
                .uri(CONFERENCE_PATH + "/{id}/pc-chair?_csrf={csrf}", conferenceId, csrfToken)
                .header("Cookie", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        ConferenceDTO conference = this.webTestClient.get()
                .uri(CONFERENCE_PATH + "/{id}", conferenceId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectBody(ConferenceDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(conference.getUsers()).anyMatch(pcChair -> pcChair.username().equals("username")
                && pcChair.fullName().equals("Full Name")
                && pcChair.roleTypes().stream().anyMatch(roleType -> roleType.equals(RoleType.ROLE_PC_CHAIR)));
        assertThat(conference.getUsers()).anyMatch(pcChair -> pcChair.username().equals("another username")
                && pcChair.fullName().equals("Test")
                && pcChair.roleTypes().stream().anyMatch(roleType -> roleType.equals(RoleType.ROLE_PC_CHAIR)));
    }

    @Test
    void shouldFindConferences() {
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
        UUID conferenceId1 = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        requestBody = """
                {
                    "name": "another name",
                    "description": "another description"
                }
                """;

        response = this.webTestClient.post()
                .uri(CONFERENCE_PATH + "?_csrf={csrf}", csrfToken)
                .header("Cookie", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .returnResult();

        location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        UUID conferenceId2 = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

    /*
            The order that the conferences were inserted in the db is not guaranteed to be the same when fetching them
            from the db. In the code below we see how we would handle the cases where we don't know the exact order of
            the conferences in the list.In our case the conferences are sorted by their name, so we can use jsonPath as
            show in the code below the comment

        List<PCChairConferenceDTO> conferences = this.webTestClient.get()
                .uri(CONFERENCE_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectBodyList(PCChairConferenceDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(conferences).hasSize(2)
                .anyMatch(conferenceDTO -> conferenceDTO.getId().equals(conferenceId1)
                        && conferenceDTO.getName().equals("name")
                        && conferenceDTO.getDescription().equals("description")
                        && conferenceDTO.getConferenceState().equals(ConferenceState.CREATED)
                        && conferenceDTO.getUsers().stream().anyMatch(userDTO ->
                        userDTO.username().equals("username")
                                && userDTO.fullName().equals("Full Name")
                                && userDTO.roleTypes().contains(RoleType.ROLE_PC_CHAIR))
                        && conferenceDTO.getPapers().isEmpty())
                .anyMatch(conferenceDTO -> conferenceDTO.getId().equals(conferenceId2)
                        && conferenceDTO.getName().equals("another name")
                        && conferenceDTO.getDescription().equals("another description")
                        && conferenceDTO.getConferenceState().equals(ConferenceState.CREATED)
                        && conferenceDTO.getUsers().stream().anyMatch(userDTO ->
                        userDTO.username().equals("username")
                                && userDTO.fullName().equals("Full Name")
                                && userDTO.roleTypes().contains(RoleType.ROLE_PC_CHAIR))
                        && conferenceDTO.getPapers().isEmpty());
    */

        this.webTestClient.get()
                .uri(CONFERENCE_PATH) // Replace with your actual endpoint
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId) // If required for your context
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(conferenceId2.toString())
                .jsonPath("$[0].name").isEqualTo("another name")
                .jsonPath("$[0].description").isEqualTo("another description")
                .jsonPath("$[0].conferenceState").isEqualTo(ConferenceState.CREATED.toString())
                .jsonPath("$[0].users[0].username").isEqualTo("username")
                .jsonPath("$[0].users[0].fullName").isEqualTo("Full Name")
                .jsonPath("$[0].users[0].roleTypes").value(roleTypes -> assertThat((List<String>) roleTypes).contains(
                        RoleType.ROLE_PC_CHAIR.name()))
                .jsonPath("$[0].papers.length()").isEqualTo(0)
                .jsonPath("$[1].id").isEqualTo(conferenceId1.toString())
                .jsonPath("$[1].name").isEqualTo("name")
                .jsonPath("$[1].description").isEqualTo("description")
                .jsonPath("$[1].conferenceState").isEqualTo(ConferenceState.CREATED.toString())
                .jsonPath("$[1].users[0].fullName").isEqualTo("Full Name")
                .jsonPath("$[1].users[0].roleTypes").value(roleTypes -> assertThat((List<String>) roleTypes).contains(
                        RoleType.ROLE_PC_CHAIR.name()))
                .jsonPath("$[1].papers.length()").isEqualTo(0);
    }

    @Test
    void shouldDeleteConference() {
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

        this.webTestClient.delete()
                .uri(CONFERENCE_PATH + "/{id}?_csrf={csrf}", conferenceId, csrfToken)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isNoContent();

        this.webTestClient.get()
                .uri(CONFERENCE_PATH + "/{id}", conferenceId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isNotFound();
    }
}
