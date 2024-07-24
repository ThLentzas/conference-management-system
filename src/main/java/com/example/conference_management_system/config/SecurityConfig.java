package com.example.conference_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.example.conference_management_system.security.CustomAccessDeniedHandler;
import com.example.conference_management_system.security.CustomAuthenticationEntryPoint;
import com.example.conference_management_system.security.CsrfCookieFilter;

@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity
public class SecurityConfig {

    /*
        We can't use authorize.requestMatchers(HttpMethod.GET, "/api/v1/papers/**").permitAll(); because the endpoint
        to download a paper is api/v1/papers/{id}/download, and it is not permit all.

        https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/papers", "/api/v1/papers/{id}").permitAll();
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/conferences/**").permitAll();
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll();
                    authorize.requestMatchers("/api/v1/auth/**").permitAll();
                    authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    authorize.anyRequest().authenticated();
                })
                //The default one, just to know what's there
                .sessionManagement(sessionManagementConfigurer ->
                                sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> {
                    csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
                    // Why new CsrfTokenRequestAttributeHandler() instead of SPA in the docs?
                    // Not vulnerable to BREACH. https://auth0.com/blog/spring-boot-angular-crud/
                    csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> {
                    exception.accessDeniedHandler(new CustomAccessDeniedHandler());
                    exception.authenticationEntryPoint(new CustomAuthenticationEntryPoint());
                })
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }
}