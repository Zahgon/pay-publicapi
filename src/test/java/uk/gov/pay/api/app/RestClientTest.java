package uk.gov.pay.api.app;

import org.junit.jupiter.api.Test;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;

import java.lang.reflect.Field;
import java.util.List;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The TLS selection of the backend {@link RestClient} (formerly tested on {@code RestClientFactory.buildClient}):
 * TLSv1.2 when secure internal communication is on, the JVM default when it is off.
 */
public class RestClientTest {

    @Test
    public void restClient_shouldUseSSLWhenSecureInternalCommunicationIsOn() throws Exception {
        //given
        RestClientConfig clientConfiguration = mock(RestClientConfig.class);
        when(clientConfiguration.isDisabledSecureConnection()).thenReturn(false);

        //when
        RestClient client = new RestClient(configurationWith(clientConfiguration));

        //then
        SSLSocketFactory socketFactory = sslSocketFactoryOf(client);
        try (SSLSocket socket = (SSLSocket) socketFactory.createSocket()) {
            List<String> protocols = List.of(socket.getEnabledProtocols());
            assertThat(protocols, hasItem("TLSv1.2"));
            assertThat(protocols, not(hasItem("TLSv1.3")));
        }
    }

    @Test
    public void restClient_shouldNotUseSSLWhenSecureInternalCommunicationIsOff() throws Exception {
        //given
        RestClientConfig clientConfiguration = mock(RestClientConfig.class);
        when(clientConfiguration.isDisabledSecureConnection()).thenReturn(true);

        //when
        RestClient client = new RestClient(configurationWith(clientConfiguration));

        //then: no TLSv1.2 context of its own, so connections use the JVM default
        assertThat(sslSocketFactoryOf(client), is(nullValue()));
    }

    private static PublicApiConfig configurationWith(RestClientConfig restClientConfig) {
        PublicApiConfig configuration = new PublicApiConfig();
        configuration.setRestClientConfig(restClientConfig);
        return configuration;
    }

    private static SSLSocketFactory sslSocketFactoryOf(RestClient client) throws ReflectiveOperationException {
        Field field = RestClient.class.getDeclaredField("sslSocketFactory");
        field.setAccessible(true);
        return (SSLSocketFactory) field.get(client);
    }
}
