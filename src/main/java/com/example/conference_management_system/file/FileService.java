package com.example.conference_management_system.file;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.example.conference_management_system.exception.ServerErrorException;

/*
    This class should have been a FilesUtil class not a Bean
 */
@Service
public class FileService {
    private final String paperDirectoryPath;
    private final Tika tika;
    private final Map<String, String> supportedMimeTypes;
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later";

    public FileService(@Value("${papers.directory}") String paperDirectoryPath) {
        this.paperDirectoryPath = paperDirectoryPath;
        this.tika = new Tika();
        this.supportedMimeTypes = new HashMap<>();
        this.supportedMimeTypes.put("application/pdf", ".pdf");
        this.supportedMimeTypes.put("application/x-tex", ".tex");
    }

    public boolean isFileSupported(MultipartFile file) {
        try {
            String contentType = this.tika.detect(file.getInputStream());
            return this.supportedMimeTypes.containsKey(contentType);
        } catch (IOException ioe) {
            logger.warn("Error detecting file type for {}: {}", file.getOriginalFilename(), ioe.getMessage());
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void saveFile(MultipartFile file, String fileName) {
        try {
            Path path = Paths.get(paperDirectoryPath + File.separator + fileName);
            Files.write(path, file.getBytes());
        } catch (IOException ioe) {
            logger.warn("Error storing file {}: {}", fileName , ioe.getMessage());
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void deleteFile(String fileName) {
        Path path = Paths.get(paperDirectoryPath + File.separator + fileName);
        try {
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Successfully deleted file: {}", fileName);
            } else {
                logger.warn("File to delete not found: {}", fileName);
            }
        } catch (IOException ioe) {
            logger.warn("Error deleting file {}: {}", fileName, ioe.getMessage());
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    /*
        Since we are retrieving the file as part of the GET request to retrieve a paper, if paper is not found we
        already threw ResourceNotFoundException so in the case of not finding a file for a paper that exists which is
        the case for the else block we will return 500 because a paper can not exist without a pdf or a latex file.
     */
    public Resource getFile(String fileName)  {
        try {
            Path path = Paths.get(paperDirectoryPath + File.separator + fileName);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists()) {
                logger.info("Successfully retrieved file: {}", fileName);
                return resource;
            } else {
                logger.warn("Failed to retrieve file: {}", fileName);
                throw new ServerErrorException(SERVER_ERROR_MSG);
            }
        } catch (MalformedURLException mue) {
            logger.warn("Malformed URL for file: {}, Error: {}", fileName, mue.getMessage());
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public String findFileExtension(MultipartFile file) {
        try {
            String contentType = this.tika.detect(file.getInputStream());
            return this.supportedMimeTypes.get(contentType);
        } catch (IOException ioe) {
            logger.warn("Error finding file extension for {}: {}", file.getOriginalFilename(), ioe.getMessage());
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }
}
