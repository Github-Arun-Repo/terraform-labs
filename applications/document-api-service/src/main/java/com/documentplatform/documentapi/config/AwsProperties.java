package com.documentplatform.documentapi.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
    private final DynamoDb dynamodb = new DynamoDb();

    @Valid
    private final S3 s3 = new S3();

    @Getter
    @Setter
    public static class DynamoDb {

        @NotBlank
        private String tableName;

        @NotBlank
        private String customerIndexName = "GSI1";

        @NotBlank
        private String reviewIndexName = "GSI2";
    }

    @Getter
    @Setter
    public static class S3 {

        @NotBlank
        private String bucketName;

        @Min(1)
        private long uploadUrlExpiryMinutes = 10;

        @Min(1)
        private long viewUrlExpiryMinutes = 5;
    }
}
