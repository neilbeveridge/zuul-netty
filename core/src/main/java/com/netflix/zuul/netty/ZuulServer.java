package com.netflix.zuul.netty;

import com.netflix.zuul.netty.filter.RequestContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 */
public class ZuulServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZuulServer.class);
    private final int port;
    private volatile Channel channel;
    private volatile ServerBootstrap bootstrap;

    private List<HttpHandler> httpHandlers = new ArrayList<>();

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
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("decoder", new HttpRequestDecoder());
                            pipeline.addLast("encoder", new HttpResponseEncoder());
                            pipeline.addLast("codec", new HttpClientCodec());
                            pipeline.addLast("aggregator", new HttpObjectAggregator(8194));
                            pipeline.addLast("handler", new ZuulHttpChannelHandler(httpHandlers));
                        }
                    });

            channel = bootstrap.bind().sync().channel();
            LOGGER.info("Server started at {}", port);
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        return this;

    }


    public ZuulServer add(HttpHandler handler) {
        httpHandlers.add(handler);
        return this;
    }


    public boolean isRunning() {
        return channel != null && channel.isActive();
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


    private static class ZuulHttpChannelHandler extends SimpleChannelInboundHandler<HttpObject> {

        private final List<HttpHandler> httpHandlers;

        public ZuulHttpChannelHandler(List<HttpHandler> httpHandlers) {
            this.httpHandlers = httpHandlers;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
            if (msg instanceof FullHttpRequest) {
                FullHttpRequest request = (FullHttpRequest) msg;
                HttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
                handleHttpRequest(ctx, request, response);
            }

        }

        private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest, HttpResponse httpResponse) {
            RequestContext requestContext = new RequestContext(ctx, httpRequest, httpResponse);
            for (HttpHandler handler : httpHandlers) {
                handler.handle(requestContext);
            }

        }
    }

}
