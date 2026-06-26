package com.documentplatform.documentapi.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentIdGenerator {

    public String generate() {
        return "doc-" + UUID.randomUUID();
    }
}
