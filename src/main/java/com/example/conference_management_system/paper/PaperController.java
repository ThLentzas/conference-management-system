package com.example.conference_management_system.paper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
                                     UriComponentsBuilder uriBuilder) {
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(title, abstractText, authors, keywords, file);
        Long id = this.paperService.createPaper(paperCreateRequest);

        URI location = uriBuilder
                .path("/api/v1/papers/{paperId}")
                .buildAndExpand(id)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }
}
