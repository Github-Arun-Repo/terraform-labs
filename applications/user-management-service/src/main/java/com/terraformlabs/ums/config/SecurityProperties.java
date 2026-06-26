package com.terraformlabs.ums.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app-security")
@Validated
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Password password = new Password();
    private final Cors cors = new Cors();

    public Jwt getJwt() {
        return jwt;
    }

    public Password getPassword() {
        return password;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Jwt {

        @NotBlank
        private String issuer;

        @NotBlank
        private String secret;

        @Min(1)
        private long accessTokenExpiryMinutes = 15;

        @Min(1)
        private long refreshTokenExpiryDays = 7;

        private String privateKeyPath;

        private String publicKeyPath;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpiryMinutes() {
            return accessTokenExpiryMinutes;
        }

        public void setAccessTokenExpiryMinutes(long accessTokenExpiryMinutes) {
            this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        }

        public long getRefreshTokenExpiryDays() {
            return refreshTokenExpiryDays;
        }

        public void setRefreshTokenExpiryDays(long refreshTokenExpiryDays) {
            this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getPublicKeyPath() {
            return publicKeyPath;
        }

        public void setPublicKeyPath(String publicKeyPath) {
            this.publicKeyPath = publicKeyPath;
        }
    }

    public static class Password {

        @Min(4)
        private int bcryptStrength = 12;

        @Min(1)
        private int maxLoginAttempts = 5;

        @Min(1)
        private int accountLockMinutes = 15;

        public int getBcryptStrength() {
            return bcryptStrength;
        }

        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }

        public int getMaxLoginAttempts() {
            return maxLoginAttempts;
        }

        public void setMaxLoginAttempts(int maxLoginAttempts) {
            this.maxLoginAttempts = maxLoginAttempts;
        }

        public int getAccountLockMinutes() {
            return accountLockMinutes;
        }

        public void setAccountLockMinutes(int accountLockMinutes) {
            this.accountLockMinutes = accountLockMinutes;
        }
    }

    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        // Supports comma-separated env value and YAML list form.
        public void setAllowedOrigins(String csv) {
            this.allowedOrigins = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .toList();
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
