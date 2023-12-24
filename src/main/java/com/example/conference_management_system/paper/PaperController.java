package com.example.conference_management_system.paper;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/papers")
class PaperController {
    private final PaperService paperService;

    //@PreAuthorize("hasAnyRole('PC_CHAIR', 'PC_MEMBER')")
    @PostMapping
    ResponseEntity<Void> createPaper(@RequestParam("title") String title,
                                     @RequestParam("abstractText") String abstractText,
                                     @RequestParam("authors") String authors,
                                     @RequestParam("keywords") String keywords,
                                     @RequestParam("file") MultipartFile file,
                                     UriComponentsBuilder uriBuilder
    ) {
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(title, abstractText, authors, keywords, file);
        Long id = this.paperService.createPaper(paperCreateRequest);

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
    ResponseEntity<PaperDTO> getPaper(@PathVariable ("id") Long id) {
        PaperDTO paper = this.paperService.findPaperById(id);

        return new ResponseEntity<>(paper, HttpStatus.OK);
    }
}
