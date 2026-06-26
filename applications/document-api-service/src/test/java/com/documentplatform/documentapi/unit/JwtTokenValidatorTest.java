package com.documentplatform.documentapi.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.documentplatform.documentapi.config.JwtProperties;
import com.documentplatform.documentapi.security.AuthenticatedUser;
import com.documentplatform.documentapi.security.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenValidatorTest {

    @Test
    void shouldValidateAndExtractClaims() throws Exception {
        JwtProperties props = new JwtProperties();
        props.setIssuer("document-platform");
        props.setSecret("change-this-secret");

        byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(props.getSecret().getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("user-123")
                .issuer("document-platform")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .claim("email", "a@b.com")
                .claim("roles", List.of("ADMIN"))
                .signWith(Keys.hmacShaKeyFor(keyBytes), Jwts.SIG.HS256)
                .compact();

        JwtTokenValidator validator = new JwtTokenValidator(props);
        AuthenticatedUser user = validator.validateAndExtract(token);

        assertEquals("user-123", user.userId());
        assertEquals("a@b.com", user.email());
    }

    @Test
    void shouldRejectInvalidIssuer() {
        JwtProperties props = new JwtProperties();
        props.setIssuer("document-platform");
        props.setSecret("change-this-secret");

        JwtTokenValidator validator = new JwtTokenValidator(props);
        assertThrows(Exception.class, () -> validator.validateAndExtract("invalid.token"));
    }
}
