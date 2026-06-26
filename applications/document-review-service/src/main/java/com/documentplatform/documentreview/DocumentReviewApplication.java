package com.documentplatform.documentreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocumentReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentReviewApplication.class, args);
    }
}
