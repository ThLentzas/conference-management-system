package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;
import com.example.conference_management_system.conference.mapper.ConferenceDTOMapper;
import com.example.conference_management_system.conference.mapper.PCChairConferenceDTOMapper;
import com.example.conference_management_system.entity.*;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.paper.PaperRepository;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final UserRepository userRepository;
    private final PaperRepository paperRepository;
    private final ReviewRepository reviewRepository;
    private final RoleService roleService;
    private final AuthService authService;
    private final ConferenceDTOMapper conferenceDTOMapper = new ConferenceDTOMapper();
    private final PCChairConferenceDTOMapper pcChairConferenceDTOMapper = new PCChairConferenceDTOMapper();
    private static final Logger logger = LoggerFactory.getLogger(ConferenceService.class);
    private static final String CONFERENCE_NOT_FOUND_MSG = "Conference not found with id: ";
    private static final String PAPER_NOT_FOUND_MSG = "Paper not found with id: ";
    private static final String ACCESS_DENIED_MSG = "Access denied";

    /*
        The user that made the request to create the conference must also be assigned the role ROLE_PC_CHAIR for
        that conference.
     */
    UUID createConference(ConferenceCreateRequest conferenceCreateRequest,
                          Authentication authentication,
                          HttpServletRequest servletRequest) {
        validateName(conferenceCreateRequest.name());

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        if (this.conferenceRepository.existsByNameIgnoringCase(conferenceCreateRequest.name())) {
            logger.error("Conference creation failed. Duplicate name: {}", conferenceCreateRequest.name());
            throw new DuplicateResourceException("A conference with the provided name already exists");
        }

        if (this.roleService.assignRole(securityUser.user(), RoleType.ROLE_PC_CHAIR)) {
            logger.warn("Current user was assigned a new role. Invalidating current session");
            this.authService.invalidateSession(servletRequest);
        }

        Conference conference = new Conference(
                conferenceCreateRequest.name(),
                conferenceCreateRequest.description(),
                Set.of(securityUser.user())
        );
        this.conferenceRepository.save(conference);
        this.userRepository.save(securityUser.user());

        return conference.getId();
    }

    void startSubmission(UUID id, Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (!this.conferenceRepository.isPcChairAtConference(id, securityUser.user().getId())) {
            logger.error("Start submission failed. User with id: {} is not PC_CHAIR at conference with id: {}",
                    securityUser.user().getId(), id);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        Conference conference = this.conferenceRepository.findById(id).orElseThrow(() -> {
            logger.error("Start submission failed. Conference not found with id: {}", id);

            return new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + id);
        });

        if (!conference.getState().equals(ConferenceState.CREATED)) {
            logger.error("Start submission failed. Conference with id: {} is in state: {} and can not transition to: "
                    + "{}", id, conference.getState(), ConferenceState.SUBMISSION);

            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
                    + " and can not start submission");
        }

        conference.setState(ConferenceState.SUBMISSION);
        this.conferenceRepository.save(conference);
        logger.info("Conference with id: {} transitioned to state: {}", id, conference.getState());
    }

    void startAssignment(UUID id, Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (!this.conferenceRepository.isPcChairAtConference(id, securityUser.user().getId())) {
            logger.error("Start assignment failed. User with id: {} is not PC_CHAIR at conference with id: {}",
                    securityUser.user().getId(), id);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        Conference conference = this.conferenceRepository.findById(id).orElseThrow(() -> {
            logger.error("Start assignment failed. Conference not found with id: {}", id);

            return new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + id);
        });

        if (!conference.getState().equals(ConferenceState.SUBMISSION)) {
            logger.error("Start assignment failed. Conference with id: {} is in state: {} and can not transition to: "
                    + "{}", id, conference.getState(), ConferenceState.ASSIGNMENT);

            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
                    + " and can not start assignment");
        }

        conference.setState(ConferenceState.ASSIGNMENT);
        this.conferenceRepository.save(conference);
        logger.info("Conference with id: {} transitioned to state: {}", id, conference.getState());
    }

    void startReview(UUID id, Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (!this.conferenceRepository.isPcChairAtConference(id, securityUser.user().getId())) {
            logger.error("Start review failed. User with id: {} is not PC_CHAIR at conference with id: {}",
                    securityUser.user().getId(), id);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        Conference conference = this.conferenceRepository.findById(id).orElseThrow(() -> {
            logger.error("Start review failed. Conference not found with id: {}", id);

            return new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + id);
        });

        if (!conference.getState().equals(ConferenceState.ASSIGNMENT)) {
            logger.error("Start review failed. Conference with id: {} is in state: {} and can not transition to: "
                    + "{}", id, conference.getState(), ConferenceState.REVIEW);

            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
                    + " and can not start reviews");
        }

        conference.setState(ConferenceState.REVIEW);
        this.conferenceRepository.save(conference);
        logger.info("Conference with id: {} transitioned to state: {}", id, conference.getState());
    }

