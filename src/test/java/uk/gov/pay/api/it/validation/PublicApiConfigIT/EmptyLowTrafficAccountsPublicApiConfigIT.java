package uk.gov.pay.api.it.validation.PublicApiConfigIT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.api.app.config.RateLimiterConfig;
import uk.gov.pay.api.it.support.PublicApiTestApp;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EmptyLowTrafficAccountsPublicApiConfigIT {

    @RegisterExtension
    private static final PublicApiTestApp EXT = PublicApiTestApp.withConfig("config/empty-low-traffic-accounts-test-config.yaml");

    @Test
    void shouldParseRateLimitConfigurationForLowTrafficAccounts() {
        RateLimiterConfig rateLimiterConfig = EXT.getConfiguration().getRateLimiterConfig();
        assertThat(rateLimiterConfig.getNoOfReqForLowTrafficAccounts(), is(4500));
        assertThat(rateLimiterConfig.getNoOfPostReqForLowTrafficAccounts(), is(2));
        assertThat(rateLimiterConfig.getIntervalInMillisForLowTrafficAccounts(), is(60000));
        assertThat(rateLimiterConfig.getLowTrafficAccounts(), is(Collections.emptyList()));
    }
}
