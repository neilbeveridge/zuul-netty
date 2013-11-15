package com.netflix.zuul.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendClientChannelHandler extends SimpleChannelInboundHandler {

	private static final Logger LOG = LoggerFactory.getLogger(BackendClientChannelHandler.class);
	
	// TODO : Does this channel need to be volatile to make it thread-safe ?
    private final Channel inboundChannel;

    public BackendClientChannelHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	
    	LOG.info("channel is now active");
        //ctx.read();
        
        // TODO : should we be checking for whether this is a chunked transfer & also if this is the last chunk ?        
    }

    /*
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        
    	LOG.info("channel read, msg = " + msg);
    	
    	ChannelFuture future = inboundChannel.writeAndFlush(msg);
    	
    	future.addListener(new ChannelFutureListener() {
    		
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
            	
                if (future.isSuccess()) {
                	LOG.info("successfully wrote to outbound channel");
                    ctx.channel().read();
                } else {
                	LOG.info("Unable to write to outbound channel, so closing the channel");
                    future.channel().close();
                }
            }
        });
    }
    */

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	HandlerUtil.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.info("exception : " + cause);
        HandlerUtil.closeOnFlush(ctx.channel());
    }

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, Object msg) throws Exception {
		
		LOG.info("channel read, msg = " + msg);
    	
    	ChannelFuture future = inboundChannel.writeAndFlush(msg);
    	
    	future.addListener(new ChannelFutureListener() {
    		
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
            	
                if (future.isSuccess()) {
                	LOG.info("successfully wrote to inbound channel");
                    ctx.channel().read();
                } else {
                	LOG.info("Unable to write to inbound channel, so closing the channel");
                    future.channel().close();
                }
            }
        });
		
	}
}
