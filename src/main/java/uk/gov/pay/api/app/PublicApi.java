package uk.gov.pay.api.app;

import io.micronaut.runtime.Micronaut;

import java.util.Arrays;
import javax.net.ssl.HttpsURLConnection;

public class PublicApi {

    /**
     * The service used to be started as {@code java -jar <jar> server <config.yaml>}. Micronaut reads its configuration
     * from {@code application.yml} and the environment, so the {@code server} command and a trailing configuration file
     * argument are accepted and ignored.
     */
    public static void main(String[] args) {
        initialiseSSLSocketFactory();
        Micronaut.build(withoutLegacyArguments(args))
                .mainClass(PublicApi.class)
                .banner(false)
                .start();
    }

    static String[] withoutLegacyArguments(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("-"))
                .toArray(String[]::new);
    }

    /**
     * Initialise the default SSL socket factory at startup (kept from the original start-up, JERSEY-3124).
     */
    private static void initialiseSSLSocketFactory() {
        HttpsURLConnection.getDefaultSSLSocketFactory();
    }
}
