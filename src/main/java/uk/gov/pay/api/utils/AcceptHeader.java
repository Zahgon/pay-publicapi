package uk.gov.pay.api.utils;

import io.micronaut.http.MediaType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The media ranges of a request's {@code Accept} headers with their quality values, most preferred first.
 */
public final class AcceptHeader {

    private final List<Range> ranges;
    private final boolean invalid;

    private AcceptHeader(List<Range> ranges, boolean invalid) {
        this.ranges = ranges;
        this.invalid = invalid;
    }

    public static AcceptHeader parse(List<String> headers) {
        List<Range> ranges = new ArrayList<>();
        boolean invalid = false;
        for (String header : headers) {
            for (String element : header.split(",")) {
                if (element.isBlank()) {
                    continue;
                }
                if (!MediaTypes.isValid(element)) {
                    invalid = true;
                    continue;
                }
                MediaType type = MediaType.of(element.trim());
                double quality = type.getQualityAsNumber().doubleValue();
                ranges.add(new Range(new MediaType(type.getType() + "/" + type.getSubtype()), quality, ranges.size(),
                        parameters(element)));
            }
        }
        ranges.sort(Comparator.comparingDouble(Range::quality).reversed()
                .thenComparing(range -> specificity(range.type()), Comparator.reverseOrder())
                .thenComparingInt(Range::position));
        return new AcceptHeader(ranges, invalid);
    }

    /**
     * Whether a media type the resource produces is acceptable: no {@code Accept} header accepts anything.
     */
    public boolean accepts(MediaType produced) {
        if (ranges.isEmpty() && !invalid) {
            return true;
        }
        return ranges.stream().anyMatch(range -> matches(range.type(), produced));
    }

    public MediaType preferred() {
        return ranges.stream().filter(range -> range.quality() > 0).map(Range::type).findFirst().orElse(null);
    }

    public boolean prefersPlainText() {
        MediaType preferred = preferred();
        return preferred != null && matches(preferred, MediaType.TEXT_PLAIN_TYPE);
    }

    private static boolean matches(MediaType range, MediaType type) {
        boolean typeMatches = range.getType().equals("*") || range.getType().equalsIgnoreCase(type.getType());
        boolean subtypeMatches = range.getSubtype().equals("*") || range.getSubtype().equalsIgnoreCase(type.getSubtype());
        return typeMatches && subtypeMatches;
    }

    private static int specificity(MediaType type) {
        if (type.getType().equals("*")) {
            return 0;
        }
        return type.getSubtype().equals("*") ? 1 : 2;
    }

    /**
     * The media type an answer of the given type is labelled with: the most preferred range naming exactly that type,
     * with its parameters other than the quality; or the type itself.
     */
    public String responseType(MediaType produced) {
        return ranges.stream()
                .filter(range -> range.type().getType().equalsIgnoreCase(produced.getType())
                        && range.type().getSubtype().equalsIgnoreCase(produced.getSubtype()))
                .findFirst()
                .filter(range -> !range.parameters().isEmpty())
                .map(range -> produced.getType() + "/" + produced.getSubtype() + ";" + range.parameters())
                .orElse(produced.getType() + "/" + produced.getSubtype());
    }

    private static String parameters(String element) {
        List<String> parameters = new ArrayList<>();
        String[] parts = element.split(";");
        for (int i = 1; i < parts.length; i++) {
            String parameter = parts[i].trim();
            if (!parameter.isEmpty() && !parameter.toLowerCase(java.util.Locale.ROOT).startsWith("q=")) {
                int equals = parameter.indexOf('=');
                parameters.add(parameter.substring(0, equals).trim() + "=" + parameter.substring(equals + 1).trim());
            }
        }
        return String.join(";", parameters);
    }

    private record Range(MediaType type, double quality, int position, String parameters) {
    }
}
