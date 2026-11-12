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
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchAgreementsLedgerServicePactTest {
    @RegisterExtension
    public PactProviderExtension ledgerRule = new PactProviderExtension("ledger");
    @Mock
    private PublicApiConfig mockConfiguration;

    private LedgerService ledgerService;

    private static final String LEDGER_SERVICE_URL = "http://ledger.service.backend/";

    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());

        RestClient client = RestClients.newRestClient(false);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);

        ledgerService = new LedgerService(client, ledgerUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-agreements-with-page-and-display-size"})
    public void searchWithPageAndDisplaySize_shouldReturnCorrectPageAndPaginationLinks() {
        AgreementSearchParams params = new AgreementSearchParams(null, null, "2", "1");
        Account account = new Account("777", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);

        assertThat(results.getResults().size(), is(1));
        assertThat(results.getCount(), is(1));
        assertThat(results.getTotal(), is(3));
        assertThat(results.getPage(), is(2));
        assertThat(results.getLinks().getSelf().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=2"));
        assertThat(results.getLinks().getFirstPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=1"));
        assertThat(results.getLinks().getLastPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=3"));
        assertThat(results.getLinks().getPrevPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=1"));
        assertThat(results.getLinks().getNextPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=3"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-agreements-with-status"})
    public void searchWithStatus_shouldReturnCorrectAgreement() {
        AgreementSearchParams params = new AgreementSearchParams(null, "active", "1", "20");
        Account account = new Account("777", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);
        assertThat(results.getResults().size(), is(1));
        assertThat(results.getResults().get(0).getStatus(), is("ACTIVE"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-agreements-with-reference"})
    public void searchWithReference_shouldReturnCorrectAgreement() {
        AgreementSearchParams params = new AgreementSearchParams("a-valid-reference", null, "1", "20");
        Account account = new Account("3456", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);
        assertThat(results.getResults().size(), is(1));
        assertThat(results.getResults().get(0).getReference(), is("a-valid-reference"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-agreement-not-found"})
    public void getOneAgreementNotFound() {
        AgreementSearchParams params = new AgreementSearchParams("invalid-reference", null, "1", "20");
        Account account = new Account("3456", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);
        assertThat(results.getResults().size(), is(0));
        assertThat(results.getCount(), is(0));
        assertThat(results.getTotal(), is(0));
    }
}
