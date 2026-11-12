package uk.gov.pay.api.service;

import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CancelPaymentServicePactTest {

    private final Account ACCOUNT = new Account("123456", TokenPaymentType.CARD, "a-token-link");
    
    private CancelPaymentService cancelPaymentService;
    
    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    @Mock
    private PublicApiConfig mockConfiguration;
    
    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        cancelPaymentService = new CancelPaymentService(client, connectorUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-cancel-payment-with-created-state"})
    public void cancelAPaymentWithCreatedState() {
        HttpResponse<?> cancelPaymentResponse = cancelPaymentService.cancel(ACCOUNT, "charge8133029783750964639");
        assertThat(cancelPaymentResponse.code(), is(204));
    }
}
