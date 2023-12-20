package com.example.conference_management_system.paper;

import com.example.conference_management_system.content.ContentRepository;
import com.example.conference_management_system.entity.Content;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.UnsupportedFileException;
import com.example.conference_management_system.utility.FileService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaperService {
    private final PaperRepository paperRepository;
    private final FileService fileService;
    private final ContentRepository contentRepository;

    /*
        https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html#content-type-validation

        Following OWASP best practices for file uploads, we have to rename the file to something generated by the
        application like a UUID. We store the file with the new name in our file system, and we keep a reference in our
        db with the original file name as provided by the user, the file name generated by the application and the
        correct file extension found by using Apache Tika not the one extracted by the original file name.

        Using file.getContentType() would not work. We have to use Apache Tika that verifies the file signature(magic
        number) bytes at the start of each file to correctly identify the Mime type and the file extension.
     */
    Long createPaper(PaperCreateRequest paperCreateRequest) {
        validatePaper(paperCreateRequest);

        if (this.paperRepository.existsByTitleIgnoreCase(paperCreateRequest.title())) {
            throw new DuplicateResourceException("A paper with the provided title already exists");
        }

        String originalFileName = paperCreateRequest.content().getOriginalFilename();
        String generatedFileName = UUID.randomUUID().toString();
        this.fileService.storeFile(paperCreateRequest.content(), generatedFileName);

        String fileExtension = this.fileService.findFileExtension(paperCreateRequest.content());
        Content content = new Content(originalFileName, generatedFileName, fileExtension);
        Paper paper = new Paper(
                paperCreateRequest.title(),
                paperCreateRequest.abstractText(),
                paperCreateRequest.authors(),
                paperCreateRequest.keywords()
        );
        paper = this.paperRepository.save(paper);
        content.setPaper(paper);
        this.contentRepository.save(content);

        return paper.getId();
    }

    private void validatePaper(PaperCreateRequest paperCreateRequest) {
        validateTitle(paperCreateRequest.title());
        validateAuthors(paperCreateRequest.authors());
        validateAbstractText(paperCreateRequest.abstractText());
        validateContent(paperCreateRequest.content());
        validateKeywords(paperCreateRequest.keywords());
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("You must provide the title of the paper");
        }

        if (title.length() > 100) {
            throw new IllegalArgumentException("Title must not exceed 100 characters");
        }
    }

    private void validateAuthors(String authorsCsv) {
        String[] authors = authorsCsv.split(",");
        if (authors.length == 0) {
            throw new IllegalArgumentException("You must provide at least one author");
        }

        for (String author : authors) {
            if (author.isBlank()) {
                throw new IllegalArgumentException("Every author you provided must have a name");
            }
        }
    }

    private void validateAbstractText(String abstractText) {
        if (abstractText.isBlank()) {
            throw new IllegalArgumentException("You must provide the abstract text of the paper");
        }
    }

    private void validateKeywords(String keywordsCsv) {
        String[] keywords = keywordsCsv.split(",");
        if (keywords.length == 0) {
            throw new IllegalArgumentException("You must provide at least one keyword");
        }

        for (String keyword : keywords) {
            if (keyword.isBlank()) {
                throw new IllegalArgumentException("Every keyword must have a value");
            }
        }
    }

    /*
        Validating both the name and the Mime type of file. We support only .pdf and .tex files.
     */
    private void validateContent(MultipartFile content) {
        String fileName = content.getOriginalFilename();

        if (fileName != null && !fileName.matches("[a-zA-Z0-9\\- ._]+")) {
            throw new IllegalArgumentException("The file name must contain only alphanumeric characters, hyphen, " +
                    "underscores_ spaces, and periods");
        }

        if (fileName != null && fileName.length() > 100) {
            throw new IllegalArgumentException("File name must not exceed 50 characters");
        }

        if (!this.fileService.isFileSupported(content)) {
            throw new UnsupportedFileException("The provided file is not supported. Make sure your file is either a"
                    + " pdf or a Latex one");
        }
    }
}
