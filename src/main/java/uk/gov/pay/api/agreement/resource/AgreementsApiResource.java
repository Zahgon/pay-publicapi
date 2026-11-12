package uk.gov.pay.api.agreement.resource;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
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
import uk.gov.pay.api.agreement.model.Agreement;
import uk.gov.pay.api.agreement.model.AgreementSearchResults;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.agreement.service.AgreementsService;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.resources.binding.RequestEntityReader;
import uk.gov.pay.api.resources.error.ApiErrorResponse;
import uk.gov.pay.api.utils.QueryParameters;
import uk.gov.pay.api.validation.RequestValidator;

import jakarta.inject.Inject;
import java.io.IOException;

import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_200_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_201_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_400_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_401_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_404_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_422_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_429_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_500_DESCRIPTION;

@Controller
@Tag(name = "Agreements")
@Produces(MediaType.APPLICATION_JSON)
public class AgreementsApiResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgreementsApiResource.class);

    private final AgreementsService agreementsService;
    private final RequestEntityReader requestEntityReader;
    private final RequestValidator requestValidator;

    @Inject
    public AgreementsApiResource(AgreementsService agreementsService, RequestEntityReader requestEntityReader,
                                 RequestValidator requestValidator) {
        this.agreementsService = agreementsService;
        this.requestEntityReader = requestEntityReader;
        this.requestValidator = requestValidator;
    }

    @Post("/v1/agreements")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Create an agreement",
            summary = "Create an agreement for recurring payments",
            description = "You can use this endpoint to create a new agreement.",
            responses = {
                    @ApiResponse(responseCode = "201", description = RESPONSE_201_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = Agreement.class))),
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
    public HttpResponse<Agreement> createAgreement(
            @Parameter(hidden = true) Account account, HttpRequest<?> request) throws IOException {
        CreateAgreementRequest createAgreementRequest = requestEntityReader.read(request, CreateAgreementRequest.class);
        requestValidator.validate(createAgreementRequest);
        LOGGER.info("Creating new agreement for reference {} and gateway accountID {}", 
                createAgreementRequest.getReference(), account.accountId());
        var agreementCreatedResponse = agreementsService.createAgreement(account, createAgreementRequest);
        var agreementLedgerResponse = agreementsService.getAgreement(account, agreementCreatedResponse.getAgreementId());
        
        LOGGER.info("Agreement returned (created): [ {} ]", agreementCreatedResponse);
        return HttpResponse.created(Agreement.from(agreementLedgerResponse));
    }

    @Get("/v1/agreements/{agreementId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get an agreement",
            summary = "Get information about a single agreement for recurring payments",
            description = "You can use this endpoint to get information about a single recurring payments agreement.",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = Agreement.class))),
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
    public Agreement getAgreement(
            @Parameter(hidden = true) Account account,
            @PathVariable("agreementId") 
            @Parameter(name = "agreementId", 
                    description = "Returns the agreement with the matching `agreement_id`. " +
                            "GOV.UK Pay generated an `agreement_id` when you created the agreement.", 
                    example = "cgc1ocvh0pt9fqs0ma67r42l58") String agreementId)
    {
        LOGGER.info("Get agreement {} request", agreementId);
        var agreementLedgerResponse = agreementsService.getAgreement(account, agreementId);
        return Agreement.from(agreementLedgerResponse);
    }

    @Get("/v1/agreements")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Search agreements",
            summary = "Search agreements for recurring payments",
            description = "You can use this endpoint to search for recurring payments agreements. " +
                    "The agreements are sorted by date, with the most recently-created agreements appearing first.",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = AgreementSearchResults.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "422",
                            description = RESPONSE_422_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public AgreementSearchResults getAgreements(@Parameter(hidden = true) Account account, HttpRequest<?> request) {
        AgreementSearchParams searchParams = AgreementSearchParams.from(QueryParameters.of(request));
        return agreementsService.searchAgreements(account, searchParams);
    }

    @Post("/v1/agreements/{agreementId}/cancel")
    @Consumes(MediaType.ALL)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Cancel an agreement",
            summary = "Cancel an agreement for recurring payments",
            description = "You can use this endpoint to cancel a recurring payments agreement in the `active` status.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content"),
                    @ApiResponse(responseCode = "400", description = "Cancellation of agreement failed",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
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
    public HttpResponse<?> cancelAgreement(@Parameter(hidden = true) Account account, @PathVariable("agreementId") @Parameter(name = "agreementId",
            description = "The `agreement_id` of the agreement you are cancelling",
            example = "cgc1ocvh0pt9fqs0ma67r42l58") String agreementId)
    {
        agreementsService.cancelAgreement(account, agreementId);
        return HttpResponse.noContent();
    }

}
