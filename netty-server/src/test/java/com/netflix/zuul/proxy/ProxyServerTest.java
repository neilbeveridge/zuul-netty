package com.netflix.zuul.proxy;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.CharStreams;

public class ProxyServerTest {
    private static final String BOOKING_CONFIRMED = "booking confirmed";

    private static final String LOCAL_HOST = "localhost";
    private static final int REMOTE_PORT = 8081;
    private static final int LOCAL_PORT = 8080;

    private MockEndpoint backEndService;
    private ProxyServer proxyServer;

    @Before
    public void setup() throws Exception {
        backEndService = new MockEndpoint(REMOTE_PORT, BOOKING_CONFIRMED);
        backEndService.start();

        proxyServer = new ProxyServer(LOCAL_PORT, LOCAL_HOST, REMOTE_PORT);
        proxyServer.start();
    }

    @After
    public void stop() throws Exception {
        proxyServer.stop();
        backEndService.stop();
    }

    @Test
    public void canProxyRequestToBookingService() throws Exception {
        String responseBody = sendRequest("http://localhost:8080/booking");

        assertEquals(BOOKING_CONFIRMED, responseBody);
    }

    public String sendRequest(final String uri) throws Exception {
        URL url = new URL(uri.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try (InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream())) {
            return CharStreams.toString(inputStreamReader);
        }
    }

}
