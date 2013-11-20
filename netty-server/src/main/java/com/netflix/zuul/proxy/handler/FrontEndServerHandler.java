package com.netflix.zuul.proxy.handler;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.BackendClientInitializer;

/**
 * @author HWEB
 */
public class FrontEndServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(FrontEndServerHandler.class);

    private final String remoteHost;
    private final int remotePort;
    private final RoutingRules routingRules;

    // outboundChannel is volatile since it is shared between threads (this handler & BackendClientChannelHandler).
    private volatile Channel backEndChannel;

    public FrontEndServerHandler(String remoteHost, int remotePort, RoutingRules routingRules) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.routingRules = routingRules;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("!!! channelActive");

        connectToBackEndService(ctx);
    }

    private void connectToBackEndService(ChannelHandlerContext ctx) {
        // get a reference to the inbound channel, which allows us to read data from external clients.
        final Channel inboundChannel = ctx.channel();

        // Start the connection attempt. We need to be a CLIENT to the back-end server, so create a (client) Bootstrap, as opposed to a ServerBootstrap.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
        .channel(ctx.channel().getClass())
        .handler(new BackendClientInitializer(inboundChannel))
        .option(ChannelOption.AUTO_READ, false);

        // connect to the back-end server
        ChannelFuture future = b.connect(remoteHost, remotePort);

        // get a reference to the outbound channel, which allows us to write data to the back-end server.
        backEndChannel = future.channel();

        future.addListener(new DoFirstRead(inboundChannel, remoteHost, remotePort));
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        LOG.debug("!!! channelRead");

        // wait for the outboundChannel to be active, i.e. this will only happen once the channelActive() method has completed
        if (backEndChannel.isActive()) {

            // Only forward HttpRequest messages to the back-end server.
            if (msg instanceof FullHttpRequest) {

                FullHttpRequest completeMsg = (FullHttpRequest) msg;

                String destination = routingRules.destinationForSource(completeMsg.getUri());

                if (destination != null) {
                    sendToDestination(ctx, completeMsg, destination);
                } else {
                    respondWith404NotFound(ctx);
                }
            }
        }
    }

    private void respondWith404NotFound(final ChannelHandlerContext ctx) {
        LOG.debug("Responding 404");

        final Channel inboundChannel = ctx.channel();

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
        response.headers().set(CONTENT_LENGTH, 0);

        ChannelFuture future = inboundChannel.writeAndFlush(response);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    private void sendToDestination(final ChannelHandlerContext ctx, FullHttpRequest completeMsg, String destination) {
        completeMsg.setUri(destination);

        ChannelFuture future = backEndChannel.writeAndFlush(completeMsg);

        future.addListener(new DoAnotherRead(ctx));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        LOG.info("channelReadComplete triggered, so writing data");

        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        if (backEndChannel != null) {
            HandlerUtil.closeOnFlush(backEndChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        LOG.info("caught exception : {}", cause);

        HandlerUtil.closeOnFlush(ctx.channel());
    }

    private final class DoFirstRead implements ChannelFutureListener {
        private final Channel inboundChannel;
        private final String remoteHost;
        private final int remotePort;

        private DoFirstRead(Channel inboundChannel, String remoteHost, int remotePort) {
            this.inboundChannel = inboundChannel;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                LOG.info("successfully connected to remote server {} on port {}", remoteHost, remotePort);
                // connection complete start to read first data
                inboundChannel.read();
            } else {
                LOG.info("Unable to connect to remote server {} on port {}", remoteHost, remotePort);
                // Close the connection if the connection attempt has failed.
                inboundChannel.close();
            }
        }
    }

    private final class DoAnotherRead implements ChannelFutureListener {
        private final ChannelHandlerContext ctx;

        private DoAnotherRead(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {

            if (future.isSuccess()) {
                LOG.info("successfully wrote to outbound channel");
                // was able to flush out data, start to read the next chunk
                ctx.channel().read();
            } else {
                LOG.info("Unable to write to outbound channel due to : ", future.cause());
                future.channel().close();
            }
        }
    }
}
