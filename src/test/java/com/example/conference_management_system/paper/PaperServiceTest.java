package com.example.conference_management_system.paper;

import com.example.conference_management_system.auth.AuthService;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.review.ReviewService;
import com.example.conference_management_system.role.RoleService;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.example.conference_management_system.content.ContentRepository;
import com.example.conference_management_system.utility.FileService;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.exception.UnsupportedFileException;

@ExtendWith(MockitoExtension.class)
class PaperServiceTest {
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private AuthService authService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private FileService fileService;
    private PaperService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new PaperService(
                paperRepository,
                contentRepository,
                userRepository,
                roleService,
                authService,
                reviewService,
                fileService);
    }

    @Test
    void shouldCreatePaper() throws IOException {
        //Arrange
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
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        User user = new User();
        user.setId(1L);
        user.setFullName("author 3");
        SecurityUser securityUser = new SecurityUser(user);

        Paper paper = new Paper(
                "title",
                "abstractText",
                "author 1, author2, author3",
                "keyword 1, keyword 2",
                Set.of(user));
        paper.setId(1L);

        when(this.authService.getAuthenticatedUser()).thenReturn(Optional.of(securityUser));
        when(this.paperRepository.existsByTitleIgnoreCase(any(String.class))).thenReturn(false);
        doNothing().when(this.roleService).assignRole(
                any(SecurityUser.class),
                any(RoleType.class),
                any(HttpServletRequest.class));
        doNothing().when(this.fileService).saveFile(any(MultipartFile.class), any(String.class));
        when(this.fileService.isFileSupported(any(MultipartFile.class))).thenReturn(true);
        when(this.paperRepository.save(any(Paper.class))).thenReturn(paper);

        //Act
        Long actual = this.underTest.createPaper(paperCreateRequest, httpServletRequest);

        //Assert
        assertThat(actual).isEqualTo(1L);
        verify(this.fileService, times(1)).saveFile(
                any(MultipartFile.class),
                any(String.class));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTitleIsBlankInPaperCreateRequest() throws IOException {
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

        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must provide the title of the paper");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTitleExceedsMaxLengthInPaperCreateRequest() throws IOException {
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

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title must not exceed 100 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"authors 1, ", ""})
    void shouldThrowIllegalArgumentExceptionForEmptyAuthorsInPaperCreateRequest(String authorsCsv)
            throws IOException {
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

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Every author you provide must have a name");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAbstractTextIsBlankInPaperCreateRequest() throws IOException {
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

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must provide the abstract text of the paper");
    }

    @ParameterizedTest
    @ValueSource(strings = {"keyword 1, ", ""})
    void shouldThrowIllegalArgumentExceptionForEmptyKeywordsInPaperCreateRequest(String keywordsCsv)
            throws IOException {
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

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Every keyword you provide must have a value");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFileNameContainsInvalidCharactersInPaperCreateRequest()
            throws Exception {
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

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The file name must contain only alphanumeric characters, hyphen, underscores spaces, " +
                        "and periods");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFileNameExceedsMaxLengthInPaperCreateRequest() throws Exception {
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

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File name must not exceed 100 characters");
    }

    @Test
    void shouldThrowUnsupportedFileExceptionWhenFileTypeIsNotSupportedInPaperCreateRequest() throws Exception {
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

        when(this.fileService.isFileSupported(any(MultipartFile.class))).thenReturn(false);

        //Act & Assert
        assertThatThrownBy(() -> this.underTest.createPaper(paperCreateRequest, httpServletRequest))
                .isInstanceOf(UnsupportedFileException.class)
                .hasMessage("The provided file is not supported. Make sure your file is either a pdf or a Latex one");
    }

    private byte[] getFileContent() throws IOException {
        Path pdfPath = ResourceUtils.getFile("classpath:files/test.pdf").toPath();

        return Files.readAllBytes(pdfPath);
    }
}
