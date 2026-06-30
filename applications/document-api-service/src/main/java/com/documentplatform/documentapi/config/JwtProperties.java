package com.documentplatform.documentapi.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    private String secret;

    /**
     * Optional path to a PEM-encoded RSA public key. When present and readable, tokens are
     * verified with RS256 (matching user-management-service RSA mode); otherwise HS256 with the
     * shared secret is used.
     */
    private String publicKeyPath;
}
