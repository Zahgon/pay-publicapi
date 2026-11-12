package uk.gov.pay.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.exception.ServiceUnavailableException;
import uk.gov.pay.api.model.publicauth.AuthResponse;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Asks public auth which gateway account an API key belongs to.
 */
@Singleton
public class AccountAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(AccountAuthenticator.class);

    private final RestClient client;
    private final String publicAuthUrl;

    public AccountAuthenticator(RestClient client, PublicApiConfig configuration) {
        this.client = client;
        this.publicAuthUrl = configuration.getPublicAuthUrl();
    }

    public Optional<Account> authenticate(String bearerToken) {

        RestClientResponse response = client.get(publicAuthUrl, Map.of(
                "Authorization", "Bearer " + bearerToken,
                "Accept", "application/json"));

        if (response.getStatus() == 200) {
            AuthResponse authResponse = response.readEntity(AuthResponse.class);
            logger.info(format("Successfully authenticated using API key with token_link %s", authResponse.getTokenLink()),
                    kv("token_link", authResponse.getTokenLink()));
            return Optional.of(new Account(authResponse.getAccountId(), authResponse.getTokenType(), authResponse.getTokenLink()));
        } else if (response.getStatus() == 401) {
            JsonNode unauthorisedResponse = response.readEntity(JsonNode.class);
            ErrorIdentifier errorIdentifier = ErrorIdentifier.valueOf(unauthorisedResponse.get("error_identifier").asText());
            if (errorIdentifier == ErrorIdentifier.AUTH_TOKEN_REVOKED) {
                String tokenLink = unauthorisedResponse.get("token_link").asText();
                logger.warn(format("Attempt to authenticate using revoked API key with token_link %s", tokenLink), kv("token_link", tokenLink));
            } else {
                logger.warn("Attempt to authenticate using invalid API key with valid checksum");
            }
            return Optional.empty();
        } else {
            logger.warn("Unexpected status code " + response.getStatus() + " from auth.");
            throw new ServiceUnavailableException();
        }
    }
}
