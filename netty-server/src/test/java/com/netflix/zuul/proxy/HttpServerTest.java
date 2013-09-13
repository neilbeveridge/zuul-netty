package com.netflix.zuul.proxy;

import org.junit.Test;

/**
 * @author HWEB
 */
public class HttpServerTest {

    private HttpServer httpServer;

    @Test
    public void registeresHandlersOnceServerStarted() throws Exception {
        httpServer = new HttpServer(9090);


    }


}
