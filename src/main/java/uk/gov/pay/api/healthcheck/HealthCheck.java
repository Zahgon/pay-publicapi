package uk.gov.pay.api.healthcheck;

/**
 * A named check of the service's health, run for each health check request.
 */
public interface HealthCheck {

    String name();

    boolean isHealthy();
}
