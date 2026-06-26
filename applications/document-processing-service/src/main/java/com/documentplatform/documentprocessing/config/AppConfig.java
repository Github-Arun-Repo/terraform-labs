package com.documentplatform.documentprocessing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AwsProperties.class, AppProperties.class})
public class AppConfig {
}
