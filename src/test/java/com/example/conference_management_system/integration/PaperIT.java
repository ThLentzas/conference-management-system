package com.example.conference_management_system.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.conference_management_system.AbstractIntegrationTest;

@AutoConfigureWebTestClient
class PaperIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String PAPER_PATH = "/api/v1/papers";

    /*
        Non-static @TempDir with @BeforeEach: The temporary directory is created for each test method, and the  system
        property is set each time. However, the Spring context has already been initialized before the first
        test method runs, then changing the system property in subsequent methods won't  reconfigure beans that are
        already created and configured, like our FileService which gets a value from the papers.directory property.
        The beans would continue using the value of papers.directory that was resolved when they were first created.
        The value at the time of the creation is still C://papers

        Static @TempDir with @BeforeAll: The temporary directory is created once before any tests run, and the system
        property is set before the Spring context initializes. That way all the beans get the correct property value
        right from the start.
     */
    @TempDir
    static Path tempDirectory;

    @BeforeAll
    static void setupTempDirectory() {
        System.setProperty("papers.directory", tempDirectory.toString());
    }

    /*
        We test the happy path with a pdf file.
     */
    @Test
    void shouldCreatePaper() throws IOException {
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
                        "PC_CHAIR"
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

        /*
            Each .part() call adds a part to the multipart request
            1st argument: name of the part. Must match the @RequestParam in the PaperController
            2nd argument: content
         */
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("title", "title");
        bodyBuilder.part("abstractText", "abstractText");
        bodyBuilder.part("authors", "author 1, author 2");
        bodyBuilder.part("keywords", "keyword 1, keyword 2");
        bodyBuilder.part("file", getFileContent()).filename("test.pdf");

        MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();

        webTestClient.post()
                .uri(PAPER_PATH + "?_csrf={csrf}", csrfToken)
                .header("Cookie", sessionId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location");
    }

    private byte[] getFileContent() throws IOException {
        Path pdfPath = ResourceUtils.getFile("classpath:files/test.pdf").toPath();
        return Files.readAllBytes(pdfPath);
    }
}
