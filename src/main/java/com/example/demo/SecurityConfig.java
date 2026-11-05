package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

// Who may do what:
// - everything needs a signed-in user, except the login page itself;
// - VIEWER and ADMIN can read through the API, only ADMIN can change data;
// - API calls without a session get 401 instead of a redirect to /login;
// - CSRF protection is on: the page sends the XSRF-TOKEN cookie back as a header.
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**")
                        .hasAnyRole(AppUser.ADMIN, AppUser.VIEWER)
                        .requestMatchers("/api/**").hasRole(AppUser.ADMIN)
                        .anyRequest().authenticated())
                .formLogin(form -> form.defaultSuccessUrl("/", true))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .csrf(csrf -> csrf.spa())
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
