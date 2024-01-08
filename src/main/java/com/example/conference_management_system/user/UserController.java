package com.example.conference_management_system.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.example.conference_management_system.user.dto.UserDTO;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserController {
    private final UserService userService;

    @GetMapping
    ResponseEntity<UserDTO> findUserByUsername(@RequestParam("username") String username) {
        UserDTO user = this.userService.findUserByUsername(username);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
