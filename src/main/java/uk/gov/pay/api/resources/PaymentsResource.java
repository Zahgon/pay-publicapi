package uk.gov.pay.api.resources;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CaptureChargeException;
import uk.gov.pay.api.model.CreateCardPaymentRequest;
import uk.gov.pay.api.model.CreatePaymentResult;
import uk.gov.pay.api.model.CreatedPaymentWithAllLinks;
import uk.gov.pay.api.model.IdempotencyKeyHeader;
import uk.gov.pay.api.model.PaymentEventsResponse;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.pay.api.model.search.card.PaymentSearchResults;
import uk.gov.pay.api.resources.binding.QueryParameter;
import uk.gov.pay.api.resources.binding.RequestEntityReader;
import uk.gov.pay.api.resources.error.ApiErrorResponse;
import uk.gov.pay.api.service.CancelPaymentService;
import uk.gov.pay.api.service.CapturePaymentService;
import uk.gov.pay.api.service.CreatePaymentService;
import uk.gov.pay.api.service.GetPaymentEventsService;
import uk.gov.pay.api.service.GetPaymentService;
import uk.gov.pay.api.service.PaymentSearchParams;
import uk.gov.pay.api.service.PaymentSearchService;
import uk.gov.pay.api.service.PublicApiUriGenerator;
import uk.gov.pay.api.validation.RequestValidator;

import jakarta.inject.Inject;
import java.io.IOException;

import static java.lang.String.format;
import static org.apache.hc.core5.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.hc.core5.http.HttpHeaders.PRAGMA;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_200_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_400_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_401_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_404_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_409_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_422_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_429_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_500_DESCRIPTION;

@Controller
@Tag(name = "Card payments")
@Produces(MediaType.APPLICATION_JSON)
public class PaymentsResource {

    private static final Logger logger = LoggerFactory.getLogger(PaymentsResource.class);

    private final CreatePaymentService createPaymentService;
    private final PublicApiUriGenerator publicApiUriGenerator;
    private final PaymentSearchService paymentSearchService;
    private final GetPaymentService getPaymentService;
    private final CapturePaymentService capturePaymentService;
    private final CancelPaymentService cancelPaymentService;
    private final GetPaymentEventsService getPaymentEventsService;
    private final RequestEntityReader requestEntityReader;
    private final RequestValidator requestValidator;

    @Inject
    public PaymentsResource(CreatePaymentService createPaymentService,
                            PaymentSearchService paymentSearchService,
                            PublicApiUriGenerator publicApiUriGenerator,
                            GetPaymentService getPaymentService,
                            CapturePaymentService capturePaymentService,
                            CancelPaymentService cancelPaymentService,
                            GetPaymentEventsService getPaymentEventsService,
                            RequestEntityReader requestEntityReader,
                            RequestValidator requestValidator) {
        this.requestEntityReader = requestEntityReader;
        this.requestValidator = requestValidator;
        this.createPaymentService = createPaymentService;
        this.publicApiUriGenerator = publicApiUriGenerator;
        this.paymentSearchService = paymentSearchService;
        this.getPaymentService = getPaymentService;
        this.capturePaymentService = capturePaymentService;
        this.cancelPaymentService = cancelPaymentService;
        this.getPaymentEventsService = getPaymentEventsService;
    }

