package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Creates the first accounts from environment variables, so no password is
// ever written in the code: APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD, and
// optionally APP_VIEWER_USERNAME / APP_VIEWER_PASSWORD. An account that
// already exists is left unchanged.
@Component
public class InitialAccounts implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;
    private static final Logger log = LoggerFactory.getLogger(InitialAccounts.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String viewerUsername;
    private final String viewerPassword;

    public InitialAccounts(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_ADMIN_USERNAME:}") String adminUsername,
            @Value("${APP_ADMIN_PASSWORD:}") String adminPassword,
            @Value("${APP_VIEWER_USERNAME:}") String viewerUsername,
            @Value("${APP_VIEWER_PASSWORD:}") String viewerPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername.trim();
        this.adminPassword = adminPassword;
        this.viewerUsername = viewerUsername.trim();
        this.viewerPassword = viewerPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing(adminUsername, adminPassword, AppUser.ADMIN);
        createIfMissing(viewerUsername, viewerPassword, AppUser.VIEWER);
        if (appUserRepository.count() == 0) {
            log.warn("No user accounts exist, so nobody can sign in. "
                    + "Set APP_ADMIN_USERNAME and APP_ADMIN_PASSWORD and restart.");
        }
    }

    private void createIfMissing(String username, String password, String role) {
        if (username.isEmpty() || appUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("The password for the " + role
                    + " account must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        appUserRepository.save(new AppUser(username, passwordEncoder.encode(password), role));
        log.info("Created the {} account {}.", role, username);
    }
}
