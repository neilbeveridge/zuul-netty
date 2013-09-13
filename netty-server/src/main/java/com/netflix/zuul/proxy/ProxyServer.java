package com.netflix.zuul.proxy;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
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

    public ProxyServer(int port) {
        this.port = port;
    }

    public FutureTask<ProxyServer> run() {
        FutureTask<ProxyServer> future = new FutureTask<>(new Callable<ProxyServer>() {

            @Override
            public ProxyServer call() throws Exception {
                // Configure the server.
                bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

                bootstrap.setPipelineFactory(new CommonHttpPipeline(TIMER));
                bootstrap.setOption("child.tcpNoDelay", true);
                channel = bootstrap.bind(new InetSocketAddress(port));
                LOG.info("server bound to port {}", port);

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

}
