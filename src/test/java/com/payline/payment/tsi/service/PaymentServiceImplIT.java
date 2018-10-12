package com.payline.payment.tsi.service;

import com.payline.payment.tsi.TsiConstants;
import com.payline.payment.tsi.exception.ExternalCommunicationException;
import com.payline.payment.tsi.exception.InvalidRequestException;
import com.payline.payment.tsi.utils.http.StringResponse;
import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.common.Buyer;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.ContractProperty;
import com.payline.pmapi.bean.payment.Order;
import com.payline.pmapi.bean.payment.PaylineEnvironment;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

class PaymentServiceImplIT {

    private PaymentServiceImpl paymentService = new PaymentServiceImpl();


    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImplIT.class);

    @BeforeEach
    void setUp() {
        Configurator.setRootLevel(Level.DEBUG);
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "5000");
        System.setProperty("https.proxyHost", "localhost");
        System.setProperty("https.proxyPort", "5000");
    }

    @Test
    void createSendRequest() throws InterruptedException {
        final HashMap<String, ContractProperty> contractProperties = new HashMap<>();
        contractProperties.put(TsiConstants.CONTRACT_MERCHANT_ID, new ContractProperty("806"));
        contractProperties.put(TsiConstants.CONTRACT_KEY_ID, new ContractProperty("806"));
        contractProperties.put(TsiConstants.CONTRACT_KEY_VALUE, new ContractProperty("45f3bcf660df19f8364c222e887300fa"));
        contractProperties.put(TsiConstants.CONTRACT_PRODUCT_DESCRIPTION, new ContractProperty("Ticket Premium"));
        final HashMap<String, String> partnerConfigurationMap = new HashMap<>();
        final HashMap<String, String> sensitivePartnerConfigurationMap = new HashMap<>();
        final Amount amount = new Amount(BigInteger.valueOf(10000), Currency.getInstance("EUR"));

        final String environmentURL = "https://payline.com";
        final Buyer buyer = Buyer.BuyerBuilder.aBuyer()
                .withEmail("charlelie.bouvier@monext.net")
                .build();


        final Thread thread = new Thread(() -> {
            int count = 0;
            while (true) {

                try {
                    // Construction de la requête
                    final PaymentRequest paymentRequest = buildRequest(contractProperties, partnerConfigurationMap, sensitivePartnerConfigurationMap, amount, environmentURL, buyer);

                    // Envoie de la requête
                    new Thread(() -> {
                        final StringResponse sendRequest;
                        try {
                            sendRequest = paymentService.createSendRequest(paymentRequest);
                        } catch (IOException | InvalidRequestException | GeneralSecurityException | URISyntaxException | ExternalCommunicationException e) {
                            LOGGER.error("error", e);
                            throw new IllegalStateException("", e);
                        }
                        LOGGER.info("Reponse : {}", sendRequest.getContent());
                    }).start();
                } finally {
                    count++;
                    if (count % 5 == 0) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (count == 100) {
                        break;
                    }
                }
            }
        });

        // On démarre
        thread.start();
        // On attend la fin du Thread
        thread.join();
    }

    private PaymentRequest buildRequest(final HashMap<String, ContractProperty> contractProperties, final HashMap<String, String> partnerConfigurationMap, final HashMap<String, String> sensitivePartnerConfigurationMap, final Amount amount, final String environmentURL, final Buyer buyer) {
        final String ref = generateString();
        final String trsId = generateString();
        final Order order = Order.OrderBuilder.anOrder()
                .withReference(ref)
                .withAmount(amount)
                .withDate(new Date())
                .build();
        return PaymentRequest.builder()
                .withOrder(order)
                .withBuyer(buyer)
                .withCaptureNow(false)
                .withPartnerConfiguration(new PartnerConfiguration(partnerConfigurationMap, sensitivePartnerConfigurationMap))
                .withContractConfiguration(new ContractConfiguration("TICKET_PREMIUM", contractProperties))
                .withPaylineEnvironment(new PaylineEnvironment(environmentURL, environmentURL, environmentURL, true))
                .withLocale(Locale.FRANCE)
                .withSoftDescriptor("soft")
                .withAmount(amount)
                .withTransactionId(trsId)
                .build();
    }

    private String generateString() {
        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }
}