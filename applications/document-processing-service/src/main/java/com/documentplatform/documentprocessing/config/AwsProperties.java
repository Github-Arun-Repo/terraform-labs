package com.documentplatform.documentprocessing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
    private String region;
    private String endpointOverride;
    private final S3 s3 = new S3();
    private final Sqs sqs = new Sqs();
    private final DynamoDb dynamodb = new DynamoDb();

    @Getter
    @Setter
    public static class S3 {
        private String bucketName;
    }

    @Getter
    @Setter
    public static class Sqs {
        private String queueUrl;
        private Integer waitTimeSeconds = 10;
        private Integer maxMessages = 5;
    }

    @Getter
    @Setter
    public static class DynamoDb {
        private String tableName;
        private String s3KeyIndexName = "GSI1";
    }
}
