package com.documentplatform.documentreview.repository;

import com.documentplatform.documentreview.config.AwsProperties;
import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.model.DocumentItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
@RequiredArgsConstructor
public class DynamoDbDocumentRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;

    private DynamoDbTable<DocumentItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getDocumentTableName(), TableSchema.fromBean(DocumentItem.class));
    }

    public Optional<DocumentItem> findDocumentById(String documentId) {
        DocumentItem item = table().getItem(Key.builder().partitionValue("DOCUMENT#" + documentId).sortValue("METADATA").build());
        return Optional.ofNullable(item);
    }

    public DocumentItem save(DocumentItem item) {
        item.setUpdatedAt(Instant.now().toString());
        table().putItem(item);
        return item;
    }

    public List<DocumentItem> findReviewQueue(String customerId, String status, int limit, String nextToken, Holder<String> resolvedNextToken) {
        DynamoDbIndex<DocumentItem> index = table().index(awsProperties.getDynamodb().getReviewQueueIndexName());
        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue("CUSTOMER#" + customerId + "#STATUS#" + status).build()))
                .limit(limit);

        if (nextToken != null && !nextToken.isBlank()) {
            requestBuilder.exclusiveStartKey(Map.of(
                    "PK", AttributeValue.builder().s(nextToken).build(),
                    "SK", AttributeValue.builder().s("METADATA").build()
            ));
        }

        var pages = index.query(requestBuilder.build());
        List<DocumentItem> items = new ArrayList<>();
        String localNextToken = null;
        for (software.amazon.awssdk.enhanced.dynamodb.model.Page<DocumentItem> page : pages) {
            page.items().stream().limit(limit - items.size()).forEach(items::add);
            if (items.size() >= limit) {
                if (!page.lastEvaluatedKey().isEmpty() && page.lastEvaluatedKey().containsKey("PK")) {
                    localNextToken = page.lastEvaluatedKey().get("PK").s();
                }
                break;
            }
        }
        resolvedNextToken.value = localNextToken;
        return items;
    }

    public void updateStatus(DocumentItem item, DocumentStatus newStatus) {
        item.setStatus(newStatus);
        item.setGsi2Pk("CUSTOMER#" + item.getCustomerId() + "#STATUS#" + newStatus.name());
        item.setGsi2Sk("UPDATED_AT#" + Instant.now() + "#DOCUMENT#" + item.getDocumentId());
        item.setDocumentRevision(item.getDocumentRevision() == null ? 1L : item.getDocumentRevision() + 1);
        save(item);
    }

    public static class Holder<T> {
        public T value;
    }
}
