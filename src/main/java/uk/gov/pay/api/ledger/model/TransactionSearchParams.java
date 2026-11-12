package uk.gov.pay.api.ledger.model;

import uk.gov.pay.api.utils.QueryParameters;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.pay.api.common.SearchConstants.CARDHOLDER_NAME_KEY;
import static uk.gov.pay.api.common.SearchConstants.CARD_BRAND_KEY;
import static uk.gov.pay.api.common.SearchConstants.DISPLAY_SIZE;
import static uk.gov.pay.api.common.SearchConstants.EMAIL_KEY;
import static uk.gov.pay.api.common.SearchConstants.FIRST_DIGITS_CARD_NUMBER_KEY;
import static uk.gov.pay.api.common.SearchConstants.FROM_DATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.FROM_SETTLED_DATE;
import static uk.gov.pay.api.common.SearchConstants.GATEWAY_ACCOUNT_ID;
import static uk.gov.pay.api.common.SearchConstants.LAST_DIGITS_CARD_NUMBER_KEY;
import static uk.gov.pay.api.common.SearchConstants.PAGE;
import static uk.gov.pay.api.common.SearchConstants.REFERENCE_KEY;
import static uk.gov.pay.api.common.SearchConstants.STATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.TO_DATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.TO_SETTLED_DATE;

public class TransactionSearchParams {

    public static TransactionSearchParams from(QueryParameters query) {
        TransactionSearchParams params = new TransactionSearchParams();
        params.reference = query.getFirst("reference");
        params.email = query.getFirst("email");
        params.state = query.getFirst("state");
        params.cardBrand = query.getFirst("card_brand");
        params.fromDate = query.getFirst("from_date");
        params.toDate = query.getFirst("to_date");
        params.pageNumber = query.getFirst("page");
        params.displaySize = query.getFirst("display_size");
        params.cardHolderName = query.getFirst("cardholder_name");
        params.firstDigitsCardNumber = query.getFirst("first_digits_card_number");
        params.lastDigitsCardNumber = query.getFirst("last_digits_card_number");
        params.fromSettledDate = query.getFirst("from_settled_date");
        params.toSettledDate = query.getFirst("to_settled_date");
        return params;
    }

    private String accountId;
    private String reference;
    private String email;
    private String state;
    private String cardBrand;
    private String fromDate;
    private String toDate;
    private String pageNumber;
    private String displaySize;
    private String cardHolderName;
    private String firstDigitsCardNumber;
    private String lastDigitsCardNumber;
    private String fromSettledDate;
    private String toSettledDate;

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getReference() {
        return reference;
    }

    public String getEmail() {
        return email;
    }

    public String getState() {
        return state;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getFromDate() {
        return fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public String getDisplaySize() {
        return displaySize;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    public String getFromSettledDate() {
        return fromSettledDate;
    }

    public String getToSettledDate() {
        return toSettledDate;
    }

    public Map<String, String> getQueryMap() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(GATEWAY_ACCOUNT_ID, accountId);
        queryParams.put(REFERENCE_KEY, reference);
        queryParams.put(EMAIL_KEY, email);
        queryParams.put(STATE_KEY, state);
        queryParams.put(CARD_BRAND_KEY, cardBrand);
        queryParams.put(CARDHOLDER_NAME_KEY, cardHolderName);
        queryParams.put(FIRST_DIGITS_CARD_NUMBER_KEY, firstDigitsCardNumber);
        queryParams.put(LAST_DIGITS_CARD_NUMBER_KEY, lastDigitsCardNumber);
        queryParams.put(FROM_DATE_KEY, fromDate);
        queryParams.put(TO_DATE_KEY, toDate);
        queryParams.put(PAGE, pageNumber);
        queryParams.put(DISPLAY_SIZE, displaySize);
        queryParams.put(FROM_SETTLED_DATE, fromSettledDate);
        queryParams.put(TO_SETTLED_DATE, toSettledDate);

        return queryParams;
    }
}
