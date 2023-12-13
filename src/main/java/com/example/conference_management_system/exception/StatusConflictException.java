package com.example.conference_management_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class StatusConflictException extends RuntimeException {

    public StatusConflictException(String message) {
        super(message);
    }
}