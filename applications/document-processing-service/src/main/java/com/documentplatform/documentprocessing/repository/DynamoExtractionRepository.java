package com.documentplatform.documentprocessing.repository;

import com.documentplatform.documentprocessing.config.AwsProperties;
import com.documentplatform.documentprocessing.model.ExtractionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@RequiredArgsConstructor
public class DynamoExtractionRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;

    private DynamoDbTable<ExtractionItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getTableName(), TableSchema.fromBean(ExtractionItem.class));
    }

    public void save(ExtractionItem item) {
        table().putItem(item);
    }
}
