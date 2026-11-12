package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.agreement.model.AgreementCreatedResponse;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.model.CreateAgreementRequestBuilder;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.RECURRING_CARD_PAYMENTS_NOT_ALLOWED;

@ExtendWith(MockitoExtension.class)
public class CreateAgreementLedgerServicePactTest {

    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    @Mock
    private PublicApiConfig configuration;

    private ConnectorService connectorService;

    private Account account;

    @BeforeEach
    public void setup() {
        when(configuration.getConnectorUrl()).thenReturn(connectorRule.getUrl());

        RestClient client = RestClients.newRestClient(false);

        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(configuration);
        connectorService = new ConnectorService(client, connectorUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-create-agreement"})
    public void shouldCreateAgreement() {
        account = new Account("123456", TokenPaymentType.CARD, "a-token-link");
        CreateAgreementRequest agreementCreateRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder
                .builder()
                .description("Description for the paying user describing the purpose of the agreement")
                .userIdentifier("reference for the paying user")
                .reference("Service agreement reference"));

        AgreementCreatedResponse agreementResponse = connectorService.createAgreement(account, agreementCreateRequest);

        assertThat(agreementResponse.getAgreementId(), is("1jikqomeib6j18vp2i153b9dtu"));
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-create-agreement-for-non-rcp-account"})
    public void createAgreementForNonRCPAccountShouldReturnError() {
        account = new Account("123456789", TokenPaymentType.CARD, "a-token-link");
        CreateAgreementRequest agreementCreateRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder
                .builder()
                .description("Description for the paying user")
                .userIdentifier("reference for the paying user")
                .reference("Service agreement reference"));

        CreateAgreementException exception = assertThrows(CreateAgreementException.class, () -> {
            connectorService.createAgreement(account, agreementCreateRequest);
        });

        assertThat(exception.getErrorIdentifier(), is(RECURRING_CARD_PAYMENTS_NOT_ALLOWED));
    }
}
