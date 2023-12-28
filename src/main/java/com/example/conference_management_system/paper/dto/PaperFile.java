package com.example.conference_management_system.paper.dto;

import org.springframework.core.io.Resource;

public record PaperFile(Resource file, String originalFileName) {
}
