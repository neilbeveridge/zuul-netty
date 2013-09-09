package com.netflix.zuul.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 */
public class ZuulServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZuulServer.class);
    private final int port;
    private volatile Channel channel;
    private volatile ServerBootstrap bootstrap;

    public ZuulServer(int port) {
        this.port = port;
    }

    public ZuulServer start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ZuulServerInitializer());

            channel = bootstrap.bind().sync().channel();
            LOGGER.info("Server started at {}", port);
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        return this;

    }


    public boolean isRunning() {
        return channel != null && channel.isActive();
    }

    public ZuulServer filtersRootPath(final Path filtersRootPath) {
        return this;
    }

    public FutureTask<ZuulServer> stop() {
        FutureTask<ZuulServer> future = new FutureTask<>(new Callable<ZuulServer>() {
            @Override
            public ZuulServer call() throws Exception {
                if (channel != null) {
                    channel.close().await();
                }
                bootstrap = null;
                return ZuulServer.this;
            }
        });
        Executors.newSingleThreadExecutor().execute(future);
        return future;
    }

    private static class ZuulServerInitializer extends ChannelInitializer<Channel> {
        private static final Logger LOGGER = LoggerFactory.getLogger(ZuulServerInitializer.class);

        @Override
        protected void initChannel(final Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("handler", new ZuulHttpChannelHandler());
        }

    }

    private static class ZuulHttpChannelHandler extends SimpleChannelInboundHandler<HttpObject> {

        public ZuulHttpChannelHandler() {
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
            if (msg instanceof HttpRequest) {
                DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, OK);

                handleHttpRequest(ctx, (HttpRequest) msg, response);
            }
        }

        private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest, HttpResponse httpResponse) {
        }
    }


}
