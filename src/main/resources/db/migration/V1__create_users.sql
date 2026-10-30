CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO users (email, password_hash, full_name, role) VALUES
    ('admin@kw.local', '$2a$10$eb88ZkiY88C./SFNTa5VuO8EFJvqHkMKixXgIL2WjR8OeoGgM/BkK', 'Admin',       'ADMIN'),
    ('user1@kw.local', '$2a$10$gUL60OW8BZVkBUyw33KqQuuScwVDhhAdRObdjjRjtxXsV2AX0kmFy', 'Test User 1', 'USER'),
    ('user2@kw.local', '$2a$10$gUL60OW8BZVkBUyw33KqQuuScwVDhhAdRObdjjRjtxXsV2AX0kmFy', 'Test User 2', 'USER');
