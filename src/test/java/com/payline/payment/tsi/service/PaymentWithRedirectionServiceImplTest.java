package com.payline.payment.tsi.service;

import com.payline.payment.tsi.exception.InvalidRequestException;
import com.payline.payment.tsi.request.TsiStatusCheckRequest;
import com.payline.payment.tsi.request.TsiStatusCheckRequestTest;
import com.payline.payment.tsi.response.TsiStatusCheckResponseTest;
import com.payline.payment.tsi.utils.http.JsonHttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.RedirectionPaymentRequest;
import com.payline.pmapi.bean.payment.request.TransactionStatusRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.PaymentResponseSuccess;
import okhttp3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith( MockitoJUnitRunner.class )
public class PaymentWithRedirectionServiceImplTest {

    private static final Protocol TEST_HTTP_PROTOCOL = Protocol.HTTP_1_1;

    @Mock private TsiStatusCheckRequest.Builder requestBuilder;
    @Mock private JsonHttpClient httpClient;

    @InjectMocks
    private PaymentWithRedirectionServiceImpl service;

    @Before
    public void mockRequestBuilder() throws InvalidRequestException {
        // In most cases, the RedirectionPaymentRequest-to-TsiStatusCheckRequest mapping is not what we want to test. So we mock it for every test.
        when( requestBuilder.fromRedirectionPaymentRequest( any( RedirectionPaymentRequest.class ) ) )
                .thenReturn( TsiStatusCheckRequestTest.sample() );
    }

    @Test
    public void testFinalizeRedirectionPayment_ok() throws IOException {
        // when: the HTTP call is a success
        Response response = this.mockResponse( 200, "OK", "OK", 0, "SUCCESSFUL TRANSACTION FOUND" );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.finalizeRedirectionPayment( mock( RedirectionPaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseSuccess
        Assert.assertTrue( paymentResponse instanceof PaymentResponseSuccess );
    }

    @Test
    public void testFinalizeRedirectionPayment_invalidRequest() throws InvalidRequestException {
        // when: the PaymentRequest is invalid, i.e. the builder throws an exception
        when( requestBuilder.fromRedirectionPaymentRequest( any( RedirectionPaymentRequest.class ) ) )
                .thenThrow( InvalidRequestException.class );
        PaymentResponse paymentResponse = service.finalizeRedirectionPayment( mock( RedirectionPaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.INVALID_DATA, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testFinalizeRedirectionPayment_notFound() throws IOException {
        // when: the HTTP call returns a business error ("transaction not found" for example)
        Response response = this.mockResponse( 200, "OK", "NOK", 1, "NO SUCCESSFUL TRANSACTIONS FOUND WITHIN 6 MONTHS" );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.finalizeRedirectionPayment( mock( RedirectionPaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.PAYMENT_PARTNER_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testFinalizeRedirectionPayment_businessError() throws IOException {
        // when: an error happened on the partner side during the HTTP call
        Response response = this.mockResponse( 200, "OK", "ER", 106, "MISSING MAC" );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.finalizeRedirectionPayment( mock( RedirectionPaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.PAYMENT_PARTNER_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }


    @Test
    public void testFinalizeRedirectionPayment_noResponseBody() throws IOException {
        // when: the HTTP call returns a response without a body
        Response response = this.mockResponse( 200, "OK", null, null, null );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.finalizeRedirectionPayment( mock( RedirectionPaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.INTERNAL_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testFinalizeRedirectionPayment_httpError() throws IOException {
        // when: the HTTP call an error (503 Service Unavailable for example)
        Response response = this.mockResponse( 503, "Service Unavailable", null, null, null );
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( response );
        PaymentResponse paymentResponse = service.finalizeRedirectionPayment( mock( RedirectionPaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.COMMUNICATION_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    @Test
    public void testFinalizeRedirectionPayment_ioException() throws IOException {
        // when: the HTTP call throws an exception
        when( httpClient.doPost( anyString(), anyString(), anyString(), anyString() ) )
                .thenThrow( IOException.class );
        PaymentResponse paymentResponse = service.finalizeRedirectionPayment( mock( RedirectionPaymentRequest.class, Mockito.RETURNS_DEEP_STUBS ) );

        // then: returned object is an instance of PaymentResponseFailure with the right failure cause
        Assert.assertTrue( paymentResponse instanceof PaymentResponseFailure );
        Assert.assertEquals( FailureCause.COMMUNICATION_ERROR, ((PaymentResponseFailure) paymentResponse).getFailureCause() );
    }

    /*
    Can't really do better than that given that the method doesn't do much...
     */
    @Test
    public void testHandleSessionExpired_notNull(){
        // when: handleSessionExpired is called
        PaymentResponse response = service.handleSessionExpired( mock( TransactionStatusRequest.class ) );

        // then: result is not null
        Assert.assertNotNull( response );
    }

    private Response mockResponse( int httpCode, String httpMessage, String status, Integer erCode, String message ){
        String jsonBody = null;
        String tid = "abcdefghijklmnopqrstuvwxyz123456";
        if( status == "OK" && erCode == 0 ){
            jsonBody = TsiStatusCheckResponseTest.mockJson( "1234567", tid, status, erCode.toString(),
                    message, "12,34", "f", "2018-08-02 10:37:22", "FRA" );
        }
        else if( status != null && erCode != null ){
            jsonBody = TsiStatusCheckResponseTest.mockJson( null, tid, status, erCode.toString(),
                    message, null, null, null, null );
        }

        return ( new Response.Builder() )
                .code( httpCode )
                .message( httpMessage )
                .body( jsonBody == null ? null : ResponseBody.create( MediaType.parse( "application/json" ), jsonBody ) )
                .request( (new Request.Builder()).url("http://fake.fr").build() )
                .protocol( TEST_HTTP_PROTOCOL )
                .build();
    }

}