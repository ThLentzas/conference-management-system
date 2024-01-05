package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.PaperUserRepository;
import com.example.conference_management_system.entity.*;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.paper.PaperRepository;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;

import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldThrowIllegalArgumentExceptionWhenNameExceedsMaxLength() {
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

        when(this.conferenceRepository.existsByNameIgnoringCase(any(String.class))).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createConference(
                conferenceCreateRequest,
                authentication,
                httpServletRequest)).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A conference with the provided name already exists");
    }

    //startSubmission()
    @Test
    void shouldAccessDeniedExceptionWhenRequestingUserIsNotConferencePcChairOnStartSubmission() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conference.getId(), authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartSubmission() {
        //Arrange
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(id, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInCreatedStateOnStartSubmission() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startSubmission(conference.getId(), authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and can not start " +
                        "submission");
    }

    //startAssignment
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePcChairOnStartAssignment() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conference.getId(), authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartAssignment() {
        //Arrange
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(id, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInSubmissionStateOnStartAssignment() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startAssignment(conference.getId(), authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and can not start " +
                        "assignment");
    }

    //startReview()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotConferencePcChairOnStartReview() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conference.getId(), authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartReview() {
        //Arrange
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(id, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceIsNotInAssignmentStateOnStartReview() {
        //Arrange
        Conference conference = getConference();
        conference.setState(ConferenceState.DECISION);
        Authentication authentication = getAuthentication();

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(true);
        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.startReview(conference.getId(), authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and can not start " +
                        "reviews");
    }

    //submitPaper()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotPaperAuthorOnSubmitPaper() {
        //Arrange
        Conference conference = getConference();
        Authentication authentication = getAuthentication();
        PaperSubmissionRequest paperSubmissionRequest = new PaperSubmissionRequest(1L);

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.submitPaper(conference.getId(), paperSubmissionRequest, authentication))
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
        Conference conference = getConference();
        Authentication authentication = getAuthentication();
        ReviewerAssignmentRequest reviewerAssignmentRequest = new ReviewerAssignmentRequest(1L);

        when(this.conferenceUserRepository.existsByConferenceIdAndUserId(any(UUID.class), any(Long.class)))
                .thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.assignReviewer(
                conference.getId(),
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

    private Authentication getAuthentication() {
        User user = new User("username", "password", "Full Name", Set.of(new Role(RoleType.ROLE_AUTHOR)));
        user.setId(1L);
        SecurityUser securityUser = new SecurityUser(user);

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    private Conference getConference() {
        Conference conference = new Conference();
        conference.setId(UUID.randomUUID());
        conference.setName("conference");
        conference.setDescription("description");

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
