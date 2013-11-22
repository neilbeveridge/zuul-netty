package com.netflix.zuul.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.netty.filter.FiltersChangeNotifier;
import com.netflix.zuul.netty.filter.ZuulFiltersLoader;

/**
 * HTTP Proxy Server
 */
public class ProxyServer {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServer.class);
    private static final String DEFAULT_FILTERS_ROOT_PATH = "/filters";

    private final int localPort;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channelToWaitFor;
    private final FrontendServerInitializer channelInitialiser;

    public ProxyServer(int localPort, FiltersChangeNotifier filtersChangeNotifier) {
        this.localPort = localPort;

        channelInitialiser = new FrontendServerInitializer();
        filtersChangeNotifier.addFiltersListener(channelInitialiser);
    }

    public void start() throws Exception {
        LOG.info("Proxy server starting on port {}...", localPort);

        createEventLoopGroups();
        try {
            ServerBootstrap bootstrap = createBootstrap();

            ChannelFuture bindFuture = bindAndSync(bootstrap);

            // Get channel as field so we can stop on demand
            channelToWaitFor = bindFuture.channel();
            channelToWaitFor.closeFuture().addListener(new ShutdownGracefully());

            LOG.info("Proxy server started on port {}...", localPort);
        } catch (Exception e) {
            shutdownGracefully();
            throw e;
        }
    }

    public void stop() throws Exception {
        channelToWaitFor.close().sync();
    }

    private ChannelFuture bindAndSync(ServerBootstrap bootstrap) throws InterruptedException {
        ChannelFuture bindFuture = bootstrap.bind(localPort);

        bindFuture.addListener(new LogBindSuccessOrFailure());
        bindFuture.sync();

        return bindFuture;
    }

    private void createEventLoopGroups() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
    }

    private ServerBootstrap createBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(channelInitialiser)
        .childOption(ChannelOption.AUTO_READ, false);
        return bootstrap;
    }

    private void shutdownGracefully() {
        LOG.debug("Shutting down gracefully");

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    private class LogBindSuccessOrFailure implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                LOG.info("Successfully bound to port : {}", localPort);
            } else {
                LOG.info("Could not bind to port : {}", localPort);
            }
        }
    }

    private class ShutdownGracefully implements ChannelFutureListener {
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

    private static Path defaultFiltersRootPath() throws URISyntaxException, FileNotFoundException {
        String path = DEFAULT_FILTERS_ROOT_PATH;

        return classpathRelativePath(path);
    }

    private static Path classpathRelativePath(String path) throws URISyntaxException, FileNotFoundException {
        
    	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    	URL resource = classLoader.getResource(path);

        if (resource == null) {
            throw new FileNotFoundException(path);
        }

        URI resourceUri = resource.toURI();

        return Paths.get(resourceUri);
    }

    public static void main(String[] args) throws Exception {

        // Validate command line options.
        if (args.length < 1) {
            System.err.println("Usage: " + ProxyServer.class.getSimpleName() + " <local port> [<filters root path>]");
            return;
        }

        // Parse command line options.
        int localPort = Integer.parseInt(args[0]);

        Path filtersPath;
        if (args.length >= 2) {
            filtersPath = classpathRelativePath(args[1]);
        } else {
            filtersPath = defaultFiltersRootPath();
        }

        LOG.info("filtersPath = {}", filtersPath);

        LOG.info("Starting server...");

        ZuulFiltersLoader changeNotifier = new ZuulFiltersLoader(filtersPath);

        ProxyServer proxyServer = new ProxyServer(localPort, changeNotifier);
        changeNotifier.reload();
        proxyServer.start();
    }
}
