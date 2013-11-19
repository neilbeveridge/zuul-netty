package com.netflix.zuul.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An HTTP server that sends random HTTP number.
 */
public class MockEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MockEndpoint.class);

    private final int port;
    private final String responseToGive;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel channelToWaitFor;

    public MockEndpoint(int port) {
        this(port, null);
    }

    public MockEndpoint(int port, String responseToGive) {
        this.port = port;
        this.responseToGive = responseToGive;
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new HttpMockEndPointServerInitializer(responseToGive));

            ChannelFuture syncFuture = b.bind(port);

            syncFuture.addListener(new LogBindSuccessOrFailureOnComplete());

            syncFuture.sync();

            channelToWaitFor = syncFuture.channel();
            channelToWaitFor.closeFuture().addListener(new ShutdownGracefullyOnComplete());

            LOG.info("bound to port {}", port);
        } catch (Exception e) {
            shutdownGracefully();
            throw e;
        }
    }

    private void shutdownGracefully() {
        LOG.debug("Shutting down gracefully");

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public void stop() throws Exception {
        channelToWaitFor.close().sync();
    }

    private class LogBindSuccessOrFailureOnComplete implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                LOG.info("Successfully bound to port : {}", port);
            } else {
                LOG.info("Could not bind to port : {}", port);
            }
        }
    }

    private class ShutdownGracefullyOnComplete implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            try {
                if (!future.isSuccess()) {
                    LOG.info("Exception during channel close", future.cause());
                }
            } finally {
                shutdownGracefully();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }

        String responseToGive = null;
        if (args.length > 1) {
            responseToGive = args[1];
        }

        if (responseToGive == null) {
            new MockEndpoint(port).start();
        } else {
            new MockEndpoint(port, responseToGive).start();
        }
    }
}
