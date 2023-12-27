package com.example.conference_management_system.conference;

import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(ConferenceService.class);
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later";


    /*
        The user that made the request to create the conference must also be assigned the role ROLE_PC_CHAIR for
        that conference.
     */
    UUID createConference(ConferenceCreateRequest conferenceCreateRequest, HttpServletRequest servletRequest) {
        validateName(conferenceCreateRequest.name());

        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (this.conferenceRepository.existsByNameIgnoringCase(conferenceCreateRequest.name())) {
            logger.error("Conference creation failed. Duplicate name: {}", conferenceCreateRequest.name());
            throw new DuplicateResourceException("A conference with the provided name already exists");
        }

        this.roleService.assignRole(securityUser, RoleType.ROLE_PC_CHAIR, servletRequest);

        Conference conference = new Conference(
                conferenceCreateRequest.name(),
                conferenceCreateRequest.description(),
                Set.of(securityUser.user())
        );
        conference = this.conferenceRepository.save(conference);
        this.userRepository.save(securityUser.user());

        return conference.getId();
    }

    private void validateName(String name) {
        if (name.length() > 50) {
            logger.error("Validation failed. Name exceeds max length: {}", name);
            throw new IllegalArgumentException("Conference name must not exceed 50 characters");
        }
    }
}
