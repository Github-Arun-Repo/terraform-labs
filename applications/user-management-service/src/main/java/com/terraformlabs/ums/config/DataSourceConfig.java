package com.terraformlabs.ums.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Configuration
public class DataSourceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfig.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public DataSource dataSource(
            DataSourceProperties dataSourceProperties,
            AwsSecretsProperties awsSecretsProperties,
            SecretsManagerClient secretsManagerClient
    ) {
        String password = dataSourceProperties.getPassword();

        // If AWS secret is configured, it overrides env password at startup.
        if (StringUtils.hasText(awsSecretsProperties.getDbPasswordSecretName())) {
            try {
                String secretPayload = secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                                .secretId(awsSecretsProperties.getDbPasswordSecretName())
                                .build())
                        .secretString();

                String resolved = extractPassword(secretPayload);
                if (StringUtils.hasText(resolved)) {
                    password = resolved;
                }
            } catch (Exception ex) {
                LOGGER.warn("Could not resolve DB password from AWS Secrets Manager, using configured password fallback.");
            }
        }

        return DataSourceBuilder.create()
                .driverClassName(dataSourceProperties.getDriverClassName())
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(password)
                .build();
    }

    private String extractPassword(String secretPayload) {
        if (!StringUtils.hasText(secretPayload)) {
            return null;
        }

        String trimmed = secretPayload.trim();
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }

        try {
            JsonNode json = objectMapper.readTree(trimmed);
            if (json.hasNonNull("password")) {
                return json.get("password").asText();
            }
            if (json.hasNonNull("db_password")) {
                return json.get("db_password").asText();
            }
            if (json.hasNonNull("SPRING_DATASOURCE_PASSWORD")) {
                return json.get("SPRING_DATASOURCE_PASSWORD").asText();
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }
}
