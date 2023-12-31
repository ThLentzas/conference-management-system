package com.example.conference_management_system.conference;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceServiceTest {
    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private AuthService authService;
    private ConferenceService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new ConferenceService(conferenceRepository, userRepository, roleService, authService);
    }

    @Test
    void shouldThrowDuplicateResourceExceptionOnCreateConferenceWhenNameExists() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        Authentication authentication = getAuthentication();
        ConferenceCreateRequest conferenceCreateRequest = new ConferenceCreateRequest("name", "description");

        when(this.conferenceRepository.existsByNameIgnoringCase(any(String.class))).thenReturn(true);

        assertThatThrownBy(() -> this.underTest.createConference(
                conferenceCreateRequest,
                authentication,
                httpServletRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A conference with the provided name already exists");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameExceedsMaxLength() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        Authentication authentication = getAuthentication();
        ConferenceCreateRequest conferenceCreateRequest = new ConferenceCreateRequest(
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(50) + 51),
                "description");

        assertThatThrownBy(() -> this.underTest.createConference(
                conferenceCreateRequest,
                authentication,
                httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conference name must not exceed 50 characters");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenConferenceIsNotFoundOnStartSubmission() {
        UUID id = UUID.randomUUID();
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.underTest.startSubmission(id, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + id);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenRequestingUserIsNotPc_ChairOnStartSubmission() {
        Conference conference = getConference();
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.conferenceRepository.isPc_ChairAtConference(any(UUID.class), any(Long.class))).thenReturn(false);

        assertThatThrownBy(() -> this.underTest.startSubmission(conference.getId(), authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conference not found with id: " + conference.getId());
    }

    @Test
    void shouldThrowStateConflictExceptionWhenConferenceNotInCreatedStateOnStartSubmission() {
        Conference conference = getConference();
        conference.setState(ConferenceState.ASSIGNMENT);
        Authentication authentication = getAuthentication();

        when(this.conferenceRepository.findById(any(UUID.class))).thenReturn(Optional.of(conference));
        when(this.conferenceRepository.isPc_ChairAtConference(any(UUID.class), any(Long.class))).thenReturn(true);

        assertThatThrownBy(() -> this.underTest.startSubmission(conference.getId(), authentication))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + conference.getState().name() + " and can not start " +
                        "submission");
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
}
