package com.netflix.zuul.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author HWEB
 */
public class ProxyServer {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServer.class);

    private final int localPort;
    private final String remoteHost;
    private final int remotePort;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channelToWaitFor;

    public ProxyServer(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public void start() throws Exception {
        LOG.info("Proxying *:" + localPort + " to " + remoteHost + ':' + remotePort + " ...");

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new FrontendServerInitializer(remoteHost, remotePort))
            .childOption(ChannelOption.AUTO_READ, false);

            ChannelFuture bindFuture = b.bind(localPort);

            bindFuture.addListener(new LogBindSuccessOrFailureOnComplete());

            bindFuture.sync();

            channelToWaitFor = bindFuture.channel();
            channelToWaitFor.closeFuture().addListener(new ShutdownGracefullyOnComplete());
        } catch (Exception e) {
            shutdownGracefully();
            throw e;
        }
    }

    public void stop() throws Exception {
        channelToWaitFor.close().sync();
    }

    private void shutdownGracefully() {
        LOG.debug("Shutting down gracefully");

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    private class LogBindSuccessOrFailureOnComplete implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                LOG.info("Successfully bound to port : {}", localPort);
            } else {
                LOG.info("Could not bind to port : {}", localPort);
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

        // Validate command line options.
        if (args.length != 3) {
            System.err.println("Usage: " + ProxyServer.class.getSimpleName() + " <local port> <remote host> <remote port>");
            return;
        }

        // Parse command line options.
        int localPort = Integer.parseInt(args[0]);
        String remoteHost = args[1];
        int remotePort = Integer.parseInt(args[2]);

        new ProxyServer(localPort, remoteHost, remotePort).start();
    }
}
