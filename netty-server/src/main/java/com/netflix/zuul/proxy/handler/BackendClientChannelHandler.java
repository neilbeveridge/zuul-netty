package com.netflix.zuul.proxy.handler;

import static org.slf4j.LoggerFactory.getLogger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;

/**
 * Receives response from back-end and writes it to the front-end's channel
 */
public class BackendClientChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = getLogger(BackendClientChannelHandler.class);

    private final Channel inboundChannel;

    public BackendClientChannelHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        context.read();

        LOG.debug("channelActive");
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, Object message) throws Exception {
        LOG.debug("channelRead");

        inboundChannel.writeAndFlush(message);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        HandlerUtil.closeOnFlush(inboundChannel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        HandlerUtil.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        LOG.info("exception caught : ", cause);
        HandlerUtil.closeOnFlush(context.channel());
    }
}