    @Get("/v1/payments/{paymentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get a payment",
            summary = "Get information about a single payment",
            description = "You can use this endpoint to [get details about a single payment you’ve previously created]" +
                    "(https://docs.payments.service.gov.uk/reporting/#get-information-about-a-single-payment).",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = PaymentWithAllLinks.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public HttpResponse<PaymentWithAllLinks> getPayment(@Parameter(hidden = true) Account account,
                               @PathVariable("paymentId")
                               @Parameter(name = "paymentId", description = "Returns the payment with the matching `payment_id`.", example = "hu20sqlact5260q2nanm0q8u93")
                               String paymentId,
                               @Parameter(hidden = true) @Nullable @Header("X-Ledger") String strategyName) {

        var strategy = new GetOnePaymentStrategy(strategyName, account, paymentId, getPaymentService);
        PaymentWithAllLinks payment = strategy.validateAndExecute();
        
        logger.info("Payment returned - [ {} ]", payment);
        return HttpResponse.ok(payment)
                .header(PRAGMA, "no-cache")
                .header(CACHE_CONTROL, "no-store");
    }

    @Get("/v1/payments/{paymentId}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get events for a payment",
            summary = "Get a payment's events",
            description = "You can use this endpoint to " +
                    "[get a list of a payment’s events](https://docs.payments.service.gov.uk/reporting/#get-a-payment-s-events). " +
                    "A payment event is when a payment’s `state` changes, such as when the payment is created, or when the paying user submits their details.",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = PaymentEventsResponse.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public PaymentEventsResponse getPaymentEvents(@Parameter(hidden = true) Account account,
                                                  @PathVariable("paymentId")
                                                  @Parameter(name = "paymentId", description = "Payment identifier", example = "hu20sqlact5260q2nanm0q8u93")
                                                  String paymentId,
                                                  @Parameter(hidden = true) @Nullable @Header("X-Ledger") String strategyName) {

        var strategy = new GetPaymentEventsStrategy(strategyName, account, paymentId, getPaymentEventsService);
        PaymentEventsResponse paymentEventsResponse = strategy.validateAndExecute();
        
        logger.info("Payment events returned - [ {} ]", paymentEventsResponse);
        return paymentEventsResponse;
    }

    @Get("/v1/payments")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Search payments",
            summary = "Search payments",
            description = "You can use this endpoint to [search for payments you’ve previously created](https://docs.payments.service.gov.uk/reporting/#search-payments/). " +
                    "Payments are sorted by date, with the most recently-created payment appearing first.",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = PaymentSearchResults.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "422",
                            description = "Invalid parameters: from_date, to_date, status, display_size. See Public API documentation for the correct data formats",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public HttpResponse<String> searchPayments(@Parameter(hidden = true)
                                   Account account,
                                   @Parameter(description = "Returns payments with `reference` values exactly matching your specified value.")
                                   @Nullable @QueryParameter("reference") String reference,
                                   @Parameter(description = "Returns payments with matching `email` values. You can send full or partial email addresses. " +
                                           "`email` is the paying user’s email address.")
                                   @Nullable @QueryParameter("email") String email,
                                   @Parameter(description = "Returns payments in a matching `state`. `state` reflects where a payment is in the " +
                                           "[payment status lifecycle](https://docs.payments.service.gov.uk/api_reference/#payment-status-lifecycle).", example = "success",
                                           schema = @Schema(allowableValues = {"created", "started", "submitted", "success", "failed", "cancelled", "error"}))
                                   @Nullable @QueryParameter("state") String state,
                                   @Parameter(description = "Returns payments paid with a particular card brand.")
                                   @Nullable @QueryParameter("card_brand") String cardBrand,
                                   @Parameter(description = "Returns payments created on or after the `from_date`. " +
                                           "Date and time must be coordinated Universal Time (UTC) and ISO 8601 format to second-level accuracy - `YYYY-MM-DDThh:mm:ssZ`.", example = "2015-08-13T12:35:00Z")
                                   @Nullable @QueryParameter("from_date") String fromDate,
                                   @Parameter(description = "Returns payments created before the `to_date`. " +
                                           "Date and time must be coordinated Universal Time (UTC) and ISO 8601 format to second-level accuracy - `YYYY-MM-DDThh:mm:ssZ`.", example = "2015-08-13T12:35:00Z")
                                   @Nullable @QueryParameter("to_date") String toDate,
                                   @Parameter(description = "Returns a [specific page of results](https://docs.payments.service.gov.uk/api_reference/#pagination). Defaults to `1`.")
                                   @Nullable @QueryParameter("page") String pageNumber,
                                   @Parameter(description = "The number of payments returned " +
                                           "[per results page](https://docs.payments.service.gov.uk/api_reference/#pagination). Defaults to `500`. Maximum value is `500`.")
                                   @Nullable @QueryParameter("display_size") String displaySize,
                                   @Parameter(description = "Returns payments paid with cards under this cardholder name.")
                                   @Nullable @QueryParameter("cardholder_name") String cardHolderName,
                                   @Parameter(description = "Returns payments paid by cards beginning with the `first_digits_card_number` value. "
                                           + "`first_digits_card_number` value must be 6 digits.")
                                   @Nullable @QueryParameter("first_digits_card_number") String firstDigitsCardNumber,
                                   @Parameter(description = "Returns payments paid by cards ending with the `last_digits_card_number` value. " +
                                           "`last_digits_card_number` value must be 4 digits.")
                                   @Nullable @QueryParameter("last_digits_card_number") String lastDigitsCardNumber,
                                   @Parameter(description = "Returns payments settled on or after the `from_settled_date` value. " +
                                           "You can only search by settled date if your payment service provider is Stripe. " +
                                           "Date must be in ISO 8601 format to date-level accuracy - `YYYY-MM-DD`. " +
                                           "Payments are settled when your payment service provider sends funds to your bank account.")
                                   @Nullable @QueryParameter("from_settled_date") String fromSettledDate,
                                   @Parameter(description = "Returns payments settled before the `to_settled_date` value. " +
                                           "You can only search by settled date if your payment service provider is Stripe. " +
                                           "Date must be in ISO 8601 format to date-level accuracy - `YYYY-MM-DD`. " +
                                           "Payments are settled when your payment service provider sends funds to your bank account.")
                                   @Nullable @QueryParameter("to_settled_date") String toSettledDate,
                                   @Parameter(description = "Returns payments that were authorised using the agreement with this `agreement_id`. " +
                                           "Must be an exact match.", example = "abcefghjklmnopqr1234567890")
                                   @Nullable @QueryParameter("agreement_id") String agreementId) {

        logger.info("Payments search request - [ reference: {}, email: REDACTED, status: {}, card_brand {}, fromDate: {}, toDate: {}, page: {}, display_size: {}, cardholder_name: REDACTED, first_digits_card_number: {}, last_digits_card_number: {}, from_settled_date: {}, to_settled_date: {}, agreement_id: {} ]",
                reference, state, cardBrand, fromDate, toDate, pageNumber, displaySize, firstDigitsCardNumber, lastDigitsCardNumber, fromSettledDate, toSettledDate, agreementId);

        var paymentSearchParams = new PaymentSearchParams.Builder()
                .withReference(reference)
                .withEmail(email)
                .withState(state)
                .withCardBrand(cardBrand)
                .withFromDate(fromDate)
                .withToDate(toDate)
                .withPageNumber(pageNumber)
                .withDisplaySize(displaySize)
                .withCardHolderName(cardHolderName)
                .withFirstDigitsCardNumber(firstDigitsCardNumber)
                .withLastDigitsCardNumber(lastDigitsCardNumber)
                .withFromSettledDate(fromSettledDate)
                .withToSettledDate(toSettledDate)
                .withAgreementId(agreementId)
                .build();

        return paymentSearchService.searchLedgerPayments(account, paymentSearchParams);
    }

    @Post("/v1/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Create a payment",
            summary = "Create a payment",
            description = "You can use this endpoint to [create a new payment](https://docs.payments.service.gov.uk/making_payments/).",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = CreatePaymentResult.class))),
                    @ApiResponse(responseCode = "400", description = RESPONSE_400_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "422",
                            description = RESPONSE_422_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public HttpResponse<PaymentWithAllLinks> createNewPayment(@Parameter(hidden = true) Account account,
                                     HttpRequest<?> request,
                                     @Parameter(description = "Idempotency key: 1 to 255 alphanumeric characters and hyphens")
                                     @Nullable @Header("Idempotency-Key")
                                     String idempotencyKey) throws IOException {
        CreateCardPaymentRequest createCardPaymentRequest = requestEntityReader.read(request, CreateCardPaymentRequest.class);
        requestValidator.validate(createCardPaymentRequest, idempotencyKey == null ? null : new IdempotencyKeyHeader(idempotencyKey));
        logger.info("Payment create request parsed to {}", createCardPaymentRequest);

        CreatedPaymentWithAllLinks createdPayment = createPaymentService.create(account, createCardPaymentRequest, idempotencyKey);

        PaymentWithAllLinks paymentWithAllLinks = createdPayment.getPayment();
        MutableHttpResponse<PaymentWithAllLinks> response;

        switch (createdPayment.getWhenCreated()) {
            case BRAND_NEW -> response = HttpResponse.<PaymentWithAllLinks>status(io.micronaut.http.HttpStatus.CREATED)
                    .header("Location", publicApiUriGenerator.getPaymentURI(paymentWithAllLinks.getPaymentId()).toString());
            case EXISTING -> response = HttpResponse.ok();
            default ->
                    throw new IllegalArgumentException(format("Unrecognised WhenCreated enum: %s", createdPayment.getWhenCreated()));
        }

        response.body(paymentWithAllLinks)
                .header(PRAGMA, "no-cache")
                .header(CACHE_CONTROL, "no-store");

        logger.info("Payment returned (created): [ {} ]", paymentWithAllLinks);
        return response;
    }

    @Post("/v1/payments/{paymentId}/cancel")
    @Consumes(MediaType.ALL)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Cancel a payment",
            summary = "Cancel payment",
            description = "You can use this endpoint [to cancel an unfinished payment]" +
                    "(https://docs.payments.service.gov.uk/making_payments/#cancel-a-payment-that-s-in-progress).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content"),
                    @ApiResponse(responseCode = "400", description = "Cancellation of payment failed",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "409", description = RESPONSE_409_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public HttpResponse<?> cancelPayment(@Parameter(hidden = true) Account account,
                                  @PathVariable("paymentId")
                                  @Parameter(name = "paymentId", description = "The `payment_id` of the payment you’re cancelling.", example = "hu20sqlact5260q2nanm0q8u93")
                                  String paymentId) {

        return cancelPaymentService.cancel(account, paymentId);
    }

    @Post("/v1/payments/{paymentId}/capture")
    @Consumes(MediaType.ALL)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Capture a payment",
            summary = "Take a delayed payment",
            description = "You can use this endpoint to [take (‘capture’) a delayed payment from the paying user’s bank account]" +
                    "(https://docs.payments.service.gov.uk/delayed_capture/).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content"),
                    @ApiResponse(responseCode = "400", description = "Capture of payment failed",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "409", description = RESPONSE_409_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public HttpResponse<?> capturePayment(@Parameter(hidden = true) Account account,
                                   @PathVariable("paymentId")
                                   @Parameter(name = "paymentId", description = "The `payment_id` of the payment you’re capturing.", example = "hu20sqlact5260q2nanm0q8u93")
                                   String paymentId) {
        RestClientResponse connectorResponse = capturePaymentService.capture(account, paymentId);

        if (connectorResponse.getStatus() == HttpStatus.SC_NO_CONTENT) {
            return HttpResponse.noContent();
        }
        throw new CaptureChargeException(connectorResponse);
    }
}
