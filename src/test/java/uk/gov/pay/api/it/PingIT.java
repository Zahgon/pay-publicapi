package uk.gov.pay.api.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.api.it.support.PublicApiTestApp;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class PingIT {

    @RegisterExtension
    private static final PublicApiTestApp app = PublicApiTestApp.withConfig("config/test-config.yaml");

    @Test
    void testPing() {
        given().port(app.getAdminPort())
                .get("/healthcheck")
                .then()
                .body("ping.healthy", is(true));
    }
}
