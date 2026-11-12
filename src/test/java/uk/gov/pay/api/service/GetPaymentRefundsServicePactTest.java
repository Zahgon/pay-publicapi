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
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.RefundsResponse;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetPaymentRefundsServicePactTest {

    private static final String ACCOUNT_ID = "123456";
    @RegisterExtension
    public PactProviderExtension ledgerRule = new PactProviderExtension("ledger");
    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");
    @Mock
    private PublicApiConfig mockConfiguration;
    private GetPaymentRefundsService getPaymentRefundsService;

    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);

        RestClient client = RestClients.newRestClient(false);
        LedgerService ledgerService = new LedgerService(client, ledgerUriGenerator);

        getPaymentRefundsService = new GetPaymentRefundsService(ledgerService, publicApiUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-refunds"})
    public void shouldReturnRefundsForPaymentCorrectlyFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        RefundsResponse response = getPaymentRefundsService.getLedgerTransactionTransactions(account, "ch_123abc456xyz");
        assertThat(response.getPaymentId(), is("ch_123abc456xyz"));
        assertThat(response.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz/refunds"));
        assertThat(response.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz"));

        List<RefundResponse> refunds = response.getEmbedded().getRefunds();
        assertThat(refunds.size(), is(2));
        
        assertThat(refunds.get(0).getRefundId(), is("refund-transaction-id1"));
        assertThat(refunds.get(0).getStatus(), is("submitted"));
        assertThat(refunds.get(0).getAmount(), is(100L));
        assertThat(refunds.get(0).getFee(), is(2L));
        assertThat(refunds.get(0).getNetAmount(), is(-102L));
        assertThat(refunds.get(0).getCreatedDate(), is("2018-09-22T10:14:16.067Z"));
        assertThat(refunds.get(0).getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz"));
        assertThat(refunds.get(0).getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz/refunds/refund-transaction-id1"));

        assertThat(refunds.get(1).getRefundId(), is("refund-transaction-id2"));
        assertThat(refunds.get(1).getStatus(), is("error"));
        assertThat(refunds.get(1).getAmount(), is(200L));
        assertThat(refunds.get(1).getFee(), is(3L));
        assertThat(refunds.get(1).getNetAmount(), is(-203L));
        assertThat(refunds.get(1).getCreatedDate(), is("2018-09-22T10:16:16.067Z"));
        assertThat(refunds.get(1).getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz"));
        assertThat(refunds.get(1).getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz/refunds/refund-transaction-id2"));
    }
}
