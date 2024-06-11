package com.example.conference_management_system.paper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import com.example.conference_management_system.AbstractIntegrationTest;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.dto.UserDTO;

import static org.assertj.core.api.Assertions.assertThat;

/*
    The Csrf token is recommended by OWASP to be included as a request header

    https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#synchronizer-token-pattern
 */
@AutoConfigureWebTestClient
class PaperIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String USER_PATH = "/api/v1/users";
    private static final String PAPER_PATH = "/api/v1/papers";

    @Test
    void shouldCreatePaper() throws IOException {
        //Getting the csrf token and the cookie of the current session for subsequent requests.
        EntityExchangeResult<byte[]> response = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody()
                .returnResult();

        String csrfTokenCookie = response.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String csrtTokenValue = csrfTokenCookie.split(";")[0];

        /*
            The cookie in the response Header(SET_COOKIE) is in the form of
            SESSION=OTU2ODllODktYjZhMS00YmUxLTk1NGEtMDk0ZTBmODg0Mzhm; Path=/; HttpOnly; SameSite=Lax

            By splitting with ";" we get the first value which then we set it in the Cookie request header. The expected
            value is SESSION= plus some value.
         */
        String requestBody = """
                {
                    "username": "username",
                    "password": "CyN549!@o2Cr",
                    "fullName": "fullName",
                    "roleTypes": [
                        "ROLE_PC_CHAIR"
                    ]
                }
                """;

        this.webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", csrfToken)
                .header("X-XSRF-TOKEN", csrfToken)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated();

        /*
            Each .part() call adds a part to the multipart request
            1st argument: name of the part. Must match the @RequestParam in the PaperController
            2nd argument: content
         */
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("title", "title");
        bodyBuilder.part("abstractText", "abstractText");
        bodyBuilder.part("authors", "author 1,author 2");
        bodyBuilder.part("keywords", "keyword 1,keyword 2");
        bodyBuilder.part("file", getFileContent("test.pdf")).filename("test.pdf");

        MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();

        response = webTestClient.post()
                .uri(PAPER_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .returnResult();

        String location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        Long paperId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        /*
            At this point the user was assigned a new Role(ROLE_AUTHOR) and the previous session is invalid, so we have
            to request a new csrf and a cookie to assert that

            1) The GET request to /papers/{id} returns the correct values
            2) The GET request to /papers/{id}/download returns the file(pdf/tex)
            3) The user now has a new Role
         */
        response = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody()
                .returnResult();

        csrfToken = response.getResponseHeaders().getFirst("X-CSRF-TOKEN");
        cookieHeader = response.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        sessionId = cookieHeader.split(";")[0];

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

        /*
            GET: /api/v1/papers/{id}

            In the below assertions, we see that the array of authors is of size 3 when the initial paper had only 2,
            that's because the user who made the request was added also as an author(full name).
         */
        this.webTestClient.get()
                .uri(PAPER_PATH + "/{id}", paperId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(paperId)
                .jsonPath("$.title").isEqualTo("title")
                .jsonPath("$.abstractText").isEqualTo("abstractText")
                .jsonPath("$.authors.length()").isEqualTo(3)
                .jsonPath("$.authors[0]").isEqualTo("author 1")
                .jsonPath("$.authors[1]").isEqualTo("author 2")
                .jsonPath("$.authors[2]").isEqualTo("fullName")
                .jsonPath("$.keywords.length()").isEqualTo(2)
                .jsonPath("$.keywords[0]").isEqualTo("keyword 1")
                .jsonPath("$.keywords[1]").isEqualTo("keyword 2");

        /*
            GET: /api/v1/papers/{id}/download
         */
        EntityExchangeResult<byte[]> file = webTestClient.get()
                .uri(PAPER_PATH + "/{id}/download", paperId)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .expectHeader().contentDisposition(ContentDisposition.attachment()
                        .filename("test.pdf")
                        .build())
                .expectBody(byte[].class)
                .returnResult();

        byte[] fileContent = file.getResponseBody();

        assertThat(fileContent).isEqualTo(getFileContent("test.pdf"));

        /*
            GET: /api/v1/user

            Asserting that the user has the new role of ROLE_AUTHOR after successfully creating a paper.
         */
        this.webTestClient.get()
                .uri(USER_PATH + "?name={name}", "fullName")
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectBody()
                /*
                    Since the object is serialized we check for String values and not RoleType that's the user property.
                 */
                .jsonPath("$.roleTypes").value(roleTypes -> assertThat((List<String>) roleTypes).contains(
                        RoleType.ROLE_AUTHOR.name()));
    }

    @Test
    void shouldUpdatePaper() throws IOException {
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
                    "fullName": "Full Name",
                    "roleTypes": [
                        "ROLE_AUTHOR"
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

        /*
            Each .part() call adds a part to the multipart request
            1st argument: name of the part. Must match the @RequestParam in the PaperController
            2nd argument: content
         */
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("title", "title");
        bodyBuilder.part("abstractText", "abstractText");
        bodyBuilder.part("authors", "author 1,author 2");
        bodyBuilder.part("keywords", "keyword 1,keyword 2");
        bodyBuilder.part("file", getFileContent("test.pdf")).filename("test.pdf");

        MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();

        response = webTestClient.post()
                .uri(PAPER_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .returnResult();

        String location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        Long paperId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        /*
            At this point in the shouldCreatePaper() test the user had a new role and their current session was
            invalidated, so we had to retrieve a new csrf token and a cookie. In this test the user initially had the
            role ROLE_AUTHOR, so we continue in the same session.
         */
        bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("title", "new title");
        bodyBuilder.part("abstractText", "new abstractText");
        bodyBuilder.part("keywords", "new keyword");
        bodyBuilder.part("file", getFileContent("test.tex")).filename("test.tex");

        multipartBody = bodyBuilder.build();

        this.webTestClient.put()
                .uri(PAPER_PATH + "/{id}", paperId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isNoContent();

        /*
            GET: /api/v1/papers/{id}

            In the below assertions, we see that the keywords now are 1 instead of 2 previously
         */
        this.webTestClient.get()
                .uri(PAPER_PATH + "/{id}", paperId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(paperId)
                .jsonPath("$.title").isEqualTo("new title")
                .jsonPath("$.abstractText").isEqualTo("new abstractText")
                .jsonPath("$.authors.length()").isEqualTo(3)
                .jsonPath("$.authors[0]").isEqualTo("author 1")
                .jsonPath("$.authors[1]").isEqualTo("author 2")
                .jsonPath("$.authors[2]").isEqualTo("Full Name")
                .jsonPath("$.keywords.length()").isEqualTo(1)
                .jsonPath("$.keywords[0]").isEqualTo("new keyword");

        /*
            GET: /api/v1/papers/{id}/download
         */
        EntityExchangeResult<byte[]> download = webTestClient.get()
                .uri(PAPER_PATH + "/{id}/download", paperId)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .expectHeader().contentDisposition(ContentDisposition.attachment()
                        .filename("test.tex")
                        .build())
                .expectBody(byte[].class)
                .returnResult();

        byte[] fileContent = download.getResponseBody();
        assertThat(fileContent).isEqualTo(getFileContent("test.tex"));
    }

    @Test
    void shouldAddCoAuthor() throws IOException {
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
                    "fullName": "fullName",
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


        response = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody()
                .returnResult();

        csrfToken = response.getResponseHeaders().getFirst("X-CSRF-TOKEN");
        cookieHeader = response.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        sessionId = cookieHeader.split(";")[0];

        requestBody = """
                {
                    "username": "new username",
                    "password": "CyN549!@o2Cr",
                    "fullName": "Test",
                    "roleTypes": [
                        "ROLE_AUTHOR"
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

        /*
            Each .part() call adds a part to the multipart request
            1st argument: name of the part. Must match the @RequestParam in the PaperController
            2nd argument: content
         */
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("title", "title");
        bodyBuilder.part("abstractText", "abstractText");
        bodyBuilder.part("authors", "author 1,author 2");
        bodyBuilder.part("keywords", "keyword 1,keyword 2");
        bodyBuilder.part("file", getFileContent("test.pdf")).filename("test.pdf");

        MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();

        response = webTestClient.post()
                .uri(PAPER_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .returnResult();

        String location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        Long paperId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        UserDTO user = this.webTestClient.get()
                .uri(USER_PATH + "?name={name}", "fullName")
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
                .uri(PAPER_PATH + "/{id}/author", paperId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        this.webTestClient.get()
                .uri(USER_PATH + "?name={name}", "fullName")
                .header("Cookie", sessionId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                /*
                    Since the object is serialized we check for String values and not RoleType that's the user property.
                 */
                .jsonPath("$.roleTypes").value(roleTypes -> assertThat((List<String>) roleTypes).contains(
                        RoleType.ROLE_AUTHOR.name()));
    }

    @Test
    void shouldFindPapers() throws IOException {

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
                    "fullName": "Full Name",
                    "roleTypes": [
                        "ROLE_AUTHOR"
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

        /*
            Each .part() call adds a part to the multipart request
            1st argument: name of the part. Must match the @RequestParam in the PaperController
            2nd argument: content
         */
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("title", "title");
        bodyBuilder.part("abstractText", "abstractText");
        bodyBuilder.part("authors", "author 1,author 2");
        bodyBuilder.part("keywords", "keyword 1,keyword 2");
        bodyBuilder.part("file", getFileContent("test.pdf")).filename("test.pdf");

        MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();

        response = webTestClient.post()
                .uri(PAPER_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .returnResult();
        String location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        Long paperId1 = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("title", "another title");
        bodyBuilder.part("abstractText", "another abstractText");
        bodyBuilder.part("authors", "author 1,author 2");
        bodyBuilder.part("keywords", "keyword 1,keyword 2");
        bodyBuilder.part("file", getFileContent("test.pdf")).filename("test.pdf");

        multipartBody = bodyBuilder.build();

        response = webTestClient.post()
                .uri(PAPER_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Cookie", sessionId)
                .header("X-CSRF-TOKEN", csrfToken)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .returnResult();
        location = response.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        Long paperId2 = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

         /*
            The order that the papers were inserted in the db is not guaranteed to be the same when fetching them
            from the db. In the code below we see how we would handle the cases where we dont know the exact order the
            papers are gonna be in the list.In our case the papers are sorted by their title so we can use jsonPath as
            show in the code below the comment

           List<AuthorPaperDTO> papers = this.webTestClient.get()
                .uri(PAPER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AuthorPaperDTO.class)
                .returnResult()
                .getResponseBody();

            assertThat(papers).hasSize(2)
                    .anyMatch(authorPaperDTO -> authorPaperDTO.getId().equals(paperId1)
                            && authorPaperDTO.getCreatedDate().equals(LocalDate.now())
                            && authorPaperDTO.getTitle().equals("title")
                            && authorPaperDTO.getAbstractText().equals("abstractText")
                            && authorPaperDTO.getAuthors()[0].equals("author 1")
                            && authorPaperDTO.getAuthors()[1].equals("author 2")
                            && authorPaperDTO.getAuthors()[2].equals("Full Name")
                            && authorPaperDTO.getKeywords()[0].equals("keyword 1")
                            && authorPaperDTO.getKeywords()[1].equals("keyword 2")
                            && authorPaperDTO.getState().equals(PaperState.CREATED)
                            && authorPaperDTO.getReviews().isEmpty())
                    .anyMatch(authorPaperDTO -> authorPaperDTO.getId().equals(paperId2)
                            && authorPaperDTO.getCreatedDate().equals(LocalDate.now())
                            && authorPaperDTO.getTitle().equals("another title")
                            && authorPaperDTO.getAbstractText().equals("another abstractText")
                            && authorPaperDTO.getAuthors()[0].equals("author 1")
                            && authorPaperDTO.getAuthors()[1].equals("author 2")
                            && authorPaperDTO.getAuthors()[2].equals("Full Name")
                            && authorPaperDTO.getKeywords()[0].equals("keyword 1")
                            && authorPaperDTO.getKeywords()[1].equals("keyword 2")
                            && authorPaperDTO.getState().equals(PaperState.CREATED)
                            && authorPaperDTO.getReviews().isEmpty()
                    );
         */
        this.webTestClient.get()
                .uri(PAPER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(paperId2)
                .jsonPath("$[0].createdDate").isEqualTo(LocalDate.now().toString())
                .jsonPath("$[0].title").isEqualTo("another title")
                .jsonPath("$[0].abstractText").isEqualTo("another abstractText")
                .jsonPath("$[0].authors[0]").isEqualTo("author 1")
                .jsonPath("$[0].authors[1]").isEqualTo("author 2")
                .jsonPath("$[0].authors[2]").isEqualTo("Full Name")
                .jsonPath("$[0].keywords[0]").isEqualTo("keyword 1")
                .jsonPath("$[0].keywords[1]").isEqualTo("keyword 2")
                .jsonPath("$[0].state").isEqualTo(PaperState.CREATED.toString())
                .jsonPath("$[0].reviews.length()").isEqualTo(0)
                .jsonPath("$[1].id").isEqualTo(paperId1)
                .jsonPath("$[1].createdDate").isEqualTo(LocalDate.now().toString())
                .jsonPath("$[1].title").isEqualTo("title")
                .jsonPath("$[1].abstractText").isEqualTo("abstractText")
                .jsonPath("$[1].authors[0]").isEqualTo("author 1")
                .jsonPath("$[1].authors[1]").isEqualTo("author 2")
                .jsonPath("$[1].authors[2]").isEqualTo("Full Name")
                .jsonPath("$[1].keywords[0]").isEqualTo("keyword 1")
                .jsonPath("$[1].keywords[1]").isEqualTo("keyword 2")
                .jsonPath("$[1].state").isEqualTo(PaperState.CREATED.toString())
                .jsonPath("$[1].reviews.length()").isEqualTo(0);
    }

    private byte[] getFileContent(String fileName) throws IOException {
        Path path = ResourceUtils.getFile("classpath:files/" + fileName).toPath();
        return Files.readAllBytes(path);
    }
}
