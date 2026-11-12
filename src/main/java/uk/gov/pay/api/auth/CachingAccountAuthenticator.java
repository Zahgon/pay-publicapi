package uk.gov.pay.api.auth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import uk.gov.pay.api.app.config.PublicApiConfig;

import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Remembers the accounts of API keys for the authentication cache policy; a key public auth refuses is asked about
 * again next time.
 */
@Singleton
public class CachingAccountAuthenticator {

    private final LoadingCache<String, Account> cache;

    public CachingAccountAuthenticator(AccountAuthenticator authenticator, PublicApiConfig configuration) {
        this.cache = Caffeine.from(configuration.getAuthenticationCachePolicy())
                .build(credentials -> authenticator.authenticate(credentials).orElse(null));
    }

    public Optional<Account> authenticate(String credentials) {
        try {
            return Optional.ofNullable(cache.get(credentials));
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }
}
