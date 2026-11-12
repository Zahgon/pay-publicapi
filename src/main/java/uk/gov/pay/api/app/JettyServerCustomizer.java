package uk.gov.pay.api.app;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.Callback;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.healthcheck.HealthCheck;

import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shapes the embedded Jetty server as the service has always run: no {@code Server} header, gzip compression of
 * answers of at least 256 bytes for clients that accept it, gzip-compressed request entities inflated, and an admin
 * port ({@code ADMIN_PORT}, bound like the application port) answering {@code /ping} and {@code /healthcheck}.
 */
@Singleton
public class JettyServerCustomizer implements BeanCreatedEventListener<Server> {

    private static final String APPLICATION_CONNECTOR = "application";
    private static final String ADMIN_CONNECTOR = "admin";

    private final PublicApiConfig configuration;
    private final List<HealthCheck> healthChecks;

    public JettyServerCustomizer(PublicApiConfig configuration, List<HealthCheck> healthChecks) {
        this.configuration = configuration;
        this.healthChecks = healthChecks;
    }

    @Override
    public Server onCreated(BeanCreatedEvent<Server> event) {
        Server server = event.getBean();
        String host = null;
        for (Connector connector : server.getConnectors()) {
            HttpConnectionFactory http = connector.getConnectionFactory(HttpConnectionFactory.class);
            if (http != null) {
                http.getHttpConfiguration().setSendServerVersion(false);
                http.getHttpConfiguration().setSendXPoweredBy(false);
            }
            if (connector instanceof ServerConnector serverConnector) {
                serverConnector.setName(APPLICATION_CONNECTOR);
                host = serverConnector.getHost();
            }
        }

        GzipHandler gzip = new GzipHandler();
        gzip.setMinGzipSize(256);
        gzip.setInflateBufferSize(8192);
        gzip.setIncludedMethods("GET", "POST");
        gzip.setHandler(server.getHandler());

        ContextHandler application = new ContextHandler(gzip, "/");
        application.setVirtualHosts(List.of("@" + APPLICATION_CONNECTOR));
        ContextHandler admin = new ContextHandler(new AdminHandler(), "/");
        admin.setVirtualHosts(List.of("@" + ADMIN_CONNECTOR));
        server.setHandler(new ContextHandlerCollection(application, admin));

        ServerConnector adminConnector = new ServerConnector(server);
        adminConnector.setName(ADMIN_CONNECTOR);
        adminConnector.setHost(host);
        adminConnector.setPort(configuration.getAdminPort());
        adminConnector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setSendServerVersion(false);
        server.addConnector(adminConnector);
        return server;
    }

    /**
     * The admin port: {@code /ping} answers {@code pong}, {@code /healthcheck} runs the health checks.
     */
    private class AdminHandler extends Handler.Abstract {

        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            String path = Request.getPathInContext(request);
            if (path.equals("/ping")) {
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain;charset=utf-8");
                response.getHeaders().put(HttpHeader.CACHE_CONTROL, "must-revalidate,no-cache,no-store");
                Content.Sink.write(response, true, "pong\n", callback);
                return true;
            }
            if (path.equals("/healthcheck")) {
                boolean healthy = healthChecks.stream().allMatch(HealthCheck::isHealthy);
                String entity = healthChecks.stream()
                        .map(check -> "\"" + check.name() + "\":{\"healthy\":" + check.isHealthy() + "}")
                        .collect(Collectors.joining(",", "{", "}"));
                response.setStatus(healthy ? 200 : 500);
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json");
                response.getHeaders().put(HttpHeader.CACHE_CONTROL, "must-revalidate,no-cache,no-store");
                response.write(true, StandardCharsets.UTF_8.encode(entity), callback);
                return true;
            }
            Response.writeError(request, response, callback, 404);
            return true;
        }
    }
}
