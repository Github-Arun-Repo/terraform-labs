package com.terraformlabs.dms.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Documents documents = new Documents();

    public Security getSecurity() {
        return security;
    }

    public Documents getDocuments() {
        return documents;
    }

    public static class Security {
        private String jwtSecret;
        private long jwtExpirationMinutes;

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public long getJwtExpirationMinutes() {
            return jwtExpirationMinutes;
        }

        public void setJwtExpirationMinutes(long jwtExpirationMinutes) {
            this.jwtExpirationMinutes = jwtExpirationMinutes;
        }
    }

    public static class Documents {
        private long maxSizeBytes;
        private List<String> allowedTypes;

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }

        public List<String> getAllowedTypes() {
            return allowedTypes;
        }

        public void setAllowedTypes(List<String> allowedTypes) {
            this.allowedTypes = allowedTypes;
        }
    }
}
