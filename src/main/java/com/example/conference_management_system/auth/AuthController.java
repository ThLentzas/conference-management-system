package com.example.conference_management_system.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    ResponseEntity<Void> registerUser(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        Authentication authentication = this.authService.registerUser(request);
        setContext(authentication, session);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/login")
    ResponseEntity<Void> loginUser(@Valid @RequestBody LoginRequest request, HttpSession session) {
        Authentication authentication = this.authService.loginUser(request);
        setContext(authentication, session);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csrf")
    ResponseEntity<CsrfToken> csrf(CsrfToken token) {
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    /*
        By using Spring Security Login page and letting spring security handle the authentication via the form it will
        set the authentication object in the SecurityContext and the SecurityContext as the value in the Spring Security
        Context key attribute of the active session. When we try to authorize a user in a subsequent request we look at
        the authorities of the authentication object of the Spring Security Context attribute from the session with the
        session id we retrieved from the Cookie.

        If we have our login/signup endpoints we have to manually set the authentication in the context and then set
        the Spring Security Context key attribute. That way when we try to authorize the user for subsequent requests
        we could extract the Spring Security Context key attribute and that won't be null. Otherwise, it would result in
        403 FORBIDDEN
     */
    private static void setContext(Authentication authentication, HttpSession session) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}