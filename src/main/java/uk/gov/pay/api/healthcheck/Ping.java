package uk.gov.pay.api.healthcheck;

import jakarta.inject.Singleton;

@Singleton
public class Ping implements HealthCheck {

    @Override
    public String name() {
        return "ping";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
