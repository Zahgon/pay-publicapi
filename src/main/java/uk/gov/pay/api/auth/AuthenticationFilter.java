package uk.gov.pay.api.auth;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.web.router.MethodBasedRouteMatch;
import io.micronaut.web.router.RouteAttributes;
import uk.gov.pay.api.utils.QueryParameters;

import java.util.Arrays;
import java.util.Optional;

/**
 * Authenticates the bearer API key of every request to a resource that acts for an account, answering 401 when the
 * request carries no key public auth accepts.
 */
@ServerFilter(ServerFilter.MATCH_ALL_PATTERN)
public class AuthenticationFilter implements Ordered {

    private static final String PREFIX = "Bearer";
    private static final String ACCESS_TOKEN_PARAMETER = "access_token";
    private static final String UNAUTHORIZED_ENTITY = "Credentials are required to access this resource.";

    private final CachingAccountAuthenticator authenticator;

    public AuthenticationFilter(CachingAccountAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @RequestFilter
    @Nullable
    public HttpResponse<?> authenticate(HttpRequest<?> request) {
        if (!actsForAnAccount(request)) {
            return null;
        }
        String credentials = credentials(request.getHeaders().get("Authorization"));
        if (credentials == null) {
            credentials = QueryParameters.of(request).getFirst(ACCESS_TOKEN_PARAMETER);
        }
        Optional<Account> account = credentials == null ? Optional.empty() : authenticator.authenticate(credentials);
        if (account.isEmpty()) {
            return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", PREFIX + " realm=\"realm\"")
                    .contentType(MediaType.TEXT_PLAIN_TYPE)
                    .body(UNAUTHORIZED_ENTITY);
        }
        request.setAttribute(AccountArgumentBinder.ACCOUNT_ATTRIBUTE, account.get());
        return null;
    }

    /**
     * The credentials of an {@code Authorization} header of the form {@code Bearer <credentials>}, the scheme in any
     * case.
     */
    @Nullable
    static String credentials(@Nullable String header) {
        if (header == null) {
            return null;
        }
        int space = header.indexOf(' ');
        if (space <= 0) {
            return null;
        }
        String method = header.substring(0, space);
        if (!PREFIX.equalsIgnoreCase(method)) {
            return null;
        }
        return header.substring(space + 1);
    }

    static boolean actsForAnAccount(HttpRequest<?> request) {
        return RouteAttributes.getRouteMatch(request)
                .filter(MethodBasedRouteMatch.class::isInstance)
                .map(match -> ((MethodBasedRouteMatch<?, ?>) match).getArguments())
                .map(arguments -> Arrays.stream(arguments).anyMatch(argument -> argument.getType() == Account.class))
                .orElse(false);
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.SECURITY.order();
    }
}
