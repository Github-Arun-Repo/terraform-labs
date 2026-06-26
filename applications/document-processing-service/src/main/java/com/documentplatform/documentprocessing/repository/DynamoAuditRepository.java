package com.documentplatform.documentprocessing.repository;

import com.documentplatform.documentprocessing.config.AwsProperties;
import com.documentplatform.documentprocessing.model.AuditEventItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@RequiredArgsConstructor
public class DynamoAuditRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;

    private DynamoDbTable<AuditEventItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getTableName(), TableSchema.fromBean(AuditEventItem.class));
    }

    public void save(AuditEventItem item) {
        table().putItem(item);
    }
}
