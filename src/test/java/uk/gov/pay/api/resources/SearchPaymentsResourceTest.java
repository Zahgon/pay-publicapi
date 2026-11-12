package uk.gov.pay.api.resources;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.it.fixtures.PaymentNavigationLinksFixture;
import uk.gov.pay.api.it.support.PublicApiTestApp;
import uk.gov.pay.api.utils.ApiKeyGenerator;
import uk.gov.pay.api.utils.PublicAuthMockClientJUnit5;
import uk.gov.pay.api.utils.mocks.LedgerMockClientJUnit5;

import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.api.it.fixtures.PaginatedTransactionSearchResultFixture.aPaginatedTransactionSearchResult;

/**
 * The payments search resource, run in the real application with public auth and ledger stubbed (formerly a Jersey
 * {@code ResourceExtension} with mocked services).
 */
class SearchPaymentsResourceTest {

    private static final String API_TOKEN = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");

    @RegisterExtension
    static final WireMockExtension publicAuthServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static final WireMockExtension ledgerServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static PublicApiTestApp app;

    @BeforeAll
    static void startApp() {
        app = PublicApiTestApp.withOverrides(
                "publicapi.public-auth-url", publicAuthServer.baseUrl() + "/v1/auth",
                "publicapi.ledger-url", ledgerServer.baseUrl());
        app.before();
    }

    @AfterAll
    static void stopApp() {
        app.after();
    }

    @SuppressWarnings("unchecked")
    private final Appender<ILoggingEvent> mockLogAppender = mock(Appender.class);
    private final ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(PaymentsResource.class);

    @BeforeEach
    void setUp() {
        new PublicAuthMockClientJUnit5(publicAuthServer).mapBearerTokenToAccountId(API_TOKEN, "GATEWAY_ACCOUNT_ID");
        new LedgerMockClientJUnit5(ledgerServer).respondOk_whenSearchCharges(aPaginatedTransactionSearchResult()
                .withCount(0)
                .withPage(1)
                .withTotal(0)
                .withLinks(new PaymentNavigationLinksFixture().withSelfLink("/self"))
                .withPayments(List.of())
                .build());
        logger.setLevel(Level.INFO);
        logger.addAppender(mockLogAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(mockLogAppender);
    }

    @Test
    void emailAndCardholderName_areRedactedInLogs() {
        given().port(app.getLocalPort())
                .queryParam("email", "email@example.com")
                .queryParam("cardholder_name", "Mr. R E Dacted")
                .queryParam("reference", "ishouldbeinthelog123")
                .header("Authorization", format("Bearer %s", API_TOKEN))
                .get("/v1/payments")
                .then()
                .statusCode(200);

        verify(mockLogAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        String expectedLogMessage = "Payments search request - [ reference: ishouldbeinthelog123, email: REDACTED, status: null, card_brand null, fromDate: null, toDate: null, page: null, display_size: null, cardholder_name: REDACTED, first_digits_card_number: null, last_digits_card_number: null, from_settled_date: null, to_settled_date: null, agreement_id: null ]";
        assertThat(logStatement.getFirst().getFormattedMessage(), is(expectedLogMessage));
    }
}
