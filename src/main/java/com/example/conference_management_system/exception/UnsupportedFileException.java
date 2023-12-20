package com.example.conference_management_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedFileException extends RuntimeException {

    public UnsupportedFileException(String message) {
        super(message);
    }
}
