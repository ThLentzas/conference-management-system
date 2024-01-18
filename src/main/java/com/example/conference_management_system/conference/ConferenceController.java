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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
    @Operation(
            summary = "Create conference",
            description = "Accessible to authenticated users. Creating conference adds the role ROLE_PC_CHAIR to the user. The id of the created conference" +
                    "is in the location header to be used in subsequent requests",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
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
    @Operation(
            summary = "Update conference",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
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
    @Operation(
            summary = "Start submission. The state of the conference changes to submission and papers can be submitted",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    ResponseEntity<Void> startSubmission(@PathVariable("id") UUID id,
                                         @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startSubmission(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        The state of conference changes to ASSIGNMENT, and reviewers can be assigned to submitted papers. Papers can
        no longer be submitted.
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/assignment")
    @Operation(
            summary = "Start assignment. The state of the conference changes to assignment and reviewers can be assigned to papers. " +
                    "Papers can no longer submitted",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    ResponseEntity<Void> startAssignment(@PathVariable("id") UUID id,
                                         @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startAssignment(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        The state of conference changes to REVIEW, and the assigned reviewers can write a review for the submitted
        papers.
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/review")
    @Operation(
            summary = "Start review. The state of the conference changes to review and reviewers can write reviewers for papers they are assigned to",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    ResponseEntity<Void> startReview(@PathVariable("id") UUID id,
                                     @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startReview(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        The state of conference changes to DECISION, and a PC_CHAIR at the conference can either APPROVE or REJECT
        the submitted papers based on the reviews
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/decision")
    @Operation(
            summary = "Start decision. The state of the conference changes to decision and a pc chair at the conference can either approve or reject " +
                    "the submitted papers based on the reviews",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    ResponseEntity<Void> startDecision(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startDecision(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        The state of conference changes to FINAL, the APPROVED papers are getting ACCEPTED and the REJECTED ones are
        removed from the specific conference to be submitted to another one.
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/final")
    @Operation(
            summary = "Start final. The state of the conference changes to final. The approved papers are getting accepted and the rejected ones are  removed from " +
                    "the specific conference to be submitted to another one",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    ResponseEntity<Void> startFinal(@PathVariable("id") UUID id,
                                    @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.startFinal(id, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @PutMapping("/{id}/pc-chair")
    @Operation(
            summary = "Add a registered user as pc chair",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
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
    @Operation(
            summary = "Submit a paper to a conference",
            description = "Accessible only to users with role ROLE_AUTHOR. You must be one of the authors of the paper, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    ResponseEntity<Void> submitPaper(@PathVariable("id") UUID id,
                                     @RequestBody PaperSubmissionRequest paperSubmissionRequest,
                                     @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.submitPaper(id, paperSubmissionRequest, securityUser);


        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('PC_CHAIR')")
    @Operation(
            summary = "Assign a reviewer to a submitted paper",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    @PostMapping("/{conferenceId}/papers/{paperId}/reviewer")
    ResponseEntity<Void> assignReviewer(@PathVariable("conferenceId") UUID conferenceId,
                                        @PathVariable("paperId") Long paperId,
                                        @RequestBody ReviewerAssignmentRequest reviewerAssignmentRequest,
                                        @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.assignReviewer(conferenceId, paperId, reviewerAssignmentRequest, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        A paper that has been reviewed can either get APPROVED or REJECTED
     */
    @PreAuthorize("hasRole('PC_CHAIR')")
    @Operation(
            summary = "Based on the reviews comments and scores approve or reject a reviewed paper",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    @PutMapping("/{conferenceId}/papers/{paperId}/{decision}")
    ResponseEntity<Void> updatePaperApprovalStatus(@PathVariable("conferenceId") UUID conferenceId,
                                                   @PathVariable("paperId") Long paperId,
                                                   @PathVariable("decision") ReviewDecision decision,
                                                   @AuthenticationPrincipal SecurityUser securityUser) {
        this.conferenceService.updatePaperApprovalStatus(conferenceId, paperId, decision, securityUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    /*
        Passing the authentication object straight as parameter would not work. Since the endpoint is permitAll()
        in case of an unauthenticated user(Anonymous user) calling authentication.getPrincipal() would result in a
        NullPointerException since authentication would be null.

        https://docs.spring.io/spring-security/reference/servlet/authentication/anonymous.html
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Find conference by id",
            description = "Public endpoint. If the requested user is in a relationship with the conference extra properties are returned",
            tags = {"Conference"}
    )
    ResponseEntity<ConferenceDTO> findConferenceById(@PathVariable("id") UUID id,
                                                     @Parameter(hidden = true) @CurrentSecurityContext
                                                     SecurityContext securityContext) {
        ConferenceDTO conferenceDTO = this.conferenceService.findConferenceById(id, securityContext);

        return new ResponseEntity<>(conferenceDTO, HttpStatus.OK);
    }

    @GetMapping
    @Operation(
            summary = "Find conferences. Optional filters are: name, description. If none is provided all conferences are returned",
            description = "Public endpoint. If the requested user is in a relationship with any of the returned conferences extra properties are returned",
            tags = {"Conference"}
    )
    ResponseEntity<List<ConferenceDTO>> findConferences(
            @RequestParam(value = "name", defaultValue = "", required = false) String name,
            @RequestParam(value = "description", defaultValue = "", required = false) String description,
            @Parameter(hidden = true) @CurrentSecurityContext SecurityContext securityContext) {
        List<ConferenceDTO> conferences = this.conferenceService.findConferences(name, description, securityContext);

        return new ResponseEntity<>(conferences, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PC_CHAIR')")
    @Operation(
            summary = "Delete a conference by id",
            description = "Accessible only to users with role ROLE_PC_CHAIR. You must be one of the PC Chairs of the conference, having the role is not enough",
            tags = {"Conference"},
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-CSRF-TOKEN"),

            }, security = {
            @SecurityRequirement(name = "cookieAuth")
    })
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
