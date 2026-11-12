package uk.gov.pay.api.service;

import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.utils.UriTemplate;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Singleton
public class PublicApiUriGenerator {

    private final String baseUrl;

    @Inject
    public PublicApiUriGenerator(PublicApiConfig configuration) {
        this.baseUrl = configuration.getBaseUrl();
    }
    
    public URI getPaymentURI(String chargeId) {
        return UriTemplate.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}")
                .build(chargeId);
    }
    public URI getRefundsURI(String chargeId, String refundId) {
        return UriTemplate.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}/refunds/{refunds}")
                .build(chargeId, refundId);
    }

    public URI getPaymentEventsURI(String chargeId) {
        return UriTemplate.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}/events")
                .build(chargeId);
    }

    public URI getPaymentCancelURI(String chargeId) {
        return UriTemplate.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}/cancel")
                .build(chargeId);
    }

    public URI getPaymentRefundsURI(String chargeId) {
        return UriTemplate.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}/refunds")
                .build(chargeId);
    }
    
    public URI getPaymentCaptureURI(String chargeId) {
        return UriTemplate.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}/capture")
                .build(chargeId);
    }
    
    public URI getPaymentAuthorisationURI(){
        return UriTemplate.fromUri(baseUrl)
                .path("/v1/auth")
                .build();
    }

    public String convertHostToPublicAPI(String link) {
        URI originalUri = UriTemplate.fromUri(link).build();
        URI newUri = UriTemplate.fromUri(baseUrl)
                .path(originalUri.getPath())
                .replaceQuery(originalUri.getQuery())
                .build();
        return URLDecoder.decode(newUri.toString(), StandardCharsets.UTF_8);
    }
}
