package com.documentplatform.documentreview.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.documentplatform.documentreview.config.JwtProperties;
import com.documentplatform.documentreview.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shouldParseSubjectAndRoles() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("document-platform");
        properties.setSecret("this-is-a-long-enough-secret-for-hs256-signing");

        SecretKey key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer(properties.getIssuer())
                .subject("analyst@company.com")
                .claim("roles", List.of("FINANCE_ANALYST"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        JwtService service = new JwtService(properties);
        var claims = service.parseClaims(token);

        assertEquals("analyst@company.com", service.username(claims));
        assertEquals(List.of("FINANCE_ANALYST"), service.roles(claims));
    }
}
