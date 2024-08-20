package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.ConferenceDTO;
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
import com.example.conference_management_system.conference.mapper.ConferenceDTOMapper;
import com.example.conference_management_system.conference.mapper.PCChairConferenceDTOMapper;
import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.paper.PaperService;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.PaperUserRepository;
import com.example.conference_management_system.review.ReviewDecision;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserService;
import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final ConferenceUserRepository conferenceUserRepository;
    private final PaperUserRepository paperUserRepository;
    private final UserService userService;
    private final PaperService paperService;
    private final RoleService roleService;
    private final AuthService authService;
    private final ConferenceDTOMapper conferenceDTOMapper = new ConferenceDTOMapper();
    private final PCChairConferenceDTOMapper pcChairConferenceDTOMapper = new PCChairConferenceDTOMapper();
    private static final Logger logger = LoggerFactory.getLogger(ConferenceService.class);
    private static final String CONFERENCE_NOT_FOUND_MSG = "Conference not found with id: ";
    private static final String ACCESS_DENIED_MSG = "Access denied";

    /*
        The user that made the request to create the conference must also be assigned the role ROLE_PC_CHAIR for
        that conference.

        https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/orm/jpa/JpaTransactionManager.html

        Ignore SonarLint's suggestion to make it public

        Method visibility and @Transactional
        The @Transactional annotation is typically used on methods with public visibility. As of 6.0, protected or
        package-visible methods can also be made transactional for class-based proxies by default. Note that
        transactional methods in interface-based proxies must always be public and defined in the proxied interface.

        https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
     */
    @Transactional
    UUID createConference(ConferenceCreateRequest conferenceCreateRequest,
                          SecurityUser securityUser,
                          HttpServletRequest servletRequest) {
        validateName(conferenceCreateRequest.name());

        if (this.conferenceRepository.existsByNameIgnoringCase(conferenceCreateRequest.name())) {
            throw new DuplicateResourceException("A conference with the provided name already exists");
        }
         /*
            If the current user is assigned a new role, it means that now they have access to new endpoints in
            subsequent requests but making one request to these endpoint would result in 403 despite them having the
            role. The reason is the token/cookie was generated upon the user logging in/signing up and had the roles at
            that time. In order to give the user access to new endpoints either we invalidate the session or we revoke
            the jwt and we force them to log in again.
        */
        if (this.roleService.assignRole(securityUser.user(), RoleType.ROLE_PC_CHAIR)) {
            this.authService.invalidateSession(servletRequest);
            logger.info("Current user was assigned a new role and the current session is invalidated");
        }

        Conference conference = new Conference(
                conferenceCreateRequest.name(),
                conferenceCreateRequest.description()
        );
        ConferenceUser conferenceUser = new ConferenceUser(
                new ConferenceUserId(conference.getId(), securityUser.user().getId()),
                conference,
                securityUser.user()
        );

        Set<ConferenceUser> conferenceUsers = Set.of(conferenceUser);
        conference.setConferenceUsers(conferenceUsers);
        this.conferenceRepository.save(conference);
        this.conferenceUserRepository.save(conferenceUser);

        return conference.getId();
    }

    /*
        Ignore SonarLint's suggestion to make it public

        Method visibility and @Transactional
        The @Transactional annotation is typically used on methods with public visibility. As of 6.0, protected or
        package-visible methods can also be made transactional for class-based proxies by default. Note that
        transactional methods in interface-based proxies must always be public and defined in the proxied interface.

        https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
     */
    @Transactional
    void updateConference(UUID conferenceId,
                          ConferenceUpdateRequest conferenceUpdateRequest,
                          SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        boolean updated = false;

        if (conferenceUpdateRequest.name() != null && !conferenceUpdateRequest.name().isBlank()) {
            validateName(conferenceUpdateRequest.name());

            if (this.conferenceRepository.existsByNameIgnoringCase(conferenceUpdateRequest.name())) {
                throw new DuplicateResourceException("A conference with the provided name already exists");
            }
            conference.setName(conferenceUpdateRequest.name());
            updated = true;
        }

        if (conferenceUpdateRequest.description() != null && !conferenceUpdateRequest.description().isBlank()) {
            conference.setDescription(conferenceUpdateRequest.description());
            updated = true;
        }

        if (!updated) {
            throw new IllegalArgumentException("At least one valid property must be provided to update conference");
        }

        /*
            Since the method runs with @Transactional the below save call if the conference's properties changed is not
            necessary, since the transaction is tied with the method's execution it will update the conference if it
            detects changes during the transaction without us having to call save() explicitly.
         */
        this.conferenceRepository.save(conference);
    }

    /*
        Ignore SonarLint's suggestion to make it public

        Method visibility and @Transactional
        The @Transactional annotation is typically used on methods with public visibility. As of 6.0, protected or
        package-visible methods can also be made transactional for class-based proxies by default. Note that
        transactional methods in interface-based proxies must always be public and defined in the proxied interface.

        https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
     */
    @Transactional
    void startSubmission(UUID conferenceId, SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);
        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.CREATED)) {
            throw new StateConflictException("Conference is in the state: " + conference.getState() + " and can not " +
                    "start submission");
        }

        conference.setState(ConferenceState.SUBMISSION);
        /*
            Since the method runs with @Transactional the below save call if the conference's properties changed is not
            necessary, since the transaction is tied with the method's execution it will update the conference if it
            detects changes during the transaction without us having to call save() explicitly.
         */
        this.conferenceRepository.save(conference);
    }

    @Transactional
    void startAssignment(UUID conferenceId, SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.SUBMISSION)) {
            throw new StateConflictException("Conference is in the state: " + conference.getState() + " and can not " +
                    "start assignment");
        }

        conference.setState(ConferenceState.ASSIGNMENT);
        /*
            Since the method runs with @Transactional the below save call if the conference's properties changed is not
            necessary, since the transaction is tied with the method's execution it will update the conference if it
            detects changes during the transaction without us having to call save() explicitly.
         */
        this.conferenceRepository.save(conference);
    }

    @Transactional
    void startReview(UUID conferenceId, SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.ASSIGNMENT)) {

            throw new StateConflictException("Conference is in the state: " + conference.getState() + " and can not" +
                    " start review");
        }

        conference.setState(ConferenceState.REVIEW);
        /*
            Since the method runs with @Transactional the below save call if the conference's properties changed is not
            necessary, since the transaction is tied with the method's execution it will update the conference if it
            detects changes during the transaction without us having to call save() explicitly.
         */
        this.conferenceRepository.save(conference);
    }

    @Transactional
    void startDecision(UUID conferenceId, SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.REVIEW)) {
            throw new StateConflictException("Conference is in the state: " + conference.getState() + " and can not " +
                    "start the approval or rejection of the submitted papers");
        }

        conference.setState(ConferenceState.DECISION);

        /*
            Since the method runs with @Transactional the below save call if the conference's properties changed is not
            necessary, since the transaction is tied with the method's execution it will update the conference if it
            detects changes during the transaction without us having to call save() explicitly.
         */
        this.conferenceRepository.save(conference);
    }

    /*
        When the conference reaches its FINAL state the papers that were APPROVED get ACCEPTED and the ones that got
        REJECTEd they return to CREATED state and no longer tied to the conference, so they can be submitted to a
        different conference
     */
    @Transactional
    void startFinal(UUID conferenceId, SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsersAndPapers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.DECISION)) {

            throw new StateConflictException("Conference is in the state: " + conference.getState() + " and the " +
                    "papers can neither be accepted nor rejected");
        }

        conference.setState(ConferenceState.FINAL);
        this.conferenceRepository.save(conference);

        conference.getPapers().stream()
                .filter(paper -> paper.getState().equals(PaperState.APPROVED))
                .forEach(paper -> {
                    paper.setState(PaperState.ACCEPTED);
                    this.paperService.save(paper);
                });

        conference.getPapers().stream()
                .filter(paper -> paper.getState().equals(PaperState.REJECTED))
                .forEach(paper -> {
                    paper.setState(PaperState.CREATED);
                    paper.setConference(null);
                    this.paperService.save(paper);
                });
        /*
            Since the method runs with @Transactional the below save call if the conference's/paper's properties changed
            is not necessary, since the transaction is tied with the method's execution it will update the
            conference/papers if it detects changes during the transaction without us having to call save() explicitly.
         */
    }

    @Transactional
    void addPCChair(UUID conferenceId, PCChairAdditionRequest pcChairAdditionRequest, SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        User toBeAddedUser = this.userService.findUserByIdFetchingRoles(pcChairAdditionRequest.userId());
        if (isPCChairAtConference(conference, toBeAddedUser)) {
            throw new DuplicateResourceException("User with id: " + toBeAddedUser.getId() + " is already PCChair for " +
                    "conference with id: " + conferenceId);
        }

        this.roleService.assignRole(toBeAddedUser, RoleType.ROLE_PC_CHAIR);

        ConferenceUser conferenceUser = new ConferenceUser(
                new ConferenceUserId(conference.getId(), toBeAddedUser.getId()),
                conference,
                toBeAddedUser
        );
        this.conferenceUserRepository.save(conferenceUser);
    }

    @Transactional
    void submitPaper(UUID conferenceId, PaperSubmissionRequest paperSubmissionRequest, SecurityUser securityUser) {
        Paper paper = this.paperService.findByPaperIdFetchingPaperUsersAndConference(paperSubmissionRequest.paperId());

        if (!this.paperService.isInRelationshipWithPaper(paper, securityUser.user(), RoleType.ROLE_AUTHOR)) {
            logger.info("User with id: {} is not author for paper with id: {}", securityUser.user().getId(),
                    paperSubmissionRequest.paperId());

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        Conference conference = this.conferenceRepository.findById(conferenceId).orElseThrow(() ->
                new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + conferenceId)
        );

        if (!conference.getState().equals(ConferenceState.SUBMISSION)) {
            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
                    + " and you can not submit papers");
        }

        if (!paper.getState().equals(PaperState.CREATED)) {
            throw new StateConflictException("Paper is in state: " + paper.getState() + " and can not be submitted");
        }

        /*
            Since the method runs with @Transactional the below save call if the paper's properties changed is not
            necessary, since the transaction is tied with the method's execution it will update the papers if it detects
            changes during the transaction without us having to call save() explicitly.
         */
        paper.setState(PaperState.SUBMITTED);
        paper.setConference(conference);
        this.paperService.save(paper);
    }

    /*
        In order to assign a reviewer to a paper the following need to be true:

        1) Conference is found
        2) User who made the request has PC_CHAIR role at the conference request
        3) Conference is in ASSIGNMENT state
        4) Paper is found
        5) Paper has been submitted to the specific conference
        6) Paper is in SUBMITTED state
        7) The reviewer must be a registered user in our system with role REVIEWER
        8) The reviewer can not be assigned to a paper they have the role AUTHOR
        9) The reviewer can not be assigned to a paper they are already assigned to
        10) The maximum number(2) of reviewers has already been reached
     */
    @Transactional
    void assignReviewer(UUID conferenceId,
                        Long paperId,
                        ReviewerAssignmentRequest reviewerAssignmentRequest,
                        SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.ASSIGNMENT)) {
            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
                    + " and reviewers can not be assigned");
        }

        Paper paper = this.paperService.findByPaperIdFetchingPaperUsersAndConference(paperId);

        if (paper.getConference() == null || !paper.getConference().getId().equals(conferenceId)) {
            throw new StateConflictException("Paper with id: " + paper.getId() + " is not submitted to conference " +
                    "with id: " + conference.getId());
        }

        if (!paper.getState().equals(PaperState.SUBMITTED)) {
            throw new StateConflictException("Paper is in state: " + paper.getState() + " and a reviewer can not be " +
                    "assigned");
        }

        User reviewer = this.userService.findUserByIdFetchingRoles(reviewerAssignmentRequest.userId());

        if (this.paperService.isInRelationshipWithPaper(paper, reviewer, RoleType.ROLE_AUTHOR)) {
            throw new DuplicateResourceException("User with id: " + reviewerAssignmentRequest.userId() + " is author of " +
                    "the paper and can not be assigned as a reviewer");
        }

        Set<RoleType> roleTypes = reviewer.getRoles().stream()
                .map(Role::getType)
                .collect(Collectors.toSet());

        /*
            The below exception most likely is wrong when the user to be assigned as a reviewer does not have that
            role
        */
        if (!roleTypes.contains(RoleType.ROLE_REVIEWER)) {
            throw new StateConflictException("User is not a reviewer with id: " + reviewer.getId());
        }

        Set<User> reviewers = paper.getPaperUsers().stream()
                .filter(paperUser -> paperUser.getRoleType().equals(RoleType.ROLE_REVIEWER))
                .map(PaperUser::getUser)
                .collect(Collectors.toSet());

        if (reviewers.contains(reviewer)) {
            throw new DuplicateResourceException("User already assigned as reviewer to paper with id: " + paperId);
        }

        if (reviewers.size() == 2) {
            throw new StateConflictException("Paper has the maximum number of reviewers");
        }

        PaperUser paperUser = new PaperUser(
                new PaperUserId(paperId, reviewer.getId()),
                paper,
                reviewer,
                RoleType.ROLE_REVIEWER
        );
        this.paperUserRepository.save(paperUser);
    }

    @Transactional
    void updatePaperApprovalStatus(UUID conferenceId,
                                   Long paperId,
                                   ReviewDecision decision,
                                   SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.DECISION)) {
            throw new StateConflictException("Conference is in the state: " + conference.getState().name() + " and " +
                    "the decision to either approve or reject the paper can not be made");
        }

        Paper paper = this.paperService.findByPaperIdFetchingPaperUsersAndConference(paperId);

        if (paper.getConference() == null || !paper.getConference().getId().equals(conferenceId)) {
            throw new StateConflictException("Paper with id: " + paper.getId() + " is not submitted to conference " +
                    "with id: " + conference.getId());
        }

        if (!paper.getState().equals(PaperState.REVIEWED)) {
            throw new StateConflictException("Paper is in state: " + paper.getState() + " and can not get " +
                    "either approved or rejected");
        }

        if (decision.equals(ReviewDecision.APPROVED)) {
            paper.setState(PaperState.APPROVED);
        } else {
            paper.setState(PaperState.REJECTED);
        }

        this.paperService.save(paper);
    }


    ConferenceDTO findConferenceById(UUID conferenceId, SecurityContext securityContext) {
        Conference conference = findByConferenceIdFetchingConferenceUsersAndPapers(conferenceId);

         /*
            If the user who made the request has PC_CHAIR role at that conference, they can also see all the related
            papers and their reviews. Any other case, unauthenticated user, REVIEWER, AUTHOR, PC_CHAIR but not in the
            requested conference all see the same info
         */
        if (securityContext.getAuthentication().getPrincipal() instanceof SecurityUser securityUser
                && isPCChairAtConference(conference, securityUser.user())) {
            return this.pcChairConferenceDTOMapper.convert(conference);
        }

        return this.conferenceDTOMapper.convert(conference);
    }

    /*
        For every conference that is returned if the requesting user is PCChair at that conference we need to return
        more properties like the papers and their reviews.
     */
    List<ConferenceDTO> findConferences(String name, String description, SecurityContext securityContext) {
        ConferenceSpecs conferenceSpecs = new ConferenceSpecs(name, description);
        List<Conference> conferences = this.conferenceRepository.findAll(conferenceSpecs, Sort.by("name"));

        /*
            Case: If the current user is PCChair at any of the returned conferences we return more properties, otherwise
            we return public information about the conferences.
         */
        if (securityContext.getAuthentication().getPrincipal() instanceof SecurityUser securityUser) {
            return conferences.stream()
                    .map(conference -> associateUser(conference, securityUser.user()))
                    .toList();
        }

        // Guest user
        return conferences.stream()
                .map(this.conferenceDTOMapper::convert)
                .toList();
    }

    @Transactional
    void deleteConferenceById(UUID conferenceId, SecurityUser securityUser) {
        Conference conference = findByConferenceIdFetchingConferenceUsers(conferenceId);

        if (!isPCChairAtConference(conference, securityUser.user())) {
            logger.info("User with id: {} is not PC_CHAIR at conference with id: {}", securityUser.user().getId(),
                    conferenceId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (!conference.getState().equals(ConferenceState.CREATED)) {
            throw new StateConflictException("Conference is in the state: " + conference.getState() + " and can " +
                    "not be deleted");
        }

        /*
            For a paper to be submitted to a conference the conference has to be in SUBMISSION state and since a
            conference can be deleted only in CREATED state we do not have to update the conferenceId in the paper
            table to null
        */
        this.conferenceRepository.deleteById(conferenceId);
    }

    private void validateName(String name) {
        if (name.length() > 50) {
            throw new IllegalArgumentException("Conference name must not exceed 50 characters");
        }
    }

    private Conference findByConferenceIdFetchingConferenceUsers(UUID conferenceId) {
        return this.conferenceRepository.findByConferenceIdFetchingConferenceUsers(conferenceId).orElseThrow(() ->
                new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + conferenceId));
    }

    private Conference findByConferenceIdFetchingConferenceUsersAndPapers(UUID conferenceId) {
        return this.conferenceRepository.findByConferenceIdFetchingConferenceUsersAndPapers(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + conferenceId));
    }

    private boolean isPCChairAtConference(Conference conference, User user) {
        return conference.getConferenceUsers().stream()
                .anyMatch(conferenceUser -> conferenceUser.getUser().equals(user));
    }

    private ConferenceDTO associateUser(Conference conference, User user) {
        return isPCChairAtConference(conference, user) ? this.pcChairConferenceDTOMapper.convert(conference) :
                this.conferenceDTOMapper.convert(conference);
    }
}
