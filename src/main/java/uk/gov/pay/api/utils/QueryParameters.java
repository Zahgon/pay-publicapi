package uk.gov.pay.api.utils;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.servlet.http.ServletHttpRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The query parameters of an API request, decoded as the API always has: {@code +} is a space, escapes are read as
 * UTF-8 (malformed sequences become U+FFFD), an escape that is not two hex digits is kept as written, a pair without
 * a name is ignored and a name without {@code =} has an empty value. A parameter given more than once takes its
 * first value.
 */
public final class QueryParameters {

    /**
     * The request attribute holding the request's raw query string, as it arrived.
     */
    public static final String RAW_QUERY_ATTRIBUTE = "uk.gov.pay.api.rawQuery";

    private final Map<String, List<String>> parameters;

    private QueryParameters(Map<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    public static QueryParameters of(HttpRequest<?> request) {
        if (request instanceof ServletHttpRequest<?, ?> servletRequest
                && servletRequest.getNativeRequest() instanceof HttpServletRequest nativeRequest
                && nativeRequest.getAttribute(RAW_QUERY_ATTRIBUTE) instanceof String rawQuery) {
            return parse(rawQuery);
        }
        return parse(request.getUri().getRawQuery());
    }

    public static QueryParameters parse(@Nullable String rawQuery) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            for (String parameter : rawQuery.split("&")) {
                int equals = parameter.indexOf('=');
                if (equals > 0) {
                    parameters.computeIfAbsent(decode(parameter.substring(0, equals)), key -> new ArrayList<>())
                            .add(decode(parameter.substring(equals + 1)));
                } else if (equals < 0 && !parameter.isEmpty()) {
                    parameters.computeIfAbsent(decode(parameter), key -> new ArrayList<>()).add("");
                }
            }
        }
        return new QueryParameters(parameters);
    }

    @Nullable
    public String getFirst(String name) {
        List<String> values = parameters.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    static String decode(String text) {
        StringBuilder result = new StringBuilder();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '%' && i + 2 < text.length() + 0 + 1 && i + 2 <= text.length() - 1
                    && Character.digit(text.charAt(i + 1), 16) >= 0 && Character.digit(text.charAt(i + 2), 16) >= 0) {
                bytes.write(Character.digit(text.charAt(i + 1), 16) * 16 + Character.digit(text.charAt(i + 2), 16));
                i += 3;
                continue;
            }
            flush(bytes, result);
            result.append(c == '+' ? ' ' : c);
            i++;
        }
        flush(bytes, result);
        return result.toString();
    }

    private static void flush(ByteArrayOutputStream bytes, StringBuilder result) {
        if (bytes.size() > 0) {
            result.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            bytes.reset();
        }
    }
}
