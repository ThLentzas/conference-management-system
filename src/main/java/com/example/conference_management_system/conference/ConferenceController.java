package com.example.conference_management_system.conference;

import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conferences")
class ConferenceController {
    private final ConferenceService conferenceService;

    @PostMapping
    ResponseEntity<Void> createConference(@Valid @RequestBody ConferenceCreateRequest conferenceCreateRequest,
                                          Authentication authentication,
                                          UriComponentsBuilder uriBuilder,
                                          HttpServletRequest servletRequest) {
        UUID conferenceId = this.conferenceService.createConference(
                conferenceCreateRequest,
                authentication,
                servletRequest);

        URI location = uriBuilder
                .path("/api/v1/conferences/{id}")
                .buildAndExpand(conferenceId)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    /*
        The state of conference changes to SUBMISSION, and we can submit papers to that conference
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/start-submission")
    ResponseEntity<Void> startSubmission(@PathVariable("id") UUID id, Authentication authentication) {
        this.conferenceService.startSubmission(id, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
         https://docs.spring.io/spring-security/reference/servlet/authentication/anonymous.html
     */
    @GetMapping("/{id}")
    ResponseEntity<ConferenceDTO> findConferenceById(@PathVariable("id") UUID id,
                                                     @CurrentSecurityContext SecurityContext context) {
        ConferenceDTO conferenceDTO = this.conferenceService.findConferenceById(id, context);

        return new ResponseEntity<>(conferenceDTO, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PostMapping("/{id}/submit")
    ResponseEntity<Void> submitPaper(@PathVariable("id") UUID id,
                                     @RequestBody PaperSubmissionRequest paperSubmissionRequest) {
        this.conferenceService.submitPaper(id, paperSubmissionRequest);


        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
