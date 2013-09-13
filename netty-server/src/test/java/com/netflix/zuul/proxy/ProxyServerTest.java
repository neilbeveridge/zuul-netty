package com.netflix.zuul.proxy;

import com.netflix.zuul.netty.filter.ZuulFiltersLoader;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author HWEB
 */
public class ProxyServerTest {

    public static final Path FILTERS_ROOT_PATH = Paths.get(ProxyServerTest.class.getResource("/filters").getFile());

    private ProxyServer proxyServer;

    @After
    public void stopServer() throws Exception {
        if (proxyServer != null && proxyServer.isRunning()) {
            proxyServer.stop().get();
        }
    }

    @Test
    public void startsAndStopsServer() throws Exception {
        proxyServer = new ProxyServer(9090).run().get();
        assertTrue(proxyServer != null);
    }


    @Test
    public void registerFiltersLoader() throws Exception {
        ZuulFiltersLoader zuulFiltersLoader = new ZuulFiltersLoader(FILTERS_ROOT_PATH);
        proxyServer = new ProxyServer(9090)
                .setFiltersChangeNotifier(zuulFiltersLoader)
                .run()
                .get();

        zuulFiltersLoader.reload();



    }

    @Test
    @Ignore
    public void handlesHttpRequests() throws Exception {
        proxyServer = new ProxyServer(9090).run().get();
        String responseBody = sendRequest("http://localhost:9090");
        assertEquals("OK", responseBody);
    }

    public String sendRequest(final String uri) throws Exception {
        URL url = new URL(uri.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getResponseMessage();
    }
}
