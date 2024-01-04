package com.example.conference_management_system.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
class ApiExceptionHandler {

    /*
        Thrown by the @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ma) {
        String errorMessage = ma.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ApiError apiError = new ApiError(errorMessage);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    private ResponseEntity<ApiError> handleDuplicateResourceException(DuplicateResourceException dre) {
        ApiError apiError = new ApiError(dre.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    private ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException se) {
        ApiError apiError = new ApiError(se.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException iae) {
        ApiError apiError = new ApiError(iae.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StateConflictException.class)
    private ResponseEntity<ApiError> handleStateConflictException(StateConflictException sc) {
        ApiError apiError = new ApiError(sc.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ServerErrorException.class)
    private ResponseEntity<ApiError> handleServerErrorException(ServerErrorException see) {
        ApiError apiError = new ApiError(see.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UnsupportedFileException.class)
    private ResponseEntity<ApiError> handleUnsupportedFileException(UnsupportedFileException ufe) {
        ApiError apiError = new ApiError(ufe.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    private ResponseEntity<ApiError> handleUnauthorizedException(UnauthorizedException ue) {
        ApiError apiError = new ApiError(ue.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    /*
        The below exception is thrown when the user has the Role specified in the authorization rules at the
        @PreAuthorize, but they are not the owner of the resource.

        An example would be the user could PC_CHAIR as role, but they need to have that role for the requested
        conference
     */
    @ExceptionHandler(AccessDeniedException.class)
    private ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ad) {
        ApiError apiError = new ApiError(ad.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }
}