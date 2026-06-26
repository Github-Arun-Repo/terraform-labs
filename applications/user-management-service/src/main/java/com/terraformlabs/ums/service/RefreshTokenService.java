package com.terraformlabs.ums.service;

import com.terraformlabs.ums.config.SecurityProperties;
import com.terraformlabs.ums.entity.AppUser;
import com.terraformlabs.ums.entity.RefreshToken;
import com.terraformlabs.ums.exception.UnauthorizedException;
import com.terraformlabs.ums.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, SecurityProperties securityProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public String createRefreshToken(AppUser user) {
        String plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(plainToken));
        refreshToken.setExpiresAt(Instant.now().plus(securityProperties.getJwt().getRefreshTokenExpiryDays(), ChronoUnit.DAYS));

        refreshTokenRepository.save(refreshToken);
        return plainToken;
    }

    @Transactional(readOnly = true)
    public AppUser validateAndGetUser(String plainToken) {
        String hash = hash(plainToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token is revoked or expired");
        }

        return refreshToken.getUser();
    }

    @Transactional
    public void revokeByPlainToken(String plainToken) {
        String hash = hash(plainToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllForUser(AppUser user) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        Instant now = Instant.now();
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
            token.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(tokens);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
