package com.example.conference_management_system.paper;

import com.example.conference_management_system.conference.ConferenceState;
import com.example.conference_management_system.content.ContentRepository;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.Content;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.PaperUser;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.key.ConferenceUserId;
import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.exception.ServerErrorException;
import com.example.conference_management_system.exception.StateConflictException;
import com.example.conference_management_system.exception.UnsupportedFileException;
import com.example.conference_management_system.paper.dto.PaperFile;
import com.example.conference_management_system.review.dto.ReviewCreateRequest;
import com.example.conference_management_system.user.UserService;
import com.example.conference_management_system.utility.FileService;
import com.example.conference_management_system.paper.dto.PaperCreateRequest;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.paper.dto.AuthorAdditionRequest;
import com.example.conference_management_system.paper.dto.PaperUpdateRequest;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class PaperServiceTest {
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private PaperUserRepository paperUserRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserService userService;
    @Mock
    private AuthService authService;
    @Mock
    private RoleService roleService;
    @Mock
    private FileService fileService;

    private PaperService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new PaperService(
                paperRepository,
                paperUserRepository,
                contentRepository,
                reviewRepository,
                userService,
                authService,
                roleService,
                fileService
        );
    }

    //createPaper()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenTitleIsBlank() throws IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "",
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must provide the title of the paper");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTitleExceedsMaxLength() throws IOException {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        //Generating a random alphanumeric string of minimum length of 101.
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(100) + 101),
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title must not exceed 100 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"authors 1, ", ""})
    void shouldThrowIllegalArgumentExceptionForEmptyAuthors(String authorsCsv) throws IOException {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        //Generating a random alphanumeric string of minimum length of 101.
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "title",
                "abstractText",
                authorsCsv,
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Every author you provide must have a name");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAbstractTextIsBlank() throws IOException {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "title",
                "",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must provide the abstract text of the paper");
    }

    @ParameterizedTest
    @ValueSource(strings = {"keyword 1, ", ""})
    void shouldThrowIllegalArgumentExceptionForEmptyKeywords(String keywordsCsv) throws IOException {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        //Generating a random alphanumeric string of minimum length of 101.
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "title",
                "abstractText",
                "author 1, author 2",
                keywordsCsv,
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Every keyword you provide must have a value");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFileNameContainsInvalidCharacters() throws Exception {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test@.pdf",
                "application/pdf",
                getFileContent());
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "title",
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The file name must contain only alphanumeric characters, hyphen, underscores spaces, " +
                        "and periods");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFileNameExceedsMaxLength() throws Exception {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(100) + 101) + ".pdf",
                "application/pdf",
                getFileContent());
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "title",
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File name must not exceed 100 characters");
    }

    @Test
    void shouldThrowUnsupportedFileExceptionWhenFileTypeIsNotSupported() throws Exception {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        Path imagePath = ResourceUtils.getFile("classpath:files/test.png").toPath();
        byte[] imageContent = Files.readAllBytes(imagePath);
        MultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                imageContent);
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "title",
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                imageFile
        );
        SecurityUser securityUser = getSecurityUser();

        when(this.fileService.isFileSupported(imageFile)).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(UnsupportedFileException.class)
                .hasMessage("The provided file is not supported. Make sure your file is either a pdf or a Latex one");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenTitleExistsOnCreatePaper() throws Exception {
        //Arrange
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        PaperCreateRequest paperCreateRequest = new PaperCreateRequest(
                "title",
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        when(this.paperRepository.existsByTitleIgnoreCase(paperCreateRequest.title())).thenReturn(true);
        when(this.fileService.isFileSupported(pdfFile)).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, securityUser, httpServletRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A paper with the provided title already exists");
    }

    //updatePaper()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenAllPropertiesAreNullOnUpdatePaper() {
        //Arrange
        PaperUpdateRequest paperUpdateRequest = new PaperUpdateRequest(
                null,
                null,
                null,
                null,
                null
        );
        SecurityUser securityUser = getSecurityUser();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaper(1L, paperUpdateRequest, securityUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must provide at least one property to update the paper");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnUpdatePaper() throws Exception {
        //Arrange
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        PaperUpdateRequest paperUpdateRequest = new PaperUpdateRequest(
                "title",
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();

        when(this.paperRepository.findByPaperIdFetchingPaperUsers(1L)).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaper(1L, paperUpdateRequest, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }


    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotPaperAuthorOnUpdatePaper() throws Exception {
        //Arrange
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                getFileContent());
        PaperUpdateRequest paperUpdateRequest = new PaperUpdateRequest(
                "title",
                "abstractText",
                "author 1, author2",
                "keyword 1, keyword 2",
                pdfFile
        );
        SecurityUser securityUser = getSecurityUser();
        Paper paper = getPaper(1L);

        when(this.paperRepository.findByPaperIdFetchingPaperUsers(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaper(1L, paperUpdateRequest, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    //addCoAuthor()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnAuthorAddition() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(1L);
        SecurityUser securityUser = getSecurityUser();

        when(this.paperRepository.findByPaperIdFetchingPaperUsers(1L)).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotPaperAuthorOnCoAuthorAddition() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(1L);
        SecurityUser securityUser = getSecurityUser();
        Paper paper = getPaper(1L);
        paper.setPaperUsers(new HashSet<>());

        when(this.paperRepository.findByPaperIdFetchingPaperUsers(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenUserToBeAddedAsCoAuthorIsAlreadyReviewerForThePaper() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(2L);
        SecurityUser securityUser = getSecurityUser();
        User coAuthor = new User("test", "test", "Test User", Set.of(new Role(RoleType.ROLE_REVIEWER)));
        coAuthor.setId(2L);

        Paper paper = getPaper(1L);
        PaperUser paperUser1 = getPaperUser(paper, securityUser.user(), RoleType.ROLE_AUTHOR);
        PaperUser paperUser2 = getPaperUser(paper, coAuthor, RoleType.ROLE_REVIEWER);
        paper.setPaperUsers(Set.of(paperUser1, paperUser2));

        when(this.paperRepository.findByPaperIdFetchingPaperUsers(1L)).thenReturn(Optional.of(paper));
        when(this.userService.findUserByIdFetchingRoles(2L)).thenReturn(coAuthor);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("The user to be added as co-author is already added as a reviewer");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenAddingExistingCoAuthor() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(1L);
        SecurityUser securityUser = getSecurityUser();

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_AUTHOR);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.paperRepository.findByPaperIdFetchingPaperUsers(1L)).thenReturn(Optional.of(paper));
        when(this.userService.findUserByIdFetchingRoles(1L)).thenReturn(securityUser.user());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, securityUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with name: " + securityUser.user().getFullName() + " is already an author for the " +
                        "paper with id: " + 1L);
    }

    //reviewPaper()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnReviewPaper() {
        //Arrange
        ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest("comment", 6.1);
        SecurityUser securityUser = getSecurityUser();

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.reviewPaper(1L, reviewCreateRequest, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotAssignedAsPaperReviewerOnReviewPaper() {
        //Arrange
        ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest("comment", 6.1);
        SecurityUser securityUser = getSecurityUser();
        Paper paper = getPaper(1L);

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.reviewPaper(1L, reviewCreateRequest, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowServerErrorExceptionWhenPaperIsNotSubmittedToAnyConferenceForReview() {
        //Arrange
        ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest("comment", 6.1);
        SecurityUser securityUser = getSecurityUser();

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_REVIEWER);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.reviewPaper(1L, reviewCreateRequest, securityUser))
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("The server encountered an internal error and was unable to complete your request. " +
                        "Please try again later");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperConferenceIsNotInReviewState() {
        //Arrange
        ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest("comment", 6.1);
        SecurityUser securityUser = getSecurityUser();

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_REVIEWER);
        paper.setPaperUsers(Set.of(paperUser));
        paper.setConference(new Conference());

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.reviewPaper(1L, reviewCreateRequest, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Conference is in the state: " + paper.getConference().getState() + " and papers can not " +
                        "be " + "reviewed");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenPaperIsNotInSubmittedStateOnReviewPaper() {
        //Arrange
        ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest("comment", 6.1);
        SecurityUser securityUser = getSecurityUser();

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_REVIEWER);
        Conference conference = new Conference();
        conference.setState(ConferenceState.REVIEW);
        paper.setConference(conference);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.reviewPaper(1L, reviewCreateRequest, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Paper is in state: " + paper.getState() + " and can not be reviewed");
    }

    //withdrawPaper()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnWithdrawPaper() {
        //Arrange
        SecurityUser securityUser = getSecurityUser();

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.withdrawPaper(1L, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotPaperAuthorOnWithdrawPaper() {
        //Arrange
        SecurityUser securityUser = getSecurityUser();
        Paper paper = getPaper(1L);
        paper.setPaperUsers(new HashSet<>());

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.withdrawPaper(1L, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowStateConflictExceptionWhenToBeWithdrawnPaperIsNotSubmittedToAnyConference() {
        //Arrange
        SecurityUser securityUser = getSecurityUser();

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_AUTHOR);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.withdrawPaper(1L, securityUser))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("You can not withdraw a paper that has not been submitted to any conference");
    }

    //downloadPaper()
    @ParameterizedTest
    @EnumSource(value = RoleType.class, names = {"ROLE_AUTHOR", "ROLE_REVIEWER"})
    void shouldDownloadPaperForPaperAuthorOrReviewerOnDownloadPaper(RoleType roleType) throws Exception {
        SecurityUser securityUser = getSecurityUser();

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), roleType);
        paper.setPaperUsers(Set.of(paperUser));

        String generatedFileName = UUID.randomUUID().toString();
        Content content = new Content("test.pdf", generatedFileName, ".pdf");
        content.setId(paper.getId());
        Resource resource = new UrlResource(ResourceUtils.getFile("classpath:files/test.pdf").toPath().toUri());

        PaperFile expected = new PaperFile(resource, content.getOriginalFileName());

        when(this.paperRepository.findPaperGraphById(1L)).thenReturn(Optional.of(paper));
        when(this.contentRepository.findByPaperId(1L)).thenReturn(Optional.of(content));
        when(this.fileService.getFile(content.getGeneratedFileName())).thenReturn(resource);

        PaperFile actual = this.underTest.downloadPaperFile(1L, securityUser);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldDownloadPaperForPcChairAtConferencePaperHasBeenSubmittedTo() throws Exception {
        //Arrange
        SecurityUser securityUser = getSecurityUser();
        securityUser.user().setRoles(Set.of(new Role(RoleType.ROLE_PC_CHAIR)));

        Paper paper = getPaper(1L);
        Conference conference = new Conference();
        ConferenceUser conferenceUser = getConferenceUser(conference, securityUser.user());
        conference.setConferenceUsers(Set.of(conferenceUser));
        paper.setConference(conference);

        String generatedFileName = UUID.randomUUID().toString();
        Content content = new Content("test.pdf", generatedFileName, ".pdf");
        content.setId(paper.getId());
        Resource resource = new UrlResource(ResourceUtils.getFile("classpath:files/test.pdf").toPath().toUri());
        PaperFile expected = new PaperFile(resource, content.getOriginalFileName());

        when(this.paperRepository.findPaperGraphById(1L)).thenReturn(Optional.of(paper));
        when(this.contentRepository.findByPaperId(1L)).thenReturn(Optional.of(content));
        when(this.fileService.getFile(content.getGeneratedFileName())).thenReturn(resource);

        //Act
        PaperFile actual = this.underTest.downloadPaperFile(1L, securityUser);

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnDownloadPaper() {
        //Arrange
        SecurityUser securityUser = getSecurityUser();

        when(this.paperRepository.findPaperGraphById(1L)).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.downloadPaperFile(1L, securityUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    @Test
    @DisplayName("The requesting user is not AUTHOR or REVIEWER of the paper or PC_CHAIR at conference the paper has" +
            " been submitted to")
    void shouldThrowAccessDeniedExceptionOnDownloadPaper() {
        //Arrange
        SecurityUser securityUser = getSecurityUser();
        Paper paper = getPaper(1L);

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndConference(1L)).thenReturn(Optional.of(paper));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.withdrawPaper(1L, securityUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowServerErrorExceptionWhenContentIsNotFoundOnDownloadPaper() {
        //Arrange
        SecurityUser securityUser = getSecurityUser();

        Paper paper = getPaper(1L);
        PaperUser paperUser = getPaperUser(paper, securityUser.user(), RoleType.ROLE_AUTHOR);
        paper.setPaperUsers(Set.of(paperUser));

        when(this.paperRepository.findPaperGraphById(1L)).thenReturn(Optional.of(paper));
        when(this.contentRepository.findByPaperId(1L)).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.downloadPaperFile(1L, securityUser))
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("The server encountered an internal error and was unable to complete your request. " +
                        "Please try again later");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnFindPaperById() {
        //Arrange
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        when(this.paperRepository.findByPaperIdFetchingPaperUsersAndReviews(1L)).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.findPaperById(1L, securityContext))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: " + 1L);
    }

    private SecurityUser getSecurityUser() {
        User user = new User("username", "password", "Full Name", Set.of(new Role(RoleType.ROLE_AUTHOR)));
        user.setId(1L);

        return new SecurityUser(user);
    }

    private Paper getPaper(Long paperId) {
        Paper paper = new Paper();
        paper.setId(paperId);
        paper.setTitle("title");
        paper.setPaperUsers(new HashSet<>());

        return paper;
    }

    private PaperUser getPaperUser(Paper paper, User user, RoleType roleType) {
        return new PaperUser(
                new PaperUserId(paper.getId(), user.getId()),
                paper,
                user,
                roleType
        );
    }

    private ConferenceUser getConferenceUser(Conference conference, User user) {
        return new ConferenceUser(
                new ConferenceUserId(conference.getId(), user.getId()),
                conference,
                user
        );
    }


    private byte[] getFileContent() throws IOException {
        Path pdfPath = ResourceUtils.getFile("classpath:files/test.pdf").toPath();

        return Files.readAllBytes(pdfPath);
    }
}
