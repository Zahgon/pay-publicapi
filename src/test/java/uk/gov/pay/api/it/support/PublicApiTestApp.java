package uk.gov.pay.api.it.support;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.runtime.server.EmbeddedServer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import uk.gov.pay.api.app.config.PublicApiConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the real Micronaut application in-process on a random port (and a random admin port), configured from a
 * test configuration file under {@code src/test/resources/config} plus overrides. Replaces Dropwizard's
 * {@code DropwizardTestSupport}/{@code DropwizardAppExtension}: {@link #before()} starts it and {@link #after()}
 * stops it. Registered as a static JUnit 5 extension it runs for all the tests of a class.
 * <p>
 * Unless an override names {@code redis.endpoint}, Redis is pointed at an unused local port, so the service falls
 * back to its local rate limiter.
 */
public class PublicApiTestApp implements BeforeAllCallback, AfterAllCallback {

    public static final String DEFAULT_CONFIG = "config/test-config.yaml";

    private final Map<String, Object> properties;
    private ApplicationContext applicationContext;
    private EmbeddedServer server;

    public PublicApiTestApp(Map<String, ?> overrides) {
        this(DEFAULT_CONFIG, overrides);
    }

    public PublicApiTestApp(String configResource, Map<String, ?> overrides) {
        properties = new LinkedHashMap<>(readConfig(configResource));
        properties.put("redis.endpoint", "localhost:" + unusedPort());
        properties.putAll(overrides);
    }

    /**
     * @param keyValues pairs of a property name and its value
     */
    public static PublicApiTestApp withConfig(String configResource, Object... keyValues) {
        return new PublicApiTestApp(configResource, toMap(keyValues));
    }

    public static PublicApiTestApp withOverrides(Object... keyValues) {
        return new PublicApiTestApp(DEFAULT_CONFIG, toMap(keyValues));
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        before();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        after();
    }

    public void before() {
        applicationContext = ApplicationContext.builder()
                .deduceEnvironment(false)
                .environments("test")
                .properties(properties)
                .start();
        server = applicationContext.getBean(EmbeddedServer.class);
        server.start();
    }

    public void after() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
        if (applicationContext != null && applicationContext.isRunning()) {
            applicationContext.stop();
        }
        server = null;
        applicationContext = null;
    }

    public int getLocalPort() {
        return server.getPort();
    }

    public int getAdminPort() {
        Server jetty = applicationContext.getBean(Server.class);
        for (Connector connector : jetty.getConnectors()) {
            if ("admin".equals(connector.getName()) && connector instanceof ServerConnector serverConnector) {
                return serverConnector.getLocalPort();
            }
        }
        throw new IllegalStateException("The application has no admin connector");
    }

    public PublicApiConfig getConfiguration() {
        return applicationContext.getBean(PublicApiConfig.class);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static int unusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, Object> readConfig(String configResource) {
        try (InputStream in = PublicApiTestApp.class.getClassLoader().getResourceAsStream(configResource)) {
            if (in == null) {
                throw new IllegalArgumentException("No test configuration " + configResource);
            }
            return new YamlPropertySourceLoader().read(configResource, in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, Object> toMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Overrides must be pairs of a property name and a value");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
