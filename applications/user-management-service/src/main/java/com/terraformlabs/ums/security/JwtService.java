package com.terraformlabs.ums.security;

import com.terraformlabs.ums.config.SecurityProperties;
import com.terraformlabs.ums.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey hmacSigningKey;
    private final PrivateKey rsaPrivateKey;
    private final PublicKey rsaPublicKey;
    private final String rsaPublicKeyPem;
    private final boolean rsaModeEnabled;
    private final SecurityProperties securityProperties;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;

        KeyMaterial keyMaterial = resolveKeyMaterial(securityProperties);
        this.hmacSigningKey = keyMaterial.hmacSigningKey;
        this.rsaPrivateKey = keyMaterial.rsaPrivateKey;
        this.rsaPublicKey = keyMaterial.rsaPublicKey;
        this.rsaPublicKeyPem = keyMaterial.rsaPublicKeyPem;
        this.rsaModeEnabled = keyMaterial.rsaModeEnabled;
    }

    public String generateAccessToken(Long userId, String email, Set<Role> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(securityProperties.getJwt().getAccessTokenExpiryMinutes() * 60);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(securityProperties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("email", email)
                .claim("roles", roles.stream().map(Role::name).toList())
            .signWith(rsaModeEnabled ? rsaPrivateKey : hmacSigningKey, rsaModeEnabled ? Jwts.SIG.RS256 : Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        if (rsaModeEnabled) {
            return Jwts.parser()
                .verifyWith(rsaPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        }

        return Jwts.parser()
            .verifyWith(hmacSigningKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getAccessTokenExpirySeconds() {
        return securityProperties.getJwt().getAccessTokenExpiryMinutes() * 60;
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<Role> getRoles(String token) {
        List<String> roleNames = parseClaims(token).get("roles", List.class);
        return roleNames.stream().map(Role::valueOf).collect(java.util.stream.Collectors.toSet());
    }

    public long getExpiresAtEpochSeconds(String token) {
        return parseClaims(token).getExpiration().toInstant().getEpochSecond();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRsaModeEnabled() {
        return rsaModeEnabled;
    }

    public String getPublicKeyPem() {
        return rsaPublicKeyPem;
    }

    private KeyMaterial resolveKeyMaterial(SecurityProperties properties) {
        try {
            String privateKeyPath = properties.getJwt().getPrivateKeyPath();
            String publicKeyPath = properties.getJwt().getPublicKeyPath();

            if (StringUtils.hasText(privateKeyPath) && StringUtils.hasText(publicKeyPath)
                    && Files.exists(Path.of(privateKeyPath)) && Files.exists(Path.of(publicKeyPath))) {
                PrivateKey privateKey = readPrivateKey(privateKeyPath);
                String publicPem = Files.readString(Path.of(publicKeyPath));
                PublicKey publicKey = readPublicKey(publicPem);

                LOGGER.info("JWT configured in RSA mode using key files.");
                return new KeyMaterial(null, privateKey, publicKey, publicPem, true);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to load RSA JWT keys; falling back to shared secret mode.");
        }

        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters for HS256");
        }

        LOGGER.info("JWT configured in HMAC mode (MVP fallback). Set RSA key paths for asymmetric mode.");
        return new KeyMaterial(Keys.hmacShaKeyFor(secretBytes), null, null, null, false);
    }

    private PrivateKey readPrivateKey(String filePath) throws Exception {
        String pem = Files.readString(Path.of(filePath));
        String sanitized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(sanitized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private PublicKey readPublicKey(String pem) throws Exception {
        String sanitized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(sanitized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private record KeyMaterial(
            SecretKey hmacSigningKey,
            PrivateKey rsaPrivateKey,
            PublicKey rsaPublicKey,
            String rsaPublicKeyPem,
            boolean rsaModeEnabled
    ) {
        private KeyMaterial {
            if (rsaModeEnabled) {
                Objects.requireNonNull(rsaPrivateKey, "rsaPrivateKey must not be null");
                Objects.requireNonNull(rsaPublicKey, "rsaPublicKey must not be null");
            } else {
                Objects.requireNonNull(hmacSigningKey, "hmacSigningKey must not be null");
            }
        }
    }

}
