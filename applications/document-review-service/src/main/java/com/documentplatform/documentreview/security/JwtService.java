package com.documentplatform.documentreview.security;

import com.documentplatform.documentreview.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;
    private final SecretKey hmacKey;
    private final PublicKey rsaPublicKey;
    private final boolean rsaMode;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        PublicKey resolvedRsaKey = resolveRsaPublicKey(jwtProperties.getPublicKeyPath());
        if (resolvedRsaKey != null) {
            this.rsaPublicKey = resolvedRsaKey;
            this.hmacKey = null;
            this.rsaMode = true;
            LOGGER.info("JWT validation configured in RS256 mode using public key file.");
        } else {
            // HS256 with the raw shared secret, matching user-management-service token signing.
            this.hmacKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
            this.rsaPublicKey = null;
            this.rsaMode = false;
            LOGGER.info("JWT validation configured in HS256 shared-secret mode.");
        }
    }

    public Claims parseClaims(String token) {
        JwtParserBuilder parserBuilder = Jwts.parser().requireIssuer(jwtProperties.getIssuer());
        if (rsaMode) {
            parserBuilder.verifyWith(rsaPublicKey);
        } else {
            parserBuilder.verifyWith(hmacKey);
        }

        return parserBuilder
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String username(Claims claims) {
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> roles(Claims claims) {
        Object roleClaim = claims.get("roles");
        if (roleClaim instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private PublicKey resolveRsaPublicKey(String publicKeyPath) {
        if (!StringUtils.hasText(publicKeyPath) || !Files.exists(Path.of(publicKeyPath))) {
            return null;
        }
        try {
            String sanitized = Files.readString(Path.of(publicKeyPath))
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            LOGGER.warn("Failed to load RSA public key from {}; falling back to HS256 shared-secret mode.",
                    publicKeyPath);
            return null;
        }
    }
}
