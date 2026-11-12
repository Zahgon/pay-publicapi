package uk.gov.pay.api.service;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.agreement.model.AgreementCreatedResponse;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.agreement.service.AgreementsService;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.CreateAgreementRequestBuilder;
import uk.gov.pay.api.model.search.PaginationDecorator;
import uk.gov.pay.api.utils.mocks.CreateAgreementRequestParams;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.utils.Payloads.agreementPayload;
import static uk.gov.pay.api.utils.mocks.CreateAgreementRequestParams.CreateAgreementRequestParamsBuilder.aCreateAgreementRequestParams;

@ExtendWith(MockitoExtension.class)
class AgreementsServiceTest {

    private static final String REFERENCE_ID = "test";
    private static final String AGREEMENT_ID = "12345678901234567890123456";
    private static final String AGREEMENTS_CONNECTOR_URI = "/v1/api/accounts/GATEWAY_ACCOUNT_ID/agreements"; // pragma: allowlist secret
    private static final String AGREEMENT_CONNECTOR_CANCEL_URI = AGREEMENTS_CONNECTOR_URI + '/' + AGREEMENT_ID + "/cancel";
    @Mock
    private Account mockAccount;
    @Mock
    private ConnectorUriGenerator mockConnectorUriGenerator;
    @Mock
    private LedgerUriGenerator mockLedgerUriGenerator;
    @Mock
    private RestClient mockClient;
    @Mock
    private PublicApiConfig mockConfiguration;
    @Mock
    private RestClientResponse mockConnectorResponse;

    private AgreementsService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AgreementsService(new ConnectorService(mockClient, mockConnectorUriGenerator),
                new LedgerService(mockClient, mockLedgerUriGenerator), new PaginationDecorator(mockConfiguration));
    }

    @Test
    void shouldCreateAgreement() {
        when(mockConnectorUriGenerator.getAgreementURI(mockAccount)).thenReturn(AGREEMENTS_CONNECTOR_URI);

        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference(REFERENCE_ID)
                .build();
        when(mockClient.post(AGREEMENTS_CONNECTOR_URI, Map.of("Accept", "application/json"),
                agreementPayload(createAgreementRequestParams))).thenReturn(mockConnectorResponse);

        when(mockConnectorResponse.getStatus()).thenReturn(HttpStatus.SC_CREATED);

        var agreementResponse = new AgreementCreatedResponse(AGREEMENT_ID);
        when(mockConnectorResponse.readEntity(AgreementCreatedResponse.class)).thenReturn(agreementResponse);

        CreateAgreementRequest agreementCreateRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder
                .builder().reference(REFERENCE_ID));
        AgreementCreatedResponse agreementResponseFromService = underTest.createAgreement(mockAccount, agreementCreateRequest);

        assertThat(agreementResponseFromService.getAgreementId(), is(AGREEMENT_ID));
    }

    @Test
    void shouldThrowExceptionIfConnectorReturnsRecurringPaymentsNotAllowedExceptionWhenCreatingAgreement() {
        when(mockConnectorUriGenerator.getAgreementURI(mockAccount)).thenReturn(AGREEMENTS_CONNECTOR_URI);

        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference(REFERENCE_ID)
                .build();
        when(mockClient.post(AGREEMENTS_CONNECTOR_URI, Map.of("Accept", "application/json"),
                agreementPayload(createAgreementRequestParams))).thenReturn(mockConnectorResponse);

        when(mockConnectorResponse.getStatus()).thenReturn(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        CreateAgreementRequest agreementCreateRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder
                .builder().reference(REFERENCE_ID));
        assertThrows(CreateAgreementException.class, () -> underTest.createAgreement(mockAccount, agreementCreateRequest));

    }

    @Test
    void shouldCancelAgreement() {
        when(mockConnectorUriGenerator.cancelAgreementURI(mockAccount, AGREEMENT_ID)).thenReturn(AGREEMENT_CONNECTOR_CANCEL_URI);
        when(mockClient.post(AGREEMENT_CONNECTOR_CANCEL_URI, null)).thenReturn(mockConnectorResponse);
        when(mockConnectorResponse.getStatus()).thenReturn(HttpStatus.SC_NO_CONTENT);

        assertDoesNotThrow(() -> underTest.cancelAgreement(mockAccount, AGREEMENT_ID));

        verify(mockClient).post(AGREEMENT_CONNECTOR_CANCEL_URI, null);
    }

    @Test
    void shouldThrowExceptionIfConnectorReturnsUnexpectedStatusCodeWhenCancellingAgreement() {
        when(mockConnectorUriGenerator.cancelAgreementURI(mockAccount, AGREEMENT_ID)).thenReturn(AGREEMENT_CONNECTOR_CANCEL_URI);
        when(mockClient.post(AGREEMENT_CONNECTOR_CANCEL_URI, null)).thenReturn(mockConnectorResponse);
        when(mockConnectorResponse.getStatus()).thenReturn(HttpStatus.SC_BAD_REQUEST);

        assertThrows(CancelAgreementException.class, () -> underTest.cancelAgreement(mockAccount, AGREEMENT_ID));

    }
}
