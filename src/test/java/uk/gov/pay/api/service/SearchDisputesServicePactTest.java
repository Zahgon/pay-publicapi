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
import uk.gov.pay.api.exception.SearchDisputesException;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.search.PaginationDecorator;
import uk.gov.pay.api.model.search.dispute.DisputeForSearchResult;
import uk.gov.pay.api.model.search.dispute.DisputesSearchResults;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchDisputesServicePactTest {
    @RegisterExtension
    public PactProviderExtension ledgerRule = new PactProviderExtension("ledger");

    @Mock
    private PublicApiConfig mockConfiguration;

    private SearchDisputesService service;

    private static final String accountId = "123456";
    private static final String tokenLink = "a-token-link";

    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        RestClient client = RestClients.newRestClient(false);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);

        service = new SearchDisputesService(new LedgerService(client, ledgerUriGenerator),
                new PublicApiUriGenerator(mockConfiguration),
                new PaginationDecorator(mockConfiguration));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-disputes_by_settled_dates"})
    public void searchDisputesBySettledDatesShouldReturnFromLedger() {
        Account account = new Account(accountId, TokenPaymentType.CARD, tokenLink);
        DisputesSearchParams.Builder paramsBuilder = new DisputesSearchParams.Builder();
        DisputesSearchParams params = paramsBuilder
                .withFromSettledDate("2022-05-27")
                .withToSettledDate("2022-05-27")
                .build();
        DisputesSearchResults results = service.searchDisputes(account, params);
        assertThat(results.getResults().size(), is(1));
        assertThat(results.getCount(), is(1));
        assertThat(results.getPage(), is(1));
        assertThat(results.getTotal(), is(1));
        assertThat(results.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_settled_date=2022-05-27&to_settled_date=2022-05-27&display_size=500&page=1"));
        assertThat(results.getLinks().getFirstPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_settled_date=2022-05-27&to_settled_date=2022-05-27&display_size=500&page=1"));
        assertThat(results.getLinks().getLastPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_settled_date=2022-05-27&to_settled_date=2022-05-27&display_size=500&page=1"));

        DisputeForSearchResult dispute = results.getResults().get(0);
        assertThat(dispute.getDisputeId(), is("dispute97837509646393e3C"));
        assertThat(dispute.getReason(), is("fraudulent"));
        assertThat(dispute.getAmount(), is(1000L));
        assertThat(dispute.getFee(), is(1500L));
        assertThat(dispute.getNetAmount(), is(-2500L));
        assertThat(dispute.getPaymentId(), is("parent-abcde-12345"));
        assertThat(dispute.getCreatedDate(), is("2022-05-20T19:05:00.000Z"));
        assertThat(dispute.getEvidenceDueDate(), is("2022-05-27T19:05:00.000Z"));
        assertThat(dispute.getSettlementSummary().getSettledDate(), is("2022-05-27"));
        assertThat(dispute.getStatus(), is("lost"));
        assertThat(dispute.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/parent-abcde-12345"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-disputes_by_state"})
    public void searchDisputesByStatus_ShouldReturnFromLedger_andShouldRewriteQueryParamsAndLinks() {
        Account account = new Account(accountId, TokenPaymentType.CARD, tokenLink);
        DisputesSearchParams.Builder paramsBuilder = new DisputesSearchParams.Builder();
        DisputesSearchParams params = paramsBuilder
                .withStatus("lost")
                .build();
        DisputesSearchResults results = service.searchDisputes(account, params);
        assertThat(results.getResults().size(), is(1));
        assertThat(results.getCount(), is(1));
        assertThat(results.getPage(), is(1));
        assertThat(results.getTotal(), is(1));
        assertThat(results.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/disputes?display_size=500&page=1&status=lost"));
        assertThat(results.getLinks().getFirstPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?display_size=500&page=1&status=lost"));
        assertThat(results.getLinks().getLastPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?display_size=500&page=1&status=lost"));

        DisputeForSearchResult dispute = results.getResults().get(0);
        assertThat(dispute.getDisputeId(), is("dispute97837509646393e3C"));
        assertThat(dispute.getReason(), is("fraudulent"));
        assertThat(dispute.getAmount(), is(1000L));
        assertThat(dispute.getFee(), is(1500L));
        assertThat(dispute.getNetAmount(), is(-2500L));
        assertThat(dispute.getPaymentId(), is("parent-abcde-12345"));
        assertThat(dispute.getCreatedDate(), is("2022-05-20T19:05:00.000Z"));
        assertThat(dispute.getEvidenceDueDate(), is("2022-05-27T19:05:00.000Z"));
        assertThat(dispute.getSettlementSummary().getSettledDate(), is("2022-05-27"));
        assertThat(dispute.getStatus(), is("lost"));
        assertThat(dispute.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/parent-abcde-12345"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-disputes-by-dates"})
    public void searchDisputesByDatesShouldReturnFromLedger() {
        Account account = new Account(accountId, TokenPaymentType.CARD, tokenLink);
        DisputesSearchParams.Builder paramsBuilder = new DisputesSearchParams.Builder();
        DisputesSearchParams params = paramsBuilder
                .withFromDate("2022-05-20T19:04:00Z")
                .withToDate("2022-05-20T19:06:00Z")
                .build();
        DisputesSearchResults results = service.searchDisputes(account, params);
        assertThat(results.getResults().size(), is(1));
        assertThat(results.getCount(), is(1));
        assertThat(results.getPage(), is(1));
        assertThat(results.getTotal(), is(1));
        assertThat(results.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_date=2022-05-20T19%3A04%3A00Z&to_date=2022-05-20T19%3A06%3A00Z&display_size=500&page=1"));
        assertThat(results.getLinks().getFirstPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_date=2022-05-20T19%3A04%3A00Z&to_date=2022-05-20T19%3A06%3A00Z&display_size=500&page=1"));
        assertThat(results.getLinks().getLastPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_date=2022-05-20T19%3A04%3A00Z&to_date=2022-05-20T19%3A06%3A00Z&display_size=500&page=1"));

        DisputeForSearchResult dispute = results.getResults().get(0);
        assertThat(dispute.getDisputeId(), is("dispute97837509646393e3C"));
        assertThat(dispute.getReason(), is("fraudulent"));
        assertThat(dispute.getAmount(), is(1000L));
        assertThat(dispute.getFee(), is(1500L));
        assertThat(dispute.getNetAmount(), is(-2500L));
        assertThat(dispute.getPaymentId(), is("parent-abcde-12345"));
        assertThat(dispute.getCreatedDate(), is("2022-05-20T19:05:00.000Z"));
        assertThat(dispute.getEvidenceDueDate(), is("2022-05-27T19:05:00.000Z"));
        assertThat(dispute.getSettlementSummary().getSettledDate(), is("2022-05-27"));
        assertThat(dispute.getStatus(), is("lost"));
        assertThat(dispute.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/parent-abcde-12345"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-disputes-no-result"})
    public void searchDisputesNoResult() {
        Account account = new Account(accountId, TokenPaymentType.CARD, tokenLink);
        DisputesSearchParams.Builder paramsBuilder = new DisputesSearchParams.Builder();
        DisputesSearchParams params = paramsBuilder
                .withFromDate("2021-05-20T19:04:00Z")
                .withToDate("2021-05-20T19:06:00Z")
                .build();
        DisputesSearchResults results = service.searchDisputes(account, params);
        assertThat(results.getResults().size(), is(0));
        assertThat(results.getCount(), is(0));
        assertThat(results.getPage(), is(1));
        assertThat(results.getTotal(), is(0));
        assertThat(results.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_date=2021-05-20T19%3A04%3A00Z&to_date=2021-05-20T19%3A06%3A00Z&display_size=500&page=1"));
        assertThat(results.getLinks().getFirstPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_date=2021-05-20T19%3A04%3A00Z&to_date=2021-05-20T19%3A06%3A00Z&display_size=500&page=1"));
        assertThat(results.getLinks().getLastPage().getHref(), is("http://publicapi.test.localhost/v1/disputes?from_date=2021-05-20T19%3A04%3A00Z&to_date=2021-05-20T19%3A06%3A00Z&display_size=500&page=1"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-search-disputes-page-not-found"})
    public void shouldReturn404WhenSearchingWithNonExistentPageNumber() {
        Account account = new Account(accountId, TokenPaymentType.CARD, tokenLink);
        DisputesSearchParams.Builder paramsBuilder = new DisputesSearchParams.Builder();
        DisputesSearchParams params = paramsBuilder
                .withPage("999")
                .build();

        SearchDisputesException searchDisputesException = assertThrows(SearchDisputesException.class,
                () -> service.searchDisputes(account, params));

        assertThat(searchDisputesException, hasProperty("errorStatus", Matchers.is(404)));
    }
}