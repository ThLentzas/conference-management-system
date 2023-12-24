package com.example.conference_management_system.paper;

import org.springframework.web.multipart.MultipartFile;

record PaperUpdateRequest(String title, String abstractText, String authors, String keywords, MultipartFile file) {
}
