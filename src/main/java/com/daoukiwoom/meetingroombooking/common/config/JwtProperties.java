package com.daoukiwoom.meetingroombooking.common.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình JWT, đọc từ {@code app.jwt.*} trong application.properties.
 *
 * @param secret khoá ký HS256, tối thiểu 32 byte
 * @param ttl    thời hạn của access token
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        if (ttl == null) {
            ttl = Duration.ofHours(1);
        }
    }
}
