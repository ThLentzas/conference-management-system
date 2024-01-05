package com.example.conference_management_system.conference;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import com.example.conference_management_system.config.SecurityConfig;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

@WebMvcTest(ConferenceController.class)
@Import({
        SecurityConfig.class
})
class ConferenceControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ConferenceService conferenceService;
    private static final String CONFERENCE_PATH = "/api/v1/conferences";

    //createConference()
    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP201WhenConferenceIsCreatedSuccessfully() throws Exception {
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;
        UUID id = UUID.randomUUID();
        when(this.conferenceService.createConference(
                any(ConferenceCreateRequest.class),
                any(Authentication.class),
                any(HttpServletRequest.class))).thenReturn(id);

        this.mockMvc.perform(post(CONFERENCE_PATH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", containsString(CONFERENCE_PATH + "/" + id))
                );
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP409WhenCreatingConferenceWithExistingName() throws Exception {
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;
        String responseBody = """
                {
                    "message": "A conference with the provided name already exists"
                }
                """;

        when(this.conferenceService.createConference(
                any(ConferenceCreateRequest.class),
                any(Authentication.class),
                any(HttpServletRequest.class))).thenThrow(new DuplicateResourceException("A conference with the provided" +
                " name already exists"));

        this.mockMvc.perform(post(CONFERENCE_PATH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenCreateConferenceIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenCreateConferenceIsCalledWithInvalidCsrfToken() throws Exception {
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH).with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenCreateConferenceIsCalledWithNoCsrfToken() throws Exception {
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH).with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    //startSubmission()
    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP204WhenConferenceStartSubmissionIsSuccessful() throws Exception {
        doNothing().when(this.conferenceService).startSubmission(any(UUID.class), any(Authentication.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startSubmission(any(UUID.class), any(Authentication.class));
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP404WhenConferenceIsNotFoundOnStartSubmission() throws Exception {
        UUID id = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, id);
        doThrow(new ResourceNotFoundException("Conference not found with id: " + id)).when(this.conferenceService)
                .startSubmission(any(UUID.class), any(Authentication.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", id).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP409WhenConferenceIsNotInCreatedStateOnStartSubmission() throws Exception {
        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.ASSIGNMENT.name() + " " +
                "and can not start submission")).when(this.conferenceService).startSubmission(
                any(UUID.class),
                any(Authentication.class));

        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not start submission"
                }
                """, ConferenceState.ASSIGNMENT.name());

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenStartSubmissionIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenStartSubmissionIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenStartSubmissionIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    //startAssignment()
    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP204WhenStartAssignmentIsSuccessful() throws Exception {
        doNothing().when(this.conferenceService).startAssignment(any(UUID.class), any(Authentication.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startAssignment(any(UUID.class), any(Authentication.class));
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP404WhenConferenceIsNotFoundOnStartAssignment() throws Exception {
        UUID id = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, id);
        doThrow(new ResourceNotFoundException("Conference not found with id: " + id)).when(this.conferenceService)
                .startAssignment(any(UUID.class), any(Authentication.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", id).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP409WhenConferenceIsNotInSubmissionStateOnStartAssignment() throws Exception {
        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.DECISION.name() + " " +
                "and can not start submission")).when(this.conferenceService).startAssignment(
                any(UUID.class),
                any(Authentication.class));

        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not start submission"
                }
                """, ConferenceState.DECISION.name());

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenStartAssignmentIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenStartAssignmentIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenStartAssignmentIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }


    //startReview()
    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP204WhenConferenceStartReviewIsSuccessful() throws Exception {
        doNothing().when(this.conferenceService).startSubmission(any(UUID.class), any(Authentication.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startReview(any(UUID.class), any(Authentication.class));
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP404WhenConferenceIsNotFoundOnStartReview() throws Exception {
        UUID id = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, id);
        doThrow(new ResourceNotFoundException("Conference not found with id: " + id)).when(this.conferenceService)
                .startReview(any(UUID.class), any(Authentication.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", id).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP409WhenConferenceIsNotInAssignmentStateOnStartReview() throws Exception {
        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.DECISION.name() + " " +
                "and can not start reviews")).when(this.conferenceService).startReview(
                any(UUID.class),
                any(Authentication.class));

        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not start reviews"
                }
                """, ConferenceState.DECISION.name());

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenStartReviewIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenStartReviewIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenStartReviewIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    //submitPaper()
    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP204WhenPaperIsSubmittedSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        String requestBody = """
                {
                    "paperId": 1
                }
                """;
        doNothing().when(this.conferenceService).submitPaper(
                any(UUID.class),
                any(PaperSubmissionRequest.class),
                any(Authentication.class)
        );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).submitPaper(
                any(UUID.class),
                any(PaperSubmissionRequest.class),
                any(Authentication.class));
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP404WhenConferenceIsNotFoundOnSubmitPaper() throws Exception {
        UUID id = UUID.randomUUID();
        String requestBody = """
                {
                    "paperId": 1
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, id);
        doThrow(new ResourceNotFoundException("Conference not found with id: " + id)).when(this.conferenceService)
                .submitPaper(
                        any(UUID.class),
                        any(PaperSubmissionRequest.class),
                        any(Authentication.class)
                );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnHTTP409WhenConferenceIsNotInSubmissionStateOnSubmitPaper() throws Exception {
        UUID id = UUID.randomUUID();
        String requestBody = """
                {
                    "paperId": 1
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and you can not submit papers"
                }
                """, ConferenceState.ASSIGNMENT.name());
        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.ASSIGNMENT.name() + " " +
                "and you can not submit papers")).when(this.conferenceService).submitPaper(
                any(UUID.class),
                any(PaperSubmissionRequest.class),
                any(Authentication.class)
        );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenSubmitPaperIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers]", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenSubmitPaperIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenSubmitPaperIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/papers", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    //assignReviewer()
    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP204WhenReviewerIsAssignedSuccessfully() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        doNothing().when(this.conferenceService)
                .assignReviewer(
                        any(UUID.class),
                        any(Long.class),
                        any(ReviewerAssignmentRequest.class),
                        any(Authentication.class)
                );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", conferenceId, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNoContent()
                );

        verify(this.conferenceService, times(1)).assignReviewer(
                any(UUID.class),
                any(Long.class),
                any(ReviewerAssignmentRequest.class),
                any(Authentication.class)
        );
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP404WhenConferenceIsNotFoundOnReviewerAssignment() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);
        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId)).when(this.conferenceService)
                .assignReviewer(
                        any(UUID.class),
                        any(Long.class),
                        any(ReviewerAssignmentRequest.class),
                        any(Authentication.class)
                );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", conferenceId, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP409WhenConferenceIsNotInAssignmentStateOnReviewerAssignment() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not assign reviewers"
                }
                """, ConferenceState.DECISION.name());
        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.DECISION.name() + " and " +
                "can not assign reviewers")).when(this.conferenceService)
                .assignReviewer(
                        any(UUID.class),
                        any(Long.class),
                        any(ReviewerAssignmentRequest.class),
                        any(Authentication.class)
                );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", conferenceId, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(roles = "PC_CHAIR")
    void shouldReturnHTTP400WhenUserToBeAssignedAsReviewerDoesNotHaveReviewerRole() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "User is not a reviewer with id: %d"
                }
                """, 1L);
        doThrow(new IllegalArgumentException("User is not a reviewer with id: 1")).when(this.conferenceService)
                .assignReviewer(
                        any(UUID.class),
                        any(Long.class),
                        any(ReviewerAssignmentRequest.class),
                        any(Authentication.class)
                );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", conferenceId, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenAssignReviewerIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", UUID.randomUUID(), 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenAssignReviewerIsCalledWithInvalidCsrfToken() throws Exception {
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", UUID.randomUUID(), 1L)
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP403WhenAssignReviewerIsCalledWithNoCsrfToken() throws Exception {
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", UUID.randomUUID(), 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }
}
