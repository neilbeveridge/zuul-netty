package com.netflix.zuul.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.SocketAddress;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.BackendClientInitializer;

/**
 * @author HWEB
 */
public class FrontEndServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(FrontEndServerHandler.class);

    // outboundChannel is volatile since it is shared between threads
    private volatile Channel outboundChannel;

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        logChannelOccurrence(context, "Active");
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, Object message) throws Exception {
        LOG.debug("Channel Read");

        // wait for the outboundChannel to be active, i.e. this will only happen once the channelActive() method has completed
        if (context.channel().isActive()) {

            logChannelOccurrence(context, "channelRead");

            // Only forward HttpRequest messages to the back-end server.
            if (message instanceof FullHttpRequest) {
                connectToBackEndService(context, (FullHttpRequest) message);
            }
        }
    }

    private void connectToBackEndService(ChannelHandlerContext context, FullHttpRequest httpRequest) {
        Bootstrap backendClientBootstrap = createBackendClientBootstrap(context);

        URI route = routeFrom(context);

        // connect to the back-end server
        ChannelFuture backendConnectionFuture = backendClientBootstrap.connect(route.getHost(), route.getPort());

        // get a reference to the outbound channel, which allows us to write data to the back-end server.
        outboundChannel = backendConnectionFuture.channel();

        backendConnectionFuture.addListener(new ConnectionSuccessOrFailureLogger(route));
        backendConnectionFuture.addListener(new WriteToBackEnd(httpRequest));
    }

    private URI routeFrom(ChannelHandlerContext context) {
        // get a reference to the inbound channel, which allows us to read data from external clients (e.g. the browser or integration test).
        final Channel inboundChannel = context.channel();

        URI route = inboundChannel.attr(Routing.ROUTE_KEY).get();

        LOG.debug("route = {} in channel={}", route, inboundChannel.hashCode());

        return route;
    }

    private Bootstrap createBackendClientBootstrap(ChannelHandlerContext context) {
        Channel inboundChannel = context.channel();

        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop()).channel(context.channel().getClass()).handler(new BackendClientInitializer(inboundChannel))
        .option(ChannelOption.AUTO_READ, false);
        return b;
    }

    private void logChannelOccurrence(ChannelHandlerContext context, String occurrence) {
        if (LOG.isDebugEnabled()) {
            Channel channel = context.channel();
            int hashCode = channel.hashCode();
            SocketAddress remoteAddress = channel.remoteAddress();
            SocketAddress localAddress = channel.localAddress();

            LOG.debug("Channel {}: hashCode={}, remoteAddress={}, localAddress={}", occurrence, hashCode, remoteAddress, localAddress);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {

        LOG.debug("channelReadComplete triggered, so writing data");

        context.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {

        if (outboundChannel != null) {
            HandlerUtil.closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {

        LOG.debug("caught exception : {}", cause);

        HandlerUtil.closeOnFlush(context.channel());
    }

    private class WriteToBackEnd implements ChannelFutureListener {
        private final FullHttpRequest request;

        public WriteToBackEnd(FullHttpRequest request) {
            this.request = request;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                outboundChannel.writeAndFlush(request);
            } else {
                outboundChannel.close();
            }
        }
    }

    private class ConnectionSuccessOrFailureLogger implements ChannelFutureListener {
        private final URI route;

        public ConnectionSuccessOrFailureLogger(URI route) {
            this.route = route;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                LOG.debug("Successfully connected to remote server at {}", route);
            } else {
                LOG.debug("Unable to connect to remote server at {}", route);
            }
        }
    }
}
