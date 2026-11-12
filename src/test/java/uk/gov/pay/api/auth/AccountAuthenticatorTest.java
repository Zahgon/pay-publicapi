package uk.gov.pay.api.auth;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.exception.ServiceUnavailableException;
import uk.gov.pay.api.model.publicauth.AuthResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.AUTHORIZATION;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTH_TOKEN_INVALID;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTH_TOKEN_REVOKED;

@ExtendWith(MockitoExtension.class)
public class AccountAuthenticatorTest {

    private AccountAuthenticator accountAuthenticator;
    private ObjectMapper objectMapper = new ObjectMapper();

    private final String bearerToken = "aaa";
    private final String accountId = "accountId";
    
    @Mock
    private RestClient publicAuthMock;

    @Mock 
    private RestClientResponse mockResponse;
    
    @Mock
    private PublicApiConfig mockConfiguration;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(AccountAuthenticator.class);
        logger.setLevel(Level.INFO);
        logger.addAppender(mockAppender);
        
        when(mockConfiguration.getPublicAuthUrl()).thenReturn("/v1/api/auth");
        accountAuthenticator = new AccountAuthenticator(publicAuthMock, mockConfiguration);
        when(publicAuthMock.get("/v1/api/auth", Map.of(
                AUTHORIZATION, "Bearer " + bearerToken,
                ACCEPT, APPLICATION_JSON))).thenReturn(mockResponse);
    }

    @Test
    public void shouldReturnValidAccount() {
        AuthResponse authResponse = new AuthResponse(accountId, "a-token-link", CARD);
        when(mockResponse.getStatus()).thenReturn(OK.getCode());
        when(mockResponse.readEntity(AuthResponse.class)).thenReturn(authResponse);
        Optional<Account> maybeAccount = accountAuthenticator.authenticate(bearerToken);
        assertThat(maybeAccount.get().getName(), is(accountId));
        assertThat(maybeAccount.get().accountId(), is(accountId));
        assertThat(maybeAccount.get().paymentType(), is(CARD));
        
        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(logEvents, hasSize(1));
        assertThat(logEvents.get(0).getFormattedMessage(), is("Successfully authenticated using API key with token_link a-token-link"));
    }

    @Test
    public void shouldNotReturnAccount_ifUnauthorisedDueToTokenRevoked() {
        Map<String, String> responseEntity = ImmutableMap.of(
                "error_identifier", AUTH_TOKEN_REVOKED.toString(),
                "token_link", "a-token-link"
        );
        JsonNode response = objectMapper.valueToTree(responseEntity);
        when(mockResponse.getStatus()).thenReturn(UNAUTHORIZED.getCode());
        when(mockResponse.readEntity(JsonNode.class)).thenReturn(response);
        Optional<Account> maybeAccount = accountAuthenticator.authenticate(bearerToken);
        assertThat(maybeAccount.isPresent(), is(false));

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(logEvents, hasSize(1));
        assertThat(logEvents.get(0).getFormattedMessage(), is("Attempt to authenticate using revoked API key with token_link a-token-link"));
    }

    @Test
    public void shouldNotReturnAccount_ifUnauthorisedDueToInvalidToken() {
        Map<String, String> responseEntity = ImmutableMap.of(
                "error_identifier", AUTH_TOKEN_INVALID.toString()
        );
        JsonNode response = objectMapper.valueToTree(responseEntity);
        when(mockResponse.getStatus()).thenReturn(UNAUTHORIZED.getCode());
        when(mockResponse.readEntity(JsonNode.class)).thenReturn(response);
        Optional<Account> maybeAccount = accountAuthenticator.authenticate(bearerToken);
        assertThat(maybeAccount.isPresent(), is(false));

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(logEvents, hasSize(1));
        assertThat(logEvents.get(0).getFormattedMessage(), is("Attempt to authenticate using invalid API key with valid checksum"));
    }

    @Test
    public void shouldThrow_ifUnknownResponse() {
        when(mockResponse.getStatus()).thenReturn(NOT_FOUND.getCode());
        assertThrows(ServiceUnavailableException.class, () -> accountAuthenticator.authenticate(bearerToken));
    }
}
