package com.documentplatform.documentapi.security;

import com.documentplatform.documentapi.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenValidator {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtTokenValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(hashSecret(jwtProperties.getSecret()));
    }

    public AuthenticatedUser validateAndExtract(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(jwtProperties.getIssuer())
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);

        return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("email", String.class),
                roles == null ? Set.of() : roles.stream().map(String::toUpperCase).collect(Collectors.toSet())
        );
    }

    private byte[] hashSecret(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize JWT secret key", ex);
        }
    }
}
