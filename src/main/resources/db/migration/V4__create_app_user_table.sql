-- Accounts that can sign in. Passwords are stored only as hashes.
-- role is ADMIN (can change data) or VIEWER (read only).
CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'VIEWER'))
);
