package com.terraformlabs.ums.service;

import com.terraformlabs.ums.config.SecurityProperties;
import com.terraformlabs.ums.dto.AuthResponse;
import com.terraformlabs.ums.dto.LoginRequest;
import com.terraformlabs.ums.dto.RefreshAccessTokenResponse;
import com.terraformlabs.ums.dto.TokenValidationResponse;
import com.terraformlabs.ums.entity.AppUser;
import com.terraformlabs.ums.entity.Role;
import com.terraformlabs.ums.entity.UserStatus;
import com.terraformlabs.ums.exception.UnauthorizedException;
import com.terraformlabs.ums.repository.UserRepository;
import com.terraformlabs.ums.security.AppUserPrincipal;
import com.terraformlabs.ums.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityProperties securityProperties;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            SecurityProperties securityProperties
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        user.unlockIfExpired();

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UnauthorizedException("User account is disabled");
        }

        if (user.isLockedNow()) {
            throw new UnauthorizedException("User account is temporarily locked");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(
                    principal.getUserId(),
                    principal.getEmail(),
                    principal.getRoles()
            );
            String refreshToken = refreshTokenService.createRefreshToken(user);

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtService.getAccessTokenExpirySeconds()
            );
        } catch (BadCredentialsException ex) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    @Transactional
    public RefreshAccessTokenResponse refresh(String refreshToken) {
        AppUser user = refreshTokenService.validateAndGetUser(refreshToken);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        return new RefreshAccessTokenResponse(accessToken, jwtService.getAccessTokenExpirySeconds());
    }

    @Transactional
    public void logout(String refreshToken, Authentication authentication) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeByPlainToken(refreshToken);
            return;
        }

        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            AppUser user = userRepository.findById(principal.getUserId())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));
            refreshTokenService.revokeAllForUser(user);
        }
    }

    public TokenValidationResponse validateToken(String token) {
        try {
            if (!jwtService.isValid(token)) {
                return new TokenValidationResponse(false, null, null, Set.of(), 0);
            }

            Long userId = jwtService.getUserId(token);
            String email = jwtService.getEmail(token);
            Set<Role> roles = jwtService.getRoles(token);
            long exp = jwtService.getExpiresAtEpochSeconds(token);

            return new TokenValidationResponse(true, userId, email, roles, exp);
        } catch (JwtException | IllegalArgumentException ex) {
            return new TokenValidationResponse(false, null, null, Set.of(), 0);
        }
    }

    private void handleFailedLogin(AppUser user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= securityProperties.getPassword().getMaxLoginAttempts()) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(Instant.now().plus(securityProperties.getPassword().getAccountLockMinutes(), ChronoUnit.MINUTES));
        }

        userRepository.save(user);
    }
}
