package com.terraformlabs.dms.controller;

import com.terraformlabs.dms.dto.TokenRequest;
import com.terraformlabs.dms.dto.TokenResponse;
import com.terraformlabs.dms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/token")
    public TokenResponse issueToken(@Valid @RequestBody TokenRequest tokenRequest) {
        return authService.issueToken(tokenRequest);
    }
}
