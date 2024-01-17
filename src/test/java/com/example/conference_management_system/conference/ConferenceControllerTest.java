package com.example.conference_management_system.conference;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.conference_management_system.config.SecurityConfig;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;
import com.example.conference_management_system.review.ReviewDecision;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.conference.dto.ConferenceUpdateRequest;
import com.example.conference_management_system.conference.dto.PCChairAdditionRequest;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.security.WithMockCustomUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

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
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should201WhenConferenceIsCreatedSuccessfully() throws Exception {
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;
        UUID conferenceId = UUID.randomUUID();
        when(this.conferenceService.createConference(
                any(ConferenceCreateRequest.class),
                any(SecurityUser.class),
                any(HttpServletRequest.class))).thenReturn(conferenceId);

        this.mockMvc.perform(post(CONFERENCE_PATH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", containsString(CONFERENCE_PATH + "/" + conferenceId))
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenCreatingConferenceWithExistingName() throws Exception {
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
                any(SecurityUser.class),
                any(HttpServletRequest.class))).thenThrow(new DuplicateResourceException("A conference with the" +
                " provided name already exists"));

        this.mockMvc.perform(post(CONFERENCE_PATH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenCreateConferenceIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenCreateConferenceIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenCreateConferenceIsCalledWithNoCsrfToken() throws Exception {
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

    //updateConference()
    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenConferenceIsUpdatedSuccessfully() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;

        doNothing().when(this.conferenceService).updateConference(
                eq(conferenceId),
                any(ConferenceUpdateRequest.class),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).updateConference(
                eq(conferenceId),
                any(ConferenceUpdateRequest.class),
                any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnUpdateConference() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "name": "name",
                    "description": "description"
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).updateConference(
                        eq(conferenceId),
                        any(ConferenceUpdateRequest.class),
                        any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should403WhenRequestingUserIsNotConferencePCChairOnUpdateConference() throws Exception {
        UUID conferenceId = UUID.randomUUID();
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

        doThrow(new AccessDeniedException("Access denied")).when(this.conferenceService).updateConference(
                eq(conferenceId),
                any(ConferenceUpdateRequest.class),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should400WhenConferenceUpdateRequestContainsOnlyInvalidValues() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "name": null,
                    "description": ""
                }
                """;
        String responseBody = """
                {
                    "message": "At least one valid property must be provided to update conference"
                }
                """;

        doThrow(new IllegalArgumentException("At least one valid property must be provided to update conference"))
                .when(this.conferenceService).updateConference(
                        eq(conferenceId),
                        any(ConferenceUpdateRequest.class),
                        any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenUpdatingConferenceWithExistingName() throws Exception {
        UUID conferenceId = UUID.randomUUID();
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

        doThrow(new DuplicateResourceException("A conference with the provided name already exists"))
                .when(this.conferenceService).updateConference(
                        eq(conferenceId),
                        any(ConferenceUpdateRequest.class),
                        any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenUpdateConferenceIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenUpdateConferenceIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void shouldReturnHTTP403WhenUpdateConferenceIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    //startSubmission()
    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenConferenceStartSubmissionIsSuccessful() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        doNothing().when(this.conferenceService).startSubmission(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", conferenceId).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startSubmission(eq(conferenceId), any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnStartSubmission() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).startSubmission(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should403WhenRequestingUserIsNotConferencePCChairOnStartSubmission() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        doThrow(new AccessDeniedException("Access denied"))
                .when(this.conferenceService).startSubmission(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInCreatedStateOnStartSubmission() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not start submission"
                }
                """, ConferenceState.ASSIGNMENT.name());

        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.ASSIGNMENT + " and can " +
                "not start submission")).when(this.conferenceService).startSubmission(
                eq(conferenceId),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/submission", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenStartSubmissionIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenStartSubmissionIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenStartSubmissionIsCalledWithNoCsrfToken() throws Exception {
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
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenStartAssignmentIsSuccessful() throws Exception {
        UUID conferenceId = UUID.randomUUID();

        doNothing().when(this.conferenceService).startAssignment(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", conferenceId).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startAssignment(eq(conferenceId), any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnStartAssignment() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).startAssignment(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should403WhenRequestingUserIsNotConferencePCChairOnStartAssignment() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        doThrow(new AccessDeniedException("Access denied"))
                .when(this.conferenceService).startAssignment(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInSubmissionStateOnStartAssignment() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not start assignment"
                }
                """, ConferenceState.ASSIGNMENT.name());

        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.ASSIGNMENT + " and can " +
                "not start assignment")).when(this.conferenceService).startAssignment(
                eq(conferenceId),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/assignment", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenStartAssignmentIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenStartAssignmentIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenStartAssignmentIsCalledWithNoCsrfToken() throws Exception {
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
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenConferenceStartReviewIsSuccessful() throws Exception {
        UUID conferenceId = UUID.randomUUID();

        doNothing().when(this.conferenceService).startReview(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", conferenceId).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startReview(eq(conferenceId), any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnStartReview() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).startReview(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should403WhenRequestingUserIsNotConferencePCChairOnStartReview() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        doThrow(new AccessDeniedException("Access denied"))
                .when(this.conferenceService).startReview(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInAssignmentStateOnStartReview() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not start review"
                }
                """, ConferenceState.CREATED);

        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.CREATED + " and can " +
                "not start review")).when(this.conferenceService).startReview(
                eq(conferenceId),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/review", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenStartReviewIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenStartReviewIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenStartReviewIsCalledWithNoCsrfToken() throws Exception {
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

    //startDecision
    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenConferenceStartDecisionIsSuccessful() throws Exception {
        UUID conferenceId = UUID.randomUUID();

        doNothing().when(this.conferenceService).startDecision(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/decision", conferenceId).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startDecision(eq(conferenceId), any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnStartDecision() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).startDecision(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/decision", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should403WhenRequestingUserIsNotConferencePCChairOnStartDecision() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        doThrow(new AccessDeniedException("Access denied")).when(this.conferenceService)
                .startDecision(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/decision", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInReviewStateOnStartDecision() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and can not start the approval or rejection of the submitted papers"
                }
                """, ConferenceState.CREATED);

        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.CREATED + " and can not " +
                "start the approval or rejection of the submitted papers"))
                .when(this.conferenceService).startDecision(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/decision", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenStartDecisionIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/decision", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void should403WhenStartDecisionIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/decision", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void should403WhenStartDecisionIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/decision", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    //startFinal()
    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenConferenceStartFinalIsSuccessful() throws Exception {
        UUID conferenceId = UUID.randomUUID();

        doNothing().when(this.conferenceService).startFinal(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/final", conferenceId).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).startFinal(eq(conferenceId), any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnStartFinal() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).startFinal(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/final", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should403WhenRequestingUserIsNotConferencePCChairOnStartFinal() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        doThrow(new AccessDeniedException("Access denied")).when(this.conferenceService)
                .startFinal(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/final", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInDecisionStateOnStartFinal() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and the papers can neither be accepted nor rejected"
                }
                """, ConferenceState.CREATED);

        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.CREATED + " and the " +
                "papers can neither be accepted nor rejected"))
                .when(this.conferenceService).startFinal(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/final", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenStartFinalIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/final", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void should403WhenStartFinalIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/final", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    @Test
    void should403WhenStartFinalSubmissionIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/final-submission", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(this.conferenceService);
    }

    //addPCChair()
    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenPCChairIsAddedSuccessfully() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 4
                }
                """;
        doNothing().when(this.conferenceService).addPCChair(
                eq(conferenceId),
                any(PCChairAdditionRequest.class),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/pc-chair", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).addPCChair(
                eq(conferenceId),
                any(PCChairAdditionRequest.class),
                any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnAddPCChair() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 4
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).addPCChair(
                        eq(conferenceId),
                        any(PCChairAdditionRequest.class),
                        any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/pc-chair", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenPCChairIsAlreadyAddedOnAddPCChair() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 4
                }
                """;
        String responseBody = String.format("""
                {
                    "message": "User with id: %d is already PCChair for conference with id: %s"
                }
                """, 4L, conferenceId);

        doThrow(new DuplicateResourceException("User with id: " + 4L + " is already PCChair for conference with id: " +
                conferenceId)).when(this.conferenceService).addPCChair(
                eq(conferenceId),
                any(PCChairAdditionRequest.class),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/pc-chair", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenAddPCChairIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/pc-chair]", UUID.randomUUID()).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );
    }

    @Test
    void should403WhenAddPCChairIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/pc-chair", UUID.randomUUID())
                        .with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    @Test
    void should403WhenAddPCChairIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{id}/pc-chair", UUID.randomUUID()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );
    }

    //submitPaper()
    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should204WhenPaperIsSubmittedSuccessfully() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = String.format("""
                {
                    "paperId": %d
                }
                """, 1L);
        doNothing().when(this.conferenceService).submitPaper(
                eq(conferenceId),
                any(PaperSubmissionRequest.class),
                any(SecurityUser.class));

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).submitPaper(
                eq(conferenceId),
                any(PaperSubmissionRequest.class),
                any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should404WhenConferenceIsNotFoundOnSubmitPaper() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = String.format("""
                {
                    "paperId": %d
                }
                """, 1L);
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).submitPaper(
                        eq(conferenceId),
                        any(PaperSubmissionRequest.class),
                        any(SecurityUser.class));

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_AUTHOR")
    void should409WhenConferenceIsNotInSubmissionStateOnSubmitPaper() throws Exception {
        UUID conferenceId = UUID.randomUUID();
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
                any(SecurityUser.class)
        );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{id}/papers", conferenceId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenSubmitPaperIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenSubmitPaperIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenSubmitPaperIsCalledWithNoCsrfToken() throws Exception {
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
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenReviewerIsAssignedSuccessfully() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        doNothing().when(this.conferenceService).assignReviewer(
                eq(conferenceId),
                eq(1L),
                any(ReviewerAssignmentRequest.class),
                any(SecurityUser.class)
        );

        this.mockMvc.perform(post(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/reviewer", conferenceId, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isNoContent()
                );

        verify(this.conferenceService, times(1)).assignReviewer(
                eq(conferenceId),
                eq(1L),
                any(ReviewerAssignmentRequest.class),
                any(SecurityUser.class)
        );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnReviewerAssignment() throws Exception {
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
        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId))
                .when(this.conferenceService).assignReviewer(
                        eq(conferenceId),
                        eq(1L),
                        any(ReviewerAssignmentRequest.class),
                        any(SecurityUser.class)
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
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInAssignmentStateOnReviewerAssignment() throws Exception {
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
                "can not assign reviewers")).when(this.conferenceService).assignReviewer(
                eq(conferenceId),
                eq(1L),
                any(ReviewerAssignmentRequest.class),
                any(SecurityUser.class)
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
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should400WhenUserToBeAssignedAsReviewerDoesNotHaveReviewerRole() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String requestBody = """
                {
                    "userId": 1
                }
                """;
        String responseBody = """
                {
                    "message": "User is not a reviewer with id: 1"
                }
                """;
        doThrow(new IllegalArgumentException("User is not a reviewer with id: 1")).when(this.conferenceService)
                .assignReviewer(
                        eq(conferenceId),
                        eq(1L),
                        any(ReviewerAssignmentRequest.class),
                        any(SecurityUser.class)
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
    void should401WhenAssignReviewerIsCalledByUnauthenticatedUser() throws Exception {
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
    void should403WhenAssignReviewerIsCalledWithInvalidCsrfToken() throws Exception {
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
    void should403WhenAssignReviewerIsCalledWithNoCsrfToken() throws Exception {
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

    //updatePaperApprovalStatus()
    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenPaperApprovalStatusIsUpdatedSuccessfully() throws Exception {
        UUID conferenceId = UUID.randomUUID();

        doNothing().when(this.conferenceService).updatePaperApprovalStatus(
                eq(conferenceId),
                eq(1L),
                any(ReviewDecision.class),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/{decision}",
                        conferenceId,
                        1L,
                        ReviewDecision.APPROVED).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).updatePaperApprovalStatus(
                eq(conferenceId),
                eq(1L),
                any(ReviewDecision.class),
                any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnUpdatePaperApprovalStatus() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId)).when(
                this.conferenceService).updatePaperApprovalStatus(
                eq(conferenceId),
                eq(1L),
                any(ReviewDecision.class),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/{decision}",
                        conferenceId,
                        1L,
                        ReviewDecision.APPROVED).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInDecisionStateOnUpdatePaperApprovalStatus() throws Exception {
        UUID conferenceId = UUID.randomUUID();

        String responseBody = String.format("""
                {
                    "message": "Conference is in the state: %s and the decision to either approve or reject the paper can not be made"
                }
                """, ConferenceState.CREATED);

        doThrow(new StateConflictException("Conference is in the state: " + ConferenceState.CREATED + " and " +
                "the decision to either approve or reject the paper can not be made")).when(
                this.conferenceService).updatePaperApprovalStatus(
                eq(conferenceId),
                eq(1L),
                any(ReviewDecision.class),
                any(SecurityUser.class));

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/{decision}",
                        conferenceId,
                        1L,
                        ReviewDecision.APPROVED).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenUpdatePaperApprovalStatusIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/{decision}",
                        UUID.randomUUID(),
                        1L,
                        ReviewDecision.APPROVED).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    @Test
    void should403WhenUpdatePaperApprovalStatusIsCalledWithInvalidCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/{decision}",
                        UUID.randomUUID(),
                        1L,
                        ReviewDecision.APPROVED).with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    @Test
    void should403WhenUpdatePaperApprovalStatusIsCalledWithNoCsrfToken() throws Exception {
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(put(CONFERENCE_PATH + "/{conferenceId}/papers/{paperId}/{decision}",
                        UUID.randomUUID(),
                        1L,
                        ReviewDecision.APPROVED).with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    @Test
    void shouldReturnConferenceAndHTTP200WhenConferenceIsFoundOnFindConferenceById() throws Exception {
        String responseBody = """
                {
                    "id": "d2a57950-aab4-4d27-8c53-69ad397229af",
                    "name": "name",
                    "description": "description",
                    "users": []
                }
                """;

        when(this.conferenceService.findConferenceById(any(UUID.class), any(SecurityContext.class)))
                .thenReturn(getConferenceDTO());

        this.mockMvc.perform(get(CONFERENCE_PATH + "/{id}", UUID.fromString("d2a57950-aab4-4d27-8c53-69ad397229af"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }

    @Test
    void should404WhenConferenceIsNotFoundOnFindConferenceById() throws Exception {
        String responseBody = """
                {
                    "message": "Conference not found with id: d2a57950-aab4-4d27-8c53-69ad397229af"
                }
                """;

        when(this.conferenceService.findConferenceById(any(UUID.class), any(SecurityContext.class)))
                .thenThrow(new ResourceNotFoundException(
                        "Conference not found with id: d2a57950-aab4-4d27-8c53-69ad397229af"));

        this.mockMvc.perform(get(CONFERENCE_PATH + "/{id}", UUID.fromString("d2a57950-aab4-4d27-8c53-69ad397229af"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnListOfConferencesAnd200WhenConferencesAreFoundOnFindConferences() throws Exception {
        String responseBody = """
                [
                    {
                        "id": "d2a57950-aab4-4d27-8c53-69ad397229af",
                        "name": "name",
                        "description": "description",
                        "users": []
                    }
                ]
                """;

        when(this.conferenceService.findConferences(any(String.class), any(String.class), any(SecurityContext.class)))
                .thenReturn(List.of(getConferenceDTO()));

        this.mockMvc.perform(get(CONFERENCE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnAnEmptyListAnd200WhenConferencesAreNotFoundOnFindConferences() throws Exception {
        String responseBody = """
                    []
                """;

        when(this.conferenceService.findConferences(any(String.class), any(String.class), any(SecurityContext.class)))
                .thenReturn(Collections.emptyList());

        this.mockMvc.perform(get(CONFERENCE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should204WhenConferenceIsDeletedSuccessfully() throws Exception {
        UUID conferenceId = UUID.randomUUID();

        doNothing().when(this.conferenceService).deleteConferenceById(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(delete(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf()))
                .andExpect(status().isNoContent());

        verify(this.conferenceService, times(1)).deleteConferenceById(eq(conferenceId), any(SecurityUser.class));
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should404WhenConferenceIsNotFoundOnDeleteConference() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference not found with id: %s"
                }
                """, conferenceId);

        doThrow(new ResourceNotFoundException("Conference not found with id: " + conferenceId)).when(this.conferenceService)
                .deleteConferenceById(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(delete(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockCustomUser(roles = "ROLE_PC_CHAIR")
    void should409WhenConferenceIsNotInCreatedStateOnDeleteConference() throws Exception {
        UUID conferenceId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "message": "Conference is in state: %s and can not be deleted"
                }
                """, ConferenceState.ASSIGNMENT);

        doThrow(new StateConflictException("Conference is in state: " + ConferenceState.ASSIGNMENT + " and can not " +
                "be deleted")).when(this.conferenceService)
                .deleteConferenceById(eq(conferenceId), any(SecurityUser.class));

        this.mockMvc.perform(delete(CONFERENCE_PATH + "/{id}", conferenceId).with(csrf()))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void should401WhenDeleteConferenceIsCalledByUnauthenticatedUser() throws Exception {
        UUID id = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Unauthorized"
                }
                """;

        this.mockMvc.perform(delete(CONFERENCE_PATH + "/{id}", id).with(csrf()))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    @Test
    void should403WhenDeleteConferenceIsCalledWithInvalidCsrfToken() throws Exception {
        UUID id = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(delete(CONFERENCE_PATH + "/{id}", id).with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    @Test
    void should403WhenDeleteConferenceIsCalledWithNoCsrfToken() throws Exception {
        UUID id = UUID.randomUUID();
        String responseBody = """
                {
                    "message": "Access denied"
                }
                """;

        this.mockMvc.perform(delete(CONFERENCE_PATH + "/{id}", id))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody)
                );

        verifyNoInteractions(conferenceService);
    }

    private ConferenceDTO getConferenceDTO() {
        return new ConferenceDTO(
                UUID.fromString("d2a57950-aab4-4d27-8c53-69ad397229af"),
                "name",
                "description",
                new HashSet<>());
    }
}
