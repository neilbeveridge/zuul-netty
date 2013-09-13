package com.netflix.zuul.netty;

import com.google.common.base.Charsets;
import com.netflix.zuul.netty.filter.ZuulFilter;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author HWEB
 */
public class ZuulServerTest {


    private ZuulServer server;


    @After
    public void stopServer() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop().get();
        }
    }

    @Test
    public void startAndStopServer() throws Exception {
        server = new ZuulServer(9090);
        startServerInBackground();
        assertTrue("Server should be running", server.isRunning());
    }


    @Test
    public void executesZuulFilters() throws Exception {
        ZuulRunner handler = new ZuulRunner();
        ZuulFilter debugFilter = mock(ZuulFilter.class);
        handler.filterAdded(Paths.get("debug-filter"), debugFilter);
        server = new ZuulServer(9090)
                .add(handler);
        startServerInBackground();

        get(URI.create("http://localhost:9090"));
        verify(debugFilter).execute(any(RequestContext.class));

    }

    @Test
    public void executesNewlyAddedFilter() throws Exception {
        ZuulRunner zuulRunner = new ZuulRunner();
        server = new ZuulServer(9090)
                .add(zuulRunner);
        startServerInBackground();

        get(URI.create("http://localhost:9090"));

        ZuulFilter newFilter = mock(ZuulFilter.class);
        zuulRunner.filterAdded(Paths.get("new-filter"), newFilter);
        get(URI.create("http://localhost:9090"));

        verify(newFilter).execute(any(RequestContext.class));

    }

    @Test
    public void willNotExecuteRemovedFilters() throws Exception {
        ZuulRunner zuulRunner = new ZuulRunner();
        ZuulFilter filter = mock(ZuulFilter.class);
        Path filterPath = Paths.get("filter");
        zuulRunner.filterAdded(filterPath, filter);
        server = new ZuulServer(9090)
                .add(zuulRunner);
        startServerInBackground();
        get(URI.create("http://localhost:9090"));
        zuulRunner.filterRemoved(filterPath, filter);
        get(URI.create("http://localhost:9090"));

        verify(filter, times(1)).execute(any(RequestContext.class));

    }


    public void get(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestProperty("Accept-Charset", Charsets.UTF_8.name());
        try {
            connection.getInputStream();
        } finally {
            connection.disconnect();
        }
    }

    private void startServerInBackground() throws InterruptedException {
        FutureTask<ZuulServer> future = new FutureTask<>(new Callable<ZuulServer>() {
            @Override
            public ZuulServer call() throws Exception {
                return server.start();
            }
        });
        Executors.newSingleThreadExecutor().execute(future);
        Thread.sleep(1000);
    }


}
