package com.terraformlabs.dms.security;

import com.terraformlabs.dms.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // A 32-byte plain-text secret (not base64) — matches the resolveKey fallback path
    private static final String SECRET = "TestSecretKey_AtLeast32BytesLong!";
    private static final long EXPIRATION_MINUTES = 60L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties props = buildProps(SECRET, EXPIRATION_MINUTES);
        jwtService = new JwtService(props);
    }

    // --- generateToken ---

    @Test
    void generateToken_returnsNonBlankJwt() {
        String token = jwtService.generateToken("alice");
        assertThat(token).isNotBlank();
    }

    @Test
    void generateToken_producesThreePartJwt() {
        String token = jwtService.generateToken("alice");
        assertThat(token.split("\\.")).hasSize(3);
    }

    // --- extractUsername ---

    @Test
    void extractUsername_returnsSubjectEmbeddedInToken() {
        String token = jwtService.generateToken("alice");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractUsername_handlesUsernameWithSpecialCharacters() {
        String token = jwtService.generateToken("user_abc123");
        assertThat(jwtService.extractUsername(token)).isEqualTo("user_abc123");
    }

    // --- isTokenValid ---

    @Test
    void isTokenValid_returnsTrueForFreshToken() {
        String token = jwtService.generateToken("bob");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtService.generateToken("bob");
        // Replace the signature segment entirely with a different one
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignatureXXXXXXXXXX";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForRandomString() {
        assertThat(jwtService.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForEmptyString() {
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    // --- getExpirationSeconds ---

    @Test
    void getExpirationSeconds_returnsMinutesMultipliedBySixty() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(EXPIRATION_MINUTES * 60);
    }

    @Test
    void getExpirationSeconds_reflectsCustomExpirationMinutes() {
        AppProperties props = buildProps(SECRET, 30L);
        JwtService custom = new JwtService(props);
        assertThat(custom.getExpirationSeconds()).isEqualTo(1800L);
    }

    // --- helper ---

    private static AppProperties buildProps(String secret, long expirationMinutes) {
        AppProperties props = new AppProperties();
        props.getSecurity().setJwtSecret(secret);
        props.getSecurity().setJwtExpirationMinutes(expirationMinutes);
        return props;
    }
}
