package uk.gov.pay.api.filter;

import io.micronaut.http.HttpRequest;
import uk.gov.pay.api.utils.PathHelper;

public class RateLimiterKey {

    private final String method;
    private final String key;
    private final String keyType;

    private RateLimiterKey(String key, String keyType, String method) {
        this.key = key;
        this.keyType = keyType;
        this.method = method;
    }

    public static RateLimiterKey from(HttpRequest<?> request, String accountId) {
        return from(request.getMethodName(), request.getPath(), accountId);
    }

    public static RateLimiterKey from(String method, String path, String accountId) {
        StringBuilder builder = new StringBuilder(method);

        final String pathType = PathHelper.getPathType(path, method);
        if (!pathType.isBlank()) {
            builder.append("-").append(pathType);
        }

        final String keyType = builder.toString();
        builder.append("-").append(accountId);

        return new RateLimiterKey(builder.toString(), keyType, method);
    }

    public String getKey() {
        return key;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getMethod() {
        return method;
    }
}
