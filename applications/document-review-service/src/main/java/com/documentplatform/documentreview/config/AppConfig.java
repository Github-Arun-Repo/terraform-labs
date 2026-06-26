package com.documentplatform.documentreview.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AwsProperties.class, JwtProperties.class, CorsProperties.class, AuditProperties.class})
public class AppConfig {
}
