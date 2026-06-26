package com.documentplatform.documentapi.model;

import com.documentplatform.documentapi.enums.DocumentStatus;
import com.documentplatform.documentapi.enums.DocumentType;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@DynamoDbBean
public class DocumentItem {

    private String pk;
    private String sk;
    private String entityType;
    private String documentId;
    private String customerId;
    private DocumentType documentType;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String bucketName;
    private String s3Key;
    private DocumentStatus status;
    private String uploadedBy;
    private Integer processingAttempts;
    private Long documentRevision;
    private String createdAt;
    private String updatedAt;
    private String gsi1Pk;
    private String gsi1Sk;
    private String gsi2Pk;
    private String gsi2Sk;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }

    @DynamoDbAttribute("GSI1PK")
    public String getGsi1Pk() {
        return gsi1Pk;
    }

    @DynamoDbAttribute("GSI1SK")
    public String getGsi1Sk() {
        return gsi1Sk;
    }

    @DynamoDbAttribute("GSI2PK")
    public String getGsi2Pk() {
        return gsi2Pk;
    }

    @DynamoDbAttribute("GSI2SK")
    public String getGsi2Sk() {
        return gsi2Sk;
    }
}
