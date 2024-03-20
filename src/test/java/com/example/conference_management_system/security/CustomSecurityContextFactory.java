package com.example.conference_management_system.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.role.RoleType;

import java.util.Set;

/*
    Since we are creating a class that implements the UserDetails, we need to provide our own annotation for the mock
    user, because the default one provider by Spring Security is not enough in this case, we might have extra properties
    in our class. Creating that annotation will allow us to covert that to a custom mock spring security context that
    will use the custom authentication object.

    If we don't implement that, in a scenario where we test the below endpoint
        ResponseEntity<Void> createConference(@Valid @RequestBody ConferenceCreateRequest conferenceCreateRequest,
                                          @AuthenticationPrincipal SecurityUser securityUser,
                                          UriComponentsBuilder uriBuilder,
                                          HttpServletRequest servletRequest)
    using the default @WithMockUser the SecurityUser securityUser will be null, because the principal of the
    default authentication object provided by Spring is of type UserDetails and our tests will fail.

    https://www.youtube.com/watch?v=onD_fyhy58o&list=PLEocw3gLFc8X_a8hGWGaBnSkPFJmbb8QP&index=39
 */
public class CustomSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    /*
        This is the logic behind our custom annotation. We use the default values of our annotation and for the role we
        pass the value specified @WithMockCustomUser(roles = "ROLE_PC_CHAIR"). If we had more roles we have to adjust
        the logic
     */
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext testSecurityContext = SecurityContextHolder.createEmptyContext();
        User user = new User();
        user.setUsername(annotation.username());
        user.setPassword(annotation.password());
        user.setRoles(Set.of(new Role(RoleType.valueOf(annotation.roles()[0]))));

        SecurityUser principal = new SecurityUser(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        testSecurityContext.setAuthentication(authentication);

        return testSecurityContext;
    }
}
