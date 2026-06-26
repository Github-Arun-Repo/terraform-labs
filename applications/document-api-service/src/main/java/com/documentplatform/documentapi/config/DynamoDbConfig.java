package com.documentplatform.documentapi.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(AwsProperties awsProperties) {
        var builder = DynamoDbClient.builder()
                .region(Region.of(awsProperties.getRegion()));

        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }
}
