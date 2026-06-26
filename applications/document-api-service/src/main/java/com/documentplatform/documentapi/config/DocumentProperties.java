package com.documentplatform.documentapi.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "document")
public class DocumentProperties {

    @Min(1)
    private long maxFileSizeBytes = 20_971_520;

    @NotEmpty
    private List<String> allowedContentTypes;
}
