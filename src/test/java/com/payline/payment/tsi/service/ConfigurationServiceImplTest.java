package com.payline.payment.tsi.service;

import com.payline.payment.tsi.TsiConstants;
import com.payline.payment.tsi.exception.ExternalCommunicationException;
import com.payline.payment.tsi.response.TsiGoResponseTest;
import com.payline.payment.tsi.utils.http.JsonHttpClient;
import com.payline.payment.tsi.utils.http.ResponseMocker;
import com.payline.payment.tsi.utils.http.StringResponse;
import com.payline.pmapi.bean.configuration.ReleaseInformation;
import com.payline.pmapi.bean.configuration.parameter.AbstractParameter;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.PaylineEnvironment;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class ConfigurationServiceImplTest {

    @Mock
    private JsonHttpClient httpClient;

    @InjectMocks
    private ConfigurationServiceImpl service;

    private Map<String, String> parameters;

    @Before
    public void setup(){
        // Initialize default format-valid parameters
        parameters = new HashMap<>();
        parameters.put( TsiConstants.CONTRACT_MERCHANT_ID, "123" );
        parameters.put( TsiConstants.CONTRACT_KEY_VALUE, "secret" );
        parameters.put( TsiConstants.CONTRACT_KEY_ID, "123" );
        parameters.put( TsiConstants.CONTRACT_PRODUCT_DESCRIPTION, "Ticket Premium" );
    }

    @Test
    public void testGetParameters(){
        // when: recovering contract parameters
        List<AbstractParameter> parameters = service.getParameters( Locale.FRANCE );

        // then: exactly 2 parameters are returned
        Assert.assertEquals( 4, parameters.size() );
    }

    @Test
    public void testCheck_ok() throws IOException, URISyntaxException, ExternalCommunicationException {
        // given: valid contract properties (TSI should then respond with a status=1)
        ContractParametersCheckRequest checkRequest = ConfigurationServiceImplTest.setupCheckRequest( parameters );
        String responseBody = TsiGoResponseTest.mockJson( 1, "OK", "http://redirect-url.com", null, null );
        StringResponse response = ResponseMocker.mockString( 200, "OK", responseBody );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );

        // when: checking configuration fields values
        Map<String, String> errors = service.check( checkRequest );

        // then: result contains no error
        Assert.assertEquals( 0, errors.size() );
    }

    @Test
    public void testCheck_wrongAccountData() throws IOException, URISyntaxException, ExternalCommunicationException {
        // given: contract properties with the right format but not valid (TSI should then respond with a status != 1)
        ContractParametersCheckRequest checkRequest = ConfigurationServiceImplTest.setupCheckRequest( parameters );
        String responseBody = TsiGoResponseTest.mockJson( 15, "WRONG MAC", null, null, null );
        StringResponse response = ResponseMocker.mockString( 200, "OK", responseBody );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );

        // when: checking configuration fields values
        Map<String, String> errors = service.check( checkRequest );

        // then: result contains 1 error
        Assert.assertEquals( 1, errors.size() );
    }

    @Test
    public void testCheck_unknownError() throws IOException, URISyntaxException, ExternalCommunicationException {
        // given: contract properties validation encounter an unexpected error (Server unavailable for example)
        ContractParametersCheckRequest checkRequest = ConfigurationServiceImplTest.setupCheckRequest( parameters );
        StringResponse response = ResponseMocker.mockString( 503, "Server Unavailable", null );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );

        // when: checking configuration fields values
        Map<String, String> errors = service.check( checkRequest );

        // then: result contains 3 errors, one for each non-validated field
        Assert.assertEquals( 3, errors.size() );
    }

    @Test
    public void testCheck_incorrectMerchantId(){
        // given: a non-integer merchant id
        parameters.put( TsiConstants.CONTRACT_MERCHANT_ID, "abc" );
        ContractParametersCheckRequest checkRequest = setupCheckRequest( parameters );

        // when: checking configuration fields values
        Map<String, String> errors = service.check( checkRequest );

        // then: result contains 1 error
        Assert.assertEquals( 1, errors.size() );
    }

    @Test
    public void testCheck_incorrectKeyId(){
        // given: a non-integer key id
        parameters.put( TsiConstants.CONTRACT_KEY_ID, "ABC" );
        ContractParametersCheckRequest checkRequest = setupCheckRequest( parameters );

        // when: checking configuration fields values
        Map<String, String> errors = service.check( checkRequest );

        // then: result contains 1 error
        Assert.assertEquals( 1, errors.size() );
    }

    @Test
    public void testGetReleaseInformation_ok(){
        // when: getReleaseInformation method is called
        ReleaseInformation releaseInformation = service.getReleaseInformation();

        // then: result is not null
        Assert.assertNotNull( releaseInformation );
        Assert.assertNotEquals( "unknown", releaseInformation.getVersion() );
        Assert.assertNotEquals( 1900, releaseInformation.getDate().getYear() );
    }

    @Test
    public void testGetReleaseInformation_versionFormat(){
        // when: getReleaseInformation method is called
        ReleaseInformation releaseInformation = service.getReleaseInformation();

        // then: the version has a valid format
        Assert.assertNotNull( releaseInformation );
        Assert.assertTrue( releaseInformation.getVersion().matches( "^\\d\\.\\d(\\.\\d)?$" ) );
    }

    // TODO: Improve this test case ! Testing the result is not empty is not enough.
    @Test
    public void testGetName_notNull(){
        // when: getReleaseInformation method is called
        String name = service.getName( Locale.FRANCE );

        // then: result is not null and not empty
        Assert.assertNotNull( name );
        Assert.assertFalse( name.isEmpty() );
    }

    @Test
    public void testIsInteger_digits(){
        Assert.assertTrue( service.isInteger( "1234567890" ) );
    }

    @Test
    public void testIsInteger_zero(){
        Assert.assertTrue( service.isInteger( "0" ) );
    }

    @Test
    public void testIsInteger_null(){
        Assert.assertFalse( service.isInteger( null ) );
    }

    @Test
    public void testIsInteger_empty(){
        Assert.assertFalse( service.isInteger( "" ) );
    }

    @Test
    public void testIsInteger_negative(){
        Assert.assertFalse( service.isInteger( "-123" ) );
    }


    static ContractParametersCheckRequest setupCheckRequest( Map<String, String> accountInfo ){
        return ContractParametersCheckRequest.CheckRequestBuilder.aCheckRequest()
                .withAccountInfo( accountInfo )
                .withContractConfiguration( new ContractConfiguration( null, null ) )
                .withPaylineEnvironment( new PaylineEnvironment( "", "", "", true ) )
                .withLocale( Locale.FRANCE )
                .build();
    }

}
