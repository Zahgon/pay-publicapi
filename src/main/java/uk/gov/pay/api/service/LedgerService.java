package uk.gov.pay.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.agreement.model.AgreementLedgerResponse;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.GetAgreementException;
import uk.gov.pay.api.exception.GetChargeException;
import uk.gov.pay.api.exception.GetEventsException;
import uk.gov.pay.api.exception.GetRefundsException;
import uk.gov.pay.api.exception.GetTransactionException;
import uk.gov.pay.api.exception.SearchAgreementsException;
import uk.gov.pay.api.exception.SearchDisputesException;
import uk.gov.pay.api.exception.SearchPaymentsException;
import uk.gov.pay.api.exception.SearchRefundsException;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.Charge;
import uk.gov.pay.api.model.TransactionEvents;
import uk.gov.pay.api.model.TransactionResponse;
import uk.gov.pay.api.model.ledger.RefundTransactionFromLedger;
import uk.gov.pay.api.model.ledger.RefundsFromLedger;
import uk.gov.pay.api.model.ledger.SearchDisputesResponseFromLedger;
import uk.gov.pay.api.model.ledger.SearchRefundsResponseFromLedger;
import uk.gov.pay.api.model.search.card.PaymentSearchResponse;
import uk.gov.pay.api.validation.AgreementSearchValidator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static uk.gov.pay.api.common.SearchConstants.GATEWAY_ACCOUNT_ID;

