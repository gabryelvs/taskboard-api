package com.gabryel.taskboard.auth;

import com.gabryel.taskboard.auth.AuthDtos.*;
import com.gabryel.taskboard.common.ConflictException;
import com.gabryel.taskboard.common.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwtService,
                       @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.findByEmail(req.email()).isPresent()) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setName(req.name());
        users.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        throw new UnsupportedOperationException("implemented in Task 5");
    }

    @Transactional
    public void logout(String refreshToken) {
        throw new UnsupportedOperationException("implemented in Task 5");
    }

    AuthResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(sha256(rawRefresh));
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(refreshTtl));
        refreshTokens.save(rt);

        return new AuthResponse(jwtService.generateAccessToken(user.getId()), rawRefresh);
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
