package com.netflix.zuul.proxy.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

public class HandlerUtil {
	
	/**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
    	
    	// wait for the outboundChannel to be active, i.e. this will only happen once the channelActive() method has completed
        if (ch.isActive()) {
        	
        	// Write an empty buffer to indicate the end of the chunked transfer.
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}