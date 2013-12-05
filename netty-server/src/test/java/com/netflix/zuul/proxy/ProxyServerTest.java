package com.netflix.zuul.proxy;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.CharStreams;
import com.netflix.zuul.netty.filter.ZuulFiltersLoader;

public class ProxyServerTest {
    private static final String FILTERS_ROOT_PATH = "/filters";

    private static final String EXPECTED_REPONSE_PREFIX = "correct example response";

    private static final int REMOTE_PORT = 8081;
    private static final int LOCAL_PORT = 8080;

    private static MockEndpoint backEndService;
    private static ProxyServer proxyServer;

    @BeforeClass
    public static void setup() throws Exception {
        backEndService = new MockEndpoint(REMOTE_PORT, EXPECTED_REPONSE_PREFIX);
        backEndService.start();

        ZuulFiltersLoader filtersChangeNotifier = new ZuulFiltersLoader(filtersRootPath());

        proxyServer = new ProxyServer(LOCAL_PORT, filtersChangeNotifier);
        filtersChangeNotifier.reload();
        proxyServer.start();
    }

    private static Path filtersRootPath() throws URISyntaxException {
        URL resource = ProxyServerTest.class.getResource(FILTERS_ROOT_PATH);

        assertNotNull("File/directory not found: " + FILTERS_ROOT_PATH, resource);

        URI resourceUri = resource.toURI();

        return Paths.get(resourceUri);
    }

    @AfterClass
    public static void stop() throws Exception {
        proxyServer.stop();
        backEndService.stop();
    }

    @Test
    public void canProxyRequest() throws Exception {
        Response response = sendRequest("http://localhost:8080/example");

        assertEquals(EXPECTED_REPONSE_PREFIX + " /example", response.content());
        assertEquals(OK.code(), response.responseCode());
    }

    @Test(expected = FileNotFoundException.class)
    public void rejectsInvalidRequestWith404() throws Exception {
        sendRequest("http://localhost:8080/asdfgh");
    }

    public Response sendRequest(final String uri) throws Exception {
        URL url = new URL(uri.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        int responseCode = connection.getResponseCode();
        String content;

        try (InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream())) {
            content = CharStreams.toString(inputStreamReader);
        }

        return new Response(content, responseCode);
    }

    private class Response {
        private final String content;
        private final int responseCode;

        public Response(String content, int responseCode) {
            this.content = content;
            this.responseCode = responseCode;
        }

        public String content() {
            return content;
        }

        public int responseCode() {
            return responseCode;
        }
    }
}
