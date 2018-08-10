package com.payline.payment.tsi;

import com.payline.payment.tsi.service.PaymentServiceImpl;
import com.payline.payment.tsi.service.PaymentWithRedirectionServiceImpl;
import com.payline.payment.tsi.utils.config.ConfigProperties;
import com.payline.pmapi.AbstractPaymentIntegration;
import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.payment.*;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Integration test to verify that a full payment works.
 * It requires the Google Chrome Driver to be in the path.
 */
public class TsiPaymentIT extends AbstractPaymentIntegration {

    private PaymentServiceImpl paymentService;
    private PaymentWithRedirectionServiceImpl paymentWithRedirectionService;
    private Properties testConfig;

    @Before
    public void setup() throws IOException {
        paymentService = new PaymentServiceImpl();
        paymentWithRedirectionService = new PaymentWithRedirectionServiceImpl();

        testConfig = new Properties();
        testConfig.load( ConfigProperties.class.getClassLoader().getResourceAsStream( "testConfig.properties" ) );
    }

    @Test
    public void testFullRedirectionPayment() {
        super.fullRedirectionPayment( this.createDefaultPaymentRequest(), paymentService, paymentWithRedirectionService );
    }

    @Override
    protected Map<String, ContractProperty> generateParameterContract() {
        Map<String, ContractProperty> contractProperties = new HashMap<>();
        contractProperties.put( TsiConstants.CONTRACT_MERCHANT_ID, new ContractProperty( this.testConfig.getProperty( "contractConfiguration.merchantId" ) ) );
        contractProperties.put( TsiConstants.CONTRACT_KEY_VALUE, new ContractProperty( this.testConfig.getProperty( "contractConfiguration.keyValue" ) ) );
        contractProperties.put( TsiConstants.CONTRACT_KEY_ID, new ContractProperty( this.testConfig.getProperty( "contractConfiguration.keyId" ) ) );
        contractProperties.put( TsiConstants.CONTRACT_PRODUCT_DESCRIPTION, new ContractProperty( this.testConfig.getProperty( "contractConfiguration.productDescription" ) ) );
        return contractProperties;
    }

    @Override
    protected Map<String, Serializable> generatePaymentFormData() {
        // Don't know what's this for
        return null;
    }

    @Override
    protected String payOnPartnerWebsite( String partnerUrl ) {
        String finalUrl = null;

        // Configure private mode
        ChromeOptions options = new ChromeOptions();
        options.addArguments( "--incognito" );

        // Start browser
        WebDriver driver = new ChromeDriver( options );
        try {
            driver.manage().timeouts().implicitlyWait( 10, TimeUnit.SECONDS );

            // Go to partner's website
            driver.get( partnerUrl );

            // Login
            driver.findElement( By.id( "email" ) ).sendKeys( this.testConfig.getProperty( "tsi.login" ) );
            driver.findElement( By.id( "btn-submit" ) ).click();
            driver.findElement( By.id( "password-field" ) ).sendKeys( this.testConfig.getProperty( "tsi.password" ) );
            driver.findElement( By.id( "btn-submit" ) ).click();


            // Enter ticket premium code
            driver.findElement( By.id( "PIN" ) ).sendKeys( this.testConfig.getProperty( "tsi.pin" ) );
            driver.findElement( By.name( "SEND" ) ).click();

            // Wait for redirection to success or cancel url
            WebDriverWait wait = new WebDriverWait( driver, 60 );
            wait.until( ExpectedConditions.or( ExpectedConditions.urlToBe( SUCCESS_URL ), ExpectedConditions.urlToBe( CANCEL_URL ) ) );
            finalUrl = driver.getCurrentUrl();

        } finally {
            // Stop browser
            driver.quit();

            return finalUrl;
        }
    }

    @Override
    protected String cancelOnPartnerWebsite( String partnerUrl ) {
        // Can't cancel on TSI
        return null;
    }

    /**
     * Overrides super's method only to configure a variable transaction id.
     *
     * @return the paymentRequest created
     */
    @Override
    public PaymentRequest createDefaultPaymentRequest() {
        // Override BEGINS here ---
        final Amount amount = new Amount( BigInteger.valueOf( 150 ), Currency.getInstance( "EUR" ) );
        // Override ENDS here ---
        final ContractConfiguration contractConfiguration = new ContractConfiguration( "", this.generateParameterContract() );
        final Map<String, Serializable> paymentFormData = this.generatePaymentFormData();
        final PaylineEnvironment paylineEnvironment = new PaylineEnvironment( NOTIFICATION_URL, SUCCESS_URL, CANCEL_URL, true );
        // Override BEGINS here ---
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyyMMddHHmmss" );
        final String transactionID = "TSI" + LocalDateTime.now().format( formatter );
        // Override ENDS here ---
        final Order order = Order.OrderBuilder.anOrder().withReference( transactionID ).build();
        final String softDescriptor = "softDescriptor";

        return PaymentRequest.builder()
                .withAmount( amount )
                .withBrowser( new Browser( "", Locale.FRANCE ) )
                .withContractConfiguration( contractConfiguration )
                .withPaymentFormData( paymentFormData )
                .withPaylineEnvironment( paylineEnvironment )
                .withOrder( order )
                .withTransactionId( transactionID )
                .withSoftDescriptor( softDescriptor )
                .build();
    }
}