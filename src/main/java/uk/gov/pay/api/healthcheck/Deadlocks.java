package uk.gov.pay.api.healthcheck;

import jakarta.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Unhealthy while any threads are deadlocked.
 */
@Singleton
public class Deadlocks implements HealthCheck {

    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();

    @Override
    public String name() {
        return "deadlocks";
    }

    @Override
    public boolean isHealthy() {
        return threads.findDeadlockedThreads() == null;
    }
}
