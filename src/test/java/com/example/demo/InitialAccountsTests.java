package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "APP_ADMIN_USERNAME=first-admin",
        "APP_ADMIN_PASSWORD=a-long-admin-password",
        "APP_VIEWER_USERNAME=first-viewer",
        "APP_VIEWER_PASSWORD=a-long-viewer-password"})
class InitialAccountsTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void accountsAreCreatedFromEnvironmentVariablesWithHashedPasswords() {
        AppUser admin = appUserRepository.findByUsernameIgnoreCase("first-admin").orElseThrow();
        assertEquals(AppUser.ADMIN, admin.getRole());
        assertTrue(admin.getPasswordHash().startsWith("{bcrypt}"),
                "Passwords are stored as BCrypt hashes, never as text.");
        assertTrue(passwordEncoder.matches("a-long-admin-password", admin.getPasswordHash()));

        AppUser viewer = appUserRepository.findByUsernameIgnoreCase("first-viewer").orElseThrow();
        assertEquals(AppUser.VIEWER, viewer.getRole());
    }

    @Test
    void shortPasswordsAreRefused() {
        InitialAccounts accounts = new InitialAccounts(
                appUserRepository, passwordEncoder, "new-admin", "too-short", "", "");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> accounts.run(null));
        assertEquals("The password for the ADMIN account must be at least 12 characters.",
                error.getMessage());
    }
}
