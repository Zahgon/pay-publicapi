package uk.gov.pay.api.resources;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.RefundsResponse;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.model.search.card.RefundForSearchResult;
import uk.gov.pay.api.model.search.card.RefundResult;
import uk.gov.pay.api.resources.binding.RequestEntityReader;
import uk.gov.pay.api.resources.error.ApiErrorResponse;
import uk.gov.pay.api.service.ConnectorService;
import uk.gov.pay.api.service.CreateRefundService;
import uk.gov.pay.api.service.GetPaymentRefundService;
import uk.gov.pay.api.service.GetPaymentRefundsService;
import uk.gov.pay.api.service.GetPaymentService;

import jakarta.inject.Inject;
import java.io.IOException;

import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_200_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_401_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_404_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_429_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_500_DESCRIPTION;

@Controller
@Tag(name = "Refunding card payments")
@Produces(MediaType.APPLICATION_JSON)
public class PaymentRefundsResource {
    private static final Logger logger = LoggerFactory.getLogger(PaymentRefundsResource.class);

    private final GetPaymentRefundsService getPaymentRefundsService;
    private final GetPaymentRefundService getPaymentRefundService;
    private final CreateRefundService createRefundService;
    private final RequestEntityReader requestEntityReader;

    @Inject
    public PaymentRefundsResource(PublicApiConfig configuration,
                                  GetPaymentRefundsService getPaymentRefundsService,
                                  GetPaymentRefundService getPaymentRefundService,
                                  ConnectorService connectorService,
                                  GetPaymentService getPaymentService,
                                  CreateRefundService createRefundService,
                                  RequestEntityReader requestEntityReader) {
        this.requestEntityReader = requestEntityReader;
        this.getPaymentRefundsService = getPaymentRefundsService;
        this.getPaymentRefundService = getPaymentRefundService;
        this.createRefundService = createRefundService;
    }

    @Get("/v1/payments/{paymentId}/refunds")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get all refunds for a payment",
            summary = "Get information about a payment’s refunds",
            description = "You can use this endpoint to [get a list of refunds for a payment]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments/#get-all-refunds-for-a-single-payment).",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RefundForSearchResult.class))),
                    @ApiResponse(responseCode = "401", description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public RefundsResponse getRefunds(@Parameter(hidden = true) Account account,
                                      @PathVariable("paymentId") @Parameter(name = "paymentId", 
                                              description = "The unique `payment_id` of the payment you want a list of refunds for.") 
                                      String paymentId) {
        
        RefundsResponse refundsResponse = getPaymentRefundsService.getLedgerTransactionTransactions(account, paymentId);
        logger.debug("refund returned - [ {} ]", refundsResponse);
        return refundsResponse;
    }

    @Get("/v1/payments/{paymentId}/refunds/{refundId}")
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get a payment refund",
            summary = "Check the status of a refund",
            description = "You can use this endpoint to [get details about an individual refund]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments/#checking-the-status-of-a-refund).",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RefundResult.class))),
                    @ApiResponse(responseCode = "401", description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    @Produces(MediaType.APPLICATION_JSON)
    public RefundResponse getRefundById(@Parameter(hidden = true)
                                        Account account,
                                        @PathVariable("paymentId") @Parameter(name = "paymentId",
                                                description = "The unique `payment_id` of the payment you want to view a refund of.") String paymentId,
                                        @PathVariable("refundId") @Parameter(name = "refundId",
                                                description = "The unique `refund_id` of the refund you want to view. " + 
                                                        "If one payment has multiple refunds, each refund has a different `refund_id`.") String refundId,
                                        @Parameter(hidden = true) @Nullable @Header("X-Ledger") String strategyName) {

        var strategy = new GetPaymentRefundStrategy(strategyName, account, paymentId, refundId, getPaymentRefundService);
        RefundResponse refundResponse = strategy.validateAndExecute();
        logger.info("refund returned - [ {} ]", refundResponse);
        return refundResponse;
    }

    @Post("/v1/payments/{paymentId}/refunds")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Submit a refund for a payment",
            summary = "Refund a payment",
            description = "You can use this endpoint to [fully or partially refund a payment]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "successful operation",
                            content = @Content(schema = @Schema(implementation = RefundResult.class))),
                    @ApiResponse(responseCode = "202", description = "ACCEPTED"),
                    @ApiResponse(responseCode = "401", description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "412", description = "Refund amount available mismatch"),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public HttpResponse<RefundResponse> submitRefund(@Parameter(hidden = true) Account account,
                                 @PathVariable("paymentId") @Parameter(name = "paymentId",
                                         description = "The unique `payment_id` of the payment you want to refund.") String paymentId,
                                 HttpRequest<?> request) throws IOException {
        CreatePaymentRefundRequest requestPayload = requestEntityReader.read(request, CreatePaymentRefundRequest.class);
        
        RefundResponse refundResponse = createRefundService.createRefund(account, paymentId, requestPayload);
        return HttpResponse.accepted().body(refundResponse);
    }
}
