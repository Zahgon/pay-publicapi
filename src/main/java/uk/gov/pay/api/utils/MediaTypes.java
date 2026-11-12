package uk.gov.pay.api.utils;

import java.util.regex.Pattern;

/**
 * Syntax of a media type in a request header: {@code type/subtype} followed by {@code ;name=value} parameters, the
 * value a token or a quoted string.
 */
public final class MediaTypes {

    private static final String TOKEN = "[!#$%&'*+\\-.^_`|~0-9A-Za-z]+";
    private static final String QUOTED = "\"(?:[^\"\\\\]|\\\\.)*\"";
    private static final Pattern MEDIA_TYPE = Pattern.compile(
            "\\s*" + TOKEN + "\\s*/\\s*" + TOKEN + "\\s*(?:;\\s*" + TOKEN + "\\s*=\\s*(?:" + TOKEN + "|" + QUOTED + ")\\s*)*;?\\s*");

    private MediaTypes() {
    }

    public static boolean isValid(String mediaType) {
        return MEDIA_TYPE.matcher(mediaType).matches();
    }
}
