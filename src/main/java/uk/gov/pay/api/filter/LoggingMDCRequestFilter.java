package uk.gov.pay.api.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.web.router.RouteAttributes;
import io.micronaut.web.router.UriRouteMatch;
import org.slf4j.MDC;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.auth.AccountArgumentBinder;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.pay.api.utils.LoggingKeys.AGREEMENT_EXTERNAL_ID;
import static uk.gov.pay.api.utils.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.pay.api.utils.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.pay.api.utils.LoggingKeys.REFUND_EXTERNAL_ID;
import static uk.gov.pay.api.utils.LoggingKeys.REMOTE_ADDRESS;

/**
 * Puts the account, client address and the ids in the path of each request into the logging context.
 */
@ServerFilter(ServerFilter.MATCH_ALL_PATTERN)
public class LoggingMDCRequestFilter implements Ordered {

    @RequestFilter
    public void filter(HttpRequest<?> request) {
        Optional<Account> mayBeAccount = request.getAttribute(AccountArgumentBinder.ACCOUNT_ATTRIBUTE, Account.class);
        MDC.put(GATEWAY_ACCOUNT_ID, mayBeAccount.map(Account::getName).orElse(EMPTY));
        mayBeAccount.ifPresent(account -> MDC.put("token_link", account.tokenLink()));

        String clientAddress = Optional.ofNullable(request.getHeaders().get("X-Forwarded-For"))
                .map(forwarded -> forwarded.split(",")[0])
                .orElse(null);
        MDC.put(REMOTE_ADDRESS, clientAddress);

        getPathParameterFromRequest("paymentId", request).ifPresent(paymentId -> MDC.put(PAYMENT_EXTERNAL_ID, paymentId));
        getPathParameterFromRequest("refundId", request).ifPresent(refundId -> MDC.put(REFUND_EXTERNAL_ID, refundId));
        getPathParameterFromRequest("agreementId", request).ifPresent(agreementId -> MDC.put(AGREEMENT_EXTERNAL_ID, agreementId));
    }

    private Optional<String> getPathParameterFromRequest(String parameterName, HttpRequest<?> request) {
        return RouteAttributes.getRouteMatch(request)
                .filter(UriRouteMatch.class::isInstance)
                .map(match -> ((UriRouteMatch<?, ?>) match).getVariableValues().get(parameterName))
                .map(Object::toString);
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.SECURITY.after();
    }
}
