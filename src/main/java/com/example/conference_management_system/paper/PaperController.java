package com.example.conference_management_system.paper;

import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.conference_management_system.paper.dto.AuthorAdditionRequest;
import com.example.conference_management_system.paper.dto.PaperCreateRequest;
import com.example.conference_management_system.paper.dto.PaperDTO;
import com.example.conference_management_system.paper.dto.PaperFile;
import com.example.conference_management_system.paper.dto.PaperUpdateRequest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/papers")
class PaperController {
    private final PaperService paperService;

    @PostMapping
    ResponseEntity<Void> createPaper(@RequestParam("title") String title,
                                     @RequestParam("abstractText") String abstractText,
                                     @RequestParam("authors") String authors,
                                     @RequestParam("keywords") String keywords,
                                     @RequestParam("file") MultipartFile file,
                                     UriComponentsBuilder uriBuilder,
                                     HttpServletRequest servletRequest
    ) {
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(title, abstractText, authors, keywords, file);
        Long id = this.paperService.createPaper(paperCreateRequest, servletRequest);

        URI location = uriBuilder
                .path("/api/v1/papers/{id}")
                .buildAndExpand(id)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> updatePaper(@PathVariable("id") Long id,
                                     @RequestParam(value = "title", required = false) String title,
                                     @RequestParam(value = "abstractText", required = false) String abstractText,
                                     @RequestParam(value = "authors", required = false) String authors,
                                     @RequestParam(value = "keywords", required = false) String keywords,
                                     @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        PaperUpdateRequest paperUpdateRequest = new PaperUpdateRequest(title, abstractText, authors, keywords, file);
        this.paperService.updatePaper(id, paperUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/author")
    ResponseEntity<Void> addAuthor(@PathVariable("id") Long id,
                                   @Valid @RequestBody AuthorAdditionRequest authorAdditionRequest) {
        this.paperService.addAuthor(id, authorAdditionRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}")
    ResponseEntity<PaperDTO> getPaper(@PathVariable("id") Long id) {
        PaperDTO paper = this.paperService.findPaperById(id);
        Link download = linkTo(methodOn(PaperController.class).downloadPaper(id)).withRel("download");
        paper.add(download);

        return new ResponseEntity<>(paper, HttpStatus.OK);
    }

    /*
        Using .filename(paperFile.originalFileName(), StandardCharsets.UTF_8), it triggers the use of RFC 5987 encoding
        for non-ASCII characters, which is meant to handle special characters in filenames safely. However, this can
        lead to unexpected results in certain browsers or environments that don't handle this encoding well.
     */
    @PreAuthorize("hasAnyRole('AUTHOR', 'PC_MEMBER', 'PC_CHAIR')")
    @GetMapping("/{id}/download")
    ResponseEntity<Resource> downloadPaper(@PathVariable("id") Long id) {
        PaperFile paperFile = this.paperService.downloadPaperFile(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(paperFile.originalFileName())
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(paperFile.file(), headers, HttpStatus.OK);
    }

    //assign reviewer only to PC_MEMBER

    //PC_MEMBER are assigned to a conference for paper review.
}
