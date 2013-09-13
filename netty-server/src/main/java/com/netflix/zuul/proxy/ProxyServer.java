package com.netflix.zuul.proxy;

import com.netflix.zuul.netty.filter.FiltersChangeNotifier;
import com.netflix.zuul.netty.filter.ZuulFiltersLoader;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.JmxReporter;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @author HWEB
 */
public class ProxyServer {
    private static final Timer TIMER = new HashedWheelTimer();
    private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

    private final int port;
    private Channel channel;
    private ServerBootstrap bootstrap;
    private FiltersChangeNotifier filtersChangeNotifier;

    public ProxyServer(int port) {
        this.port = port;
    }

    public FutureTask<ProxyServer> run() {
        FutureTask<ProxyServer> future = new FutureTask<>(new Callable<ProxyServer>() {

            @Override
            public ProxyServer call() throws Exception {
                // Configure the server.
                bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
                FiltersChangeNotifier changeNotifier = filtersChangeNotifier != null ? filtersChangeNotifier : FiltersChangeNotifier.IGNORE;
                CommonHttpPipeline pipelineFactory = new CommonHttpPipeline(TIMER, changeNotifier);
                bootstrap.setPipelineFactory(pipelineFactory);
                bootstrap.setOption("child.tcpNoDelay", true);
                channel = bootstrap.bind(new InetSocketAddress(port));
                LOG.info("server bound to port {}", port);

                pipelineFactory.getPipeline();

                return ProxyServer.this;
            }
        });

        final Thread thread = new Thread(future, "Proxy Server");
        thread.start();
        return future;
    }

    public boolean isRunning() {
        return channel != null && channel.isBound();
    }


    public FutureTask<ProxyServer> stop() {
        FutureTask<ProxyServer> future = new FutureTask<>(new Callable<ProxyServer>() {

            @Override
            public ProxyServer call() throws Exception {
                if (channel != null) {
                    channel.close();
                }
                if (bootstrap != null) {
                    bootstrap.releaseExternalResources();
                }

                bootstrap = null;

                if (channel != null) {
                    channel.getCloseFuture().await();
                }

                return ProxyServer.this;
            }
        });
        final Thread thread = new Thread(future, "Proxy Server");
        thread.start();
        return future;
    }

    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        LOG.info("Starting server...");

        FiltersChangeNotifier changeNotifier = FiltersChangeNotifier.IGNORE;
        ProxyServer proxyServer = new ProxyServer(80)
                .setFiltersChangeNotifier(changeNotifier);
        proxyServer.run().get();


        JmxReporter.startDefault(Metrics.defaultRegistry());

        //ConsoleReporter.enable(1, TimeUnit.SECONDS);
    }

    public ProxyServer setFiltersChangeNotifier(FiltersChangeNotifier filtersChangeNotifier) {
        this.filtersChangeNotifier = filtersChangeNotifier;
        return this;
    }


}
