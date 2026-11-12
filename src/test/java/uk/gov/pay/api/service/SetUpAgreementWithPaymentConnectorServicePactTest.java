package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.CreateCardPaymentRequestBuilder;
import uk.gov.pay.api.model.CreatedPaymentWithAllLinks;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.links.Link;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;
import uk.gov.service.payments.commons.model.AgreementPaymentType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SetUpAgreementWithPaymentConnectorServicePactTest {

    private final Account ACCOUNT = new Account("123456", TokenPaymentType.CARD, "a-token-link");
    
    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    private ConnectorService connectorService;

    private CreatePaymentService createPaymentService;
    
    @Mock
    private PublicApiConfig mockConfiguration;

    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");
        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        connectorService = new ConnectorService(client, connectorUriGenerator);
        createPaymentService = new CreatePaymentService(client, publicApiUriGenerator, connectorUriGenerator);
    }
    
    @Test
    @Pacts(pacts = {"publicapi-connector-create-payment-to-setup-agreement"})
    public void setUpAnAgreementWithPayment() {
        var buildRequestPayload = CreateCardPaymentRequestBuilder.builder()
                .amount(1968)
                .returnUrl("https://www.google.com")
                .reference("a-valid-reference")
                .description("a-valid-description");
        
        buildRequestPayload.agreementPaymentType(AgreementPaymentType.INSTALMENT);
        buildRequestPayload.setUpAgreement("i6sjhoa36s1lhtjl07vuuhbm72");
        
        var requestPayload = buildRequestPayload.build();
        
        CreatedPaymentWithAllLinks createdPaymentWithAllLinks = createPaymentService.create(ACCOUNT, requestPayload, null);
        PaymentWithAllLinks paymentResponse = createdPaymentWithAllLinks.getPayment();
        
        assertThat(paymentResponse.getPaymentId(), is("iinvkbkkrt8kcl0atps9q7p7cm"));
        assertThat(paymentResponse.getAmount(), is(1968L));
        assertThat(paymentResponse.getReference(), is("a-valid-reference"));
        assertThat(paymentResponse.getDescription(), is("a-valid-description"));
        assertThat(paymentResponse.getAgreementId(), is("i6sjhoa36s1lhtjl07vuuhbm72"));
        assertThat(paymentResponse.getLinks().getNextUrl(), is(new Link("http://CardFrontend/secure/efbdf987-3c91-4005-b892-9d056a4bd414", "GET")));
        assertThat(paymentResponse.getAgreementPaymentType(), is(AgreementPaymentType.INSTALMENT));
    }
}
