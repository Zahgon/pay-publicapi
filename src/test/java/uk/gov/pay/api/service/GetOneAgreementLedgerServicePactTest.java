package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.agreement.model.AgreementLedgerResponse;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.GetAgreementException;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetOneAgreementLedgerServicePactTest {

    private final Account ACCOUNT = new Account("3456", TokenPaymentType.CARD, "a-token-link");

    @RegisterExtension
    public PactProviderExtension ledgerRule = new PactProviderExtension("ledger");

    private LedgerService ledgerService;
    @Mock
    private PublicApiConfig mockConfiguration;

    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        ledgerService = new LedgerService(client, ledgerUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-one-agreement"})
    public void getOneAgreement() {
        AgreementLedgerResponse getAgreementResponse = ledgerService.getAgreement(ACCOUNT, "abcdefghijklmnopqrstuvwxyz");
        assertThat(getAgreementResponse.getStatus(), is("ACTIVE"));
        assertThat(getAgreementResponse.getExternalId(), is("abcdefghijklmnopqrstuvwxyz"));
        assertThat(getAgreementResponse.getPaymentInstrument().getAgreementExternalId(), is("abcdefghijklmnopqrstuvwxyz"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-one-agreement-not-found"})
    public void getOneAgreementNotFound() {
        GetAgreementException getAgreementException = assertThrows(GetAgreementException.class,
        () -> ledgerService.getAgreement(ACCOUNT, "non-existent-agreement-id"));
        assertThat(getAgreementException, hasProperty("errorStatus", is(404)));
    }
}