//    void submitPaper(UUID id, PaperSubmissionRequest paperSubmissionRequest, Authentication authentication) {
//        Conference conference = this.conferenceRepository.findById(id).orElseThrow(() -> {
//            logger.error("Submit paper failed. Conference not found with id: {}", id);
//
//            return new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + id);
//        });
//
//        if (!conference.getState().equals(ConferenceState.SUBMISSION)) {
//            logger.error("Submit paper failed. Conference with id: {} is not in SUBMISSION state. " +
//                    "Conference state: {}", id, conference.getState());
//
//            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
//                    + " and you can not submit papers");
//        }
//
//        Paper paper = this.paperRepository.findById(paperSubmissionRequest.paperId()).orElseThrow(() -> {
//            logger.error("Submit paper failed. Paper not found with id: {}", paperSubmissionRequest.paperId());
//
//            return new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperSubmissionRequest.paperId());
//        });
//
//        if (!paper.getState().equals(PaperState.CREATED)) {
//            logger.error("Submit paper failed. Paper with id: {} is in the: {} and can not be submitted",
//                    paperSubmissionRequest.paperId(), paper.getState());
//
//            throw new StateConflictException("Paper is in state: " + paper.getState() + " and can not be submitted");
//        }
//
//        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
//        if (!this.paperRepository.isAuthorAtPaper(paperSubmissionRequest.paperId(), securityUser.user().getId())) {
//            logger.error("Submit paper failed. User with id: {} is not author for paper with id: {}",
//                    securityUser.user().getId(), paperSubmissionRequest.paperId());
//
//            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperSubmissionRequest.paperId());
//        }
//
//        paper.setState(PaperState.SUBMITTED);
//        paper.setConference(conference);
//        this.paperRepository.save(paper);
//    }

    /*
        In order to assign a reviewer to a paper the following need to be true:

        1) User who made the request has PC_CHAIR role but not at the request conference
        2) Conference is found
        3) Conference is in ASSIGNMENT state
        4) Paper is found
        5) Paper has been submitted to this specific conference
        6) Paper is in SUBMITTED state
        7) The reviewer must be a registered user in our system with role REVIEWER
        8) The same reviewer can not be assigned to a paper they are already assigned to
        9) The maximum number(2) of reviewers has already been reached
     */
