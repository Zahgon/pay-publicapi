package uk.gov.pay.api.app.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("rest-client")
public class RestClientConfig {

    private String disabledSecureConnection = "false";

    public RestClientConfig() {
    }

    public RestClientConfig(boolean disabledSecureConnection) {
        this.disabledSecureConnection = Boolean.valueOf(disabledSecureConnection).toString();
    }

    public Boolean isDisabledSecureConnection() {
        return "true".equals(disabledSecureConnection);
    }

    public void setDisabledSecureConnection(String disabledSecureConnection) {
        this.disabledSecureConnection = disabledSecureConnection;
    }
}
