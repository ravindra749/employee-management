package com.example.demo;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Lets the page show who is signed in and hide editing controls from viewers.
@Tag(name = "Session", description = "The signed-in user")
@RestController
public class CurrentUserController {

    public record CurrentUser(String username, String role) {
    }

    @GetMapping("/api/me")
    public CurrentUser currentUser(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + AppUser.ADMIN));
        return new CurrentUser(authentication.getName(), admin ? AppUser.ADMIN : AppUser.VIEWER);
    }
}
