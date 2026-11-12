package uk.gov.pay.api.it.validation.PublicApiConfigIT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.api.app.config.RateLimiterConfig;
import uk.gov.pay.api.it.support.PublicApiTestApp;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class EmptyElevatedAccountsPublicApiConfigIT {

    @RegisterExtension
    private static final PublicApiTestApp EXT = PublicApiTestApp.withConfig("config/empty-elevated-accounts-test-config.yaml");

    @Test
    void shouldParseConfiguration() {
        RateLimiterConfig rateLimiterConfig = EXT.getConfiguration().getRateLimiterConfig();
        assertThat(rateLimiterConfig.getNoOfReq(), is(1000));
        assertThat(rateLimiterConfig.getPerMillis(), is(1000));
        assertThat(rateLimiterConfig.getNoOfReqForPost(), is(1000));
        assertThat(rateLimiterConfig.getNoOfReqPerNode(), is(1));
        assertThat(rateLimiterConfig.getNoOfReqForPostPerNode(), is(1));
        assertThat(rateLimiterConfig.getNoOfReqForElevatedAccounts(), is(1000));
        assertThat(rateLimiterConfig.getNoOfPostReqForElevatedAccounts(), is(1000));
        assertThat(rateLimiterConfig.getElevatedAccounts(), is(Collections.emptyList()));
    }
}
