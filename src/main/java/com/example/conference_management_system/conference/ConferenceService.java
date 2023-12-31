package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(ConferenceService.class);
    private static final String CONFERENCE_NOT_FOUND_MSG = "Conference not found with id: ";
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later";


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

        if(this.roleService.assignRole(securityUser.user(), RoleType.ROLE_PC_CHAIR)) {
            logger.warn("Current user was assigned a new role. Invalidating current session");
            this.authService.invalidateSession(servletRequest);
        }

        Conference conference = new Conference(
                conferenceCreateRequest.name(),
                conferenceCreateRequest.description(),
                Set.of(securityUser.user())
        );
        conference = this.conferenceRepository.save(conference);
        this.userRepository.save(securityUser.user());

        return conference.getId();
    }

    void startSubmission(UUID id, Authentication authentication) {
        Conference conference = this.conferenceRepository.findById(id).orElseThrow(() -> {
            logger.error("Start submission failed. Conference not found with id: {}", id);

            return new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + id);
        });

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if(!this.conferenceRepository.isPc_ChairAtConference(id, securityUser.user().getId())) {
            logger.error("Start submission failed. User with id: {} is not PC_CHAIR at conference with id: {}",
                    securityUser.user().getId(), id);

            throw new ResourceNotFoundException(CONFERENCE_NOT_FOUND_MSG + id);
        }

        if(!conference.getState().equals(ConferenceState.CREATED)) {
            logger.error("Start submission failed. Conference with id: {} is in state: {} and can not transition to: "
                    + "{}", id, conference.getState(), ConferenceState.SUBMISSION);

            throw new StateConflictException("Conference is in the state: " + conference.getState().name()
                    + " and can not start submission");
        }

        conference.setState(ConferenceState.SUBMISSION);
        logger.info("Conference with id: {} transitioned to state: {}", id, conference.getState());
        this.conferenceRepository.save(conference);
    }

    private void validateName(String name) {
        if (name.length() > 50) {
            logger.error("Validation failed. Name exceeds max length: {}", name);
            throw new IllegalArgumentException("Conference name must not exceed 50 characters");
        }
    }
}
