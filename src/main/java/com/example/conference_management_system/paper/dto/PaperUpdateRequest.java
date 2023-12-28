package com.example.conference_management_system.paper.dto;

import org.springframework.web.multipart.MultipartFile;

public record PaperUpdateRequest(String title, String abstractText, String authors, String keywords, MultipartFile file) {
}
