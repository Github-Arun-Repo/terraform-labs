package com.documentplatform.documentprocessing.service;

public record S3EventRecord(String bucket, String key) {
}
