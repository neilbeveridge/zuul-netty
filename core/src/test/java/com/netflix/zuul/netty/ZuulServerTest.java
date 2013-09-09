package com.netflix.zuul.netty;

import org.junit.After;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertTrue;

/**
 * @author HWEB
 */
public class ZuulServerTest {

    public static final Path FILTERS_ROOT_PATH = Paths.get(ZuulServerTest.class.getResource("/filters").getFile());

    private ZuulServer server;


    @After
    public void stopServer() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop().get();
        }
    }

    @Test
    public void startAndStopServer() throws Exception {
        server = new ZuulServer(9090)
                .filtersRootPath(FILTERS_ROOT_PATH);
        startServerInBackground();
        Thread.sleep(1000);
        assertTrue("Server should be running", server.isRunning());
    }

    private void startServerInBackground() {
        FutureTask<ZuulServer> future = new FutureTask<>(new Callable<ZuulServer>() {
            @Override
            public ZuulServer call() throws Exception {
                return server.start();
            }
        });

        Executors.newSingleThreadExecutor().execute(future);
    }


}
