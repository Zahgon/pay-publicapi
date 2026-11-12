package uk.gov.pay.api.service;

import io.micronaut.http.HttpResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.exception.AuthorisationRequestException;
import uk.gov.pay.api.exception.mapper.AuthorisationRequestExceptionMapper;
import uk.gov.pay.api.model.AuthorisationRequest;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthorisationServicePactTest {
    private AuthorisationService authorisationService;
    private AuthorisationRequest request;
    private AuthorisationRequestExceptionMapper mapper;

    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    @Mock
    private PublicApiConfig mockConfiguration;

    @BeforeEach
    public void setup() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        ConnectorUriGenerator uriGenerator = new ConnectorUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        authorisationService = new AuthorisationService(client, uriGenerator);
        mapper = new AuthorisationRequestExceptionMapper();
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-valid-token"})
    public void testAuthorisePayment() {
        request = new AuthorisationRequest("onetime-12345-token", "4242424242424242", "123",
                "09/29", "Joe Boggs");
        HttpResponse<?> response = authorisationService.authoriseRequest(request);
        assertThat(response.code(), is(204));
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-invalid-token"})
    public void testInvalidToken() {
        request = new AuthorisationRequest("invalid-token", "4242424242424242", "123",
                "09/29", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(400)));
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-an-already-used-token"})
    public void testAlreadyUsedToken() {
        request = new AuthorisationRequest("onetime-12345-token", "4242424242424242", "123",
                "09/29", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(400)));
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-invalid-card-number"})
    public void testInvalidCardNumber() {
        request = new AuthorisationRequest("onetime-12345-token", "0000000000000000", "123",
                "09/29", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(402)));
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-invalid-expiry-date"})
    public void testMissingExpiryDate() {
        request = new AuthorisationRequest("onetime-12345-token", "4242424242424242", "123",
                "09/21", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(422)));
    }
}
