package com.payline.payment.tsi.request;

import com.payline.payment.tsi.exception.ExternalCommunicationException;
import com.payline.payment.tsi.response.TsiGoResponse;
import com.payline.payment.tsi.security.Hmac;
import com.payline.payment.tsi.security.HmacAlgorithm;
import com.payline.payment.tsi.utils.http.JsonHttpClient;
import com.payline.payment.tsi.utils.http.StringResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Main {

    public static Properties config;
    public static Properties testConfig;

    public static void main( String[] args ) throws IOException, GeneralSecurityException, URISyntaxException, ExternalCommunicationException {
        config = new Properties();
        config.load( Main.class.getClassLoader().getResourceAsStream( "config.properties" ) );

        testConfig = new Properties();
        testConfig.load( Main.class.getClassLoader().getResourceAsStream( "testConfig.properties" ) );

        //hmacThis( "abcdefghijklmnopkrstuvwxyz12345|806" );
        http();
    }

    private static void hmac(){
        TsiGoRequest request = new TsiGoRequest(
                Integer.parseInt( testConfig.getProperty( "contractConfiguration.merchantId" ) ),
                "abcdefghijklmnopkrstuvwxyz12346J",
                "1.02",
                "EUR",
                Integer.parseInt( testConfig.getProperty( "contractConfiguration.keyId" ) ),
                "Ticket Premium",
                "http://boutique.com/returnOK.php",
                "http://boutique.com/returnNOK.php",
                "http://boutique.com/returnS2S.php",
                "N",
                "Y",
                null
        );
        String key = testConfig.getProperty( "contractConfiguration.keyValue" );

        String message = request.buildSealMessage();
        System.out.println( "message: " + message );
        System.out.println( "key: " + key );
        Hmac hmac = new Hmac( key, HmacAlgorithm.MD5 );
        System.out.println( "hmac: " + hmac.digest( message ) );
    }

    private static void hmacThis( String message ){
        String key = testConfig.getProperty( "contractConfiguration.keyValue" );
        Hmac hmac = new Hmac( key, HmacAlgorithm.MD5 );
        System.out.println( hmac.digest( message ) );
    }

    private static void http() throws IOException, GeneralSecurityException, URISyntaxException, ExternalCommunicationException {
        JsonHttpClient httpClient = new JsonHttpClient( 5, 10, 15 );

        // Build request
        Map<String, Object> custom = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "000000000000000000yyyyMMddHHmmss" );
        TsiGoRequest request = new TsiGoRequest(
                Integer.parseInt( testConfig.getProperty( "contractConfiguration.merchantId" ) ),
                LocalDateTime.now().format( formatter ),
                "1.02",
                "EUR",
                Integer.parseInt( testConfig.getProperty( "contractConfiguration.keyId" ) ),
                "Ticket Premium",
                "http://boutique.com/returnOK.php",
                "http://boutique.com/returnNOK.php",
                "http://boutique.com/returnS2S.php",
                "N",
                "Y",
                null
        );

        // Seal request
        request.seal( testConfig.getProperty( "contractConfiguration.keyValue" ) );

        // Build request body
        System.out.println( request.buildBody() );

        // Send request
        StringResponse response = httpClient.doPost(
                config.getProperty( "test.tsi.scheme" ),
                config.getProperty( "test.tsi.host" ),
                config.getProperty( "test.tsi.go.path" ),
                request.buildBody()
        );
        System.out.println( response );

        // Parse the response
        TsiGoResponse tsiGoResponse = (new TsiGoResponse.Builder()).fromJson(response.getContent());
        System.out.println( "TsiGoResponse[" +
                "status=" + tsiGoResponse.getStatus() +
                ", message=\"" + tsiGoResponse.getMessage() + "\"" +
                ", url=\"" + tsiGoResponse.getUrl() + "\"" +
                ", tid=" + tsiGoResponse.getTid() +
                ", keyId=" + tsiGoResponse.getKeyId() +
                "]" );

        Hmac hmac = new Hmac( testConfig.getProperty( "contractConfiguration.keyValue" ), HmacAlgorithm.MD5 );
        String statusCheckMac = hmac.digest( tsiGoResponse.getTid() + "|" + tsiGoResponse.getKeyId() );
        System.out.println( "Status Check mac: " + statusCheckMac );
    }

}
