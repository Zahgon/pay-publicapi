package uk.gov.pay.api.service;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CancelAgreementConnectorServicePactTest {

    private final Account ACCOUNT = new Account("123456", TokenPaymentType.CARD, "a-token-link");
    
    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    private ConnectorService connectorService;
    @Mock
    private PublicApiConfig mockConfiguration;

    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        connectorService = new ConnectorService(client, connectorUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-cancel-agreement-with-active-status"})
    public void cancelAnAgreementWithActiveStatus() {
        connectorService.cancelAgreement(ACCOUNT, "agreement1234567");
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-cancel-agreement-with-cancelled-status"})
    public void cancelAnAgreementWithCancelledStatus() {
        CancelAgreementException cancelAgreementException = assertThrows(CancelAgreementException.class,
                () -> connectorService.cancelAgreement(ACCOUNT, "agreement9876543"));
        assertThat(cancelAgreementException, hasProperty("errorStatus", Matchers.is(400)));
        assertThat(cancelAgreementException, hasProperty("errorIdentifier", is(ErrorIdentifier.AGREEMENT_NOT_ACTIVE)));
        assertThat(cancelAgreementException.getConnectorErrorMessage(), is("Payment instrument not active."));
    }
    
}
