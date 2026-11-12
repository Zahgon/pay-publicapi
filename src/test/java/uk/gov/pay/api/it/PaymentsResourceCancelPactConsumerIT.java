package uk.gov.pay.api.it;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.it.support.PublicApiTestApp;
import uk.gov.pay.api.utils.ApiKeyGenerator;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.micronaut.http.HttpHeaders.AUTHORIZATION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.WiremockStubbing.stubPublicAuthV1ApiAuth;

public class PaymentsResourceCancelPactConsumerIT {

    private static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");

    @RegisterExtension
    public WireMockExtension publicAuth = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    public PactProviderExtension connector = new PactProviderExtension("connector");

    private PublicApiTestApp app;

    @BeforeEach
    public void setup() throws Exception {
        app = PublicApiTestApp.withOverrides(
                "publicapi.connector-url", "http://localhost:" + connector.getPort(),
                "publicapi.public-auth-url", publicAuth.baseUrl() + "/v1/api/auth");
        app.before();
        stubPublicAuthV1ApiAuth(publicAuth, new Account("123456", CARD, "a-token-link"), API_KEY);
    }

    @AfterEach
    public void stopApp() {
        app.after();
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-cancel-already-canceled-payment"})
    public void cancelAPaymentThatIsAlreadyCanceled() {
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(String.format("/v1/payments/%s/cancel", "charge8133029783750964630"))
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("code", is("P0501"))
                .body("description", is("Cancellation of payment failed"));
    }
}
