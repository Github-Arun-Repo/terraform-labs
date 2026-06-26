package com.documentplatform.documentreview.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {
    private boolean readEventsEnabled = false;
}
