package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateRefundException;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateRefundServicePactTest {
    
    private CreateRefundService createRefundService;

    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    @Mock
    private PublicApiConfig configuration;
    
    @Mock
    private GetPaymentService getPaymentService;

    private Account account;

    @BeforeEach
    public void setup() {
        when(configuration.getConnectorUrl()).thenReturn(connectorRule.getUrl()); // We will actually send real requests here, which will be intercepted by pact
        when(configuration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        RestClient client = RestClients.newRestClient(false);
        
        createRefundService = new CreateRefundService(getPaymentService, client, configuration);
        account = new Account("123456", TokenPaymentType.CARD, "a-token-link");
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-create-refund-for-disabled-account"})
    public void creating_refund_for_disabled_account_should_return_403() {
        var requestPayload = new CreatePaymentRefundRequest(100, 100);

        try {
            createRefundService.createRefund(account,"654321" , requestPayload);
            fail("Expected CreateRefundException to be thrown");
        } catch (CreateRefundException e) {
            assertThat(e.getErrorIdentifier(), is(ErrorIdentifier.ACCOUNT_DISABLED));
        }
    }
}
