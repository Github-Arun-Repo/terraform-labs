package com.documentplatform.documentapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocumentApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentApiServiceApplication.class, args);
    }
}
