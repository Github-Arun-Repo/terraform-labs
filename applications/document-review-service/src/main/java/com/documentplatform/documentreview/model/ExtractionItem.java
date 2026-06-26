package com.documentplatform.documentreview.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@DynamoDbBean
public class ExtractionItem {

    private String pk;
    private String sk;
    private String entityType;
    private String documentId;
    private String invoiceNumber;
    private String supplierName;
    private String supplierAddress;
    private String customerName;
    private String invoiceDate;
    private String dueDate;
    private String currency;
    private Double subtotalAmount;
    private Double taxAmount;
    private Double totalAmount;
    private String iban;
    private Double confidenceScore;
    private List<Map<String, Object>> lineItems;
    private List<String> validationErrors;
    private List<Map<String, Object>> manualCorrections;
    private String rawTextractS3Key;
    private String normalizedJsonS3Key;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
}
