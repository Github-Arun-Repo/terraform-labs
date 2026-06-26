package com.documentplatform.documentapi.repository;

import com.documentplatform.documentapi.config.AwsProperties;
import com.documentplatform.documentapi.enums.DocumentStatus;
import com.documentplatform.documentapi.enums.DocumentType;
import com.documentplatform.documentapi.model.DocumentItem;
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
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
@RequiredArgsConstructor
public class DynamoDbDocumentRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;

    private DynamoDbTable<DocumentItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getTableName(), TableSchema.fromBean(DocumentItem.class));
    }

    public void save(DocumentItem item) {
        table().putItem(item);
    }

    public Optional<DocumentItem> findByDocumentId(String documentId) {
        DocumentItem item = table().getItem(Key.builder().partitionValue("DOCUMENT#" + documentId).sortValue("METADATA").build());
        return Optional.ofNullable(item);
    }

    public List<DocumentItem> listByCustomerAndStatus(
            String customerId,
            DocumentStatus status,
            DocumentType documentType,
            int limit,
            String nextToken,
            Holder<String> resolvedNextToken
    ) {
        DynamoDbIndex<DocumentItem> reviewIndex = table().index(awsProperties.getDynamodb().getReviewIndexName());
        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(
                Key.builder().partitionValue("CUSTOMER#" + customerId + "#STATUS#" + status.name()).build()))
                .scanIndexForward(false)
                .limit(limit);

        if (nextToken != null && !nextToken.isBlank()) {
            requestBuilder.exclusiveStartKey(Map.of(
                    "PK", AttributeValue.builder().s(nextToken).build(),
                    "SK", AttributeValue.builder().s("METADATA").build()
            ));
        }

        List<DocumentItem> result = new ArrayList<>();
        String localNextToken = null;
        for (Page<DocumentItem> page : reviewIndex.query(requestBuilder.build())) {
            for (DocumentItem item : page.items()) {
                if (item.getSk() == null || !"METADATA".equals(item.getSk())) {
                    continue;
                }
                if (status != null && item.getStatus() != status) {
                    continue;
                }
                if (documentType != null && item.getDocumentType() != documentType) {
                    continue;
                }
                result.add(item);
                if (result.size() == limit) {
                    break;
                }
            }
            if (result.size() == limit) {
                if (!page.lastEvaluatedKey().isEmpty() && page.lastEvaluatedKey().containsKey("PK")) {
                    localNextToken = page.lastEvaluatedKey().get("PK").s();
                }
                break;
            }
        }

        resolvedNextToken.value = localNextToken;
        return result;
    }

    public static class Holder<T> {
        public T value;
    }
}
