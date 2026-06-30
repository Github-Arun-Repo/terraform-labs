package com.documentplatform.documentapi.security;

import com.documentplatform.documentapi.config.JwtProperties;
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
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwtProperties jwtProperties;
    private final SecretKey hmacKey;
    private final PublicKey rsaPublicKey;
    private final boolean rsaMode;

    public JwtTokenValidator(JwtProperties jwtProperties) {
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

    public AuthenticatedUser validateAndExtract(String token) {
        JwtParserBuilder parserBuilder = Jwts.parser().requireIssuer(jwtProperties.getIssuer());
        if (rsaMode) {
            parserBuilder.verifyWith(rsaPublicKey);
        } else {
            parserBuilder.verifyWith(hmacKey);
        }

        Claims claims = parserBuilder
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
