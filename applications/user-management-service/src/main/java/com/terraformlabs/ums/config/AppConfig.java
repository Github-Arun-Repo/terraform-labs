package com.terraformlabs.ums.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@EnableConfigurationProperties({SecurityProperties.class, AwsSecretsProperties.class})
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder(SecurityProperties securityProperties) {
        return new BCryptPasswordEncoder(securityProperties.getPassword().getBcryptStrength());
    }

    @Bean
    public SecretsManagerClient secretsManagerClient(AwsSecretsProperties awsSecretsProperties) {
        return SecretsManagerClient.builder()
                .region(Region.of(awsSecretsProperties.getRegion()))
                .build();
    }
}
