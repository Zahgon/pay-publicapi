package uk.gov.pay.api.utils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the URIs of the API's links and of the requests sent to connector and ledger, encoding them exactly as the
 * service always has:
 * <ul>
 * <li>a path keeps valid percent escapes and the characters a path allows, and encodes the rest as UTF-8;</li>
 * <li>a query parameter added by name keeps only unreserved characters and valid escapes, and writes a space as
 * {@code +};</li>
 * <li>a whole query keeps the characters a query allows and writes a space as {@code %20};</li>
 * <li>text in braces is a template variable: {@link #build(Object...)} fills it in order, {@link #toString()} leaves
 * it, and an unclosed brace is refused.</li>
 * </ul>
 * Once a query parameter has been replaced, the query is held as a map of parameter names to values and written in
 * the map's order.
 */
public final class UriTemplate {

    private enum Component {
        PATH, PATH_SEGMENT, QUERY, QUERY_PARAM
    }

    private static final String UNRESERVED = "-._~";
    private static final String SUB_DELIMS = "!$&'()*+,;=";

    private final String prefix;
    private final StringBuilder path;
    private final StringBuilder query = new StringBuilder();
    private Map<String, List<String>> queryParams;

    private UriTemplate(String prefix, String path) {
        this.prefix = prefix;
        this.path = new StringBuilder(path);
    }

    /**
     * A template from an absolute or relative URI: scheme and authority are kept as they are.
     */
    public static UriTemplate fromUri(String uri) {
        String withoutFragment = uri;
        int hash = withoutFragment.indexOf('#');
        if (hash >= 0) {
            withoutFragment = withoutFragment.substring(0, hash);
        }
        String rawQuery = null;
        int questionMark = withoutFragment.indexOf('?');
        if (questionMark >= 0) {
            rawQuery = withoutFragment.substring(questionMark + 1);
            withoutFragment = withoutFragment.substring(0, questionMark);
        }
        String prefix = "";
        String rawPath = withoutFragment;
        int schemeEnd = withoutFragment.indexOf("://");
        if (schemeEnd > 0) {
            int pathStart = withoutFragment.indexOf('/', schemeEnd + 3);
            prefix = pathStart < 0 ? withoutFragment : withoutFragment.substring(0, pathStart);
            rawPath = pathStart < 0 ? "" : withoutFragment.substring(pathStart);
        }
        UriTemplate template = new UriTemplate(prefix, encode(rawPath, Component.PATH, true));
        if (rawQuery != null) {
            template.query.append(encode(rawQuery, Component.QUERY, true));
        }
        return template;
    }

    /**
     * A template whose path is the given text, scheme and authority included.
     */
    public static UriTemplate fromPath(String path) {
        return new UriTemplate("", encode(path, Component.PATH, true));
    }

    public UriTemplate path(String segment) {
        String encoded = encode(segment, Component.PATH, true);
        if (encoded.isEmpty()) {
            return this;
        }
        boolean pathEndsWithSlash = !path.isEmpty() && path.charAt(path.length() - 1) == '/';
        boolean segmentStartsWithSlash = encoded.charAt(0) == '/';
        if (pathEndsWithSlash && segmentStartsWithSlash) {
            path.append(encoded, 1, encoded.length());
        } else if (!path.isEmpty() && !pathEndsWithSlash && !segmentStartsWithSlash) {
            path.append('/').append(encoded);
        } else {
            path.append(encoded);
        }
        return this;
    }

    public UriTemplate queryParam(String name, Object value) {
        String encodedName = encode(name, Component.QUERY_PARAM, true);
        String encodedValue = encode(String.valueOf(value), Component.QUERY_PARAM, true);
        if (queryParams != null) {
            add(queryParams, encodedName, encodedValue);
        } else {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(encodedName).append('=').append(encodedValue);
        }
        return this;
    }

    /**
     * Replaces the whole query, given unencoded or partly encoded; null removes it.
     */
    public UriTemplate replaceQuery(String newQuery) {
        query.setLength(0);
        queryParams = null;
        if (newQuery != null) {
            query.append(encode(newQuery, Component.QUERY, true));
        }
        return this;
    }

    /**
     * Removes every value of a query parameter.
     */
    public UriTemplate removeQueryParam(String name) {
        if (queryParams == null) {
            queryParams = parseQuery(query.toString());
            query.setLength(0);
        }
        queryParams.remove(encode(name, Component.QUERY_PARAM, true));
        return this;
    }

    /**
     * The URI with its template variables filled with the given values, in order of appearance; the values are
     * encoded in full.
     */
    public URI build(Object... values) {
        return URI.create(resolve(values));
    }

    @Override
    public String toString() {
        return prefix + path + queryString();
    }

    private String queryString() {
        StringBuilder result = new StringBuilder();
        if (queryParams != null) {
            for (Map.Entry<String, List<String>> parameter : queryParams.entrySet()) {
                for (String value : parameter.getValue()) {
                    result.append(result.isEmpty() ? "" : "&").append(parameter.getKey()).append('=').append(value);
                }
            }
        } else {
            result.append(query);
        }
        return result.isEmpty() ? "" : "?" + result;
    }

    private String resolve(Object... values) {
        String encodedPath = fill(path.toString(), Component.PATH_SEGMENT, values, 0);
        int used = countTemplates(path.toString());
        String encodedQuery = fill(queryString(), Component.QUERY_PARAM, values, used);
        return prefix + encodedPath + encodedQuery;
    }

    private static String fill(String text, Component component, Object[] values, int offset) {
        StringBuilder result = new StringBuilder();
        Map<String, String> filled = new HashMap<>();
        int next = offset;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '{') {
                int end = text.indexOf('}', i);
                String name = text.substring(i + 1, end).split(":", 2)[0].trim();
                String value = filled.get(name);
                if (value == null) {
                    if (next >= values.length || values[next] == null) {
                        throw new IllegalArgumentException("The template variable '" + name + "' has no value");
                    }
                    value = encode(String.valueOf(values[next++]), component, false);
                    filled.put(name, value);
                }
                result.append(value);
                i = end + 1;
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private static int countTemplates(String text) {
        return (int) text.chars().filter(c -> c == '{').count();
    }

    private static Map<String, List<String>> parseQuery(String rawQuery) {
        Map<String, List<String>> parameters = new HashMap<>();
        if (rawQuery.isEmpty()) {
            return parameters;
        }
        for (String parameter : rawQuery.split("&")) {
            int equals = parameter.indexOf('=');
            if (equals > 0) {
                add(parameters, parameter.substring(0, equals), parameter.substring(equals + 1));
            } else if (equals < 0 && !parameter.isEmpty()) {
                add(parameters, parameter, "");
            }
        }
        return parameters;
    }

    /**
     * Adds a value with get and put rather than computeIfAbsent: the two place colliding keys differently in a
     * HashMap, and the query is written in the map's order.
     */
    private static void add(Map<String, List<String>> parameters, String name, String value) {
        List<String> values = parameters.get(name);
        if (values == null) {
            values = new ArrayList<>();
            parameters.put(name, values);
        }
        values.add(value);
    }

    private static boolean allowed(int c, Component component) {
        if (c >= 0x80) {
            return false;
        }
        if (Character.isLetterOrDigit(c) || UNRESERVED.indexOf(c) >= 0) {
            return true;
        }
        return switch (component) {
            case PATH -> SUB_DELIMS.indexOf(c) >= 0 || c == ':' || c == '@' || c == '/';
            case PATH_SEGMENT -> (SUB_DELIMS.indexOf(c) >= 0 && c != ';') || c == ':' || c == '@';
            case QUERY -> SUB_DELIMS.indexOf(c) >= 0 || c == '/' || c == '?';
            case QUERY_PARAM -> false;
        };
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Encodes text for a component. Contextually (templates kept, valid escapes kept) for text that becomes part of
     * the template; in full for the values that fill template variables.
     */
    private static String encode(String text, Component component, boolean contextual) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int c = text.codePointAt(i);
            if (contextual && c == '{') {
                int end = text.indexOf('}', i);
                if (end < 0) {
                    throw new IllegalArgumentException("Illegal template variable in " + text);
                }
                result.append(text, i, end + 1);
                i = end + 1;
                continue;
            }
            if (contextual && c == '}') {
                throw new IllegalArgumentException("Illegal template variable end in " + text);
            }
            if (c != '%' && allowed(c, component)) {
                result.append((char) c);
            } else if (contextual && c == '%' && i + 2 < text.length() + 0 && i + 2 <= text.length() - 1
                    && isHex(text.charAt(i + 1)) && isHex(text.charAt(i + 2))) {
                result.append(text, i, i + 3);
                i += 3;
                continue;
            } else if (c == ' ' && component == Component.QUERY_PARAM) {
                result.append('+');
            } else {
                for (byte b : new String(Character.toChars(c)).getBytes(StandardCharsets.UTF_8)) {
                    result.append('%').append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)))
                            .append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
                }
            }
            i += Character.charCount(c);
        }
        return result.toString();
    }
}
