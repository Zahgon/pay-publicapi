package uk.gov.pay.api.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.Wallet;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.mocks.ConnectorMockClientJUnit5;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetPaymentServiceLedgerPactTest {

    private static final String ACCOUNT_ID = "123456";
    private static final String tokenLink = "a-token-link";
    private static final String CHARGE_ID_NON_EXISTENT_IN_CONNECTOR = "ch_123abc456xyz";

    private GetPaymentService getPaymentService;

    @RegisterExtension
    public static WireMockExtension connectorRule = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    public PactProviderExtension ledgerRule = new PactProviderExtension("ledger");

    @Mock
    private PublicApiConfig mockConfiguration;

    @BeforeEach
    public void setup() {
        connectorRule.resetAll();
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.baseUrl());
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        ConnectorMockClientJUnit5 connectorMockClient = new ConnectorMockClientJUnit5(connectorRule);
        connectorMockClient.respondChargeNotFound(ACCOUNT_ID, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR, "not found");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        getPaymentService = new GetPaymentService(publicApiUriGenerator,
                new ConnectorService(client, connectorUriGenerator),
                new LedgerService(client, ledgerUriGenerator));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-metadata"})
    public void testGetPaymentWithMetadataFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);
        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getMetadata(), is(notNullValue()));
        assertThat(paymentResponse.getMetadata().getMetadata().isEmpty(), is(false));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-gateway-transaction-id"})
    public void providerIdIsAvailableWhenPaymentIsSubmitted_Ledger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);
        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getProviderId(), is("gateway-tx-123456"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-delayed-capture-true"})
    public void testGetPaymentFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);

        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getState(), is(new PaymentState("created", false)));
        assertThat(paymentResponse.getDescription(), is("Test description"));
        assertThat(paymentResponse.getReference(), is("aReference"));
        assertThat(paymentResponse.getLanguage(), is(SupportedLanguage.ENGLISH));
        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getReturnUrl().get(), is("https://somewhere.gov.uk/rainbow/1"));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getCreatedDate(), is("2018-09-07T13:12:02.121Z"));
        assertThat(paymentResponse.getDelayedCapture(), is(true));
        assertThat(paymentResponse.getCorporateCardSurcharge(), is(Optional.empty()));
        assertThat(paymentResponse.getTotalAmount(), is(Optional.empty()));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.WEB));
        assertThat(paymentResponse.getLinks().getSelf().getHref(), containsString("v1/payments/" + CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getLinks().getSelf().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getRefunds().getHref(), containsString("v1/payments/" + CHARGE_ID_NON_EXISTENT_IN_CONNECTOR + "/refunds"));
        assertThat(paymentResponse.getLinks().getRefunds().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getNextUrl(), is(nullValue()));
        assertThat(paymentResponse.getLinks().getNextUrlPost(), is(nullValue()));
        assertThat(paymentResponse.getLinks().getCapture(), is(nullValue()));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-wallet-payment"})
    public void testGetPaymentFromLedgerWithWalletType() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);

        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getState(), is(new PaymentState("capturable", false)));
        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.WEB));
        assertThat(paymentResponse.getCardDetails().get().getCardHolderName(), is("J Doe"));
        assertThat(paymentResponse.getCardDetails().get().getWalletType().get(), is(Wallet.APPLE_PAY.getTitleCase()));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-corporate-surcharge"})
    public void testGetPaymentWithCorporateCardSurchargeFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getCorporateCardSurcharge().get(), is(250L));
        assertThat(paymentResponse.getTotalAmount().get(), is(2250L));
        assertThat(paymentResponse.getLinks().getCapture(), is(nullValue()));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-fee-and-net-amount"})
    public void testGetPaymentWithFeeAndNetAmountFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);

        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getFee().get(), is(5L));
        assertThat(paymentResponse.getNetAmount().get(), is(95L));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-settled-date"})
    public void testGetPaymentWithSettledDate() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, "ch_123abc456settlement");
        assertThat(paymentResponse.getSettlementSummary().isPresent(), is(true));
        assertThat(paymentResponse.getSettlementSummary().get().getSettledDate(), is("2020-09-19"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-authorisation-summary"})
    public void testGetPaymentWithAuthorisationSummaryFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getAuthorisationSummary().getThreeDSecure().isRequired(), is(true));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-rejected-recurring-payment-with-can-retry-true"})
    public void testGetRejectedPaymentWithNoRetryTrueFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getState().getCanRetry(), is(true));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.AGREEMENT));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-honoured-exemption"})
    public void testGetPaymentWithExemptionFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);

        assertThat(paymentResponse.getExemption(), is(notNullValue()));
        assertThat(paymentResponse.getExemption().getRequested(), is(true));
        assertThat(paymentResponse.getExemption().getType(), is("corporate"));
        assertThat(paymentResponse.getExemption().getOutcome().getResult(), is("honoured"));
    }
}
