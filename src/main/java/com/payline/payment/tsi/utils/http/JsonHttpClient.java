package com.payline.payment.tsi.utils.http;

import com.payline.payment.tsi.exception.ExternalCommunicationException;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

public class JsonHttpClient extends HttpClient {

    /**
     * Instantiate a HTTP client with default values.
     */
    public JsonHttpClient() throws GeneralSecurityException {
        super( 10, 10, 15 );
    }

    /**
     * Instantiate a HTTP client.
     *
     * @param connectTimeout Default connect timeout (in seconds) for new connections. A value of 0 means no timeout.
     * @param writeTimeout   Default write timeout (in seconds) for new connections. A value of 0 means no timeout.
     * @param readTimeout    Default read timeout (in seconds) for new connections. A value of 0 means no timeout.
     * @throws GeneralSecurityException
     */
    public JsonHttpClient( int connectTimeout, int writeTimeout, int readTimeout ) throws GeneralSecurityException {
        super( connectTimeout, writeTimeout, readTimeout );
    }

    /**
     * Send a POST request, with a JSON content type.
     *
     * @param scheme URL scheme
     * @param host URL host
     * @param path URL path
     * @param jsonContent The JSON content, as a string
     * @return The response returned from the HTTP call
     * @throws IOException
     * @throws URISyntaxException
     */
     public StringResponse doPost(String scheme, String host, String path, String jsonContent ) throws IOException, URISyntaxException, ExternalCommunicationException {
        return super.doPost( scheme, host, path, jsonContent, ContentType.APPLICATION_JSON.toString() );
    }
}
