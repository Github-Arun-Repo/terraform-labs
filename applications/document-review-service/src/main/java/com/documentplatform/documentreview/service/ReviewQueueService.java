package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.dto.ReviewQueueItemResponse;
import com.documentplatform.documentreview.dto.ReviewQueueResponse;
import com.documentplatform.documentreview.exception.ValidationException;
import com.documentplatform.documentreview.repository.DynamoDbDocumentRepository;
import com.documentplatform.documentreview.repository.DynamoDbExtractionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewQueueService {

    private final DynamoDbDocumentRepository documentRepository;
    private final DynamoDbExtractionRepository extractionRepository;
    private final ReviewMapper mapper;

    public ReviewQueueResponse queue(String customerId, String status, int limit, String nextToken) {
        if (customerId == null || customerId.isBlank()) {
            throw new ValidationException("customerId is required");
        }

        DynamoDbDocumentRepository.Holder<String> resolvedNextToken = new DynamoDbDocumentRepository.Holder<>();
        List<ReviewQueueItemResponse> items = documentRepository.findReviewQueue(customerId, status, limit, nextToken, resolvedNextToken).stream()
                .map(doc -> mapper.toQueueItem(doc, extractionRepository.findByDocumentId(doc.getDocumentId()).orElse(null)))
                .toList();

        return ReviewQueueResponse.builder()
                .items(items)
                .nextToken(resolvedNextToken.value)
                .build();
    }
}
