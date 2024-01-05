package com.example.conference_management_system.paper;

import com.example.conference_management_system.conference.ConferenceUserRepository;
import com.example.conference_management_system.content.ContentRepository;
import com.example.conference_management_system.utility.FileService;
import com.example.conference_management_system.exception.UnsupportedFileException;
import com.example.conference_management_system.conference.ConferenceRepository;
import com.example.conference_management_system.paper.dto.PaperCreateRequest;
import com.example.conference_management_system.review.ReviewRepository;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.user.UserRepository;
import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.exception.DuplicateResourceException;
import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.paper.dto.AuthorAdditionRequest;
import com.example.conference_management_system.paper.dto.PaperUpdateRequest;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private ConferenceUserRepository conferenceUserRepository;
    @Mock
    private ContentRepository contentRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private FileService fileService;
    @Mock
    private AuthService authService;
    private PaperService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new PaperService(
                paperRepository,
                paperUserRepository,
                conferenceUserRepository,
                contentRepository,
                userRepository,
                reviewRepository,
                roleService,
                fileService,
                authService);
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
        Authentication authentication = getAuthentication();

        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        when(this.fileService.isFileSupported(any(MultipartFile.class))).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
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
        Authentication authentication = getAuthentication();

        when(this.paperRepository.existsByTitleIgnoreCase(any(String.class))).thenReturn(true);
        when(this.fileService.isFileSupported(any(MultipartFile.class))).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, authentication, httpServletRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A paper with the provided title already exists");
    }

    //updatePaper()
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
        Authentication authentication = getAuthentication();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaper(1L, paperUpdateRequest, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

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
        Authentication authentication = getAuthentication();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaper(1L, paperUpdateRequest, authentication))
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
        Authentication authentication = getAuthentication();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.updatePaper(1L, paperUpdateRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: 1");
    }

    //addCoAuthor()
    @Test
    void shouldThrowAccessDeniedExceptionWhenRequestingUserIsNotPaperAuthorOnCoAuthorAddition() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(1L);
        Authentication authentication = getAuthentication();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaperIsNotFoundOnAuthorAddition() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(1L);
        Authentication authentication = getAuthentication();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paper not found with id: 1");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCoAuthorToAddIsNotFound() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(1L);
        Authentication authentication = getAuthentication();

        Paper paper = getPaper();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 1 to be added as co-author");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenCoAuthorIsAlreadyAdded() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(2L);
        Authentication authentication = getAuthentication();

        User coAuthor = new User("test", "test", "Test User", Set.of(new Role(RoleType.ROLE_AUTHOR)));
        coAuthor.setId(2L);
        Paper paper = getPaper();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.of(coAuthor));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, authentication))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with name: Test User is already an author for the paper with id: 1");

    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenRequestingUserAddsSelfAsCoAuthor() {
        //Arrange
        AuthorAdditionRequest authorAdditionRequest = new AuthorAdditionRequest(1L);
        Authentication authentication = getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Paper paper = getPaper();

        when(this.paperUserRepository.existsByPaperIdUserIdAndRoleType(any(Long.class),
                any(Long.class),
                any(RoleType.class))).thenReturn(true);
        when(this.paperRepository.findById(any(Long.class))).thenReturn(Optional.of(paper));
        when(this.userRepository.findById(any(Long.class))).thenReturn(Optional.of(securityUser.user()));

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.addCoAuthor(1L, authorAdditionRequest, authentication))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User with name: Full Name is already an author for the paper with id: 1");
    }

    private Paper getPaper() {
        Paper paper = new Paper();
        paper.setId(1L);
        paper.setTitle("title");

        return paper;
    }

    private Authentication getAuthentication() {
        User user = new User("username", "password", "Full Name", Set.of(new Role(RoleType.ROLE_AUTHOR)));
        user.setId(1L);
        SecurityUser securityUser = new SecurityUser(user);

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    private byte[] getFileContent() throws IOException {
        Path pdfPath = ResourceUtils.getFile("classpath:files/test.pdf").toPath();

        return Files.readAllBytes(pdfPath);
    }
}
