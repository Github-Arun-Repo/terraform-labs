package com.terraformlabs.dms.service;

import com.terraformlabs.dms.dto.TokenRequest;
import com.terraformlabs.dms.dto.TokenResponse;
import com.terraformlabs.dms.entity.AppUser;
import com.terraformlabs.dms.exception.UnauthorizedException;
import com.terraformlabs.dms.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, passwordEncoder, jwtService);
    }

    @Test
    void issueToken_returnsTokenResponseOnValidCredentials() {
        AppUser user = userWithPassword("$2a$10$hashedpw");
        when(userService.getByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("secret", "$2a$10$hashedpw")).thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("jwt.token.here");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        TokenResponse response = authService.issueToken(new TokenRequest("alice", "secret"));

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void issueToken_throwsUnauthorizedWhenPasswordDoesNotMatch() {
        AppUser user = userWithPassword("$2a$10$hashedpw");
        when(userService.getByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$10$hashedpw")).thenReturn(false);

        assertThatThrownBy(() -> authService.issueToken(new TokenRequest("alice", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // --- helper ---

    private static AppUser userWithPassword(String hash) {
        AppUser u = new AppUser();
        u.setUsername("alice");
        u.setPasswordHash(hash);
        return u;
    }
}
