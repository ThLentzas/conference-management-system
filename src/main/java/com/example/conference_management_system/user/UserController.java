package com.example.conference_management_system.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
class UserController {
    private final UserService userService;

    /*
        Returns the current authenticated user, by looking at the principal of the authentication object
     */
    @GetMapping
    ResponseEntity<UserDTO> findUser() {
        UserDTO user = this.userService.findUserById();

        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
