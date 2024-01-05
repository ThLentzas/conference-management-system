package com.example.conference_management_system.paper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.conference_management_system.paper.dto.AuthorAdditionRequest;
import com.example.conference_management_system.paper.dto.PaperCreateRequest;
import com.example.conference_management_system.paper.dto.PaperDTO;
import com.example.conference_management_system.paper.dto.PaperFile;
import com.example.conference_management_system.paper.dto.PaperUpdateRequest;
import com.example.conference_management_system.review.dto.ReviewCreateRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/papers")
class PaperController {
    private final PaperService paperService;
    private static final Logger logger = LoggerFactory.getLogger(PaperController.class);

    @PostMapping
    ResponseEntity<Void> createPaper(@RequestParam("title") String title,
                                     @RequestParam("abstractText") String abstractText,
                                     @RequestParam("authors") String authors,
                                     @RequestParam("keywords") String keywords,
                                     @RequestParam("file") MultipartFile file,
                                     Authentication authentication,
                                     UriComponentsBuilder uriBuilder,
                                     HttpServletRequest servletRequest) {
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(title, abstractText, authors, keywords, file);
        logger.info("Paper create request: {}", paperCreateRequest);

        Long id = this.paperService.createPaper(paperCreateRequest, authentication, servletRequest);
        URI location = uriBuilder
                .path("/api/v1/papers/{id}")
                .buildAndExpand(id)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PutMapping("/{id}")
    ResponseEntity<Void> updatePaper(@PathVariable("id") Long id,
                                     @RequestParam(value = "title", required = false) String title,
                                     @RequestParam(value = "abstractText", required = false) String abstractText,
                                     @RequestParam(value = "authors", required = false) String authors,
                                     @RequestParam(value = "keywords", required = false) String keywords,
                                     @RequestParam(value = "file", required = false) MultipartFile file,
                                     Authentication authentication) {
        PaperUpdateRequest paperUpdateRequest = new PaperUpdateRequest(title, abstractText, authors, keywords, file);
        logger.info("Paper update request: {}", paperUpdateRequest);

        this.paperService.updatePaper(id, paperUpdateRequest, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PutMapping("/{id}/author")
    ResponseEntity<Void> addCoAuthor(@PathVariable("id") Long id,
                                     @Valid @RequestBody AuthorAdditionRequest authorAdditionRequest,
                                     Authentication authentication) {
        logger.info("Paper co-author addition request: {}", authorAdditionRequest);
        this.paperService.addCoAuthor(id, authorAdditionRequest, authentication);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('REVIEWER')")
    @PostMapping("/{id}/reviews")
    ResponseEntity<Void> reviewPaper(@PathVariable("id") Long id,
                                     @Valid @RequestBody ReviewCreateRequest reviewCreateRequest,
                                     Authentication authentication,
                                     UriComponentsBuilder uriBuilder) {

        Long reviewId = this.paperService.reviewPaper(id, reviewCreateRequest, authentication);

        URI location = uriBuilder
                .path("/api/v1/papers/{paperId}/reviews/{reviewId}")
                .buildAndExpand(id, reviewId)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    ResponseEntity<PaperDTO> findPaperById(@PathVariable("id") Long id,
                                           @CurrentSecurityContext SecurityContext securityContext) {
        PaperDTO paper = this.paperService.findPaperById(id, securityContext);

        return new ResponseEntity<>(paper, HttpStatus.OK);
    }

    /*
        Using .filename(paperFile.originalFileName(), StandardCharsets.UTF_8), it triggers the use of RFC 5987 encoding
        for non-ASCII characters, which is meant to handle special characters in filenames safely. However, this can
        lead to unexpected results in certain browsers or environments that don't handle this encoding well.
     */
    @PreAuthorize("hasAnyRole('AUTHOR', 'PC_MEMBER', 'PC_CHAIR')")
    @GetMapping("/{id}/download")
    ResponseEntity<Resource> downloadPaper(@PathVariable("id") Long id, Authentication authentication) {
        PaperFile paperFile = this.paperService.downloadPaperFile(id, authentication);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(paperFile.originalFileName())
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(paperFile.file(), headers, HttpStatus.OK);
    }
}
