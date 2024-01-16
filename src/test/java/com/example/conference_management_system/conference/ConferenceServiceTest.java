package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.dto.*;
import com.example.conference_management_system.paper.PaperService;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.PaperUserRepository;
import com.example.conference_management_system.entity.*;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.review.ReviewDecision;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserService;
import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
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
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.C;

@ExtendWith(MockitoExtension.class)
class ConferenceServiceTest {
    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceUserRepository conferenceUserRepository;
    @Mock
    private PaperUserRepository paperUserRepository;
    @Mock
    private UserService userService;
    @Mock
    private PaperService paperService;
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
                userService,
                paperService,
                roleService,
                authService
        );
    }

    //createConference()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameExceedsMaxLengthOnCreateConference() {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        SecurityUser securityUser = getSecurityUser();
        ConferenceCreateRequest conferenceCreateRequest = new ConferenceCreateRequest(
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(50) + 51),
                "description"
        );

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createConference(
                conferenceCreateRequest,
                securityUser,
                httpServletRequest)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conference name must not exceed 50 characters");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionOnCreateWhenNameExistsOnCreateConference() {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        SecurityUser securityUser = getSecurityUser();
        ConferenceCreateRequest conferenceCreateRequest = new ConferenceCreateRequest("name", "description");

        when(this.conferenceRepository.existsByNameIgnoringCase(conferenceCreateRequest.name())).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createConference(
                conferenceCreateRequest,
                securityUser,
                httpServletRequest)).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A conference with the provided name already exists");
    }

    //updateConference()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnUpdateConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest("name", "description");
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId, conferenceUpdateRequest, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnUpdateConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest("name", "description");
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(
                conferenceId,
                conferenceUpdateRequest,
                securityUser)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameExceedsMaxLengthOnUpdateConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(50) + 51),
                "description");
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(
                conferenceId,
                conferenceUpdateRequest,
                securityUser)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conference name must not exceed 50 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenNameIsNullOrEmptyAndDescriptionIsNull(String name) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(name, null);
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                securityUser)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenNameIsNullOrEmptyAndDescriptionIsBlank(String name) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(name, "");
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                securityUser)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenDescriptionIsNullOrEmptyAndNameIsNull(String description) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(null, description);
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                securityUser)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenDescriptionIsNullOrEmptyAndNameIsEmpty(String description) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest("", description);
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(conferenceId,
                conferenceUpdateRequest,
                securityUser)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one valid property must be provided to update conference");
    }

    //startSubmission()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartSubmission() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conferenceId, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartSubmission() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conferenceId, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInCreatedStateOnStartSubmission() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start " +
                        "submission");
    }

    //startAssignment
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conferenceId, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conferenceId, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInSubmissionStateOnStartAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start " +
                        "assignment");
    }

    //startReview()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartReview() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conferenceId, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartReview() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conferenceId, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInAssignmentStateOnStartReview() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start review");
    }

    //startDecision()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartDecision() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startDecision(conferenceId, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartDecision() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startDecision(conferenceId, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInReviewStateOnStartDecision() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startDecision(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start the " +
                        "approval or rejection of the submitted papers");
    }

    //startFinal()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartFinal() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsersAndPapers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startFinal(conferenceId, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnStartFinal() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsersAndPapers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startFinal(conferenceId, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInDecisionStateOnStartFinal() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.CREATED);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsersAndPapers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startFinal(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and the papers can neither " +
                        "be accepted nor rejected");
    }

    //addPCChair()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnAddPCChair() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conferenceId, pcChairAdditionRequest, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnAddPCChair() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);

        Conference conference = getConference();
        conference.setId(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conferenceId, pcChairAdditionRequest, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    /*
        this.conferenceUserRepository.existsByConferenceIdAndUserId() gets invoked twice. First to see if the user that
        made the request is PCChair of the conference and second to see if the to be added as PCChair is already PCChair
        of the conference, so we have to stab twice
    */
    @Test
    void shouldThrowDuplicateResourceExceptionWhenPCChairIsAlreadyAdded() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setConference(conference);
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setId(conferenceId);

        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);
        User user = getUser(2L, Set.of(new Role(RoleType.ROLE_PC_CHAIR)));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.userService.findUserByIdFetchingRoles(any(Long.class))).thenReturn(user);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conferenceId, pcChairAdditionRequest, securityUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with id: " + user.getId() + " is already PCChair for conference with id: " +
                        conference.getId());
    }

    //submitPaper()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotPaperAuthorOnSubmitPaper() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        securityUser.user().setRoles(Set.of(new Role(RoleType.ROLE_AUTHOR)));

        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);
        Paper paper = getPaper();

        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conferenceId, paperSubmissionRequest, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnSubmitPaper() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        securityUser.user().setRoles(Set.of(new Role(RoleType.ROLE_AUTHOR)));

        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);
        Paper paper = getPaper();

        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);
        when(this.paperService.isInRelationshipWithPaper(paper, securityUser.user(), RoleType.ROLE_AUTHOR))
                .thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conferenceId, paperSubmissionRequest, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + paperSubmissionRequest.paperId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInSubmissionStateOnSubmitPaper() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Conference conference = getConference();
        conference.setId(conferenceId);
        SecurityUser securityUser = getSecurityUser();
        securityUser.user().setRoles(Set.of(new Role(RoleType.ROLE_AUTHOR)));

        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);
        Paper paper = getPaper();
        PaperUser paperUser = new PaperUser();
        paperUser.setUser(securityUser.user());
        paper.setPaperUsers(Set.of(paperUser));

        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);
        when(this.conferenceRepository.findById(conferenceId)).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conferenceId, paperSubmissionRequest, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and you can not submit " +
                        "papers");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInCreatedStateOnSubmitPaper() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        Conference conference = getConference();
        conference.setState(ConferenceState.SUBMISSION);
        conference.setId(conferenceId);
        SecurityUser securityUser = getSecurityUser();
        securityUser.user().setRoles(Set.of(new Role(RoleType.ROLE_AUTHOR)));

        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);
        Paper paper = getPaper();
        PaperUser paperUser = new PaperUser();
        paperUser.setUser(securityUser.user());
        paper.setPaperUsers(Set.of(paperUser));
        paper.setState(PaperState.REVIEWED);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conferenceId, paperSubmissionRequest, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Paper is in state: " + paper.getState() + " and can not be submitted");
    }

    //assignReviewer()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference();
        conference.setConferenceUsers(new HashSet<>());

        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInAssignmentStateOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and reviewers can not " +
                        "be assigned");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        SecurityUser securityUser = getSecurityUser();
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
                securityUser)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: 1");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperDoesNotBelongToAnyConferenceOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        SecurityUser securityUser = getSecurityUser();
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
                securityUser)).isInstanceOf(ResourceNotFoundException.class)
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
        SecurityUser securityUser = getSecurityUser();
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
                securityUser)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInSubmittedStateOnReviewerAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Paper paper = getPaper();
        paper.setConference(conference);
        SecurityUser securityUser = getSecurityUser();
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
                securityUser)).isInstanceOf(StateConflictException.class)
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
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userService.findUserByIdFetchingRoles(any(Long.class))).thenReturn(user);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(conference.getId(),
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(IllegalArgumentException.class)
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
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userService.findUserByIdFetchingRoles(any(Long.class))).thenReturn(user);
        when(this.reviewRepository.findByPaperId(any(Long.class))).thenReturn(List.of(review));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
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
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(3L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userService.findUserByIdFetchingRoles(any(Long.class))).thenReturn(user3);
        when(this.reviewRepository.findByPaperId(any(Long.class))).thenReturn(List.of(review1, review2));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessageContaining("Paper has the maximum number of reviewers");
    }

    //updatePaperApprovalStatus()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePcChairOnUpdatePaperApprovalStatus() {
        //Arrange
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                UUID.randomUUID(),
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnUpdatePaperApprovalStatus() {
        Conference conference = getConference();
        SecurityUser securityUser = getSecurityUser();


        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInDecisionStateOnUpdatePaperApprovalStatus() {
        //Arrange
        Conference conference = getConference();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and the decision to " +
                        "either approve or reject the paper can not be made");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnUpdatePaperApprovalStatus() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: 1");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInReviewedStateOnUpdatePaperApprovalStatus() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        Paper paper = getPaper();
        paper.setConference(conference);
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conference.getId(),
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper is in state: " + paper.getState() + " and can not get either approved or rejected");
    }

    //findConferenceById()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnFindConferenceById() {
        //Arrange
        UUID id = UUID.randomUUID();
        SecurityContext context = SecurityContextHolder.createEmptyContext();

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
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(eq(id), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(id, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnDeleteConference() {
        //Arrange
        Conference conference = getConference();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId((eq(conference.getId())), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(conference.getId(), securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInCreatedStateOnDeleteConference() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(eq(conference.getId()), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(conference.getId(), securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not be deleted");
    }

    private SecurityUser getSecurityUser() {
        User user = new User("username", "password", "Full Name", Set.of(new Role(RoleType.ROLE_PC_CHAIR)));
        user.setId(1L);

        return new SecurityUser(user);
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
