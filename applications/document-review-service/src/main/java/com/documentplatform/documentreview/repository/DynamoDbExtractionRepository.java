package com.documentplatform.documentreview.repository;

import com.documentplatform.documentreview.config.AwsProperties;
import com.documentplatform.documentreview.model.ExtractionItem;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@RequiredArgsConstructor
public class DynamoDbExtractionRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;

    private DynamoDbTable<ExtractionItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getDocumentTableName(), TableSchema.fromBean(ExtractionItem.class));
    }

    public Optional<ExtractionItem> findByDocumentId(String documentId) {
        ExtractionItem item = table().getItem(Key.builder().partitionValue("DOCUMENT#" + documentId).sortValue("EXTRACTION#LATEST").build());
        return Optional.ofNullable(item);
    }

    public void save(ExtractionItem item) {
        table().putItem(item);
    }
}
