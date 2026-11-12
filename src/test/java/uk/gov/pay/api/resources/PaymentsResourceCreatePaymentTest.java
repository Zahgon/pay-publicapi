package uk.gov.pay.api.resources;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.CardDetails;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.CreateCardPaymentRequest;
import uk.gov.pay.api.model.CreateCardPaymentRequestBuilder;
import uk.gov.pay.api.model.CreatedPaymentWithAllLinks;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.pay.api.resources.binding.RequestEntityReader;
import uk.gov.pay.api.service.CancelPaymentService;
import uk.gov.pay.api.service.CapturePaymentService;
import uk.gov.pay.api.service.CreatePaymentService;
import uk.gov.pay.api.service.GetPaymentEventsService;
import uk.gov.pay.api.service.GetPaymentService;
import uk.gov.pay.api.service.PaymentSearchService;
import uk.gov.pay.api.service.PublicApiUriGenerator;
import uk.gov.pay.api.validation.RequestValidator;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.net.URI;
import java.util.Collections;

import static org.apache.hc.core5.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.hc.core5.http.HttpHeaders.PRAGMA;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.model.CreatedPaymentWithAllLinks.WhenCreated.BRAND_NEW;

@ExtendWith(MockitoExtension.class)
public class PaymentsResourceCreatePaymentTest {

    @InjectMocks
    private PaymentsResource paymentsResource;

    @Mock
    private CreatePaymentService createPaymentService;

    @Mock
    private PaymentSearchService paymentSearchService;

    @Mock
    private PublicApiUriGenerator publicApiUriGenerator;

    @Mock
    private GetPaymentService getPaymentService;

    @Mock
    private CapturePaymentService capturePaymentService;

    @Mock
    private CancelPaymentService cancelPaymentService;

    @Mock
    private GetPaymentEventsService getPaymentEventsService;

    @Mock
    private RequestEntityReader requestEntityReader;

    @Mock
    private RequestValidator requestValidator;

    private final String PAYMENT_URI = "https://my.link/v1/payments/abc123";

    @BeforeEach
    public void setup() {
        when(publicApiUriGenerator.getPaymentURI(anyString())).thenReturn(URI.create(PAYMENT_URI));
    }

    @Test
    void createNewPayment_withCardPayment_invokesCreatePaymentService() throws Exception {
        Account account = new Account("foo", TokenPaymentType.CARD, "a-token-link");
        var createPaymentRequest = CreateCardPaymentRequestBuilder.builder()
                .amount(100)
                .returnUrl("https://somewhere.test")
                .reference("my_ref")
                .description("New passport")
                .build();

        PaymentWithAllLinks injectedResponse = aSuccessfullyCreatedPayment();
        CreatedPaymentWithAllLinks payment = CreatedPaymentWithAllLinks.of(injectedResponse, BRAND_NEW);

        when(createPaymentService.create(account, createPaymentRequest, null)).thenReturn(payment);
        HttpRequest<?> request = HttpRequest.POST("/v1/payments", "{}");
        when(requestEntityReader.read(request, CreateCardPaymentRequest.class)).thenReturn(createPaymentRequest);

        HttpResponse<PaymentWithAllLinks> newPayment = paymentsResource.createNewPayment(account, request, null);

        assertThat(newPayment.getHeaders().get(PRAGMA), is("no-cache"));
        assertThat(newPayment.getHeaders().get(CACHE_CONTROL), is("no-store"));
        assertThat(newPayment.code(), is(201));
        assertThat(newPayment.getHeaders().get("Location"), is(URI.create(PAYMENT_URI).toString()));
        assertThat(newPayment.body(), sameInstance(injectedResponse));
    }


    private PaymentWithAllLinks aSuccessfullyCreatedPayment() {
        final Address cardholderAddress = new Address("123 Acacia Ave", "", "", "London", "GB");
        return new PaymentWithAllLinks.PaymentWithAllLinksBuilder()
                .withChargeId("abc123")
                .withAmount(100L)
                .withState(new PaymentState("created", false))
                .withReturnUrl("https://somewhere.test")
                .withDescription("New Passport")
                .withReference("my_ref")
                .withEmail("made.up@example.com")
                .withPaymentProvider("sandbox")
                .withCreatedDate("2018-01-01T11:12:13Z")
                .withLanguage(SupportedLanguage.ENGLISH)
                .withDelayedCapture(false)
                .withMoto(false)
                .withRefundSummary(new RefundSummary())
                .withSettlementSummary(new PaymentSettlementSummary())
                .withCardDetails(new CardDetails("9876", "482393", "Anne Onymous", "12/20", cardholderAddress, "visa", null, null))
                .withPaymentConnectorResponseLinks(Collections.emptyList())
                .withSelfLink(URI.create(PAYMENT_URI))
                .withPaymentEventsUri(URI.create(PAYMENT_URI + "/events"))
                .withPaymentCancelUri(URI.create(PAYMENT_URI + "/cancel"))
                .withPaymentRefundsUri(URI.create(PAYMENT_URI + "/refunds"))
                .withPaymentCaptureUri(URI.create(PAYMENT_URI + "/capture"))
                .withPaymentAuthorisationUri(URI.create(PAYMENT_URI + "/auth"))
                .withCorporateCardSurcharge(null)
                .withTotalAmount(null)
                .withProviderId("providerId")
                .withMetadata(null)
                .withFee(null)
                .withNetAmount(null)
                .withAuthorisationSummary(null)
                .withAgreementId(null)
                .withAuthorisationMode(AuthorisationMode.WEB)
                .build();
    }
}
