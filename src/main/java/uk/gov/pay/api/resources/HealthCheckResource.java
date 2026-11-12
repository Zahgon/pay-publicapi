package uk.gov.pay.api.resources;

import com.google.common.collect.ImmutableMap;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import uk.gov.pay.api.healthcheck.HealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HealthCheckResource {

    public static final String HEALTHCHECK = "healthcheck";
    public static final String HEALTHY = "healthy";

    private final List<HealthCheck> healthChecks;

    public HealthCheckResource(List<HealthCheck> healthChecks) {
        this.healthChecks = healthChecks;
    }

    @Get("/" + HEALTHCHECK)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<Map<String, Map<String, Boolean>>> healthCheck() {
        Map<String, Map<String, Boolean>> response = new HashMap<>();
        boolean healthy = true;
        for (HealthCheck healthCheck : healthChecks) {
            boolean checkHealthy = healthCheck.isHealthy();
            response.put(healthCheck.name(), ImmutableMap.of(HEALTHY, checkHealthy));
            healthy &= checkHealthy;
        }
        if (healthy) {
            return HttpResponse.ok(response);
        }
        return HttpResponse.<Map<String, Map<String, Boolean>>>status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
