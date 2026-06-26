package com.documentplatform.documentreview.repository;

import com.documentplatform.documentreview.config.AwsProperties;
import com.documentplatform.documentreview.model.AuditEventItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Repository
@RequiredArgsConstructor
public class DynamoDbAuditRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;

    private DynamoDbTable<AuditEventItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getDocumentTableName(), TableSchema.fromBean(AuditEventItem.class));
    }

    public void save(AuditEventItem item) {
        table().putItem(item);
    }

    public List<AuditEventItem> findByDocumentId(String documentId, int limit) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue("DOCUMENT#" + documentId).build()))
                .limit(limit)
                .scanIndexForward(false)
                .build();

        List<AuditEventItem> result = new ArrayList<>();
        table().query(request).stream().flatMap(p -> p.items().stream())
                .filter(i -> i.getSk() != null && i.getSk().startsWith("AUDIT#"))
                .limit(limit)
                .forEach(result::add);
        return result;
    }
}
