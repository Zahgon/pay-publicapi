package uk.gov.pay.api.resources;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.gov.pay.api.model.RequestError.Code.REQUEST_DENIED_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Controller
public class RequestDeniedResource {

    private static final Logger logger = LoggerFactory.getLogger(RequestDeniedResource.class);

    @Get("/request-denied")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.ALL)
    public HttpResponse<?> requestDeniedGet(@Nullable @Header("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    @Post("/request-denied")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.ALL)
    public HttpResponse<?> requestDeniedPost(@Nullable @Header("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    @Put("/request-denied")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.ALL)
    public HttpResponse<?> requestDeniedPut(@Nullable @Header("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    @Delete("/request-denied")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.ALL)
    public HttpResponse<?> requestDeniedDelete(@Nullable @Header("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    private HttpResponse<?> requestDenied(String naxsiViolatedRules) {
        logger.info("Naxsi rules violated - [ {} ]", naxsiViolatedRules);
        return HttpResponse.badRequest(aRequestError(REQUEST_DENIED_ERROR));
    }
}
