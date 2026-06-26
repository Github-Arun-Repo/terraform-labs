package com.terraformlabs.ums.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.secrets")
public class AwsSecretsProperties {

    private String dbPasswordSecretName;
    private String region;

    public String getDbPasswordSecretName() {
        return dbPasswordSecretName;
    }

    public void setDbPasswordSecretName(String dbPasswordSecretName) {
        this.dbPasswordSecretName = dbPasswordSecretName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
