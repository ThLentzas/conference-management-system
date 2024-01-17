package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.ConferenceUpdateRequest;
import com.example.conference_management_system.conference.dto.PCChairAdditionRequest;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.PaperUser;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.key.ConferenceUserId;
import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.paper.PaperService;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.PaperUserRepository;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.review.ReviewDecision;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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
    private UserService userService;
    @Mock
    private PaperService paperService;
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
        Conference conference = getConference(conferenceId);

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
                "description"
        );
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
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

    @Test
    void shouldDuplicateResourceExceptionWhenNameExistsOnUpdateConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest("name", "description");
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.conferenceRepository.existsByNameIgnoringCase(conferenceUpdateRequest.name())).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updateConference(
                conferenceId,
                conferenceUpdateRequest,
                securityUser)).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A conference with the provided name already exists");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenNameIsNullOrEmptyAndDescriptionIsNull(String name) {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        ConferenceUpdateRequest conferenceUpdateRequest = new ConferenceUpdateRequest(name, null);
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
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
        Conference conference = getConference(conferenceId);

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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start submission");
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
        Conference conference = getConference(conferenceId);

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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers((conferenceId)))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not start assignment");
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
        Conference conference = getConference(conferenceId);
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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
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
        Conference conference = getConference(conferenceId);

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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
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
        Conference conference = getConference(conferenceId);

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

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));

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
        Conference conference = getConference(conferenceId);
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conferenceId, pcChairAdditionRequest, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenToBeAddedUserIsAlreadyPCChairAtConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        PCChairAdditionRequest pcChairAdditionRequest = new PCChairAdditionRequest(1L);
        User toBeAdded = getUser(2L, Set.of(new Role(RoleType.ROLE_PC_CHAIR)));

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser1 = getConferenceUser(conference, securityUser.user());
        ConferenceUser conferenceUser2 = getConferenceUser(conference, toBeAdded);
        conference.setConferenceUsers(Set.of(conferenceUser1, conferenceUser2));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.userService.findUserByIdFetchingRoles(pcChairAdditionRequest.userId())).thenReturn(toBeAdded);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addPCChair(conferenceId, pcChairAdditionRequest, securityUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with id: " + toBeAdded.getId() + " is already PCChair for conference with id: " +
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

        Paper paper = getPaper(paperSubmissionRequest.paperId());

        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(paperSubmissionRequest.paperId()))
                .thenReturn(paper);

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

        Paper paper = getPaper(paperSubmissionRequest.paperId());

        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(paperSubmissionRequest.paperId()))
                .thenReturn(paper);
        when(this.paperService.isInRelationshipWithPaper(paper, securityUser.user(), RoleType.ROLE_AUTHOR))
                .thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conferenceId, paperSubmissionRequest, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInSubmissionStateOnSubmitPaper() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);
        Conference conference = getConference(conferenceId);
        SecurityUser securityUser = getSecurityUser();
        securityUser.user().setRoles(Set.of(new Role(RoleType.ROLE_AUTHOR)));

        Paper paper = getPaper(paperSubmissionRequest.paperId());
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_AUTHOR);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(paperSubmissionRequest.paperId()))
                .thenReturn(paper);
        when(this.paperService.isInRelationshipWithPaper(paper, securityUser.user(), RoleType.ROLE_AUTHOR))
                .thenReturn(true);
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
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);
        SecurityUser securityUser = getSecurityUser();
        securityUser.user().setRoles(Set.of(new Role(RoleType.ROLE_AUTHOR)));

        Conference conference = getConference(conferenceId);
        conference.setState(ConferenceState.SUBMISSION);
        Paper paper = getPaper(paperSubmissionRequest.paperId());
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_AUTHOR);
        paper.setPaperUsers(Set.of(paperUser));
        paper.setState(PaperState.REVIEWED);

        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(paperSubmissionRequest.paperId()))
                .thenReturn(paper);
        when(this.paperService.isInRelationshipWithPaper(paper, securityUser.user(), RoleType.ROLE_AUTHOR))
                .thenReturn(true);
        when(this.conferenceRepository.findById(conferenceId)).thenReturn(Optional.of(conference));

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

        Conference conference = getConference(conferenceId);
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
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and reviewers can not " +
                        "be assigned");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperDoesNotBelongToAnyConferenceOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.ASSIGNMENT);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper with id: " + paper.getId() + " is not submitted to conference with id: " +
                        conference.getId());
    }

    @Test
    void shouldStateConflictExceptionWhenPaperBelongsToDifferentConferenceOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.ASSIGNMENT);
        conference.setConferenceUsers(Set.of(conferenceUser));
        Conference differentConference = new Conference();
        differentConference.setId(UUID.randomUUID());

        Paper paper = getPaper(1L);
        paper.setConference(differentConference);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);


        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper with id: " + paper.getId() + " is not submitted to conference with id: " +
                        conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInSubmittedStateOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.ASSIGNMENT);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);
        paper.setConference(conference);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper is in state: " + paper.getState() + " and a reviewer can not be assigned");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenUserToBeAssignedAsReviewerIsAlreadyAuthorOfThePaper() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);
        User reviewer = getUser(2L, Set.of(new Role(RoleType.ROLE_AUTHOR), new Role(RoleType.ROLE_REVIEWER)));

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.ASSIGNMENT);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, reviewer, RoleType.ROLE_AUTHOR);
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);
        when(this.userService.findUserByIdFetchingRoles(reviewerAssignmentRequest.userId())).thenReturn(reviewer);
        when(this.paperService.isInRelationshipWithPaper(paper, reviewer, RoleType.ROLE_AUTHOR)).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with id: " + reviewerAssignmentRequest.userId() + " is author of the " +
                        "paper and can not be assigned as a reviewer");
    }

    @Test
    void shouldStateConflictExceptionWhenUserToBeAssignedAsReviewerDoesNotHaveReviewerRole() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        User reviewer = getUser(2L, Set.of(new Role(RoleType.ROLE_AUTHOR)));

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.ASSIGNMENT);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);
        when(this.userService.findUserByIdFetchingRoles(reviewerAssignmentRequest.userId())).thenReturn(reviewer);
        when(this.paperService.isInRelationshipWithPaper(paper, reviewer, RoleType.ROLE_AUTHOR)).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("User is not a reviewer with id: " + reviewer.getId());
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenUserIsAlreadyReviewerOnRequestedPaperOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(2L);
        User reviewer = getUser(2L, Set.of(new Role(RoleType.ROLE_REVIEWER)));

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.ASSIGNMENT);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, reviewer, RoleType.ROLE_REVIEWER);
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);
        when(this.userService.findUserByIdFetchingRoles(reviewerAssignmentRequest.userId())).thenReturn(reviewer);
        when(this.paperService.isInRelationshipWithPaper(paper, reviewer, RoleType.ROLE_AUTHOR)).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User already assigned as reviewer to paper with id: " + 1L);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperAlreadyHasMaxReviewersOnReviewerAssignment() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        User reviewer1 = getUser(2L, Set.of(new Role(RoleType.ROLE_REVIEWER)));
        User reviewer2 = getUser(3L, Set.of(new Role(RoleType.ROLE_REVIEWER)));
        User reviewer3 = getUser(4L, Set.of(new Role(RoleType.ROLE_REVIEWER)));

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = new ConferenceUser();
        conferenceUser.setUser(securityUser.user());
        conference.setId(conferenceId);
        conference.setState(ConferenceState.ASSIGNMENT);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);
        PaperUser paperUser1 = getPaperUser(paper, reviewer1, RoleType.ROLE_REVIEWER);
        PaperUser paperUser2 = getPaperUser(paper, reviewer2, RoleType.ROLE_REVIEWER);
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        paper.setPaperUsers(Set.of(paperUser1, paperUser2));
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(4L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);
        when(this.userService.findUserByIdFetchingRoles(reviewerAssignmentRequest.userId())).thenReturn(reviewer3);
        when(this.paperService.isInRelationshipWithPaper(paper, reviewer3, RoleType.ROLE_AUTHOR)).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conferenceId,
                1L,
                reviewerAssignmentRequest,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper has the maximum number of reviewers");
    }

    //updatePaperApprovalStatus()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnUpdatePaperApprovalStatus() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conferenceId,
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnUpdatePaperApprovalStatus() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conferenceId,
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInDecisionStateOnUpdatePaperApprovalStatus() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conferenceId,
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and the decision to either " +
                        "approve or reject the paper can not be made");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperDoesNotBelongToAnyConferenceOnUpdatePaperApprovalStatus() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.DECISION);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conferenceId,
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper with id: " + paper.getId() + " is not submitted to conference with id: " +
                        conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperBelongsToDifferentConferenceOnUpdatePaperApprovalStatus() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.DECISION);
        conference.setConferenceUsers(Set.of(conferenceUser));
        Conference differentConference = getConference(UUID.randomUUID());

        Paper paper = getPaper(1L);
        paper.setConference(differentConference);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conferenceId,
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper with id: " + paper.getId() + " is not submitted to conference with id: " +
                        conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInReviewedStateOnUpdatePaperApprovalStatus() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setState(ConferenceState.DECISION);
        conference.setConferenceUsers(Set.of(conferenceUser));

        Paper paper = getPaper(1L);
        paper.setConference(conference);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));
        when(this.paperService.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(paper);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaperApprovalStatus(
                conferenceId,
                1L,
                ReviewDecision.APPROVED,
                securityUser)).isInstanceOf(StateConflictException.class)
                .hasMessage("Paper is in state: " + paper.getState() + " and can not get either approved or rejected");
    }

    //findConferenceById()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnFindConferenceById() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.findConferenceById(conferenceId, context))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnDeleteConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(conferenceId, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conferenceId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePCChairOnDeleteConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();
        Conference conference = getConference(conferenceId);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(conferenceId, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInCreatedStateOnDeleteConference() {
        //Arrange
        UUID conferenceId = UUID.randomUUID();
        SecurityUser securityUser = getSecurityUser();

        Conference conference = getConference(conferenceId);
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));
        conference.setState(ConferenceState.DECISION);

        when(this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId))
                .thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.deleteConferenceById(conferenceId, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState() + " and can not be deleted");
    }

    private SecurityUser getSecurityUser() {
        User user = new User("username", "password", "Full Name", Set.of(new Role(RoleType.ROLE_PC_CHAIR)));
        user.setId(1L);

        return new SecurityUser(user);
    }

    private Conference getConference(UUID conferenceId) {
        Conference conference = new Conference();
        conference.setId(conferenceId);
        conference.setName("conference");
        conference.setDescription("description");
        conference.setConferenceUsers(new HashSet<>());

        return conference;
    }

    private ConferenceUser getConferenceUser(Conference conference, User user) {
        return new ConferenceUser(
                new ConferenceUserId(conference.getId(), user.getId()),
                conference,
                user
        );
    }

    private Paper getPaper(Long paperId) {
        Paper paper = new Paper();
        paper.setId(paperId);
        paper.setTitle("title");

        return paper;
    }

    private PaperUser getPaperUser(Paper paper, User user, RoleType roleType) {
        return new PaperUser(
                new PaperUserId(paper.getId(), user.getId()),
                paper,
                user,
                roleType
        );
    }

    private User getUser(Long id, Set<Role> roles) {
        User user = new User();
        user.setId(id);
        user.setRoles(roles);

        return user;
    }
}
