package uk.gov.pay.api.filter;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.auth.AccountArgumentBinder;
import uk.gov.pay.api.filter.ratelimit.RateLimitException;
import uk.gov.pay.api.filter.ratelimit.RateLimiter;
import uk.gov.pay.api.resources.error.ApiErrorResponse;

import static uk.gov.pay.api.resources.error.ApiErrorResponse.anApiErrorResponse;

/**
 * Allow only a certain number of requests from the same source (the authenticated account) within the time configured
 * in the {@link RateLimiter}.
 * <p>
 * 429 Too Many Requests will be returned when rate limit is reached.
 */
@ServerFilter(ServerFilter.MATCH_ALL_PATTERN)
public class RateLimiterFilter implements Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterFilter.class);
    private static final int TOO_MANY_REQUESTS_STATUS_CODE = 429;
    private static final String UTF8_CHARACTER_ENCODING = "utf-8";

    private final RateLimiter rateLimiter;

    /**
     * @param rateLimiter Limiter in number of requests per given time coming from the same source (Authorization)
     */
    public RateLimiterFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @RequestFilter
    @Nullable
    public HttpResponse<?> filter(HttpRequest<?> request) {
        if ("/healthcheck".equals(request.getPath())) {
            return null;
        }

        String accountId = request.getAttribute(AccountArgumentBinder.ACCOUNT_ATTRIBUTE, Account.class)
                .map(Account::accountId)
                .orElse("");
        RateLimiterKey key = RateLimiterKey.from(request, accountId);
        try {
            rateLimiter.checkRateOf(accountId, key);
        } catch (RateLimitException e) {
            LOGGER.info("Rate limit reached for current service [account - {}, method - {}]. Sending response '429 Too Many Requests'",
                    accountId, key.getMethod());
            return HttpResponse.status(io.micronaut.http.HttpStatus.valueOf(TOO_MANY_REQUESTS_STATUS_CODE))
                    .contentType(MediaType.APPLICATION_JSON_TYPE)
                    .header("Content-Encoding", UTF8_CHARACTER_ENCODING)
                    .body(anApiErrorResponse(ApiErrorResponse.Code.TOO_MANY_REQUESTS_ERROR));
        }
        return null;
    }

    /**
     * After authentication and after the logging context is filled in.
     */
    @Override
    public int getOrder() {
        return ServerFilterPhase.SECURITY.after() + 10;
    }
}
