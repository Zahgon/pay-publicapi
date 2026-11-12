package uk.gov.pay.api.resources;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
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
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.model.search.card.SearchRefundsResults;
import uk.gov.pay.api.resources.binding.QueryParameter;
import uk.gov.pay.api.service.RefundsParams;
import uk.gov.pay.api.service.SearchRefundsService;

import jakarta.inject.Inject;

import static java.lang.String.format;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_200_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_401_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_500_DESCRIPTION;
import static uk.gov.pay.api.validation.RefundSearchValidator.validateSearchParameters;

@Controller
@Tag(name = "Refunding card payments")
@Produces(MediaType.APPLICATION_JSON)
public class SearchRefundsResource {

    private static final Logger logger = LoggerFactory.getLogger(SearchRefundsResource.class);

    private final SearchRefundsService searchRefundsService;

    @Inject
    public SearchRefundsResource(SearchRefundsService searchRefundsService, PublicApiConfig configuration) {
        this.searchRefundsService = searchRefundsService;
    }

    @Get("/v1/refunds")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Search refunds",
            summary = "Search refunds",
            description = "You can use this endpoint to [search refunds you’ve previously created]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments/#searching-refunds). " +
                    "The refunds are sorted by date, with the most recently created refunds appearing first.",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION, content = @Content(schema = @Schema(implementation = SearchRefundsResults.class))),
                    @ApiResponse(responseCode = "401", description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "422", description = "Invalid parameters. See Public API documentation for the correct data formats",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
            }
    )
    public SearchRefundsResults searchRefunds(@Parameter(hidden = true)
                                  Account account,
                                  @Parameter(description = "Returns refunds created on or after the `from_date`. " +
                                          "Date and time must use Coordinated Universal Time (UTC) and ISO 8601 format to second-level accuracy - `YYYY-MM-DDThh:mm:ssZ`.", example = "2015-08-13T12:35:00Z")
                                  @Nullable @QueryParameter("from_date") String fromDate,
                                  @Parameter(description = "Returns refunds created before the `to_date`. " +
                                          "Date and time must use Coordinated Universal Time (UTC) and ISO 8601 format to second-level accuracy - `YYYY-MM-DDThh:mm:ssZ`.", example = "2015-08-13T12:35:00Z")
                                  @Nullable @QueryParameter("to_date") String toDate,
                                  @Parameter(description = "Returns refunds settled on or after the `from_settled_date` value. " +
                                          "You can only use `from_settled_date` if your payment service provider is Stripe. " +
                                          "Date must use ISO 8601 format to date-level accuracy - `YYYY-MM-DD`. " +
                                          "Refunds are settled when Stripe takes the refund from your account balance.", example="2022-08-13")
                                  @Nullable @QueryParameter("from_settled_date") String fromSettledDate,
                                  @Parameter(description = "Returns refunds settled before the `to_settled_date` value. " +
                                          "You can only use `to_settled_date` if your payment service provider is Stripe. " +
                                          "Date must use ISO 8601 format to date-level accuracy - `YYYY-MM-DD`. " +
                                          "Refunds are settled when Stripe takes the refund from your account balance.", example="2022-08-13")
                                  @Nullable @QueryParameter("to_settled_date") String toSettledDate,
                                  @Parameter(description = "Returns a [specific page of results](https://docs.payments.service.gov.uk/api_reference/#pagination). " +
                                          "Defaults to `1`.")
                                  @Nullable @QueryParameter("page") String pageNumber,
                                  @Parameter(description = "The number of refunds returned [per results page](https://docs.payments.service.gov.uk/api_reference/#pagination). " +
                                          "Defaults to `500`. Maximum value is `500`.", hidden = false)
                                  @Nullable @QueryParameter("display_size") String displaySize) {

        
        RefundsParams refundsParams = new RefundsParams(fromDate, toDate, pageNumber, displaySize,
                fromSettledDate, toSettledDate);
        
        validateSearchParameters(refundsParams);

        logger.info("Refunds search request - [ {} ]",
                format("from_date: %s, to_date: %s, page: %s, display_size: %s," +
                                "from_settled_date: %s, to_settled_date: %s",
                        fromDate, toDate, pageNumber, displaySize, fromSettledDate, toSettledDate));


        return searchRefundsService.searchLedgerRefunds(account, refundsParams);
    }
}
