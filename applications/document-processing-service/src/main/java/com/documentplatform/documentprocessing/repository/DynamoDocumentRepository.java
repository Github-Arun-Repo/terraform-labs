package com.documentplatform.documentprocessing.repository;

import com.documentplatform.documentprocessing.config.AwsProperties;
import com.documentplatform.documentprocessing.enums.DocumentStatus;
import com.documentplatform.documentprocessing.model.DocumentItem;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
@RequiredArgsConstructor
public class DynamoDocumentRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;
    private final AwsProperties awsProperties;

    public enum ProcessingStartOutcome {
        STARTED,
        SKIPPED_FINAL,
        SKIPPED_RECENT_PROCESSING,
        MAX_ATTEMPTS_EXCEEDED,
        TRANSITION_CONFLICT
    }

    private DynamoDbTable<DocumentItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getTableName(), TableSchema.fromBean(DocumentItem.class));
    }

    public Optional<DocumentItem> findByDocumentId(String documentId) {
        DocumentItem item = table().getItem(Key.builder().partitionValue("DOCUMENT#" + documentId).sortValue("METADATA").build());
        return Optional.ofNullable(item);
    }

    public Optional<DocumentItem> findByS3Key(String s3Key) {
        DynamoDbIndex<DocumentItem> index = table().index(awsProperties.getDynamodb().getS3KeyIndexName());
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue("S3KEY#" + s3Key).build()))
                .limit(1)
                .build();

        return index.query(request)
                .stream()
                .flatMap(page -> page.items().stream())
                .filter(item -> "METADATA".equals(item.getSk()))
                .findFirst();
    }

    public void save(DocumentItem item) {
        table().putItem(item);
    }

    public ProcessingStartOutcome tryStartProcessing(
            DocumentItem document,
            Instant now,
            int maxAttempts,
            int staleTimeoutMinutes
    ) {
        DocumentStatus currentStatus = document.getStatus();
        if (isFinal(currentStatus)) {
            return ProcessingStartOutcome.SKIPPED_FINAL;
        }

        int attempts = document.getProcessingAttempts() == null ? 0 : document.getProcessingAttempts();
        if (attempts >= maxAttempts) {
            return ProcessingStartOutcome.MAX_ATTEMPTS_EXCEEDED;
        }

        boolean staleProcessing = false;
        if (currentStatus == DocumentStatus.PROCESSING) {
            Instant startedAt = parseInstant(document.getProcessingStartedAt());
            if (startedAt == null) {
                return ProcessingStartOutcome.SKIPPED_RECENT_PROCESSING;
            }
            Duration age = Duration.between(startedAt, now);
            if (age.toMinutes() < staleTimeoutMinutes) {
                return ProcessingStartOutcome.SKIPPED_RECENT_PROCESSING;
            }
            staleProcessing = true;
        }

        String nowIso = now.toString();
        Map<String, String> names = Map.of(
                "#status", "status",
                "#attempts", "processingAttempts",
                "#startedAt", "processingStartedAt",
                "#updatedAt", "updatedAt",
                "#revision", "documentRevision",
                "#gsi2pk", "GSI2PK",
                "#gsi2sk", "GSI2SK"
        );

        Map<String, AttributeValue> values = new java.util.HashMap<>();
        values.put(":processing", AttributeValue.builder().s(DocumentStatus.PROCESSING.name()).build());
        values.put(":uploadRequested", AttributeValue.builder().s(DocumentStatus.UPLOAD_REQUESTED.name()).build());
        values.put(":uploaded", AttributeValue.builder().s(DocumentStatus.UPLOADED.name()).build());
        values.put(":zero", AttributeValue.builder().n("0").build());
        values.put(":one", AttributeValue.builder().n("1").build());
        values.put(":now", AttributeValue.builder().s(nowIso).build());
        values.put(":maxAttempts", AttributeValue.builder().n(String.valueOf(maxAttempts)).build());
        values.put(":oneRevision", AttributeValue.builder().n("1").build());
        values.put(":gsi2pk", AttributeValue.builder()
                .s("CUSTOMER#" + document.getCustomerId() + "#STATUS#" + DocumentStatus.PROCESSING.name()).build());
        values.put(":gsi2sk", AttributeValue.builder()
                .s("UPDATED_AT#" + nowIso + "#DOCUMENT#" + document.getDocumentId()).build());

        String conditionExpression;
        if (staleProcessing) {
            values.put(":expectedStartedAt", AttributeValue.builder().s(document.getProcessingStartedAt()).build());
            conditionExpression = "#status = :processing AND #startedAt = :expectedStartedAt AND (attribute_not_exists(#attempts) OR #attempts < :maxAttempts)";
        } else {
            conditionExpression = "(#status = :uploadRequested OR #status = :uploaded) AND (attribute_not_exists(#attempts) OR #attempts < :maxAttempts)";
        }

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(awsProperties.getDynamodb().getTableName())
                .key(Map.of(
                        "PK", AttributeValue.builder().s(document.getPk()).build(),
                        "SK", AttributeValue.builder().s(document.getSk()).build()
                ))
                .updateExpression("SET #status = :processing, #attempts = if_not_exists(#attempts, :zero) + :one, #startedAt = :now, #updatedAt = :now, #revision = if_not_exists(#revision, :zero) + :oneRevision, #gsi2pk = :gsi2pk, #gsi2sk = :gsi2sk")
                .conditionExpression(conditionExpression)
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .returnValues("ALL_NEW")
                .build();

        try {
            var response = dynamoDbClient.updateItem(request);
            Map<String, AttributeValue> attrs = response.attributes();
            document.setStatus(DocumentStatus.PROCESSING);
            if (attrs.containsKey("processingAttempts")) {
                document.setProcessingAttempts(Integer.parseInt(attrs.get("processingAttempts").n()));
            } else {
                document.setProcessingAttempts(attempts + 1);
            }
            document.setProcessingStartedAt(nowIso);
            document.setUpdatedAt(nowIso);
            if (attrs.containsKey("documentRevision")) {
                document.setDocumentRevision(Long.parseLong(attrs.get("documentRevision").n()));
            }
            document.setGsi2Pk("CUSTOMER#" + document.getCustomerId() + "#STATUS#" + DocumentStatus.PROCESSING.name());
            document.setGsi2Sk("UPDATED_AT#" + nowIso + "#DOCUMENT#" + document.getDocumentId());
            return ProcessingStartOutcome.STARTED;
        } catch (ConditionalCheckFailedException ex) {
            return ProcessingStartOutcome.TRANSITION_CONFLICT;
        }
    }

    public boolean createDuplicateIndexItem(
            String duplicateKey,
            String documentId,
            String customerId,
            String supplierNameNormalized,
            String invoiceNumberNormalized,
            Instant now
    ) {
        Map<String, AttributeValue> item = Map.of(
                "PK", AttributeValue.builder().s(duplicateKey).build(),
                "SK", AttributeValue.builder().s("INDEX").build(),
                "entityType", AttributeValue.builder().s("DUPLICATE_INDEX").build(),
                "documentId", AttributeValue.builder().s(documentId).build(),
                "customerId", AttributeValue.builder().s(customerId).build(),
                "supplierNameNormalized", AttributeValue.builder().s(supplierNameNormalized).build(),
                "invoiceNumberNormalized", AttributeValue.builder().s(invoiceNumberNormalized).build(),
                "createdAt", AttributeValue.builder().s(now.toString()).build()
        );

        PutItemRequest request = PutItemRequest.builder()
                .tableName(awsProperties.getDynamodb().getTableName())
                .item(item)
                .conditionExpression("attribute_not_exists(PK)")
                .build();

        try {
            dynamoDbClient.putItem(request);
            return true;
        } catch (ConditionalCheckFailedException ex) {
            return false;
        }
    }

    private boolean isFinal(DocumentStatus status) {
        return status == DocumentStatus.PENDING_APPROVAL
                || status == DocumentStatus.MANUAL_REVIEW_REQUIRED
                || status == DocumentStatus.DUPLICATE_DETECTED
                || status == DocumentStatus.EXTRACTION_FAILED
                || status == DocumentStatus.FAILED
                || status == DocumentStatus.APPROVED
                || status == DocumentStatus.REJECTED;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
