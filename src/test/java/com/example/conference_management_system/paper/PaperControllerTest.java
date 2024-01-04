package com.example.conference_management_system.paper;

import com.example.conference_management_system.conference.ConferenceState;
import com.example.conference_management_system.conference.dto.ReviewerAssignmentRequest;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.paper.dto.AuthorAdditionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ResourceUtils;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.conference_management_system.config.SecurityConfig;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.paper.dto.PaperCreateRequest;
import com.example.conference_management_system.paper.dto.PaperUpdateRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(PaperController.class)
@Import({
        SecurityConfig.class
})
class PaperControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PaperService paperService;
    private static final String PAPER_PATH = "/api/v1/papers";

    /*
        For the same reason that I didn't write tests for the validation in both create paper and update paper in the
        PaperService, same here as well.
     */
    @Test
    @WithMockUser(roles = "PC_MEMBER")
    void shouldReturnHTTP201WhenPaperIsCreatedSuccessfully() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );

        when(this.paperService.createPaper(
                any(PaperCreateRequest.class),
                any(Authentication.class),
                any(HttpServletRequest.class))).thenReturn(1L);

        this.mockMvc.perform(multipart(PAPER_PATH).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", containsString(PAPER_PATH + "/" + 1L))
                );
    }

    @Test
    @WithMockUser(roles = "PC_MEMBER")
    void shouldReturnHTTP409WhenCreatingPaperWithExistingTitle() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "A paper with the provided title already exists"
                }
                """;

        when(this.paperService.createPaper(
                any(PaperCreateRequest.class),
                any(Authentication.class),
                any(HttpServletRequest.class))).thenThrow(new DuplicateResourceException(
                "A paper with the provided title already exists"));

        this.mockMvc.perform(multipart(PAPER_PATH).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenCreatePaperIsCalledByUnauthenticatedUser() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(multipart(PAPER_PATH).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void shouldReturnHTTP403WhenCreatePaperIsCalledWithInvalidCsrfToken() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(multipart(PAPER_PATH).file(pdfFile).with(csrf().useInvalidToken())
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void shouldReturnHTTP403WhenCreatePaperIsCalledWithNoCsrfToken() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(multipart(PAPER_PATH).file(pdfFile)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    @WithMockUser(roles = "PC_MEMBER")
    void shouldReturnHTTP400WhenTitleIsBlankOnPaperCreate() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()

        );
        String responseBody = """
                {
                    "message": "You must provide the title of the paper"
                }
                """;

        when(this.paperService.createPaper(
                any(PaperCreateRequest.class),
                any(Authentication.class),
                any(HttpServletRequest.class))).thenThrow(new IllegalArgumentException(
                "You must provide the title of the paper")
        );

        this.mockMvc.perform(multipart(PAPER_PATH).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        })
                        .param("title", "")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP204WhenPaperIsUpdatedSuccessfully() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );

        doNothing().when(this.paperService).updatePaper(
                any(Long.class),
                any(PaperUpdateRequest.class),
                any(Authentication.class));

        this.mockMvc.perform(multipart(PAPER_PATH + "/{id}", 1L).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNoContent());

        verify(this.paperService, times(1)).updatePaper(
                any(Long.class),
                any(PaperUpdateRequest.class),
                any(Authentication.class));
    }

    /*
        Setting each argument to null in param() would not work. param("title", "null") => null as a String value
        param("title", null) => error values must not be empty.
     */
    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP400WhenAllValuesAreNullOnPaperUpdate() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "You must provide at least one property to update the paper"
                }
                """;

        doThrow(new IllegalArgumentException("You must provide at least one property to update the paper")).when(
                this.paperService).updatePaper(any(Long.class), any(PaperUpdateRequest.class), any(Authentication.class));

        this.mockMvc.perform(multipart(PAPER_PATH + "/{id}", 1L).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP404WhenPaperIsNotFoundOnPaperUpdate() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "Paper not found with id: 1"
                }
                """;

        doThrow(new ResourceNotFoundException("Paper not found with id: 1")).when(this.paperService).updatePaper(
                any(Long.class),
                any(PaperUpdateRequest.class),
                any(Authentication.class));

        this.mockMvc.perform(multipart(PAPER_PATH + "/{id}", 1L).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturn401WhenUpdatePaperIsCalledByUnauthenticatedUser() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(multipart(PAPER_PATH + "/{id}", 1L).file(pdfFile).with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void shouldReturnHTTP403WhenUpdatePaperIsCalledWithInvalidCsrfToken() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(multipart(PAPER_PATH + "/{id}", 1L).file(pdfFile).with(csrf().useInvalidToken())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void shouldReturnHTTP403WhenUpdatePaperIsCalledWithNoCsrfToken() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(multipart(PAPER_PATH + "/{id}", 1L).file(pdfFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "title")
                        .param("abstractText", "abstractText")
                        .param("authors", "author 1, author 2")
                        .param("keywords", "keyword 1, keyword 2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP204WhenCoAuthorIsAddedSuccessfully() throws Exception {
        String requestBody = """
                {
                    "id": 2
                }
                """;

        doNothing().when(this.paperService).addCoAuthor(
                any(Long.class),
                any(Authentication.class),
                any(AuthorAdditionRequest.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP404WhenPaperIsNotFoundOnAuthorAddition() throws Exception {
        String requestBody = """
                {
                    "id": 2
                }
                """;
        String responseBody = """
                {
                    "message": "Paper was not found with id: 1"
                }
                """;

        doThrow(new ResourceNotFoundException("Paper was not found with id: 1")).when(this.paperService).addCoAuthor(
                any(Long.class),
                any(Authentication.class),
                any(AuthorAdditionRequest.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP409WhenCoAuthorIsAlreadyAddedOnAuthorAddition() throws Exception {
        String requestBody = """
                {
                    "id": 2
                }
                """;
        String responseBody = """
                {
                    "message": "User with name: Test User is already an author for the paper with id: 1"
                }
                """;

        doThrow(new DuplicateResourceException(
                "User with name: Test User is already an author for the paper with id: 1")).when(this.paperService)
                .addCoAuthor(any(Long.class), any(Authentication.class), any(AuthorAdditionRequest.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );

    }

    @Test
    void shouldReturnHTTP401WhenAddCoAuthorIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "id": 2
                }
                """;
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void shouldReturnHTTP403WhenAddCoAuthorIsCalledWithInvalidCsrf() throws Exception {
        String requestBody = """
                {
                    "id": 2
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void shouldReturnHTTP403WhenAddCoAuthorIsCalledWithNoCsrf() throws Exception {
        String requestBody = """
                {
                    "id": 2
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    private byte[] getFileContent() throws IOException {
        Path pdfPath = ResourceUtils.getFile("classpath:files/test.pdf").toPath();

        return Files.readAllBytes(pdfPath);
    }
}
