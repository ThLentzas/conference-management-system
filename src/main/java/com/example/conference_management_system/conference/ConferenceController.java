package com.example.conference_management_system.conference;

import com.example.conference_management_system.review.ReviewDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.ConferenceUpdateRequest;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;

import java.net.URI;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conferences")
class ConferenceController {
    private final ConferenceService conferenceService;
    private static final Logger logger = LoggerFactory.getLogger(ConferenceController.class);

    @PostMapping
    ResponseEntity<Void> createConference(@Valid @RequestBody ConferenceCreateRequest conferenceCreateRequest,
                                          Authentication authentication,
                                          UriComponentsBuilder uriBuilder,
                                          HttpServletRequest servletRequest) {
        logger.info("Conference create request: {}", conferenceCreateRequest);

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

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}")
    ResponseEntity<Void> updateConference(@PathVariable("id") UUID id,
                                          @RequestBody ConferenceUpdateRequest conferenceUpdateRequest,
                                          Authentication authentication) {
        logger.info("Conference update request: {}", conferenceUpdateRequest);

        this.conferenceService.updateConference(id, conferenceUpdateRequest, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        The state of conference changes to SUBMISSION, and we can submit papers to that conference
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/submission")
    ResponseEntity<Void> startSubmission(@PathVariable("id") UUID id, Authentication authentication) {
        this.conferenceService.startSubmission(id, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/assignment")
    ResponseEntity<Void> startAssignment(@PathVariable("id") UUID id, Authentication authentication) {
        this.conferenceService.startAssignment(id, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/review")
    ResponseEntity<Void> startReview(@PathVariable("id") UUID id, Authentication authentication) {
        this.conferenceService.startReview(id, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/decision")
    ResponseEntity<Void> startDecision(@PathVariable("id") UUID id, Authentication authentication) {
        this.conferenceService.startDecision(id, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        It's a POST request with the id of the paper in the request body since it creates the relationship between
        paper and conference and POST should always have body
     */
    @PreAuthorize("hasRole('AUTHOR')")
    @PostMapping("/{id}/papers")
    ResponseEntity<Void> submitPaper(@PathVariable("id") UUID id,
                                     @RequestBody PaperSubmissionRequest paperSubmissionRequest,
                                     Authentication authentication) {
        this.conferenceService.submitPaper(id, paperSubmissionRequest, authentication);


        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PostMapping("/{conferenceId}/papers/{paperId}/reviewer")
    ResponseEntity<Void> assignReviewer(@PathVariable("conferenceId") UUID conferenceId,
                                        @PathVariable("paperId") Long paperId,
                                        @RequestBody ReviewerAssignmentRequest reviewerAssignmentRequest,
                                        Authentication authentication) {
        this.conferenceService.assignReviewer(conferenceId, paperId, reviewerAssignmentRequest, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        A paper that has been reviewed can either get approved or rejected
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{conferenceId}/papers/{paperId}/{decision}")
    ResponseEntity<Void> updatePaperApprovalStatus(@PathVariable("conferenceId") UUID conferenceId,
                                                   @PathVariable("paperId") Long paperId,
                                                   @PathVariable("decision") ReviewDecision decision,
                                                   Authentication authentication) {
        this.conferenceService.updatePaperApprovalStatus(conferenceId, paperId, decision, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
         https://docs.spring.io/spring-security/reference/servlet/authentication/anonymous.html
     */
    @GetMapping("/{id}")
    ResponseEntity<ConferenceDTO> findConferenceById(@PathVariable("id") UUID id,
                                                     @CurrentSecurityContext SecurityContext securityContext) {
        ConferenceDTO conferenceDTO = this.conferenceService.findConferenceById(id, securityContext);

        return new ResponseEntity<>(conferenceDTO, HttpStatus.OK);
    }
}
