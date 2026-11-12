package uk.gov.pay.api.resources.telephone;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.model.telephone.TelephonePaymentResponse;
import uk.gov.pay.api.resources.binding.RequestEntityReader;
import uk.gov.pay.api.service.telephone.CreateTelephonePaymentService;
import uk.gov.pay.api.validation.RequestValidator;

import jakarta.inject.Inject;
import java.io.IOException;

@Controller
public class TelephonePaymentNotificationResource {

    private static final Logger logger = LoggerFactory.getLogger(TelephonePaymentNotificationResource.class);

    private final CreateTelephonePaymentService createTelephonePaymentService;
    private final RequestEntityReader requestEntityReader;
    private final RequestValidator requestValidator;

    @Inject
    public TelephonePaymentNotificationResource(CreateTelephonePaymentService createTelephonePaymentService,
                                                RequestEntityReader requestEntityReader, RequestValidator requestValidator) {
        this.createTelephonePaymentService = createTelephonePaymentService;
        this.requestEntityReader = requestEntityReader;
        this.requestValidator = requestValidator;
    }


    @Post("/v1/payment_notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> newPayment(Account account, HttpRequest<?> request) throws IOException {
        CreateTelephonePaymentRequest createTelephonePaymentRequest = requestEntityReader.read(request, CreateTelephonePaymentRequest.class);
        requestValidator.validate(createTelephonePaymentRequest);
        Pair<TelephonePaymentResponse, Integer> responseAndStatusCode = createTelephonePaymentService.create(account, createTelephonePaymentRequest);
        var response = responseAndStatusCode.getLeft();
        var statusCode = responseAndStatusCode.getRight();

        return HttpResponse.status(HttpStatus.valueOf(statusCode)).body(response);
    }
}

