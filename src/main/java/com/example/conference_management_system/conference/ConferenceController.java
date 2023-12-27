package com.example.conference_management_system.conference;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conferences")
class ConferenceController {
    private final ConferenceService conferenceService;

    @PostMapping
    ResponseEntity<Void> createConference(@Valid @RequestBody ConferenceCreateRequest conferenceCreateRequest,
                                          UriComponentsBuilder uriBuilder,
                                          HttpServletRequest servletRequest) {
        UUID conferenceId = this.conferenceService.createConference(conferenceCreateRequest, servletRequest);

        URI location = uriBuilder
                .path("/api/v1/conferences/{id}")
                .buildAndExpand(conferenceId)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }
}
