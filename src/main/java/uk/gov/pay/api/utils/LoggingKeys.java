package uk.gov.pay.api.utils;

/**
 * The structured logging keys the service writes, as named across GOV.UK Pay.
 */
public final class LoggingKeys {

    public static final String GATEWAY_ACCOUNT_ID = "gateway_account_id";
    public static final String REMOTE_ADDRESS = "remote_address";
    public static final String PAYMENT_EXTERNAL_ID = "payment_external_id";
    public static final String REFUND_EXTERNAL_ID = "refund_external_id";
    public static final String AGREEMENT_EXTERNAL_ID = "agreement_external_id";

    private LoggingKeys() {
    }
}
