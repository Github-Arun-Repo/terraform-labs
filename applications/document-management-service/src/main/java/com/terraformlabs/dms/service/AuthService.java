package com.terraformlabs.dms.service;

import com.terraformlabs.dms.dto.TokenRequest;
import com.terraformlabs.dms.dto.TokenResponse;
import com.terraformlabs.dms.entity.AppUser;
import com.terraformlabs.dms.exception.UnauthorizedException;
import com.terraformlabs.dms.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse issueToken(TokenRequest request) {
        AppUser user = userService.getByUsername(request.username());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername());
        return new TokenResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }
}
