package com.documentplatform.documentapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AwsProperties.class, JwtProperties.class, DocumentProperties.class, CorsProperties.class})
public class AppConfig {
}
