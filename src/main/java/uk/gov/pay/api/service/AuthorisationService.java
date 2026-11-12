package uk.gov.pay.api.service;

import io.micronaut.http.HttpResponse;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.exception.AuthorisationRequestException;
import uk.gov.pay.api.model.AuthorisationRequest;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;

@Singleton
public class AuthorisationService {

    private final RestClient client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public AuthorisationService(RestClient client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public HttpResponse<?> authoriseRequest(AuthorisationRequest authorisationRequest) {
        RestClientResponse response = client.postJson(connectorUriGenerator.authorisationURI(), authorisationRequest);

        if (response.getStatus() != SC_NO_CONTENT) {
            throw new AuthorisationRequestException(response);
        }

        return HttpResponse.noContent();
    }

}
