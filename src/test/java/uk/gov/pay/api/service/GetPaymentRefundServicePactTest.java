package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GetPaymentRefundServicePactTest {

    private static final String ACCOUNT_ID = "123456";
    private static final String CHARGE_ID = "123456789";
    private static final String REFUND_ID = "r_123abc456def";

    private GetPaymentRefundService getPaymentService;

    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");
    
    @RegisterExtension
    public PactProviderExtension ledgerRule = new PactProviderExtension("ledger");

    @Mock
    private PublicApiConfig mockConfiguration;

    @BeforeEach
    public void setup() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        getPaymentService = new GetPaymentRefundService(new ConnectorService(client, connectorUriGenerator),
                new LedgerService(client, ledgerUriGenerator), publicApiUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-get-payment-refund"})
    public void testGetPayment() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        RefundResponse refund = getPaymentService.getConnectorPaymentRefund(account, CHARGE_ID, REFUND_ID);

        assertThat(refund.getRefundId(), is(REFUND_ID));
        assertThat(refund.getStatus(), is("success"));
        assertThat(refund.getAmount(), is(100L));
        assertThat(refund.getFee(), is(2L));
        assertThat(refund.getNetAmount(), is(-102L));
        assertThat(refund.getCreatedDate(), is("2018-09-22T10:14:16.067Z"));
        assertThat(refund.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789"));
        assertThat(refund.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789/refunds/r_123abc456def"));
    }
    
    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-refund"})
    public void testGetLedgerPaymentRefund() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        RefundResponse refund = getPaymentService.getLedgerPaymentRefund(account, CHARGE_ID, REFUND_ID);

        assertThat(refund.getRefundId(), is(REFUND_ID));
        assertThat(refund.getStatus(), is("success"));
        assertThat(refund.getAmount(), is(100L));
        assertThat(refund.getFee(), is(2L));
        assertThat(refund.getNetAmount(), is(-102L));
        assertThat(refund.getCreatedDate(), is("2018-09-22T10:14:16.067Z"));
        assertThat(refund.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789"));
        assertThat(refund.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789/refunds/r_123abc456def"));
    }
}
