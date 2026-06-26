package com.documentplatform.documentprocessing.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.textract.TextractClient;

@Configuration
public class AwsClientConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(AwsProperties awsProperties) {
        DynamoDbClient.Builder builder = DynamoDbClient.builder().region(Region.of(awsProperties.getRegion()));
        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        S3Client.Builder builder = S3Client.builder().region(Region.of(awsProperties.getRegion()));
        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient(AwsProperties awsProperties) {
        SqsClient.Builder builder = SqsClient.builder().region(Region.of(awsProperties.getRegion()));
        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public TextractClient textractClient(AwsProperties awsProperties) {
        TextractClient.Builder builder = TextractClient.builder().region(Region.of(awsProperties.getRegion()));
        if (StringUtils.hasText(awsProperties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpointOverride()));
        }
        return builder.build();
    }
}
