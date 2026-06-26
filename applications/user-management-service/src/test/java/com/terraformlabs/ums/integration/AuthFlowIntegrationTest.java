package com.terraformlabs.ums.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.terraformlabs.ums.dto.AuthResponse;
import com.terraformlabs.ums.dto.LoginRequest;
import com.terraformlabs.ums.dto.RegisterUserRequest;
import com.terraformlabs.ums.dto.RefreshAccessTokenResponse;
import com.terraformlabs.ums.dto.RefreshTokenRequest;
import com.terraformlabs.ums.dto.TokenValidationRequest;
import com.terraformlabs.ums.dto.TokenValidationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterLoginRefreshAndValidate() {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "finance1@example.com",
                "Finance User One",
                "Password@123"
        );

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerRequest,
                String.class
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        AuthResponse authResponse = restTemplate.postForObject(
                "/api/v1/auth/login",
                new LoginRequest("finance1@example.com", "Password@123"),
                AuthResponse.class
        );

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.refreshToken()).isNotBlank();
        assertThat(authResponse.expiresIn()).isEqualTo(900);

        RefreshAccessTokenResponse refreshed = restTemplate.postForObject(
                "/api/v1/auth/refresh",
                new RefreshTokenRequest(authResponse.refreshToken()),
                RefreshAccessTokenResponse.class
        );

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.expiresIn()).isEqualTo(900);

        TokenValidationResponse validation = restTemplate.postForObject(
                "/api/v1/auth/validate",
                new TokenValidationRequest(authResponse.accessToken()),
                TokenValidationResponse.class
        );

        assertThat(validation).isNotNull();
        assertThat(validation.valid()).isTrue();
        assertThat(validation.email()).isEqualTo("finance1@example.com");
        assertThat(validation.roles()).contains(com.terraformlabs.ums.entity.Role.SUPPLIER);

        restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(null),
                Void.class
        );
    }
}
