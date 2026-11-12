package uk.gov.pay.api.it.telephone;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.it.support.InMemoryRedisExtension;
import uk.gov.pay.api.it.support.PublicApiTestApp;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.utils.ApiKeyGenerator;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.micronaut.http.HttpHeaders.AUTHORIZATION;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public abstract class TelephonePaymentResourceITBase {
    //Must use same secret set in test-config.xml's apiKeyHmacSecret
    protected static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");
    protected static final String GATEWAY_ACCOUNT_ID = "GATEWAY_ACCOUNT_ID";
    protected static final String PAYMENTS_PATH = "/v1/payment_notification/";
    protected static final HashMap<String, Object> requestBody = new HashMap<>();
    protected static final CreateTelephonePaymentRequest.Builder createTelephonePaymentRequest = new CreateTelephonePaymentRequest.Builder();

    @RegisterExtension
    public static InMemoryRedisExtension redisDockerRule = new InMemoryRedisExtension();

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @RegisterExtension
    static final WireMockExtension connectorServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    protected static final WireMockExtension publicAuthServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    public static PublicApiTestApp app;

    @BeforeAll
    static void beforeAll() throws Exception {
        app = PublicApiTestApp.withOverrides(
                "publicapi.connector-url", connectorServer.baseUrl(),
                "publicapi.public-auth-url", publicAuthServer.baseUrl() + "/v1/auth",
                "redis.endpoint", redisDockerRule.getRedisUrl()
        );
        app.before();
    }

    @AfterAll
    static void afterAll() {
        app.after();
    }

    protected ValidatableResponse postPaymentResponse(String payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(PAYMENTS_PATH)
                .then();
    }

    protected static String toJson(Map map) {
        return GSON.toJson(map);
    }
}
