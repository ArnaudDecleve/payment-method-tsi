package com.payline.payment.tsi.service;

import com.payline.payment.tsi.error.ErrorCodesMap;
import com.payline.payment.tsi.exception.ExternalCommunicationException;
import com.payline.payment.tsi.exception.InvalidRequestException;
import com.payline.payment.tsi.request.TsiGoRequest;
import com.payline.payment.tsi.request.TsiGoRequestTest;
import com.payline.payment.tsi.response.TsiGoResponseTest;
import com.payline.payment.tsi.utils.http.JsonHttpClient;
import com.payline.payment.tsi.utils.http.ResponseMocker;
import com.payline.payment.tsi.utils.http.StringResponse;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class PaymentServiceImplTest {

    @Mock private TsiGoRequest.Builder requestBuilder;
    @Mock private JsonHttpClient httpClient;

    @InjectMocks
    private PaymentServiceImpl service;

    @Before
    public void mockRequestBuilder() throws InvalidRequestException, NoSuchAlgorithmException {
        // In most cases, the PaymentRequest-to-TsiGoRequest mapping is not what we want to test. So we mock it for every test.
        when( requestBuilder.fromPaymentRequest( any( PaymentRequest.class ) ) )
                .thenReturn( TsiGoRequestTest.sample() );
    }

    @Test
    public void testPaymentRequest_ok() throws IOException, URISyntaxException, ExternalCommunicationException {
        // when: the HTTP call is a success
        String content = TsiGoResponseTest.mockJson( 1, "OK", "http://redirect-url.com", "123", null );
        StringResponse response = ResponseMocker.mockString( 200, "OK", content );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.paymentRequest( mock( PaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseRedirect
        Assert.assertTrue( paymentResponse instanceof PaymentResponseRedirect);
    }

    @Test
    public void testPaymentRequest_invalidRequest() throws InvalidRequestException, NoSuchAlgorithmException {
        // when: the PaymentRequest is invalid, i.e. the builder throws an exception
        when( requestBuilder.fromPaymentRequest( any( PaymentRequest.class ) ) )
                .thenThrow( InvalidRequestException.class );
        PaymentResponse paymentResponse = service.paymentRequest( mock( PaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure);
        Assert.assertEquals( FailureCause.INVALID_DATA, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testPaymentRequest_businessError() throws IOException, URISyntaxException, ExternalCommunicationException {
        // when: the HTTP call returns a business error (wrong HMAC for example)
        String content = TsiGoResponseTest.mockJson( 15, "WRONG HMAC", null, null, null );
        StringResponse response = ResponseMocker.mockString( 200, "OK", content );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.paymentRequest( mock( PaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( ErrorCodesMap.getFailureCause( 15 ), ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testPaymentRequest_noResponseBody() throws IOException, URISyntaxException, ExternalCommunicationException {
        // when: the HTTP call returns a response without body
        StringResponse response = ResponseMocker.mockString( 200, "OK", null );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.paymentRequest( mock( PaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.INTERNAL_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testPaymentRequest_httpError() throws IOException, URISyntaxException, ExternalCommunicationException {
        // when: the HTTP call returns a HTTP error (503 Service Unavailable par example)
        StringResponse response = ResponseMocker.mockString( 503, "Service Unavailable", "ERROR!" );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.paymentRequest( mock( PaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.COMMUNICATION_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testPaymentRequest_ioException() throws IOException, URISyntaxException, ExternalCommunicationException {
        // when: the HTTP call throws an exception
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenThrow( IOException.class );
        PaymentResponse paymentResponse = service.paymentRequest( mock( PaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.COMMUNICATION_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }



}
