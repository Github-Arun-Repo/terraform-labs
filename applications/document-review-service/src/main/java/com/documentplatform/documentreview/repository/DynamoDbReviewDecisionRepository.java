package com.documentplatform.documentreview.repository;

import com.documentplatform.documentreview.config.AwsProperties;
import com.documentplatform.documentreview.model.ReviewDecisionItem;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@RequiredArgsConstructor
public class DynamoDbReviewDecisionRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;

    private DynamoDbTable<ReviewDecisionItem> table() {
        return enhancedClient.table(awsProperties.getDynamodb().getDocumentTableName(), TableSchema.fromBean(ReviewDecisionItem.class));
    }

    public Optional<ReviewDecisionItem> findLatestByDocumentId(String documentId) {
        ReviewDecisionItem item = table().getItem(Key.builder().partitionValue("DOCUMENT#" + documentId).sortValue("DECISION#LATEST").build());
        return Optional.ofNullable(item);
    }

    public void save(ReviewDecisionItem item) {
        table().putItem(item);
    }
}
