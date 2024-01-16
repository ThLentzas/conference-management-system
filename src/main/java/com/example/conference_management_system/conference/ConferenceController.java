package com.example.conference_management_system.conference;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.conference_management_system.user.dto.ReviewerAssignmentRequest;
import com.example.conference_management_system.conference.dto.ConferenceCreateRequest;
import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.conference.dto.ConferenceUpdateRequest;
import com.example.conference_management_system.conference.dto.PCChairAdditionRequest;
import com.example.conference_management_system.conference.dto.PaperSubmissionRequest;
import com.example.conference_management_system.review.ReviewDecision;
import com.example.conference_management_system.security.SecurityUser;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conferences")
class ConferenceController {
    private final ConferenceService conferenceService;

    @PostMapping
    ResponseEntity<Void> createConference(@Valid @RequestBody ConferenceCreateRequest conferenceCreateRequest,
                                          @AuthenticationPrincipal SecurityUser securityUser,
                                          UriComponentsBuilder uriBuilder,
                                          HttpServletRequest servletRequest) {
        UUID conferenceId = this.conferenceService.createConference(
                conferenceCreateRequest,
                securityUser,
                servletRequest
        );

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
                                          @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.updateConference(id, conferenceUpdateRequest, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        The state of conference changes to SUBMISSION, and we can submit papers to that conference
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/submission")
    ResponseEntity<Void> startSubmission(@PathVariable("id") UUID id,
                                         @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startSubmission(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/assignment")
    ResponseEntity<Void> startAssignment(@PathVariable("id") UUID id,
                                         @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startAssignment(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/review")
    ResponseEntity<Void> startReview(@PathVariable("id") UUID id,
                                     @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startReview(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/decision")
    ResponseEntity<Void> startDecision(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startDecision(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/final")
    ResponseEntity<Void> startFinal(@PathVariable("id") UUID id,
                                    @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startFinal(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/pc-chair")
    ResponseEntity<Void> addPCChair(@PathVariable("id") UUID id,
                                    @Valid @RequestBody PCChairAdditionRequest pcChairAdditionRequest,
                                    @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.addPCChair(id, pcChairAdditionRequest, securityUser);

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
                                     @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.submitPaper(id, paperSubmissionRequest, securityUser);


        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PostMapping("/{conferenceId}/papers/{paperId}/reviewer")
    ResponseEntity<Void> assignReviewer(@PathVariable("conferenceId") UUID conferenceId,
                                        @PathVariable("paperId") Long paperId,
                                        @RequestBody ReviewerAssignmentRequest reviewerAssignmentRequest,
                                        @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.assignReviewer(conferenceId, paperId, reviewerAssignmentRequest, securityUser);

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
                                                   @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.updatePaperApprovalStatus(conferenceId, paperId, decision, securityUser);

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

    @GetMapping
    ResponseEntity<List<ConferenceDTO>> findConferences(
            @RequestParam(value = "name", defaultValue = "", required = false) String name,
            @RequestParam(value = "description", defaultValue = "", required = false) String description,
            @CurrentSecurityContext SecurityContext securityContext) {
        List<ConferenceDTO> conferences = this.conferenceService.findConferences(name, description, securityContext);

        return new ResponseEntity<>(conferences, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PC_CHAIR')")
    ResponseEntity<Void> deleteConferenceById(@PathVariable("id") UUID id,
                                              @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.deleteConferenceById(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        There is no GET endpoint to find the papers of a given conference because only the PC_CHAIR of the given
        conference have access to them and can see them when they request to see the conference, they part of the
        response
    */
}
