package com.example.conference_management_system.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

/*
    We need to set the @Retention to RUNTIME, because by default it is SOURCE and our annotation will not be visible
    to be intercepted via reflection at apps execution

    https://www.youtube.com/watch?v=onD_fyhy58o&list=PLEocw3gLFc8X_a8hGWGaBnSkPFJmbb8QP&index=39

    @WithSecurityContext(factory = CustomSecurityContextFactory.class) Whenever a test sees our custom annotation we
    specify which security context to be used.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = CustomSecurityContextFactory.class)
public @interface WithMockCustomUser {
    String username() default "user";
    String password() default "password";
    String[] roles();
}