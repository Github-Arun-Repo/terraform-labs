package com.documentplatform.documentprocessing.controller;

import com.documentplatform.documentprocessing.service.DocumentProcessingService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/processing")
public class ProcessingController {

    private final DocumentProcessingService processingService;

    public ProcessingController(DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping("/process")
    public Map<String, Object> process(@RequestBody Map<String, String> request) {
        boolean success = processingService.process(request.get("bucket"), request.get("key"));
        return Map.of("success", success);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
