package uk.gov.pay.api.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonassert.JsonAssert;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;
import uk.gov.pay.api.healthcheck.HealthCheck;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class HealthCheckResourceTest {

    private HealthCheckResource resource;

    private static HealthCheck healthCheck(String name, boolean healthy) {
        return new HealthCheck() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean isHealthy() {
                return healthy;
            }
        };
    }

    @Test
    public void checkHealthCheck_isUnHealthy() throws JsonProcessingException {
        resource = new HealthCheckResource(List.of(
                healthCheck("ping", false),
                healthCheck("deadlocks", false)));

        HttpResponse<Map<String, Map<String, Boolean>>> response = resource.healthCheck();

        assertThat(response.code(), is(503));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.body());
        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.ping.healthy", is(false))
                .assertThat("$.deadlocks.healthy", is(false));
    }

    @Test
    public void checkHealthCheck_isHealthy() throws JsonProcessingException {
        resource = new HealthCheckResource(List.of(
                healthCheck("ping", true),
                healthCheck("deadlocks", true)));

        HttpResponse<Map<String, Map<String, Boolean>>> response = resource.healthCheck();

        assertThat(response.code(), is(200));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.body());

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.ping.healthy", is(true))
                .assertThat("$.deadlocks.healthy", is(true));
    }

    @Test
    public void checkHealthCheck_isUnhealthyIfPartiallyHealthy() throws JsonProcessingException {
        resource = new HealthCheckResource(List.of(
                healthCheck("ping", true),
                healthCheck("deadlocks", false)));

        HttpResponse<Map<String, Map<String, Boolean>>> response = resource.healthCheck();

        assertThat(response.code(), is(503));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.body());

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.ping.healthy", is(true))
                .assertThat("$.deadlocks.healthy", is(false));
    }

}
