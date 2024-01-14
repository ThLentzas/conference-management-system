package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.dto.*;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.PaperUserRepository;
import com.example.conference_management_system.entity.*;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.paper.PaperRepository;
import com.example.conference_management_system.review.ReviewDecision;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;
import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ConferenceServiceTest {
    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceUserRepository conferenceUserRepository;
    @Mock
    private PaperUserRepository paperUserRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private AuthService authService;
    private ConferenceService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new ConferenceService(
                conferenceRepository,
                conferenceUserRepository,
                paperUserRepository,
                userRepository,
                paperRepository,
                reviewRepository,
                roleService,
                authService
        );
    }

    //createConference()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameExceedsMaxLengthOnCreateConference() {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        Authentication authentication = getAuthentication();
        ConferenceCreateRequest conferenceCreateRequest = new ConferenceCreateRequest(
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(50) + 51),
                "description"
        );

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createConference(
                conferenceCreateRequest,
                authentication,
                httpServletRequest)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conference name must not exceed 50 characters");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionOnCreateWhenNameExistsOnCreateConference() {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        Authentication authentication = getAuthentication();
        ConferenceCreateRequest conferenceCreateRequest = new ConferenceCreateRequest("name", "description");

        when(this.conferenceRepository.existsByNameIgnoringCase(conferenceCreateRequest.name())).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createConference(
                conferenceCreateRequest,
                authentication,
                httpServletRequest)).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A conference with the provided name already exists");
    }

    //updateConference()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnUpdateConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest("name", "description");
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(any(UUID.class)))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId, conferenceUpdateRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnUpdateConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest("name", "description");
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(
                conferenceId,
                conferenceUpdateRequest,
                authentication)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameExceedsMaxLengthOnUpdateConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(50) + 51),
                "description");
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(
                conferenceId,
                conferenceUpdateRequest,
                authentication)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conference name must not exceed 50 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenNameIsNullOrEmptyAndDescriptionIsNull(String name) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(name, null);
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                authentication)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenNameIsNullOrEmptyAndDescriptionIsBlank(String name) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(name, "");
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                authentication)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenDescriptionIsNullOrEmptyAndNameIsNull(String description) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(null, description);
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                authentication)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenDescriptionIsNullOrEmptyAndNameIsEmpty(String description) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest("", description);
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                authentication)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    //startSubmission()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartSubmission() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conferenceId, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartSubmission() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conferenceId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInCreatedStateOnStartSubmission() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conferenceId, authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start " +
                        "submission");
    }

    //startAssignment
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conferenceId, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conferenceId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInSubmissionStateOnStartAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conferenceId, authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start " +
                        "assignment");
    }

    //startReview()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartReview() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conferenceId, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartReview() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conferenceId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInAssignmentStateOnStartReview() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conferenceId, authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start review");
    }

    //startDecision()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartDecision() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startDecision(conferenceId, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartDecision() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startDecision(conferenceId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInReviewStateOnStartDecision() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startDecision(conferenceId, authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start the " +
                        "approval or rejection of the submitted papers");
    }


    //startFinal()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePcChairOnStartFinal() {
        //Arrange
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startFinal(UUID.randomUUID(), authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartFinal() {
        //Arrange
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startFinal(id, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInDecisionStateOnStartFinal() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startFinal(conference.getId(), authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and the approved " +
                        "papers final submission is not allowed");
    }

    //addPCChair()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnAddPCChair() {
        //Arrange
        Authentication authentication = getAuthentication();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(UUID.randomUUID(), pcChairAdditionRequest, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnAddPCChair() {
        //Arrange
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(id, pcChairAdditionRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPCChairToAddIsNotFound() {
        //Arrange
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conference.getId(), pcChairAdditionRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + 1L + " to be added as PCChair");
    }

    /*
        this.conferenceUserRepository.existsByConferenceIdAndUserId() gets invoked twice. First to see if the user that
        made the request is PCChair of the conference and second to see if the to be added as PCChair is already PCChair
        of the conference, so we have to stab twice
    */
    @Test
    void shouldThrowDuplicateResourceExceptionWhenPCChairIsAlreadyAdded() {
        //Arrange
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);
        User user = getUser(2L, Set.of(new Role(RoleType.ROLE_PC_CHAIR)));

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true)
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.of(user));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conference.getId(), pcChairAdditionRequest, authentication))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with id: " + user.getId() + " is already PCChair for conference with id: " +
                        conference.getId());

        verify(this.conferenceUserRepository, times(2)).existsByConferenceIdAndUserId(any(UUID.class), any(Long.class));
    }

    /*
        this.conferenceUserRepository.existsByConferenceIdAndUserId() gets invoked twice. First to see if the user that
        made the request is PCChair of the conference and second to see if the to be added as PCChair is already PCChair
        of the conference, so we have to stab twice
     */
    @Test
    void shouldThrowDuplicateResourceExceptionWhenRequestingUserAddsSelfAsPCChair() {
        //Arrange
        Authentication authentication = getAuthentication();
        Conference conference = getConference();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true)
                .thenReturn(false);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.of(securityUser.user()));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conference.getId(), pcChairAdditionRequest, authentication))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with id: " + securityUser.user().getId() + " is already PCChair for conference " +
                        "with id: " + conference.getId());

        verify(this.conferenceUserRepository, times(2)).existsByConferenceIdAndUserId(any(UUID.class), any(Long.class));
    }

    //submitPaper()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotPaperAuthorOnSubmitPaper() {
        //Arrange
        Authentication authentication = getAuthentication();
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(UUID.randomUUID(), paperSubmissionRequest, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnSubmitPaper() {
        //Arrange
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(id, paperSubmissionRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInSubmissionStateOnSubmitPaper() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conference.getId(), paperSubmissionRequest, authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and you can not submit " +
                        "papers");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnSubmitPaper() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.SUBMISSION);
        Authentication authentication = getAuthentication();
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conference.getId(), paperSubmissionRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + paperSubmissionRequest.paperId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInCreatedStateOnSubmitPaper() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.SUBMISSION);
        Authentication authentication = getAuthentication();
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);
        Paper paper = getPaper();
        paper.setState(PaperState.REVIEWED);

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conference.getId(), paperSubmissionRequest, authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Paper is in state: " + paper.getState() + " and can not be submitted");
    }

    //assignReviewer()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePcChairOnReviewerAssignment() {
        //Arrange
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                UUID.randomUUID(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInAssignmentStateOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and reviewers can not " +
                        "be assigned");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: 1");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperDoesNotBelongToAnyConferenceOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperBelongsToDifferentConferenceOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        Conference differentConference = getConference();
        differentConference.setId(UUID.randomUUID());
        paper.setConference(differentConference);
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInSubmittedStateOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        paper.setConference(conference);
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(StateConflictException.class)
                .hasMessageContaining("Paper is in state: " + paper.getState() + " and can not assign reviewer");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserToBeAssignedAsReviewerWasNotFoundOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found to be assigned as reviewer with id: "
                        + reviewerAssignmentRequest.userId());

    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUserToBeAssignedAsReviewerDoesNotHaveReviewerRole() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        User user = getUser(2L, new HashSet<>());
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.of(user));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User is not a reviewer with id: " + user.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenUserIsAlreadyReviewerOnRequestedPaperOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        User user = getUser(1L, Set.of(new Role(RoleType.ROLE_REVIEWER)));
        Review review = new Review();
        review.setPaper(paper);
        review.setUser(user);
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.of(user));
        when(this.reviewRepository.findByPaperId(any(Long.class))).thenReturn(List.of(review));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(StateConflictException.class)
                .hasMessageContaining("User already assigned as reviewer");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperAlreadyHasMaxReviewersOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        User user1 = getUser(1L, Set.of(new Role(RoleType.ROLE_REVIEWER)));
        User user2 = getUser(2L, Set.of(new Role(RoleType.ROLE_REVIEWER)));
        User user3 = getUser(3L, Set.of(new Role(RoleType.ROLE_REVIEWER)));
        Review review1 = new Review();
        review1.setPaper(paper);
        review1.setUser(user1);
        Review review2 = new Review();
        review2.setPaper(paper);
        review2.setUser(user2);
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(3L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.of(user3));
        when(this.reviewRepository.findByPaperId(any(Long.class))).thenReturn(List.of(review1, review2));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                authentication)).isInstanceOf(StateConflictException.class)
                .hasMessageContaining("Paper has the maximum number of reviewers");
    }

    //updatePaperApprovalStatus()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePcChairOnUpdatePaperApprovalStatus() {
        //Arrange
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                UUID.randomUUID(),
                1L,
                ReviewDecision.APPROVED,
                authentication)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnUpdatePaperApprovalStatus() {
        Conference conference = getConference();
        Authentication authentication = getAuthentication();


        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                authentication)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInDecisionStateOnUpdatePaperApprovalStatus() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                authentication)).isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and the decision to " +
                        "either approve or reject the paper can not be made");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnUpdatePaperApprovalStatus() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                authentication)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: 1");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInReviewedStateOnUpdatePaperApprovalStatus() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        Paper paper = getPaper();
        paper.setConference(conference);
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                authentication)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper is in state: " + paper.getState() + " and can not get either approved or rejected");
    }

    //findConferenceById()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnFindConferenceById() {
        //Arrange
        UUID id = UUID.randomUUID();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getAuthentication());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.findConferenceById(id, context))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    //findConferences()
    @Test
    void shouldFindAllConferencesWhenBothNameAndDescriptionAreBlank() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getAuthentication());
        Conference conference = getConference();
        ConferenceDTO expected = new ConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                new HashSet<>()
        );

        when(this.conferenceRepository.findAll()).thenReturn(List.of(conference));

        List<ConferenceDTO> actual = this.underTest.findConferences("", "  ", context);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualTo(expected);

        verify(this.conferenceRepository, never()).findConferencesByNameContainingIgnoringCase(any(String.class));
        verify(this.conferenceRepository, never()).findConferencesByDescriptionContainingIgnoringCase(
                any(String.class));
    }

    @Test
    void shouldFindConferencesByNameWhenDescriptionIsBlank() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getAuthentication());
        Conference conference = getConference();
        ConferenceDTO expected = new ConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                new HashSet<>()
        );

        when(this.conferenceRepository.findConferencesByNameContainingIgnoringCase(any(String.class)))
                .thenReturn(List.of(conference));

        List<ConferenceDTO> actual = this.underTest.findConferences("conference", "  ", context);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualTo(expected);

        verify(this.conferenceRepository, never()).findAll();
        verify(this.conferenceRepository, never()).findConferencesByDescriptionContainingIgnoringCase(
                any(String.class));
    }

    @Test
    void shouldFindConferencesByDescriptionWhenNameIsBlank() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getAuthentication());
        Conference conference = getConference();
        ConferenceDTO expected = new ConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                new HashSet<>()
        );

        when(this.conferenceRepository.findConferencesByDescriptionContainingIgnoringCase(any(String.class)))
                .thenReturn(List.of(conference));

        List<ConferenceDTO> actual = this.underTest.findConferences("", "description", context);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualTo(expected);

        verify(this.conferenceRepository, never()).findAll();
        verify(this.conferenceRepository, never()).findConferencesByNameContainingIgnoringCase(any(String.class));
    }

    @Test
    void shouldFindConferencesByNameAndDescriptionWhenBothNameAndDescriptionAreNotBlank() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getAuthentication());
        Conference conference = getConference();
        ConferenceDTO expected = new ConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                new HashSet<>()
        );

        when(this.conferenceRepository.findConferencesByNameContainingIgnoringCase(any(String.class)))
                .thenReturn(List.of(conference));
        when(this.conferenceRepository.findConferencesByDescriptionContainingIgnoringCase(any(String.class)))
                .thenReturn(List.of(conference));

        List<ConferenceDTO> actual = this.underTest.findConferences("conference", "description", context);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualTo(expected);

        verify(this.conferenceRepository, never()).findAll();
    }

    @Test
    void shouldReturnAnEmptyListWhenNoConferencesAreFoundOnFindConferences() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getAuthentication());

        when(this.conferenceRepository.findAll()).thenReturn(Collections.emptyList());

        List<ConferenceDTO> actual = this.underTest.findConferences("", "", context);

        assertThat(actual).isEmpty();

        verify(this.conferenceRepository, never()).findConferencesByNameContainingIgnoringCase(any(String.class));
        verify(this.conferenceRepository, never()).findConferencesByDescriptionContainingIgnoringCase(
                any(String.class));
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnDeleteConference() {
        //Arrange
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(eq(id), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(id, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnDeleteConference() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId((eq(conference.getId())), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(conference.getId(), authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInCreatedStateOnDeleteConference() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(eq(conference.getId()), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(conference.getId(), authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not be deleted");
    }

    private Authentication getAuthentication() {
        User user = new User("username", "password", "Full Name", Set.of(new Role(RoleType.ROLE_AUTHOR)));
        user.setId(1L);
        SecurityUser securityUser = new SecurityUser(user);

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    private Conference getConference() {
        Conference conference = new Conference();
        conference.setName("conference");
        conference.setDescription("description");
        conference.setConferenceUsers(new HashSet<>());

        return conference;
    }

    private Paper getPaper() {
        Paper paper = new Paper();
        paper.setId(1L);
        paper.setTitle("title");

        return paper;
    }

    private User getUser(Long id, Set<Role> roles) {
        User user = new User();
        user.setId(id);
        user.setRoles(roles);

        return user;
    }
}
