package uk.gov.pay.api.app.config;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

@ConfigurationProperties("publicapi")
public class PublicApiConfig {

    @NotNull
    private String baseUrl;

    @NotNull
    private String connectorUrl;

    @NotNull
    private String ledgerUrl;

    @NotNull
    private String publicAuthUrl;

    @NotNull
    private Boolean allowHttpForReturnUrl;

    private String apiKeyHmacSecret;

    @NotNull
    private String authenticationCachePolicy;

    private int adminPort;

    private URI ecsContainerMetadataUriV4;

    private RestClientConfig restClientConfig;

    private RedisConfiguration redis;

    private RateLimiterConfig rateLimiterConfig;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getConnectorUrl() {
        return connectorUrl;
    }

    public void setConnectorUrl(String connectorUrl) {
        this.connectorUrl = connectorUrl;
    }

    public String getLedgerUrl() {
        return ledgerUrl;
    }

    public void setLedgerUrl(String ledgerUrl) {
        this.ledgerUrl = ledgerUrl;
    }

    public String getPublicAuthUrl() {
        return publicAuthUrl;
    }

    public void setPublicAuthUrl(String publicAuthUrl) {
        this.publicAuthUrl = publicAuthUrl;
    }

    public Boolean getAllowHttpForReturnUrl() {
        return allowHttpForReturnUrl;
    }

    public void setAllowHttpForReturnUrl(Boolean allowHttpForReturnUrl) {
        this.allowHttpForReturnUrl = allowHttpForReturnUrl;
    }

    public String getApiKeyHmacSecret() {
        return apiKeyHmacSecret;
    }

    public void setApiKeyHmacSecret(String apiKeyHmacSecret) {
        this.apiKeyHmacSecret = apiKeyHmacSecret;
    }

    public CaffeineSpec getAuthenticationCachePolicy() {
        return CaffeineSpec.parse(authenticationCachePolicy);
    }

    public void setAuthenticationCachePolicy(String authenticationCachePolicy) {
        this.authenticationCachePolicy = authenticationCachePolicy;
    }

    public int getAdminPort() {
        return adminPort;
    }

    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
    }

    public Optional<URI> getEcsContainerMetadataUriV4() {
        return Optional.ofNullable(ecsContainerMetadataUriV4);
    }

    public void setEcsContainerMetadataUriV4(@Nullable String ecsContainerMetadataUriV4) {
        this.ecsContainerMetadataUriV4 = ecsContainerMetadataUriV4 == null || ecsContainerMetadataUriV4.isBlank()
                ? null : URI.create(ecsContainerMetadataUriV4);
    }

    public RestClientConfig getRestClientConfig() {
        return restClientConfig;
    }

    @Inject
    public void setRestClientConfig(RestClientConfig restClientConfig) {
        this.restClientConfig = restClientConfig;
    }

    public RateLimiterConfig getRateLimiterConfig() {
        return rateLimiterConfig;
    }

    @Inject
    public void setRateLimiterConfig(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    public RedisConfiguration getRedisConfiguration() {
        return redis;
    }

    @Inject
    public void setRedisConfiguration(RedisConfiguration redis) {
        this.redis = redis;
    }
}
