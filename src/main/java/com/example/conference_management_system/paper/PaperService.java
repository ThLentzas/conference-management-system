package com.example.conference_management_system.paper;

import com.example.conference_management_system.conference.ConferenceRepository;
import com.example.conference_management_system.content.ContentRepository;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.Content;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.ServerErrorException;
import com.example.conference_management_system.exception.UnsupportedFileException;
import com.example.conference_management_system.review.ReviewDTO;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;
import com.example.conference_management_system.utility.FileService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class PaperService {
    private final PaperRepository paperRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ConferenceRepository conferenceRepository;
    private final RoleService roleService;
    private final FileService fileService;
    private static final Logger logger = LoggerFactory.getLogger(PaperService.class);
    private static final String PAPER_NOT_FOUND_MSG = "Paper not found with id: ";
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later";

    /*
        In order to create a paper a user has to be authenticated. The user that made the request to create the paper
        must also be assigned the role ROLE_AUTHOR and their name must be added as one of the paper's authors if not
        already. In both cases if the condition is true prior the check, both ignored.
     */
    Long createPaper(PaperCreateRequest paperCreateRequest, HttpServletRequest servletRequest) {
        validatePaper(paperCreateRequest);

        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (this.paperRepository.existsByTitleIgnoreCase(paperCreateRequest.title())) {
            logger.error("Paper creation failed. Duplicate title: {}", paperCreateRequest.title());
            throw new DuplicateResourceException("A paper with the provided title already exists");
        }

        this.roleService.assignRole(securityUser, RoleType.ROLE_AUTHOR, servletRequest);
        Set<String> authors = new HashSet<>(List.of(paperCreateRequest.authors().split(";")));
        authors.add(securityUser.user().getFullName());

        Content content = new Content();
        setupContent(content, paperCreateRequest.file());
        Paper paper = new Paper(
                paperCreateRequest.title(),
                paperCreateRequest.abstractText(),
                paperCreateRequest.keywords(),
                String.join(",", authors),
                Set.of(securityUser.user())
        );
        paper = this.paperRepository.save(paper);
        content.setPaper(paper);
        this.contentRepository.save(content);
        this.userRepository.save(securityUser.user());

        return paper.getId();
    }

    void updatePaper(Long id, PaperUpdateRequest paperUpdateRequest) {
        if (paperUpdateRequest.title() == null
                && paperUpdateRequest.authors() == null
                && paperUpdateRequest.abstractText() == null
                && paperUpdateRequest.keywords() == null
                && paperUpdateRequest.file() == null
        ) {
            logger.error("Paper update failed: Only null values were provided");
            throw new IllegalArgumentException("You must provide at least one property to update the paper");
        }

        this.paperRepository.findById(id).ifPresentOrElse(paper -> {
            if (paperUpdateRequest.title() != null) {
                validateTitle(paperUpdateRequest.title());

                if (this.paperRepository.existsByTitleIgnoreCase(paperUpdateRequest.title())) {
                    logger.error("Paper update failed. Duplicate title: {}", paperUpdateRequest.title());
                    throw new DuplicateResourceException("A paper with the provided title already exists");
                }
                paper.setTitle(paperUpdateRequest.title());
            }

            updatePropertyIfNonNull(
                    paperUpdateRequest.authors(),
                    this::validateAuthors,
                    paper::setAuthors
            );

            updatePropertyIfNonNull(
                    paperUpdateRequest.abstractText(),
                    this::validateAbstractText,
                    paper::setAbstractText
            );

            updatePropertyIfNonNull(
                    paperUpdateRequest.keywords(),
                    this::validateKeywords,
                    paper::setKeywords
            );

            if (paperUpdateRequest.file() != null) {
                validateFile(paperUpdateRequest.file());
                /*
                    1) Find the content for the specific paper that contains the original file name, the generated file
                       name(UUID) and the file extension
                    2) Delete the file that is stored with the generated name
                    3) Set up the content values from the new file
                    4) Update the record in DB
                 */
                this.contentRepository.findByPaperId(id).ifPresent(
                        content -> {
                            this.fileService.deleteFile(content.getGeneratedFileName());
                            setupContent(content, paperUpdateRequest.file());
                            this.contentRepository.save(content);
                        }
                );
            }
        }, () -> {
            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + id);
        });
    }

    void addAuthor(Long paperId, AuthorAdditionRequest authorAdditionRequest) {
        this.paperRepository.findById(paperId).ifPresentOrElse(paper -> {
            /*
                 List<String> authors = Arrays.asList(paper.getAuthors().split(",")); We could not call create our list
                 that way because Arrays.asList() returns a fixed-size list backed by the original array. This means
                 that the list cannot be structurally modified (we can't add or remove elements from it) When we called
                 authors.add(author) we would get an UnsupportedOperationException
             */
            List<String> authors = new ArrayList<>(Arrays.asList(paper.getAuthors().split(",")));

            if (!authors.contains(authorAdditionRequest.author())) {
                authors.add(authorAdditionRequest.author());
                String authorsCsv = String.join(",", authors);
                paper.setAuthors(authorsCsv);

                this.paperRepository.save(paper);
            }
        }, () -> {
            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperId);
        });
    }

    /*
        Based on the role of the user that makes the GET request for a paper, we have to return different properties of
        the PaperDTO.

        A visitor does not have access to the actual pdf or latex file of the paper
        The author of the paper can see the reviews but not the name of the reviewers
        The reviewer(ROLE_PC_MEMBER) can see all the information about the paper but not other reviews
        The PC_CHAIR if the paper belongs to the conference where they have the role PC_CHAIR have full access to that
        paper

        In any other case we return the same information that a visitor will have if the paper is in ACCEPTED state,
        otherwise 404 NOT_FOUND
     */
    PaperDTO findPaperById(Long paperId) {
        Set<ReviewDTO> reviews = new HashSet<>();
        ReviewDTO reviewDTO;
        /*
            Since we want to have a return value if the paper is found we can't use ifPresentOrElse().
         */
        Paper paper = this.paperRepository.findById(paperId).orElseThrow(() -> new ResourceNotFoundException(
                PAPER_NOT_FOUND_MSG + paperId));
        Resource resource = this.fileService.getFile(paper.getContent().getGeneratedFileName());
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (paper.getConference() != null && this.conferenceRepository.isPC_CHAIR(
                paper.getConference().getId(),
                securityUser.user().getId())) {
            for (Review review : paper.getReviews()) {
                reviewDTO = ReviewDTO.builder()
                        .id(review.getId())
                        .score(review.getScore())
                        .comment(review.getComment())
                        .createdDate(review.getCreatedDate())
                        .reviewer(review.getUser().getUsername())
                        .build();
                reviews.add(reviewDTO);
            }

            return PaperDTO.builder()
                    .id(paperId)
                    .title(paper.getTitle())
                    .abstractText(paper.getAbstractText())
                    .keywords(paper.getKeywords().split(","))
                    .authors(paper.getAuthors().split(","))
                    .state(paper.getState())
                    .createdDate(paper.getCreatedDate())
                    .file(resource)
                    .reviews(reviews)
                    .build();
        }

        if (this.reviewRepository.isReviewer(paperId, securityUser.user().getId())) {
            return PaperDTO.builder()
                    .id(paperId)
                    .title(paper.getTitle())
                    .abstractText(paper.getAbstractText())
                    .keywords(paper.getKeywords().split(","))
                    .authors(paper.getAuthors().split(","))
                    .state(paper.getState())
                    .createdDate(paper.getCreatedDate())
                    .file(resource)
                    .build();
        }

        if (this.paperRepository.isAuthor(paperId, securityUser.user().getId())) {
            for (Review review : paper.getReviews()) {
                reviewDTO = ReviewDTO.builder()
                        .id(review.getId())
                        .score(review.getScore())
                        .comment(review.getComment())
                        .createdDate(review.getCreatedDate())
                        .build();
                reviews.add(reviewDTO);
            }

            return PaperDTO.builder()
                    .id(paperId)
                    .title(paper.getTitle())
                    .abstractText(paper.getAbstractText())
                    .keywords(paper.getKeywords().split(","))
                    .authors(paper.getAuthors().split(","))
                    .createdDate(paper.getCreatedDate())
                    .reviews(reviews)
                    .file(resource)
                    .build();
        }

        if (!paper.getState().equals(PaperState.ACCEPTED)) {
            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperId);
        }

        return PaperDTO.builder()
                .id(paperId)
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .keywords(paper.getKeywords().split(","))
                .authors(paper.getAuthors().split(","))
                .createdDate(paper.getCreatedDate())
                .build();
    }

    private void validatePaper(PaperCreateRequest paperCreateRequest) {
        validateTitle(paperCreateRequest.title());
        validateAuthors(paperCreateRequest.authors());
        validateAbstractText(paperCreateRequest.abstractText());
        validateKeywords(paperCreateRequest.keywords());
        validateFile(paperCreateRequest.file());
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            logger.error("Validation failed. Title is blank: {}", title);
            throw new IllegalArgumentException("You must provide the title of the paper");
        }

        if (title.length() > 100) {
            logger.error("Validation failed. Title exceeds max length: {}", title);
            throw new IllegalArgumentException("Title must not exceed 100 characters");
        }
    }

    private void validateAuthors(String authorsCsv) {
        String[] authors = authorsCsv.split(",");
        for (String author : authors) {
            if (author.isBlank()) {
                logger.error("Validation failed: One or more authors are blank: {}", (Object) authors);
                throw new IllegalArgumentException("Every author you provide must have a name");
            }
        }
    }

    private void validateAbstractText(String abstractText) {
        if (abstractText.isBlank()) {
            logger.error("Validation failed. No abstract text was provided: {}", abstractText);
            throw new IllegalArgumentException("You must provide the abstract text of the paper");
        }
    }

    private void validateKeywords(String keywordsCsv) {
        String[] keywords = keywordsCsv.split(",");
        for (String keyword : keywords) {
            if (keyword.isBlank()) {
                logger.error("Validation failed: One or more keywords are blank: {}", (Object) keywords);
                throw new IllegalArgumentException("Every keyword you provide must have a value");
            }
        }
    }

    /*
        Validating both the name and the Mime type of file. We support only .pdf and .tex files.
     */
    private void validateFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        if (fileName != null && !fileName.matches("[a-zA-Z0-9\\- ._]+")) {
            logger.error("Validation failed. File name contains invalid character: {}", fileName);
            throw new IllegalArgumentException("The file name must contain only alphanumeric characters, hyphen, " +
                    "underscores spaces, and periods");
        }

        if (fileName != null && fileName.length() > 100) {
            logger.error("Validation failed. File name exceeds max length: {}", fileName.length());
            throw new IllegalArgumentException("File name must not exceed 100 characters");
        }

        if (!this.fileService.isFileSupported(file)) {
            logger.error("Validation failed: Unsupported file type: {}", fileName);
            throw new UnsupportedFileException("The provided file is not supported. Make sure your file is either a"
                    + " pdf or a Latex one");
        }
    }

    private <T> void updatePropertyIfNonNull(T property, Consumer<T> validator, Consumer<T> updater) {
        if (property != null) {
            validator.accept(property);
            updater.accept(property);
        }
    }

    /*
        https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html#content-type-validation

        Following OWASP best practices for file uploads, we have to rename the file to something generated by the
        application like a UUID. We store the file with the new name in our file system, and we keep a reference in our
        db with the original file name as provided by the user, the file name generated by the application and the
        correct file extension found by using Apache Tika not the one extracted by the original file name.

        Using file.getContentType() would not work. We have to use Apache Tika that verifies the file signature(magic
        number) bytes at the start of each file to correctly identify the Mime type and the file extension.
     */
    private void setupContent(Content content, MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String generatedFileName = UUID.randomUUID().toString();
        String fileExtension = this.fileService.findFileExtension(file);
        this.fileService.saveFile(file, generatedFileName);

        content.setOriginalFileName(originalFileName);
        content.setGeneratedFileName(generatedFileName);
        content.setFileExtension(fileExtension);
    }
}
