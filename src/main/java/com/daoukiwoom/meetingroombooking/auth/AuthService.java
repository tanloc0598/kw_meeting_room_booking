package com.daoukiwoom.meetingroombooking.auth;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.daoukiwoom.meetingroombooking.auth.dto.LoginRequest;
import com.daoukiwoom.meetingroombooking.auth.dto.LoginResponse;
import com.daoukiwoom.meetingroombooking.common.config.JwtProperties;
import com.daoukiwoom.meetingroombooking.common.exception.ErrorCode;
import com.daoukiwoom.meetingroombooking.common.exception.KwException;
import com.daoukiwoom.meetingroombooking.user.User;
import com.daoukiwoom.meetingroombooking.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new KwException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.ttl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", jwtProperties.ttl().toSeconds());
    }
}
