package com.documentplatform.documentprocessing.service;

import com.documentplatform.documentprocessing.config.AwsProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsPollingService {

    private final SqsClient sqsClient;
    private final AwsProperties awsProperties;
    private final S3EventParser eventParser;
    private final DocumentProcessingService processingService;

    @Scheduled(fixedDelayString = "${DOCUMENT_INGESTION_POLL_INTERVAL_MS:2000}")
    public void poll() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(awsProperties.getSqs().getQueueUrl())
                .maxNumberOfMessages(awsProperties.getSqs().getMaxMessages())
                .waitTimeSeconds(awsProperties.getSqs().getWaitTimeSeconds())
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();
        for (Message message : messages) {
            boolean success = false;
            try {
                S3EventRecord event = eventParser.parse(message.body());
                success = processingService.process(event.bucket(), event.key());
            } catch (Exception ex) {
                log.error("failed to process SQS message", ex);
            }

            if (success) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(awsProperties.getSqs().getQueueUrl())
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        }
    }
}
