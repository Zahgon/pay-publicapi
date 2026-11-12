package uk.gov.pay.api.utils.pact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Plays the provider side of the consumer pacts in {@code src/test/resources/pacts} with WireMock, replacing the
 * pact-jvm JUnit 4 {@code PayPactProviderRule}. Before each test it serves the interactions of the test's
 * {@link Pacts} that belong to its provider; after the test it verifies, as {@code @PactVerification} did, that
 * every interaction was requested once, exactly as the pact describes (method, path, query, headers and body) and that no
 * request outside the pact was made.
 * <p>
 * Register it on an instance field: the mock server starts when the extension is created (so its URL can be used in
 * the test's set-up) and stops after the test.
 */
public class PactProviderExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String provider;
    private final WireMockServer server;
    private final Map<UUID, Interaction> interactionsByStub = new HashMap<>();

    public PactProviderExtension(String provider) {
        this.provider = provider;
        this.server = new WireMockServer(wireMockConfig().dynamicPort());
        this.server.start();
    }

    public String getUrl() {
        return "http://localhost:" + server.port();
    }

    public int getPort() {
        return server.port();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getTestMethod()
                .map(method -> method.getAnnotation(Pacts.class))
                .ifPresent(pacts -> {
                    for (String pactName : pacts.pacts()) {
                        if (pactName.contains(provider)) {
                            loadInteractions(pactName).forEach(this::stub);
                        }
                    }
                });
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            verify();
        } finally {
            server.stop();
        }
    }

    private void verify() {
        List<String> problems = new ArrayList<>();
        for (LoggedRequest unmatched : server.findAllUnmatchedRequests()) {
            problems.add("Unexpected request " + unmatched.getMethod() + " " + unmatched.getUrl()
                    + (unmatched.getBodyAsString().isEmpty() ? "" : " with body " + unmatched.getBodyAsString()));
        }
        List<ServeEvent> serveEvents = server.getAllServeEvents();
        for (Map.Entry<UUID, Interaction> entry : interactionsByStub.entrySet()) {
            Interaction interaction = entry.getValue();
            List<ServeEvent> served = serveEvents.stream()
                    .filter(event -> event.getStubMapping() != null && entry.getKey().equals(event.getStubMapping().getId()))
                    .toList();
            if (served.isEmpty()) {
                problems.add("Missing request for interaction '" + interaction.description + "' of " + interaction.pactName
                        + ": " + interaction.method + " " + interaction.path);
            } else if (served.size() > 1) {
                problems.add("Interaction '" + interaction.description + "' of " + interaction.pactName + " was requested "
                        + served.size() + " times; the pact expects it once");
            }
            for (ServeEvent event : served) {
                Set<String> actualQueryNames = queryParameterNames(event.getRequest().getUrl());
                if (!actualQueryNames.equals(interaction.query.keySet())) {
                    problems.add("Interaction '" + interaction.description + "' of " + interaction.pactName
                            + " expected query parameters " + interaction.query.keySet() + " but was requested with "
                            + actualQueryNames);
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new AssertionError("Pact verification failed for provider " + provider + ":\n  " + String.join("\n  ", problems));
        }
    }

    private void stub(Interaction interaction) {
        MappingBuilder mapping = WireMock.request(interaction.method, urlPathEqualTo(interaction.path));
        interaction.query.forEach((name, values) -> {
            if (values.size() == 1) {
                mapping.withQueryParam(name, equalTo(values.getFirst()));
            } else {
                mapping.withQueryParam(name, havingExactly(values.stream().map(WireMock::equalTo).toArray(StringValuePattern[]::new)));
            }
        });
        interaction.requestHeaders.forEach((name, value) -> mapping.withHeader(name, equalTo(value)));
        if (interaction.requestBody != null) {
            mapping.withRequestBody(equalToJson(interaction.requestBody, false, false));
        }

        ResponseDefinitionBuilder response = aResponse().withStatus(interaction.status);
        interaction.responseHeaders.forEach(response::withHeader);
        if (interaction.responseBody != null) {
            response.withBody(interaction.responseBody);
        }
        StubMapping stubMapping = server.stubFor(mapping.willReturn(response));
        interactionsByStub.put(stubMapping.getId(), interaction);
    }

    private static Set<String> queryParameterNames(String url) {
        String query = URI.create(url).getRawQuery();
        Set<String> names = new TreeSet<>();
        if (query == null || query.isEmpty()) {
            return names;
        }
        for (String pair : query.split("&")) {
            String name = pair.contains("=") ? pair.substring(0, pair.indexOf('=')) : pair;
            names.add(URLDecoder.decode(name, StandardCharsets.UTF_8));
        }
        return names;
    }

    static List<Interaction> loadInteractions(String pactName) {
        String resource = "pacts/" + pactName + ".json";
        try (InputStream in = PactProviderExtension.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("No pact " + resource);
            }
            JsonNode pact = OBJECT_MAPPER.readTree(in);
            List<Interaction> interactions = new ArrayList<>();
            for (JsonNode interaction : pact.get("interactions")) {
                interactions.add(new Interaction(pactName, interaction));
            }
            return interactions;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static final class Interaction {
        final String pactName;
        final String description;
        final String method;
        final String path;
        final Map<String, List<String>> query = new TreeMap<>();
        final Map<String, String> requestHeaders = new TreeMap<>();
        final String requestBody;
        final int status;
        final Map<String, String> responseHeaders = new TreeMap<>();
        final String responseBody;

        Interaction(String pactName, JsonNode interaction) throws IOException {
            this.pactName = pactName;
            this.description = interaction.path("description").asText();
            JsonNode request = interaction.get("request");
            this.method = request.get("method").asText().toUpperCase();
            this.path = request.get("path").asText();
            JsonNode queryNode = request.get("query");
            if (queryNode != null) {
                for (Iterator<Map.Entry<String, JsonNode>> fields = queryNode.fields(); fields.hasNext(); ) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    List<String> values = new ArrayList<>();
                    if (field.getValue().isArray()) {
                        field.getValue().forEach(value -> values.add(value.asText()));
                    } else {
                        values.add(field.getValue().asText());
                    }
                    query.put(field.getKey(), values);
                }
            }
            readHeaders(request.get("headers"), requestHeaders);
            this.requestBody = bodyOf(request);
            JsonNode response = interaction.get("response");
            this.status = response.get("status").asInt();
            readHeaders(response.get("headers"), responseHeaders);
            this.responseBody = bodyOf(response);
        }

        private static void readHeaders(JsonNode headers, Map<String, String> into) {
            if (headers != null) {
                headers.fields().forEachRemaining(header -> into.put(header.getKey(), header.getValue().asText()));
            }
        }

        private static String bodyOf(JsonNode message) throws IOException {
            JsonNode body = message.get("body");
            if (body == null || body.isNull()) {
                return null;
            }
            return body.isTextual() ? body.asText() : OBJECT_MAPPER.writeValueAsString(body);
        }
    }
}
