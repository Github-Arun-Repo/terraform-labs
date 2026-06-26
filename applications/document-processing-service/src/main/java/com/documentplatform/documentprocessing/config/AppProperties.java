package com.documentplatform.documentprocessing.config;

import com.documentplatform.documentprocessing.enums.ExtractorMode;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "processing")
public class AppProperties {
    private int maxProcessingAttempts = 3;
    private int processingStaleTimeoutMinutes = 10;
    private ExtractorMode extractorMode = ExtractorMode.MOCK;
    private List<String> allowedContentTypes = List.of("application/pdf", "image/png", "image/jpeg", "image/tiff");
    private long maxFileSizeBytes = 20 * 1024 * 1024;
}
