package uk.gov.pay.api.filter;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.PublicApiFactory;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.auth.AccountArgumentBinder;
import uk.gov.pay.api.filter.ratelimit.RateLimitException;
import uk.gov.pay.api.filter.ratelimit.RateLimiter;
import uk.gov.pay.api.model.TokenPaymentType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RateLimiterFilterTest {

    public static final String ACCOUNT_ID = "account-id";
    private RateLimiterFilter rateLimiterFilter;
    private RateLimiter rateLimiter;

    @BeforeEach
    public void setup() {
        rateLimiter = mock(RateLimiter.class);
        rateLimiterFilter = new RateLimiterFilter(rateLimiter);
    }

    @Test
    public void shouldCheckRateLimitsWhenFilterIsInvoked() throws Exception {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "some-token-link");
        MutableHttpRequest<?> request = HttpRequest.POST("", null);
        request.setAttribute(AccountArgumentBinder.ACCOUNT_ATTRIBUTE, account);

        assertNull(rateLimiterFilter.filter(request));

        verify(rateLimiter).checkRateOf(eq(ACCOUNT_ID), any());
    }

    @Test
    public void shouldSendErrorResponse_whenRateLimitExceeded() throws Exception {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "some-token-link");
        MutableHttpRequest<?> request = HttpRequest.GET("");
        request.setAttribute(AccountArgumentBinder.ACCOUNT_ATTRIBUTE, account);
        doThrow(RateLimitException.class).when(rateLimiter).checkRateOf(eq("account-id"), any());

        HttpResponse<?> response = rateLimiterFilter.filter(request);

        assertNotNull(response);
        assertEquals(429, response.code());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("utf-8", response.getHeaders().get("Content-Encoding"));
        assertEquals("{\"code\":\"P0900\",\"description\":\"Too many requests\"}",
                new PublicApiFactory().objectMapper().writeValueAsString(response.body()));
    }

    @Test
    public void shouldNotCheckRateLimit_on_healthcheck() throws Exception {
        assertNull(rateLimiterFilter.filter(HttpRequest.GET("/healthcheck")));
        verify(rateLimiter, never()).checkRateOf(anyString(), any());
    }
}
