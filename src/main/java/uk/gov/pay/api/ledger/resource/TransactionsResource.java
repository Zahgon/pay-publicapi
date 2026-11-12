package uk.gov.pay.api.ledger.resource;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.model.TransactionSearchParams;
import uk.gov.pay.api.ledger.service.TransactionSearchService;
import uk.gov.pay.api.model.search.card.PaymentForSearchResult;
import uk.gov.pay.api.utils.QueryParameters;

import jakarta.inject.Inject;

@Controller
@Produces(MediaType.APPLICATION_JSON)
public class TransactionsResource {

    private final TransactionSearchService transactionSearchService;

    @Inject
    public TransactionsResource(TransactionSearchService transactionSearchService) {
        this.transactionSearchService = transactionSearchService;
    }

    @Get("/v1/transactions")
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResults<PaymentForSearchResult> getTransactions(Account account, HttpRequest<?> request) {
        TransactionSearchParams searchParams = TransactionSearchParams.from(QueryParameters.of(request));
        return transactionSearchService.doSearch(account, searchParams);
    }
}
