package com.terraformlabs.ums.controller;

import com.terraformlabs.ums.dto.AuthResponse;
import com.terraformlabs.ums.dto.LoginRequest;
import com.terraformlabs.ums.dto.PublicKeyResponse;
import com.terraformlabs.ums.dto.RegisterUserRequest;
import com.terraformlabs.ums.dto.RefreshAccessTokenResponse;
import com.terraformlabs.ums.dto.RefreshTokenRequest;
import com.terraformlabs.ums.dto.TokenValidationRequest;
import com.terraformlabs.ums.dto.TokenValidationResponse;
import com.terraformlabs.ums.dto.UserResponse;
import com.terraformlabs.ums.security.JwtService;
import com.terraformlabs.ums.service.AuthService;
import com.terraformlabs.ums.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, UserService userService, JwtService jwtService) {
        this.authService = authService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.registerUser(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public RefreshAccessTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken,
            Authentication authentication
    ) {
        authService.logout(refreshToken, authentication);
    }

    @PostMapping("/validate")
    public TokenValidationResponse validate(@Valid @RequestBody TokenValidationRequest request) {
        return authService.validateToken(request.token());
    }

    @GetMapping("/public-key")
    public PublicKeyResponse publicKey() {
        return new PublicKeyResponse(
                jwtService.isRsaModeEnabled(),
                jwtService.isRsaModeEnabled() ? "RS256" : "HS256",
                jwtService.getPublicKeyPem()
        );
    }
}