//    void assignReviewer(UUID conferenceId,
//                        Long paperId,
//                        ReviewerAssignmentRequest reviewerAssignmentRequest,
//                        Authentication authentication) {
//        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
//        if (!this.conferenceRepository.isPcChairAtConference(conferenceId, securityUser.user().getId())) {
//            logger.error("Reviewer assignment failed. User with id: {} is not PC_CHAIR at conference with id: {}",
//                    securityUser.user().getId(), conferenceId);
//
//            throw new AccessDeniedException(ACCESS_DENIED_MSG);
//        }
//
//        Conference conference = this.conferenceRepository.findById(conferenceId).orElseThrow(() -> {
//            logger.error("Reviewer assignment failed. Conference not found with id: {}", conferenceId);
//
//            return new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + conferenceId);
//        });
//
//        if (!conference.getState().equals(ConferenceState.ASSIGNMENT)) {
//            logger.error("Reviewer assignment failed. Conference with id: {} is in state: {} and reviewers can not be " +
//                    "assigned", conferenceId, conference.getState());
//
//            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
//                    + " and reviewers can not be assigned");
//        }
//
//        Paper paper = this.paperRepository.findById(paperId).orElseThrow(() -> {
//            logger.error("Reviewer assignment failed. Paper not found with id: {}", paperId);
//
//            return new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperId);
//        });
//
//        /*
//            An alternative would be to call conference().getPapers().contains(paper) and see if the paper for the given
//            id belongs to the conference
//        */
//        if (paper.getConference() == null || !paper.getConference().getId().equals(conferenceId)) {
//            logger.error("Reviewer assignment failed. Paper with id: {} is not submitted to conference with id: {}",
//                    paper, conferenceId);
//
//            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperId);
//        }
//
//        if (!paper.getState().equals(PaperState.SUBMITTED)) {
//            logger.error("Reviewer assignment failed. Paper with id: {} is not in SUBMITTED state in order to assign a "
//                    + " reviewer. Paper state: {}", paperId, paper.getState());
//
//            throw new StateConflictException("Paper is in state: " + paper.getState() + " and can not assign reviewer");
//        }
//
//        this.userRepository.findById(reviewerAssignmentRequest.userId()).ifPresentOrElse(user -> {
//            Set<RoleType> roleTypes = user.getRoles().stream()
//                    .map(Role::getType)
//                    .collect(Collectors.toSet());
//
//            /*
//                The below exception most likely is wrong when the user to be assigned as a reviewer does not have that
//                role
//             */
//            if (!roleTypes.contains(RoleType.ROLE_REVIEWER)) {
//                logger.error("Reviewer assignment failed. User with id: {} does not have the role type: {}",
//                        reviewerAssignmentRequest.userId(), RoleType.ROLE_REVIEWER);
//
//                throw new IllegalArgumentException("User is not a reviewer with id: " + user.getId());
//            }
//
//            List<Review> reviews = this.reviewRepository.findByPaperId(paperId);
//            Set<User> reviewers = reviews.stream()
//                    .map(Review::getUser)
//                    .collect(Collectors.toSet());
//
//            /*
//                An alternative was to call this.reviewRepository.isReviewerAtPaper(paperId, user.getId()) or the
//                reviewerAssignmentRequest.userId(), same thing
//             */
//            if (reviewers.contains(user)) {
//                logger.error("Reviewer assignment failed: User with id: {} is already a reviewer in paper with id: {}",
//                        user.getId(), paperId);
//
//                throw new StateConflictException("User already assigned as reviewer");
//            }
//
//            if(reviewers.size() == 2) {
//                logger.error("Reviewer assignment failed. Paper with id: {} has the maximum numbers of reviewers",
//                        paperId);
//
//                throw new StateConflictException("Paper has the maximum number of reviewers");
//            }
//
//            Review review = new Review(paper, user);
//            this.reviewRepository.save(review);
//        }, () -> {
//            logger.error("User not found with id: {} to be assigned as reviewer", reviewerAssignmentRequest.userId());
//
//            throw new ResourceNotFoundException("User not found to be assigned as reviewer with id: " +
//                    reviewerAssignmentRequest.userId());
//        });
//    }

    ConferenceDTO findConferenceById(UUID id, SecurityContext securityContext) {
        Conference conference = this.conferenceRepository.findById(id).orElseThrow(() -> {
            logger.error("Conference not found with id: {}", id);

            return new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + id);
        });

         /*
            Passing the authentication object straight as parameter would not work. Since the endpoint is permitAll()
            in case of an unauthenticated user(Anonymous user) calling authentication.getPrincipal() would result in a
            NullPointerException since authentication would be null.
            https://docs.spring.io/spring-security/reference/servlet/authentication/anonymous.html

            If the user who made the request has PC_CHAIR role at that conference, they can also see all the related
            papers and their reviews. Any other case, unauthenticated user, PC_MEMBER, AUTHOR, all see the same info
            about the conference.
         */
        if (securityContext.getAuthentication().getPrincipal() instanceof SecurityUser securityUser
                && this.conferenceRepository.isPcChairAtConference(id, securityUser.user().getId())) {
            return this.pcChairConferenceDTOMapper.apply(conference);
        }

        return this.conferenceDTOMapper.apply(conference);
    }

    private void validateName(String name) {
        if (name.length() > 50) {
            logger.error("Validation failed. Name exceeds max length: {}", name);
            throw new IllegalArgumentException("Conference name must not exceed 50 characters");
        }
    }
}
