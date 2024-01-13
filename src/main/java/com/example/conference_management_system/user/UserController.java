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

    /*
        This endpoint is tested in the IT of both PaperIT and ConferenceIT when we search for a user by username
        to verify they get a new role after creating paper/conference
     */
    @GetMapping
    ResponseEntity<UserDTO> findUserByUsername(@RequestParam("username") String username) {
        UserDTO user = this.userService.findUserByUsername(username);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /*
        We don't have endpoints to return resources for the active user, if we wanted we could do something
        like /user/conferences or /user/papers
     */
}
