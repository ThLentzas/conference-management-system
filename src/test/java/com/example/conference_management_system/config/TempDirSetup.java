package com.example.conference_management_system.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.annotation.PreDestroy;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TempDirSetup {
    static Path tempDir;

    static {
        try {
            tempDir = Files.createTempDirectory("papers");
            System.setProperty("papers.directory", tempDir.toString());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @PreDestroy
    void cleanup() throws IOException {
        Files.deleteIfExists(tempDir);
    }
}