package com.netflix.zuul.proxy;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author HWEB
 */
public class ProxyServerTest {

    private ProxyServer proxyServer;

    @After
    public void stopServer() throws Exception {
        if (proxyServer != null && proxyServer.isRunning()) {
            proxyServer.stop().get();
        }
    }

    @Test
    public void startsAndStopsServer() throws Exception {
        proxyServer = new ProxyServer(9090).bootstrap().get();
        assertTrue(proxyServer != null);
    }

}
