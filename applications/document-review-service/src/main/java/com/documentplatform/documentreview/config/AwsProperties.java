package com.documentplatform.documentreview.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    @NotBlank
    private String region;

    private String endpointOverride;

    @Valid
    private final S3 s3 = new S3();

    @Valid
    private final DynamoDb dynamodb = new DynamoDb();

    @Getter
    @Setter
    public static class S3 {
        @NotBlank
        private String bucketName;
        private long viewUrlExpiryMinutes = 5;
    }

    @Getter
    @Setter
    public static class DynamoDb {
        @NotBlank
        private String documentTableName;
        @NotBlank
        private String reviewQueueIndexName = "GSI2";
    }
}
