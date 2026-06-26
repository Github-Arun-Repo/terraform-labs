package com.documentplatform.documentapi.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(AwsProperties awsProperties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(awsProperties.getRegion()));

        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }

        return builder.build();
    }
}