@Singleton
public class LedgerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LedgerService.class);

    private static final String PARAM_ACCOUNT_ID = "account_id";
    private static final String PARAM_TRANSACTION_TYPE = "transaction_type";
    public static final String PARAM_EXACT_REFERENCE_MATCH = "exact_reference_match";
    private static final String PAYMENT_TRANSACTION_TYPE = "PAYMENT";
    private static final String REFUND_TRANSACTION_TYPE = "REFUND";
    private static final String DISPUTE_TRANSACTION_TYPE = "DISPUTE";

    private final RestClient client;
    private final LedgerUriGenerator ledgerUriGenerator;

    @Inject
    public LedgerService(RestClient client, LedgerUriGenerator ledgerUriGenerator) {
        this.client = client;
        this.ledgerUriGenerator = ledgerUriGenerator;
    }

    public Charge getPaymentTransaction(Account account, String paymentId) {
        RestClientResponse response = client.get(ledgerUriGenerator.transactionURI(account, paymentId, PAYMENT_TRANSACTION_TYPE), Map.of());

        if (response.getStatus() == SC_OK) {
            TransactionResponse transactionResponse = response.readEntity(TransactionResponse.class);
            return Charge.from(transactionResponse);
        }

        throw new GetChargeException(response);
    }

    public RefundTransactionFromLedger getRefundTransaction(Account account, String transactionId, String parentExternalId) {
        RestClientResponse response = client.get(ledgerUriGenerator.transactionURI(account, transactionId, REFUND_TRANSACTION_TYPE, parentExternalId), Map.of());

        if (response.getStatus() == SC_OK) {
            return response.readEntity(RefundTransactionFromLedger.class);
        }

        throw new GetTransactionException(response);
    }

    public TransactionEvents getTransactionEvents(Account account, String paymentId) {
        RestClientResponse response = client.get(ledgerUriGenerator.transactionEventsURI(account, paymentId), Map.of());

        if (response.getStatus() == SC_OK) {
            return response.readEntity(TransactionEvents.class);
        }
        throw new GetEventsException(response);
    }

    public RefundsFromLedger getPaymentRefunds(String accountId, String paymentId) {
        RestClientResponse response = client.get(ledgerUriGenerator.transactionsForTransactionURI(accountId, paymentId, REFUND_TRANSACTION_TYPE), Map.of());

        if (response.getStatus() == SC_OK) {
            return response.readEntity(RefundsFromLedger.class);
        }

        throw new GetRefundsException(response);
    }

    public SearchRefundsResponseFromLedger searchRefunds(Account account, Map<String, String> paramsAsMap) {

        paramsAsMap.put(PARAM_ACCOUNT_ID, account.accountId());
        paramsAsMap.put(PARAM_TRANSACTION_TYPE, REFUND_TRANSACTION_TYPE);

        RestClientResponse response = client.get(ledgerUriGenerator.transactionsURIWithParams(paramsAsMap), Map.of("Accept", "application/json"));

        if (response.getStatus() == SC_OK) {
            try {
                return response.readEntity(SearchRefundsResponseFromLedger.class);
            } catch (RestClientResponse.RestClientProcessingException exception) {
                throw new SearchRefundsException(exception);
            }
        }

        throw new SearchRefundsException(response);
    }

    public SearchDisputesResponseFromLedger searchDisputes(Account account, Map<String, String> paramsAsMap) {
        paramsAsMap.put(PARAM_ACCOUNT_ID, account.accountId());
        paramsAsMap.put(PARAM_TRANSACTION_TYPE, DISPUTE_TRANSACTION_TYPE);

        if (paramsAsMap.containsKey("status")) {
            paramsAsMap.put("state", paramsAsMap.remove("status"));
        }

        RestClientResponse response = client.get(ledgerUriGenerator.transactionsURIWithParams(paramsAsMap), Map.of("Accept", "application/json"));

        if (response.getStatus() == SC_OK) {
            try {
                return response.readEntity(SearchDisputesResponseFromLedger.class);
            } catch (RestClientResponse.RestClientProcessingException exception) {
                throw new SearchDisputesException(exception);
            }
        }

        throw new SearchDisputesException(response);
    }

    public PaymentSearchResponse<TransactionResponse> searchPayments(Account account, Map<String, String> paramsAsMap) {

        paramsAsMap.put(PARAM_ACCOUNT_ID, account.accountId());
        paramsAsMap.put(PARAM_TRANSACTION_TYPE, PAYMENT_TRANSACTION_TYPE);
        paramsAsMap.put(PARAM_EXACT_REFERENCE_MATCH, "true");

        RestClientResponse response = client.get(ledgerUriGenerator.transactionsURIWithParams(paramsAsMap), Map.of("Accept", "application/json"));

        if (response.getStatus() == SC_OK) {
            try {
                return response.readEntity(new TypeReference<>() {
                });
            } catch (RestClientResponse.RestClientProcessingException ex) {
                throw new SearchPaymentsException(ex);
            }
        }

        throw new SearchPaymentsException(response);
    }

    public AgreementLedgerResponse getAgreement(Account account, String agreementId) {
        RestClientResponse response = client.get(ledgerUriGenerator.agreementURI(account, agreementId), Map.of("X-Consistent", "true"));

        if (response.getStatus() == SC_OK) {
            return response.readEntity(AgreementLedgerResponse.class);
        }

        throw new GetAgreementException(response);
    }

    public SearchResults<AgreementLedgerResponse> searchAgreements(Account account, AgreementSearchParams searchParams) {
        AgreementSearchValidator.validateSearchParameters(searchParams);

        var params = new HashMap<>(searchParams.getQueryMap());
        params.put(GATEWAY_ACCOUNT_ID, account.accountId());
        params.put(PARAM_EXACT_REFERENCE_MATCH, "true");

        String url = ledgerUriGenerator.agreementsURIWithParams(params);
        RestClientResponse ledgerResponse = client.get(url, Map.of("Accept", "application/json"));
        LOGGER.info("response from ledger for agreement search: {}", ledgerResponse);

        if (ledgerResponse.getStatus() == SC_OK) {
            try {
                return ledgerResponse.readEntity(new TypeReference<>() {
                });
            } catch (RestClientResponse.RestClientProcessingException ex) {
                throw new SearchAgreementsException(ex);
            }
        }
        throw new SearchAgreementsException(ledgerResponse);
    }
}
