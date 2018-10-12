package com.payline.payment.tsi.service;

import com.payline.pmapi.bean.paymentform.bean.PaymentFormLogo;
import com.payline.pmapi.bean.paymentform.request.PaymentFormConfigurationRequest;
import com.payline.pmapi.bean.paymentform.request.PaymentFormLogoRequest;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import com.payline.pmapi.bean.paymentform.response.logo.PaymentFormLogoResponse;
import com.payline.pmapi.bean.paymentform.response.logo.impl.PaymentFormLogoResponseFile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

import static org.mockito.Mockito.mock;

@RunWith( MockitoJUnitRunner.class )
public class PaymentFormConfigurationServiceImplTest {

    @InjectMocks
    private PaymentFormConfigurationServiceImpl service;

    @Test
    public void testGetPaymentFormConfiguration(){
        // when: getPaymentFormConfiguration is called
        final PaymentFormConfigurationRequest mock = mock(PaymentFormConfigurationRequest.class);
        Mockito.when(mock.getLocale()).thenReturn(Locale.FRANCE);
        PaymentFormConfigurationResponse response = service.getPaymentFormConfiguration(mock);

        // then: returned object is an instance of PaymentFormConfigurationResponseProvided
        Assert.assertTrue( response instanceof PaymentFormConfigurationResponseSpecific);
    }

    @Test
    public void testGetLogo() throws IOException {
        // when: getLogo is called
        PaymentFormLogo paymentFormLogo = service.getLogo("", Locale.getDefault() );

        // then: returned elements are not null
        Assert.assertNotNull( paymentFormLogo );
        Assert.assertNotNull( paymentFormLogo.getFile() );
        Assert.assertNotNull( paymentFormLogo.getContentType() );
    }

    @Test
    public void testGetPaymentFormLogo() throws IOException {
        // given: the logo image read from resources
        String filename = "ticketpremium-logo.png";
        InputStream input = PaymentFormConfigurationServiceImpl.class.getClassLoader().getResourceAsStream( filename );
        BufferedImage image = ImageIO.read( input );
        String guessedContentType = Files.probeContentType( new File( filename ).toPath() );

        // when: getPaymentFormLogo is called
        PaymentFormLogoRequest request = PaymentFormLogoRequest.PaymentFormLogoRequestBuilder.aPaymentFormLogoRequest()
                .withLocale( Locale.getDefault() )
                .build();
        PaymentFormLogoResponse paymentFormLogoResponse = service.getPaymentFormLogo( request );

        // then: returned elements match the image file data
        Assert.assertTrue( paymentFormLogoResponse instanceof PaymentFormLogoResponseFile);
        PaymentFormLogoResponseFile casted = (PaymentFormLogoResponseFile) paymentFormLogoResponse;
        Assert.assertEquals( image.getHeight(), casted.getHeight() );
        Assert.assertEquals( image.getWidth(), casted.getWidth() );
        Assert.assertNotNull( casted.getTitle() );
        Assert.assertNotNull( casted.getAlt() );
    }

}
