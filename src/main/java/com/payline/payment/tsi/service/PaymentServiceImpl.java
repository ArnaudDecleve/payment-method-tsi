package com.payline.payment.tsi.service;

import com.payline.payment.tsi.error.ErrorCodesMap;
import com.payline.payment.tsi.exception.ExternalCommunicationException;
import com.payline.payment.tsi.exception.InvalidRequestException;
import com.payline.payment.tsi.request.TsiGoRequest;
import com.payline.payment.tsi.response.TsiGoResponse;
import com.payline.payment.tsi.utils.config.ConfigEnvironment;
import com.payline.payment.tsi.utils.config.ConfigProperties;
import com.payline.payment.tsi.utils.http.StringResponse;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;

public class PaymentServiceImpl extends AbstractPaymentHttpService<PaymentRequest> implements PaymentService {

    private static final Logger logger = LogManager.getLogger( PaymentServiceImpl.class );

    private TsiGoRequest.Builder requestBuilder;

    public PaymentServiceImpl() {
        super();
        this.requestBuilder = new TsiGoRequest.Builder();
    }

    @Override
    public PaymentResponse paymentRequest( PaymentRequest paymentRequest ) {
        return processRequest( paymentRequest );
    }

    @Override
    public StringResponse createSendRequest(PaymentRequest paymentRequest ) throws IOException, InvalidRequestException, GeneralSecurityException, URISyntaxException, ExternalCommunicationException {
        // Create Go request from Payline request
        TsiGoRequest tsiGoRequest = requestBuilder.fromPaymentRequest( paymentRequest );

        // Send Go request
        ConfigEnvironment env = Boolean.FALSE.equals( paymentRequest.getPaylineEnvironment().isSandbox() ) ? ConfigEnvironment.PROD : ConfigEnvironment.TEST;
        String scheme = ConfigProperties.get( "tsi.scheme", env );
        String host = ConfigProperties.get( "tsi.host", env );
        String path = ConfigProperties.get( "tsi.go.path", env );
        return getHttpClient().doPost( scheme, host, path, tsiGoRequest.buildBody() );
    }

    @Override
    public PaymentResponse processResponse(final StringResponse response) throws IOException {
        // Parse response
        final TsiGoResponse tsiGoResponse = (new TsiGoResponse.Builder()).fromJson(response.getContent());

        // If status == 1, proceed with the redirection
        if( tsiGoResponse.getStatus() == 1 ){
            String redirectUrl = tsiGoResponse.getUrl();

            PaymentResponseRedirect.RedirectionRequest redirectionRequest = new PaymentResponseRedirect.RedirectionRequest( new URL( redirectUrl ) );
            return PaymentResponseRedirect.PaymentResponseRedirectBuilder.aPaymentResponseRedirect()
                    .withRedirectionRequest( redirectionRequest )
                    .withTransactionIdentifier( tsiGoResponse.getTid() )
                    .build();
        }
        else {
            logger.error( "TSI Go request returned an error: " + tsiGoResponse.getMessage() + "(" + Integer.toString( tsiGoResponse.getStatus() ) + ")" );
            return buildPaymentResponseFailure( tsiGoResponse.getMessage(), ErrorCodesMap.getFailureCause( tsiGoResponse.getStatus() ) );
        }
    }
}
