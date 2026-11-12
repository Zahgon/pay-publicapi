package uk.gov.pay.api.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class SecuritytxtResource {

    // https://gds-way.cloudapps.digital/standards/vulnerability-disclosure.html
    private static final URI CABINET_OFFICE_SECURITY_TXT = URI.create("https://vdp.cabinetoffice.gov.uk/.well-known/security.txt");

    @Get("/.well-known/security.txt")
    @Produces(MediaType.ALL)
    public HttpResponse<?> redirectFromWellKnownSecuritytxt() {
        return redirectToCabinetOfficeSecuritytxt();
    }

    @Get("/security.txt")
    @Produces(MediaType.ALL)
    public HttpResponse<?> redirectFromSecuritytxt() {
        return redirectToCabinetOfficeSecuritytxt();
    }

    private static HttpResponse<?> redirectToCabinetOfficeSecuritytxt() {
        return HttpResponse.status(HttpStatus.FOUND)
                .header("Location", CABINET_OFFICE_SECURITY_TXT.toString())
                .header("Cache-Control", "no-cache")
                .header("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
    }
}
