package com.payline.payment.tsi.utils.http;

import com.payline.payment.tsi.exception.ExternalCommunicationException;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.CommunicationException;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This utility class provides a basic HTTP client to send requests, using OkHttp library.
 * Refactored from p24 HttpClient
 * It must be extended to match each payment method needs.
 */
public abstract class HttpClient {

    protected CloseableHttpClient client;

    private static final Logger LOGGER = LogManager.getLogger(HttpClient.class);

    /**
     *  Instantiate a HTTP client.
     *
     * @param connectTimeout Determines the timeout in milliseconds until a connection is established
     * @param requestTimeout The timeout in milliseconds used when requesting a connection from the connection manager
     * @param socketTimeout Defines the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout for waiting for data or, put differently, a maximum period inactivity between two consecutive data packets)
     */
    public HttpClient( int connectTimeout, int requestTimeout, int socketTimeout ) {

        final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout * 1000)
            .setConnectionRequestTimeout(requestTimeout * 1000)
            .setSocketTimeout(socketTimeout * 1000)
            .build();

        ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase
                        ("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 5 * 1000;
        };

        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.useSystemProperties()
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCredentialsProvider(new BasicCredentialsProvider())
                .setSSLSocketFactory(new SSLConnectionSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory(), SSLConnectionSocketFactory.getDefaultHostnameVerifier()));
        final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(2);
        connManager.setMaxTotal(2);
        builder.setConnectionManager(connManager);
        builder.setKeepAliveStrategy(keepAliveStrategy);
        this.client = builder.build();
    }

    /**
     * Send a POST request.
     *
     * @param scheme URL scheme
     * @param host URL host
     * @param path URL path
     * @param body Request body
     * @param contentType The content type of the request body
     * @return The response returned from the HTTP call
     * @throws IOException
     * @throws URISyntaxException
     */
    public StringResponse doPost(String scheme, String host, String path, String body, String contentType )
            throws URISyntaxException, UnsupportedEncodingException, ExternalCommunicationException {

        final URI uri = new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPath(path)
                .build();

        final HttpPost httpPostRequest = new HttpPost(uri);
        httpPostRequest.setEntity(new StringEntity(body));
        httpPostRequest.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        LOGGER.info("appel partenaire... [HOST: {}]", host);
        final long start = System.currentTimeMillis();
        int count = 0;
        StringResponse strResp = null;
        while (count < 3 && strResp == null) {
            try (CloseableHttpResponse httpResp = this.client.execute(httpPostRequest)) {

                strResp = new StringResponse();
                strResp.setCode(httpResp.getStatusLine().getStatusCode());
                strResp.setMessage(httpResp.getStatusLine().getReasonPhrase());

                if (httpResp.getEntity() != null) {
                    final String responseAsString = EntityUtils.toString(httpResp.getEntity()); // , "UTF-8"
                    strResp.setContent(responseAsString);
                }
                final long end = System.currentTimeMillis();

                LOGGER.info("fin d'appel [T: {}ms] [CODE: {}]", end - start, strResp.getCode());

            } catch (final IOException e) {
                LOGGER.error("Erreur durant l'appel [T: {}ms]", System.currentTimeMillis() - start, e);
                strResp = null;
            } finally {
                count++;
            }
        }

        if (strResp == null) {
            throw new ExternalCommunicationException("Reponse vide");
        }

        return strResp;
    }
}
