package com.documentplatform.documentprocessing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.documentplatform.documentprocessing.config.AwsProperties;
import com.documentplatform.documentprocessing.enums.DocumentStatus;
import com.documentplatform.documentprocessing.model.DocumentItem;
import com.documentplatform.documentprocessing.repository.DynamoDocumentRepository;
import com.documentplatform.documentprocessing.repository.DynamoDocumentRepository.ProcessingStartOutcome;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Integration test for the concurrency-sensitive {@code tryStartProcessing} conditional
 * update against a real DynamoDB (LocalStack). Validates the idempotency / single-winner
 * guarantee that protects against duplicate SQS deliveries.
 *
 * Requires Docker to run (LocalStack container).
 */
@Testcontainers
class DocumentProcessingIdempotencyIntegrationTest {

    private static final String TABLE = "DocumentInventory";
    private static final int MAX_ATTEMPTS = 3;
    private static final int STALE_TIMEOUT_MINUTES = 10;

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private static DynamoDbClient dynamoDbClient;
    private static DynamoDocumentRepository repository;

    @BeforeAll
    static void setup() {
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(LOCALSTACK.getEndpoint().toString()))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();

        createTable();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        AwsProperties props = new AwsProperties();
        props.getDynamodb().setTableName(TABLE);

        repository = new DynamoDocumentRepository(enhancedClient, dynamoDbClient, props);
    }

    @AfterAll
    static void tearDown() {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
    }

    @Test
    void duplicateDeliveryForSameInMemoryDocumentIsSkipped() {
        String documentId = seedUploadRequestedDocument();
        DocumentItem document = repository.findByDocumentId(documentId).orElseThrow();
        Instant now = Instant.now();

        ProcessingStartOutcome first = repository.tryStartProcessing(document, now, MAX_ATTEMPTS, STALE_TIMEOUT_MINUTES);
        ProcessingStartOutcome second = repository.tryStartProcessing(document, now.plusSeconds(5), MAX_ATTEMPTS, STALE_TIMEOUT_MINUTES);

        assertThat(first).isEqualTo(ProcessingStartOutcome.STARTED);
        // The same document is already PROCESSING and recently started -> not re-processed.
        assertThat(second).isEqualTo(ProcessingStartOutcome.SKIPPED_RECENT_PROCESSING);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(document.getProcessingAttempts()).isEqualTo(1);
    }

    @Test
    void concurrentWorkersProduceASingleWinnerViaConditionalUpdate() {
        String documentId = seedUploadRequestedDocument();
        // Two independent reads simulate two SQS consumers racing on the same document.
        DocumentItem worker1 = repository.findByDocumentId(documentId).orElseThrow();
        DocumentItem worker2 = repository.findByDocumentId(documentId).orElseThrow();
        Instant now = Instant.now();

        ProcessingStartOutcome firstWinner = repository.tryStartProcessing(worker1, now, MAX_ATTEMPTS, STALE_TIMEOUT_MINUTES);
        ProcessingStartOutcome loser = repository.tryStartProcessing(worker2, now, MAX_ATTEMPTS, STALE_TIMEOUT_MINUTES);

        assertThat(firstWinner).isEqualTo(ProcessingStartOutcome.STARTED);
        // Second worker's stale UPLOAD_REQUESTED view fails the DynamoDB condition expression.
        assertThat(loser).isEqualTo(ProcessingStartOutcome.TRANSITION_CONFLICT);

        DocumentItem persisted = repository.findByDocumentId(documentId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(persisted.getProcessingAttempts()).isEqualTo(1);
    }

    private String seedUploadRequestedDocument() {
        String documentId = UUID.randomUUID().toString();
        String now = Instant.now().toString();
        String s3Key = "invoice/raw/CUST-1/" + documentId + "-invoice.pdf";

        DocumentItem item = new DocumentItem();
        item.setPk("DOCUMENT#" + documentId);
        item.setSk("METADATA");
        item.setEntityType("DOCUMENT");
        item.setDocumentId(documentId);
        item.setCustomerId("CUST-1");
        item.setDocumentType("INVOICE");
        item.setFileName("invoice.pdf");
        item.setContentType("application/pdf");
        item.setFileSize(1024L);
        item.setBucketName("documents-inventory-s3");
        item.setS3Key(s3Key);
        item.setStatus(DocumentStatus.UPLOAD_REQUESTED);
        item.setProcessingAttempts(0);
        item.setDocumentRevision(1L);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setGsi1Pk("S3KEY#" + s3Key);
        item.setGsi1Sk("DOCUMENT#" + documentId);
        item.setGsi2Pk("CUSTOMER#CUST-1#STATUS#" + DocumentStatus.UPLOAD_REQUESTED.name());
        item.setGsi2Sk("UPDATED_AT#" + now + "#DOCUMENT#" + documentId);

        repository.save(item);
        return documentId;
    }

    private static void createTable() {
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        attr("PK"), attr("SK"),
                        attr("GSI1PK"), attr("GSI1SK"),
                        attr("GSI2PK"), attr("GSI2SK"))
                .keySchema(
                        KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
                .globalSecondaryIndexes(
                        gsi("GSI1", "GSI1PK", "GSI1SK"),
                        gsi("GSI2", "GSI2PK", "GSI2SK"))
                .build());

        dynamoDbClient.waiter().waitUntilTableExists(b -> b.tableName(TABLE));
    }

    private static AttributeDefinition attr(String name) {
        return AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.S).build();
    }

    private static GlobalSecondaryIndex gsi(String name, String hashKey, String rangeKey) {
        return GlobalSecondaryIndex.builder()
                .indexName(name)
                .keySchema(
                        KeySchemaElement.builder().attributeName(hashKey).keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName(rangeKey).keyType(KeyType.RANGE).build())
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .build();
    }
}
