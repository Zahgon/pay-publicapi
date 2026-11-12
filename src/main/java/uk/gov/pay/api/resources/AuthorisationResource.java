package uk.gov.pay.api.resources;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.model.AuthorisationRequest;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.resources.binding.RequestEntityReader;
import uk.gov.pay.api.resources.error.ApiErrorResponse;
import uk.gov.pay.api.service.AuthorisationService;
import uk.gov.pay.api.validation.RequestValidator;

import jakarta.inject.Inject;
import java.io.IOException;

@Controller
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authorise card payments")
public class AuthorisationResource {

    private static final Logger logger = LoggerFactory.getLogger(AuthorisationResource.class);
    private final AuthorisationService authorisationService;
    private final RequestEntityReader requestEntityReader;
    private final RequestValidator requestValidator;

    @Inject
    public AuthorisationResource(AuthorisationService authorisationService, RequestEntityReader requestEntityReader,
                                 RequestValidator requestValidator) {
        this.authorisationService = authorisationService;
        this.requestEntityReader = requestEntityReader;
        this.requestValidator = requestValidator;
    }

    @Post("/v1/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "Authorise a MOTO payment",
            summary = "Send card details to authorise a MOTO payment",
            description = "You can use this endpoint to [authorise payments](https://docs.payments.service.gov.uk/moto_payments/moto_send_card_details_api/) you have created with `authorisation_mode` set to `moto_api`.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Your authorisation request was successful."),
                    @ApiResponse(responseCode = "400", description = "Your request is invalid. Check the `code` and `description` in the response to find out why your request failed.",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "402",
                            description = "The `card_number` you sent is not a valid card number or you chose not to accept this card type. Check the `code` and `description` fields in the response to find out why your request failed.",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "422", description = "A value you sent is invalid or missing. Check the `code` and `description` in the response to find out why your request failed.",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "There is something wrong with GOV.UK Pay. If there are no issues on our status page (https://payments.statuspage.io), you can contact us with your error code and we'll investigate.",
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public HttpResponse<?> authorisePayment(HttpRequest<?> request) throws IOException {
        AuthorisationRequest authorisationRequest = requestEntityReader.readOptional(request, AuthorisationRequest.class);
        requestValidator.validate(authorisationRequest);
        return authorisationService.authoriseRequest(authorisationRequest);
    }
}
