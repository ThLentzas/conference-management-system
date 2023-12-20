package com.example.conference_management_system.utility;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.example.conference_management_system.exception.ServerErrorException;

@Service
public class FileService {
    @Value("${papers.directory}")
    private String paperDirectoryPath;
    private final Tika tika;
    private final Map<String, String> supportedMimeTypes;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later";

    public FileService() {
        this.tika = new Tika();
        this.supportedMimeTypes = new HashMap<>();
        this.supportedMimeTypes.put("application/pdf", ".pdf");
        this.supportedMimeTypes.put("application/x-tex", ".tex");
    }

    public boolean isFileSupported(MultipartFile file) {
        boolean supported;

        try {
            String contentType = this.tika.detect(file.getInputStream());
            supported = this.supportedMimeTypes.containsKey(contentType);
        } catch (IOException ioe) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }

        return supported;
    }

    public void storeFile(MultipartFile file, String generatedFileName) {
        try {
            Path path = Paths.get(paperDirectoryPath + File.separator + generatedFileName);
            Files.write(path, file.getBytes());
        } catch (IOException ioe) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public String findFileExtension(MultipartFile file) {
        String fileExtension;

        try {
            String contentType = this.tika.detect(file.getInputStream());
            fileExtension = supportedMimeTypes.get(contentType);
        } catch (IOException ioe) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }

        return fileExtension;
    }
}
