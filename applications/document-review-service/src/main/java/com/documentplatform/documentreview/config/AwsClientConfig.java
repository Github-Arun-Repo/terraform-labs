package com.documentplatform.documentreview.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsClientConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(AwsProperties awsProperties) {
        var builder = DynamoDbClient.builder().region(Region.of(awsProperties.getRegion()));
        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsProperties awsProperties) {
        var builder = S3Presigner.builder().region(Region.of(awsProperties.getRegion()));
        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }
        return builder.build();
    }
}
