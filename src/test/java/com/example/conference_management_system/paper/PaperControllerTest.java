package com.example.conference_management_system.paper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.conference_management_system.config.SecurityConfig;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ServerErrorException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.paper.dto.AuthorAdditionRequest;
import com.example.conference_management_system.paper.dto.PaperCreateRequest;
import com.example.conference_management_system.paper.dto.PaperDTO;
import com.example.conference_management_system.paper.dto.PaperFile;
import com.example.conference_management_system.paper.dto.PaperUpdateRequest;
import com.example.conference_management_system.review.dto.ReviewCreateRequest;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.security.WithMockCustomUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

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

    //createPaper()
    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should201WhenPaperIsCreatedSuccessfully() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );

        when(this.paperService.createPaper(
                any(PaperCreateRequest.class),
                any(SecurityUser.class),
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
                        status().isCreated()
                );

        verify(this.paperService, times(1)).createPaper(
                any(PaperCreateRequest.class),
                any(SecurityUser.class),
                any(HttpServletRequest.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should409WhenCreatingPaperWithExistingTitle() throws Exception {
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
                any(SecurityUser.class),
                any(HttpServletRequest.class))).thenThrow(new DuplicateResourceException("A paper with the provided" +
                " title already exists"));

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
    void should401WhenCreatePaperIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenCreatePaperIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenCreatePaperIsCalledWithNoCsrfToken() throws Exception {
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
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should400WhenTitleIsBlankOnPaperCreate() throws Exception {
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
                any(SecurityUser.class),
                any(HttpServletRequest.class))).thenThrow(new IllegalArgumentException("You must provide the title " +
                "of the paper"));

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

    //updatePaper()
    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should204WhenPaperIsUpdatedSuccessfully() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );

        doNothing().when(this.paperService).updatePaper(eq(1L), any(PaperUpdateRequest.class), any(SecurityUser.class));

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

        verify(this.paperService, times(1)).updatePaper(eq(1L), any(PaperUpdateRequest.class), any(SecurityUser.class));
    }

    /*
        Setting each argument to null in param() would not work. param("title", "null") => null as a String value
        param("title", null) => error values must not be empty.
     */
    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should400WhenAllValuesAreNullOnPaperUpdate() throws Exception {
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
                this.paperService).updatePaper(eq(1L), any(PaperUpdateRequest.class), any(SecurityUser.class));

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
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should404WhenPaperIsNotFoundOnPaperUpdate() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent()
        );
        String responseBody = String.format("""
                {
                    "message": "Paper not found with id: %d"
                }
                """, 1L);

        doThrow(new ResourceNotFoundException("Paper not found with id: " + 1L)).when(this.paperService).updatePaper(
                eq(1L),
                any(PaperUpdateRequest.class),
                any(SecurityUser.class));

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
    void should401WhenUpdatePaperIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenUpdatePaperIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenUpdatePaperIsCalledWithNoCsrfToken() throws Exception {
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

    //addCoAuthor()
    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should204WhenCoAuthorIsAddedSuccessfully() throws Exception {
        String requestBody = """
                {
                    "userId": 2
                }
                """;

        doNothing().when(this.paperService).addCoAuthor(
                eq(1L),
                any(AuthorAdditionRequest.class),
                any(SecurityUser.class)
        );

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should404WhenPaperIsNotFoundOnAuthorAddition() throws Exception {
        String requestBody = """
                {
                    "userId": 2
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Paper not found with id: %d"
                }
                """, 1L);

        doThrow(new ResourceNotFoundException("Paper not found with id: " + 1L)).when(this.paperService)
                .addCoAuthor(eq(1L), any(AuthorAdditionRequest.class), any(SecurityUser.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should409WhenCoAuthorIsAlreadyAddedOnAuthorAddition() throws Exception {
        String authorsName = "author";
        String requestBody = """
                {
                    "userId": 2
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "User with name: %s is already an author for the paper with id: %d"
                }
                """, authorsName, 1L);

        doThrow(new DuplicateResourceException(
                "User with name: " + authorsName + " is already an author for the paper with id: " + 1L))
                .when(this.paperService).addCoAuthor(
                        eq(1L),
                        any(AuthorAdditionRequest.class),
                        any(SecurityUser.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/author", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenAddCoAuthorIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "userId": 2
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
    void should403WhenAddCoAuthorIsCalledWithInvalidCsrf() throws Exception {
        String requestBody = """
                {
                    "userId": 2
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
    void should403WhenAddCoAuthorIsCalledWithNoCsrf() throws Exception {
        String requestBody = """
                {
                    "userId": 2
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

    //reviewPaper()
    @Test
    @WithMockCustomUser(roles = "ROLE_REVIEWER")
    void should201WhenPaperIsReviewedSuccessfully() throws Exception {
        String requestBody = """
                {
                    "comment": "comment",
                    "score": 5.9
                }
                """;

        when(this.paperService.reviewPaper(eq(1L), any(ReviewCreateRequest.class), any(SecurityUser.class)))
                .thenReturn(1L);

        this.mockMvc.perform(post(PAPER_PATH + "/{id}/reviews", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", containsString(PAPER_PATH + "/" + 1L + "/reviews/" + 1L))
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_REVIEWER")
    void should404WhenPaperIsNotFoundOnReviewPaper() throws Exception {
        String requestBody = """
                {
                    "comment": "comment",
                    "score": 5.9
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Paper not found with id: %d"
                }
                """, 1L);

        when(this.paperService.reviewPaper(eq(1L), any(ReviewCreateRequest.class), any(SecurityUser.class)))
                .thenThrow(new ResourceNotFoundException("Paper not found with id: " + 1L));

        this.mockMvc.perform(post(PAPER_PATH + "/{id}/reviews", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_REVIEWER")
    void should500WhenPaperIsNotInSubmittedToAnyConferenceOnReviewPaper() throws Exception {
        String requestBody = """
                {
                    "comment": "comment",
                    "score": 5.9
                }
                """;
        String responseBody = """
                {
                    "message": "The server encountered an internal error and was unable to complete your request. Please try again later"
                }
                """;

        when(this.paperService.reviewPaper(eq(1L), any(ReviewCreateRequest.class), any(SecurityUser.class)))
                .thenThrow(new ServerErrorException("The server encountered an internal error and was unable to " +
                        "complete your request. Please try again later"));

        this.mockMvc.perform(post(PAPER_PATH + "/{id}/reviews", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().is5xxServerError(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_REVIEWER")
    void should409WhenPaperIsNotInSubmittedStateOnReviewPaper() throws Exception {
        String requestBody = """
                {
                    "comment": "comment",
                    "score": 5.9
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Paper is in state: %s and can not be reviewed"
                }
                """, PaperState.CREATED);

        when(this.paperService.reviewPaper(eq(1L), any(ReviewCreateRequest.class), any(SecurityUser.class)))
                .thenThrow(new StateConflictException("Paper is in state: " + PaperState.CREATED + " and can not be " +
                        "reviewed"));

        this.mockMvc.perform(post(PAPER_PATH + "/{id}/reviews", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenReviewPaperIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "comment": "comment",
                    "score": 5.9
                }
                """;
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(post(PAPER_PATH + "/{id}/reviews", 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void should403WhenReviewPaperIsCalledWithInvalidCsrf() throws Exception {
        String requestBody = """
                {
                    "comment": "comment",
                    "score": 5.9
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(PAPER_PATH + "/{id}/reviews", 1L).with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void should403WhenReviewPaperIsCalledWithNoCsrf() throws Exception {
        String requestBody = """
                {
                    "comment": "comment",
                    "score": 5.9
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(PAPER_PATH + "/{id}/reviews", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should204WhenPaperIsWithdrawnSuccessfully() throws Exception {
        doNothing().when(this.paperService).withdrawPaper(eq(1L), any(SecurityUser.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/withdraw", 1L).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.paperService, times(1)).withdrawPaper(eq(1L), any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should404WhenPaperIsNotFoundOnWithdrawPaper() throws Exception {
        String responseBody = String.format("""
                {
                    "message": "Paper not found with id: %d"
                }
                """, 1L);

        doThrow(new ResourceNotFoundException("Paper not found with id: " + 1L)).when(this.paperService)
                .withdrawPaper(eq(1L), any(SecurityUser.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/withdraw", 1L).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should409WhenPaperIsNotSubmittedToAnyConferenceOnWithdrawPaper() throws Exception {
        String responseBody = """
                {
                    "message": "You can not withdraw a paper that has not been submitted to any conference"
                }
                """;

        doThrow(new StateConflictException("You can not withdraw a paper that has not been submitted to any " +
                "conference")).when(this.paperService).withdrawPaper(eq(1L), any(SecurityUser.class));

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/withdraw", 1L).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenWithdrawPaperIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/withdraw", 1L).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void should403WhenWithdrawPaperIsCalledWithInvalidCsrf() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/withdraw", 1L).with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    @Test
    void should403WhenWithdrawPaperIsCalledWithNoCsrf() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(PAPER_PATH + "/{id}/withdraw", 1L))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    //downloadPaper()
    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should200WhenDownloadPaperIsSuccessful() throws Exception {
        PaperFile paperFile = new PaperFile(new ByteArrayResource((getFileContent())), "test.pdf");

        when(this.paperService.downloadPaperFile(eq(1L), any(SecurityUser.class))).thenReturn(paperFile);

        MvcResult result = this.mockMvc.perform(get(PAPER_PATH + "/{id}/download", 1L)
                        .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_OCTET_STREAM),
                        header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(paperFile.file().getContentAsByteArray());
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should404WhenPaperIsNotFoundOnDownloadPaper() throws Exception {
        String responseBody = String.format("""
                {
                    "message": "Paper not found with id: %d"
                }
                """, 1L);

        when(this.paperService.downloadPaperFile(eq(1L), any(SecurityUser.class)))
                .thenThrow(new ResourceNotFoundException("Paper not found with id: " + 1L));

        this.mockMvc.perform(get(PAPER_PATH + "/{id}/download", 1L)
                        .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    @DisplayName("The requesting user is not AUTHOR or REVIEWER of the paper or PC_CHAIR at conference the paper has" +
            " been submitted to")
    void should403OnDownloadPaper() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;
        when(this.paperService.downloadPaperFile(eq(1L), any(SecurityUser.class)))
                .thenThrow(new AccessDeniedException("Access denied"));

        this.mockMvc.perform(get(PAPER_PATH + "/{id}/download", 1L)
                        .accept(MediaType.APPLICATION_OCTET_STREAM,
                                MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should500WhenContentIsNotFoundOnDownloadPaper() throws Exception {
        String responseBody = """
                {
                    "message": "The server encountered an internal error and was unable to complete your request. Please try again later"
                }
                """;

        when(this.paperService.downloadPaperFile(eq(1L), any(SecurityUser.class)))
                .thenThrow(new ServerErrorException("The server encountered an internal error and was unable to " +
                        "complete your request. Please try again later"));

        this.mockMvc.perform(get(PAPER_PATH + "/{id}/download", 1L)
                        .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().is5xxServerError(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenDownloadPaperIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(get(PAPER_PATH + "/{id}/download", 1L)
                        .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(paperService);
    }

    //findByPaperId()
    @Test
    void should200WhenPaperIsFoundOnFindPaperById() throws Exception {
        String responseBody = """
                {
                    "id": 4,
                    "createdDate": "2024-01-11",
                    "title": "title",
                    "abstractText": "abstract",
                    "authors": [
                        "author 1",
                        "author 2"
                    ],
                    "keywords": [
                        "keyword 1",
                        "keyword 2"
                    ]
                }
                """;

        when(this.paperService.findPaperById(eq(1L), any(SecurityContext.class))).thenReturn(getPapers()[0]);

        this.mockMvc.perform(get(PAPER_PATH + "/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }

    @Test
    void should404WhenPaperIsNotFoundOnFindPaperById() throws Exception {
        String responseBody = String.format("""
                {
                    "message": "Paper not found with id: %d"
                }
                """, 1L);

        when(this.paperService.findPaperById(eq(1L), any(SecurityContext.class)))
                .thenThrow(new ResourceNotFoundException("Paper not found with id: " + 1L));

        this.mockMvc.perform(get(PAPER_PATH + "/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    //findPapers()
    @Test
    void should200OnFindPapers() throws Exception {
        String responseBody = """
                [
                    {
                        "id": 4,
                        "createdDate": "2024-01-11",
                        "title": "title",
                        "abstractText": "abstract",
                        "authors": [
                            "author 1",
                            "author 2"
                        ],
                        "keywords": [
                            "keyword 1",
                            "keyword 2"
                        ]
                    }, {
                        "id": 2,
                        "createdDate": "2024-01-19",
                        "title": "another title",
                        "abstractText": "another abstract",
                        "authors": [
                            "author 1",
                            "author 2"
                        ],
                        "keywords": [
                            "keyword 1",
                            "keyword 2"
                        ]
                    }
                ]
                """;

        when(this.paperService.findPapers(
                any(String.class),
                any(String.class),
                any(String.class),
                any(SecurityContext.class))).thenReturn(Arrays.stream(getPapers()).toList());

        this.mockMvc.perform(get(PAPER_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }

    private byte[] getFileContent() throws IOException {
        Path pdfPath = ResourceUtils.getFile("classpath:files/test.pdf").toPath();

        return Files.readAllBytes(pdfPath);
    }

    private PaperDTO[] getPapers() {
        PaperDTO[] papers = new PaperDTO[2];
        String[] authors = {"author 1", "author 2"};
        String[] keywords = {"keyword 1", "keyword 2"};

        papers[0] = new PaperDTO(
                4L,
                LocalDate.parse("2024-01-11"),
                "title",
                "abstract",
                authors,
                keywords
        );

        papers[1] = new PaperDTO(
                2L,
                LocalDate.parse("2024-01-19"),
                "another title",
                "another abstract",
                authors,
                keywords
        );

        return papers;
    }
}
