package com.netflix.zuul.proxy.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

public class HandlerUtil {
	
	/**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
    	
    	// only attempt to close the channel if it is active.
        if (ch.isActive()) {
        	
        	// Write an empty buffer to indicate the end of the chunked transfer.
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}