package uk.gov.pay.api.app.config;

import io.micronaut.context.annotation.ConfigurationProperties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;

import static java.lang.String.format;

@ConfigurationProperties("redis")
public class RedisConfiguration {

    @NotNull
    private String endpoint;

    private boolean ssl;

    private Duration commandTimeout;

    private Duration connectTimeout;

    @NotNull
    private Duration reconnectDelayLowerBound;

    @NotNull
    private Duration reconnectDelayUpperBound;

    private long reconnectDelayBase;

    public String getUrl() {
        return format("%s://%s", ssl ? "rediss" : "redis", endpoint);
    }

    public Long getCommandTimeout() {
        return commandTimeout.toMillis();
    }

    public Long getConnectTimeout() {
        return connectTimeout.toMillis();
    }

    public Long getReconnectDelayLowerBound() {
        return reconnectDelayLowerBound.toMillis();
    }

    public Long getReconnectDelayUpperBound() {
        return reconnectDelayUpperBound.toMillis();
    }

    public long getReconnectDelayBase() {
        return reconnectDelayBase;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public void setCommandTimeout(Duration commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReconnectDelayLowerBound(Duration reconnectDelayLowerBound) {
        this.reconnectDelayLowerBound = reconnectDelayLowerBound;
    }

    public void setReconnectDelayUpperBound(Duration reconnectDelayUpperBound) {
        this.reconnectDelayUpperBound = reconnectDelayUpperBound;
    }

    public void setReconnectDelayBase(long reconnectDelayBase) {
        this.reconnectDelayBase = reconnectDelayBase;
    }
}
