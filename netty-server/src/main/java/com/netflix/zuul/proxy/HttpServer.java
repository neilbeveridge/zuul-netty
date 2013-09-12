package com.netflix.zuul.proxy;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.JmxReporter;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServer {

    private static final Timer TIMER = new HashedWheelTimer();
    private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void run() {
        // Configure the server.
        ServerBootstrap b = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

        b.setPipelineFactory(new CommonHttpPipeline(TIMER));
        b.setOption("child.tcpNoDelay", true);
        b.bind(new InetSocketAddress(port));

        LOG.info("server bound to port {}", port);
    }

    public static void main(String[] args) {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        LOG.info("Starting server...");
        new HttpServer(8080).run();

        JmxReporter.startDefault(Metrics.defaultRegistry());

        //ConsoleReporter.enable(1, TimeUnit.SECONDS);
    }
}
