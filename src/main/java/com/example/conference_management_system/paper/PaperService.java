package com.example.conference_management_system.paper;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.conference.ConferenceState;
import com.example.conference_management_system.conference.ConferenceUserRepository;
import com.example.conference_management_system.content.ContentRepository;
import com.example.conference_management_system.entity.*;
import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.exception.*;
import com.example.conference_management_system.paper.dto.*;
import com.example.conference_management_system.paper.mapper.AuthorPaperDTOMapper;
import com.example.conference_management_system.paper.mapper.PCChairPaperDTOMapper;
import com.example.conference_management_system.paper.mapper.PaperDTOMapper;
import com.example.conference_management_system.paper.mapper.ReviewerPaperDTOMapper;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.review.dto.ReviewCreateRequest;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class PaperService {
    private final PaperRepository paperRepository;
    private final PaperUserRepository paperUserRepository;
    private final ConferenceUserRepository conferenceUserRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final RoleService roleService;
    private final FileService fileService;
    private final AuthService authService;
    private final PCChairPaperDTOMapper pcChairPaperDTOMapper = new PCChairPaperDTOMapper();
    private final AuthorPaperDTOMapper authorPaperDTOMapper = new AuthorPaperDTOMapper();
    private final ReviewerPaperDTOMapper reviewerPaperDTOMapper = new ReviewerPaperDTOMapper();
    private final PaperDTOMapper paperDTOMapper = new PaperDTOMapper();
    private static final Logger logger = LoggerFactory.getLogger(PaperService.class);
    private static final String PAPER_NOT_FOUND_MSG = "Paper not found with id: ";
    private static final String ACCESS_DENIED_MSG = "Access denied";

    /*
        In order to create a paper a user has to be authenticated. The user that made the request to create the paper
        must also be assigned the role ROLE_AUTHOR and their name must be added as one of the paper's authors if not
        already. In both cases if the condition is true prior the check, both ignored.
     */
    Long createPaper(PaperCreateRequest paperCreateRequest,
                     Authentication authentication,
                     HttpServletRequest servletRequest) {
        validatePaper(paperCreateRequest);

        if (this.paperRepository.existsByTitleIgnoreCase(paperCreateRequest.title())) {
            logger.error("Paper creation failed. Duplicate title: {}", paperCreateRequest.title());

            throw new DuplicateResourceException("A paper with the provided title already exists");
        }

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        /*
            If the current user is assigned a new role, in our case ROLE_AUTHOR it means that now they have access
            to AUTHOR endpoints in subsequent requests but making one request to an AUTHOR access endpoint would
            result in 403 despite them having the role. The reason is the token/cookie was generated upon the user
            logging in/signing up and had the roles at that time. In order to give the user access to new endpoints
            either we invalidate the session or we revoke the jwt and we force them to log in again.
        */
        if (this.roleService.assignRole(securityUser.user(), RoleType.ROLE_AUTHOR)) {
            logger.warn("Current user was assigned a new role. Invalidating current session");

            this.authService.invalidateSession(servletRequest);
            this.userRepository.save(securityUser.user());
        }

        Set<String> authors = new HashSet<>(List.of(paperCreateRequest.authors().split(";")));
        authors.add(securityUser.user().getFullName());

        Content content = new Content();
        setupContent(content, paperCreateRequest.file());
        Paper paper = new Paper(
                paperCreateRequest.title(),
                paperCreateRequest.abstractText(),
                String.join(",", authors),
                paperCreateRequest.keywords()
        );
        PaperUser paperUser = new PaperUser(
                new PaperUserId(paper.getId(), securityUser.user().getId()),
                paper,
                securityUser.user(),
                RoleType.ROLE_AUTHOR
        );

        Set<PaperUser> paperUsers = Set.of(paperUser);
        paper.setPaperUsers(paperUsers);
        this.paperRepository.save(paper);
        this.paperUserRepository.save(paperUser);

        content.setPaper(paper);
        this.contentRepository.save(content);

        return paper.getId();
    }

    void updatePaper(Long id, PaperUpdateRequest paperUpdateRequest, Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (!this.paperUserRepository.existsByPaperIdUserIdAndRoleType(id, securityUser.user().getId(),
                RoleType.ROLE_AUTHOR)) {
            logger.error("Paper update failed. User with id: {} is not author for paper with id: {}",
                    securityUser.user().getId(), id);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

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

            this.paperRepository.save(paper);
        }, () -> {
            logger.error("Paper update failed: Paper not found with: {}", id);

            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + id);
        });
    }

    /*
        securityUser = user who made the request, author of the paper
        user = co-author, retrieved based on the id
     */
    void addCoAuthor(Long id, AuthorAdditionRequest authorAdditionRequest, Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (!this.paperUserRepository.existsByPaperIdUserIdAndRoleType(id,
                securityUser.user().getId(),
                RoleType.ROLE_AUTHOR)) {
            logger.error("Co-author addition failed. User with id: {} is not author for paper with id: {}",
                    securityUser.user().getId(), id);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        this.paperRepository.findById(id).ifPresentOrElse(paper -> {
            /*
                Case: The user who is to be added as a co-author is not found, meaning it's not a registered user in
                our system
             */
            User coAuthor = this.userRepository.findById(authorAdditionRequest.userId()).orElseThrow(() -> {
                logger.error("Co-author addition failed. User with id: {} was not found to be added as co-author",
                        authorAdditionRequest.userId());
                return new ResourceNotFoundException("User not found with id: " + authorAdditionRequest.userId() +
                        " to be added as co-author");
            });

            /*
                If the user to be added as a co-author already is or the user who made the request, requested themselves
                to be added as author.
             */
            if (this.paperUserRepository.existsByPaperIdUserIdAndRoleType(id, coAuthor.getId(), RoleType.ROLE_AUTHOR)
                    || coAuthor.getId().equals(securityUser.user().getId())) {
                logger.error("Co-author addition failed. User with id: {} is already an author for paper with id: {}",
                        coAuthor.getId(), id);
                throw new DuplicateResourceException("User with name: " + coAuthor.getFullName() + " is already an " +
                        "author for the paper with id: " + id);
            }

            /*
                If the user(co-author) was assigned a new role we update the entry in the db
             */
            if (this.roleService.assignRole(coAuthor, RoleType.ROLE_AUTHOR)) {
                logger.info("Author addition request: User with id: {} was assigned a new role type: {}",
                        coAuthor.getId(), RoleType.ROLE_AUTHOR);
                this.userRepository.save(coAuthor);
            }

            /*
                 List<String> authors = Arrays.asList(paper.getAuthors().split(",")); We could not call create our list
                 that way because Arrays.asList() returns a fixed-size list backed by the original array. This means
                 that the list cannot be structurally modified (we can't add or remove elements from it) When we called
                 authors.add(author) we would get an UnsupportedOperationException
             */
            List<String> authors = new ArrayList<>(Arrays.asList(paper.getAuthors().split(",")));
            authors.add(coAuthor.getFullName());

            /*
                Case: We don't check if the author's name is already part of the authors in the paper because we could
                have 2 authors with the same name. If it was the addition of an author that's already an author for the
                paper we would have caught that previously.
             */
            String authorsCsv = String.join(",", authors);
            paper.setAuthors(authorsCsv);

            PaperUser paperUser = new PaperUser(new PaperUserId(paper.getId(), coAuthor.getId()),
                    paper,
                    coAuthor,
                    RoleType.ROLE_AUTHOR
            );
            paper.getPaperUsers().add(paperUser);
            this.paperUserRepository.save(paperUser);
            this.paperRepository.save(paper);
        }, () -> {
            logger.error("Co-Author addition failed. Paper not found with id: {}", id);
            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + id);
        });
    }

    Long reviewPaper(Long paperId, ReviewCreateRequest reviewCreateRequest, Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        if (!this.paperUserRepository.existsByPaperIdUserIdAndRoleType(paperId,
                securityUser.user().getId(),
                RoleType.ROLE_REVIEWER)) {
            logger.error("Review failed. Reviewer with id: {} is not assigned to paper with id: {}",
                    securityUser.user().getId(), paperId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        Paper paper = this.paperRepository.findById(paperId).orElseThrow(() -> {
            logger.error("Review failed. Paper not found with id: {}", paperId);

            return new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperId);
        });

        if(paper.getConference() == null) {
            logger.error("Review failed. Paper with id: {} is not submitted to any conference but was assigned a " +
                    "reviewer", paperId);

            throw new ServerErrorException("The server encountered an internal error and was unable to " +
                    "complete your request. Please try again later");
        }

        if (!paper.getConference().getState().equals(ConferenceState.REVIEW)) {
            logger.error("Review failed. Conference with id: {} is in state: {} and can not review papers",
                    paper.getConference().getId(), paper.getConference().getState());

            throw new StateConflictException("Conference is in the state: " + paper.getConference().getState()
                    + " and papers can not be reviewed");
        }

        if (!paper.getState().equals(PaperState.SUBMITTED)) {
            logger.error("Review failed. Paper with id: {} is not in SUBMITTED state in order to be reviewed. " +
                    "Paper state: {}", paperId, paper.getState());

            throw new StateConflictException("Paper is in state: " + paper.getState() + " and can not be reviewed");
        }

        Review review = new Review(paper,
                securityUser.user(),
                reviewCreateRequest.comment(),
                reviewCreateRequest.score());
        this.reviewRepository.save(review);
        paper.setState(PaperState.REVIEWED);
        this.paperRepository.save(paper);

        return review.getId();
    }

    void withdrawPaper(Long paperId, Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (!this.paperUserRepository.existsByPaperIdUserIdAndRoleType(paperId, securityUser.user().getId(),
                RoleType.ROLE_AUTHOR)) {
            logger.error("Paper withdrawal failed. User with id: {} is not author for paper with id: {}",
                    securityUser.user().getId(), paperId);

            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        this.paperRepository.findById(paperId).ifPresentOrElse(paper ->  {
            if(paper.getConference() == null) {
                logger.error("Review failed. Paper with id: {} is not submitted to any conference but was assigned a " +
                        "reviewer", paperId);

                throw new StateConflictException("You can not withdraw a paper that has not been submitted to any " +
                        "conference");
            }

            paper.setState(PaperState.CREATED);
            paper.setConference(null);
            this.paperRepository.save(paper);
        }, () -> {
            logger.error("Paper withdrawal failed. Paper not found with id: {}", paperId);

            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperId);
        });
    }

    /*
        Based on the role of the user that makes the GET request for a paper, we would have to return different
        properties of the PaperDTO.
     */
    PaperDTO findPaperById(Long paperId, SecurityContext context) {
        Paper paper = this.paperRepository.findById(paperId).orElseThrow(() -> {
            logger.error("Paper not found with id: {}", paperId);

            return new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + paperId);
        });

        if (context.getAuthentication().getPrincipal() instanceof SecurityUser securityUser) {
            if (paper.getConference() != null && this.conferenceUserRepository.existsByConferenceIdAndUserId(
                    paper.getConference().getId(), securityUser.user().getId())) {
                return this.pcChairPaperDTOMapper.apply(paper);
            }

            if (this.paperUserRepository.existsByPaperIdUserIdAndRoleType(paperId, securityUser.user().getId(),
                    RoleType.ROLE_AUTHOR)) {
                return this.authorPaperDTOMapper.apply(paper);
            }

            if (this.paperUserRepository.existsByPaperIdUserIdAndRoleType(paperId, securityUser.user().getId(),
                    RoleType.ROLE_REVIEWER)) {
                return this.reviewerPaperDTOMapper.apply(paper);
            }
        }

        /*
            For any other case, visitor, reviewer but not in the requested paper, PC_CHAIR but the requested paper is
            not assigned to their conference we return the same properties.
         */
        return this.paperDTOMapper.apply(paper);
    }

    /*
        The users that can download the paper file are:
            1)Authors of the paper
            2)Reviewers of the paper
            3)Pc chairs that the requested paper is submitted to their conference

        Any other case would result in 404
     */
    PaperFile downloadPaperFile(Long id, Authentication authentication) {
        Paper paper = this.paperRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                PAPER_NOT_FOUND_MSG + id));
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        if (!this.paperUserRepository.existsByPaperIdUserIdAndRoleType(id, securityUser.user().getId(),
                RoleType.ROLE_AUTHOR)
                && !this.paperUserRepository.existsByPaperIdUserIdAndRoleType(id, securityUser.user().getId(),
                RoleType.ROLE_REVIEWER)
                && (paper.getConference() == null || !this.conferenceUserRepository.existsByConferenceIdAndUserId(
                        paper.getConference().getId(), securityUser.user().getId()))) {
            throw new ResourceNotFoundException(PAPER_NOT_FOUND_MSG + id);
        }

        Resource file = this.fileService.getFile(paper.getContent().getGeneratedFileName());

        return new PaperFile(file, paper.getContent().getOriginalFileName());
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
