package com.example.conference_management_system.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileServiceTest {
    @TempDir
    Path tempDir;
    private FileService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new FileService(tempDir.toString());
    }

    /*
        isFileSupported()

        In order to create files with the correct content (pdf, tex) it's not enough to set the 3rd argument of the
        MockMultipartFile as application/pdf, application/x-tex. We need the actual content of the file to be pdf,latex,
        so we store in our test resources 3 files: 1 pdf, 1 tex and 1 png. We can then read the content of this file and
        create our own. The png file is to test for non-supported files
     */
    @Test
    void shouldReturnTrueWhenFileIsSupportedForPdfFile() throws IOException {
        //Arrange
        Path pdfPath = ResourceUtils.getFile("src/test/resources/files/test.pdf").toPath();
        byte[] pdfContent = Files.readAllBytes(pdfPath);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfContent);

        //Act & Assert
        assertThat(this.underTest.isFileSupported(pdfFile)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenFileIsSupportedForLatexFile() throws IOException {
        //Arrange
        Path latexPath = ResourceUtils.getFile("src/test/resources/files/test.tex").toPath();
        byte[] latexContent = Files.readAllBytes(latexPath);
        MultipartFile latexFile = new MockMultipartFile(
                "file",
                "test.tex",
                "application/x-tex",
                latexContent);

        //Act & Assert
        assertThat(this.underTest.isFileSupported(latexFile)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFileIsNotSupported() throws IOException {
        //Arrange
        Path imagePath = ResourceUtils.getFile("src/test/resources/files/test.png").toPath();
        byte[] imageContent = Files.readAllBytes(imagePath);
        MultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                imageContent);

        //Act & Assert
        assertThat(this.underTest.isFileSupported(imageFile)).isFalse();
    }

    //saveFile()
    @Test
    void shouldSaveFile() throws IOException {
        //Arrange
        String fileName = UUID.randomUUID().toString();
        Path pdfPath = ResourceUtils.getFile("src/test/resources/files/test.pdf").toPath();
        byte[] pdfContent = Files.readAllBytes(pdfPath);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfContent);

        //Act
        this.underTest.saveFile(pdfFile, fileName);
        Path storedFile = tempDir.resolve(fileName);

        //Assert
        assertThat(Files.exists(storedFile)).isTrue();
    }

    //deleteFile()
    @Test
    void shouldDeleteFile() throws IOException {
        //Arrange
        String fileName = UUID.randomUUID().toString();
        Path pdfPath = ResourceUtils.getFile("src/test/resources/files/test.pdf").toPath();
        byte[] pdfContent = Files.readAllBytes(pdfPath);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfContent);

        this.underTest.saveFile(pdfFile, fileName);

        //Act
        this.underTest.deleteFile(fileName);
        Path storedFile = tempDir.resolve(fileName);

        //Assert
        assertThat(Files.exists(storedFile)).isFalse();
    }

    //findFileExtension()
    @Test
    void shouldFindFileExtension() throws IOException {
        //Arrange
        Path pdfPath = ResourceUtils.getFile("src/test/resources/files/test.pdf").toPath();
        byte[] pdfContent = Files.readAllBytes(pdfPath);
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfContent);
        String expected = ".pdf";

        //Act
        String actual = this.underTest.findFileExtension(pdfFile);

        //Assert
        assertThat(actual).isEqualTo(expected);
    }
}
