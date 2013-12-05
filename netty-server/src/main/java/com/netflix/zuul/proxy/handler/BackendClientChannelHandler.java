package com.netflix.zuul.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendClientChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(BackendClientChannelHandler.class);

    private final Channel inboundChannel;

    public BackendClientChannelHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    	
    	ChannelPipeline pipeline = inboundChannel.pipeline();
        
        LOG.debug("For inboundChannel : {}, handlers in pipeline : {}", inboundChannel, pipeline.names());

        ChannelFuture future = inboundChannel.writeAndFlush(msg);

        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                	LOG.debug("successfully wrote to inbound channel");
                    ctx.channel().read();
                } else {
                	LOG.debug("Unable to write to inbound channel due to : ", future.cause());
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        HandlerUtil.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.debug("exception caught : ", cause);
        HandlerUtil.closeOnFlush(ctx.channel());
    }

}
