package uk.gov.pay.api.it.validation;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.api.it.support.InMemoryRedisExtension;
import uk.gov.pay.api.it.support.PublicApiTestApp;
import uk.gov.pay.api.utils.ApiKeyGenerator;
import uk.gov.pay.api.utils.mocks.Junit5ConnectorMockClient;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.AUTHORIZATION;
import static io.micronaut.http.HttpHeaders.CONTENT_TYPE;
import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.hc.core5.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.api.it.support.PublicApiTestApp.unusedPort;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.mocks.CreateChargeRequestParams.CreateChargeRequestParamsBuilder.aCreateChargeRequestParams;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.NON_HTTPS_RETURN_URL_NOT_ALLOWED_FOR_A_LIVE_ACCOUNT;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class CreatePaymentWithHttpReturnUrlIT {

    private static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");
    private static final String GATEWAY_ACCOUNT_ID = "GATEWAY_ACCOUNT_ID";
    private static final String PAYMENTS_PATH = "/v1/payments/";

    @RegisterExtension
    public static InMemoryRedisExtension redisDockerRule = new InMemoryRedisExtension();

    private static final int CONNECTOR_PORT = unusedPort();
    private static final int PUBLIC_AUTH_PORT = unusedPort();

    @RegisterExtension
    public static WireMockExtension connectorMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(CONNECTOR_PORT))
            .build();

    @RegisterExtension
    public static WireMockExtension publicAuthMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(PUBLIC_AUTH_PORT))
            .build();

    private final PublicApiTestApp app = PublicApiTestApp.withOverrides(
            "publicapi.connector-url", "http://localhost:" + CONNECTOR_PORT,
            "publicapi.public-auth-url", "http://localhost:" + PUBLIC_AUTH_PORT + "/v1/auth",
            "redis.endpoint", redisDockerRule.getRedisUrl(),
            "publicapi.allow-http-for-return-url", "true"); 
    
    private final Junit5ConnectorMockClient connectorMockClient = new Junit5ConnectorMockClient(connectorMock);

    private final Map<String, Object> createChargePayload = Map.of("amount", 100,
            "reference", "ref",
            "description", "desc",
            "return_url", "http://somewhere.com",
            "metadata", Map.of());

    @BeforeEach
    void setup() {
        app.before();
        publicAuthMock.stubFor(get("/v1/auth")
                .withHeader(ACCEPT, matching(APPLICATION_JSON))
                .withHeader(AUTHORIZATION, matching("Bearer " + API_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"account_id\" : \"" + GATEWAY_ACCOUNT_ID + "\", " +
                                "\"token_link\" : \"a-token-link\", " +
                                "\"token_type\" : \"" + CARD.toString() + "\"}")));
    }

    @AfterEach
    void stopApp() {
        app.after();
    }

    @Nested
    class ForGatewayAccountId {
        @Test
        void create_payment_successfully() {
            connectorMockClient.respondCreated_whenCreateCharge(GATEWAY_ACCOUNT_ID, aCreateChargeRequestParams()
                    .withAmount(100)
                    .withDescription("desc")
                    .withReference("ref")
                    .withReturnUrl("http://somewhere.com")
                    .build());

            postPaymentResponse(createChargePayload).statusCode(201);
        }

        @Test
        void handle_NON_HTTPS_RETURN_URL_NOT_ALLOWED_FOR_A_LIVE_ACCOUNT_error() {
            connectorMockClient.respondWithErrorIdentifier_whenCreateCharge(GATEWAY_ACCOUNT_ID, SC_UNPROCESSABLE_ENTITY,
                    NON_HTTPS_RETURN_URL_NOT_ALLOWED_FOR_A_LIVE_ACCOUNT);

            postPaymentResponse(createChargePayload)
                    .statusCode(422)
                    .body("field", is("return_url"))
                    .body("code", is("P0920"))
                    .body("description", is("Invalid attribute value: return_url. Must begin with https:// for a live gateway account"));
        }
    }

    private ValidatableResponse postPaymentResponse(Map<String, Object> payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(PAYMENTS_PATH)
                .then();
    }
}
