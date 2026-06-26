package com.documentplatform.documentprocessing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class S3EventParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public S3EventRecord parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            // SQS-wrapped S3 notification
            if (root.has("Message")) {
                JsonNode embedded = objectMapper.readTree(root.get("Message").asText());
                return parseS3Notification(embedded);
            }

            // Direct S3 notification
            if (root.has("Records")) {
                return parseS3Notification(root);
            }

            // Simplified local event format
            if (root.has("bucket") && root.has("key")) {
                return new S3EventRecord(root.get("bucket").asText(), decode(root.get("key").asText()));
            }

            throw new IllegalArgumentException("Unsupported event format");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot parse S3 event", ex);
        }
    }

    private S3EventRecord parseS3Notification(JsonNode root) {
        JsonNode first = root.get("Records").get(0);
        String bucket = first.get("s3").get("bucket").get("name").asText();
        String key = decode(first.get("s3").get("object").get("key").asText());
        return new S3EventRecord(bucket, key);
    }

    private String decode(String key) {
        return URLDecoder.decode(key, StandardCharsets.UTF_8);
    }
}
