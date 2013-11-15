package com.netflix.zuul.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.FullHttpRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.BackendClientInitializer;

public class FrontEndServerHandler extends ChannelInboundHandlerAdapter {
	
	private static final Logger LOG = LoggerFactory.getLogger(FrontEndServerHandler.class);
	
	private final String remoteHost;
    private final int remotePort;

    // outboundChannel is volatile since it is shared between threads (this handler & BackendClientChannelHandler).
    private volatile Channel outboundChannel;
	
	public FrontEndServerHandler(String remoteHost, int remotePort) {
		this.remoteHost = remoteHost;
        this.remotePort = remotePort;
	}

	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		
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
        outboundChannel = future.channel();
        
        future.addListener(new ChannelFutureListener() {
        	
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
            
        });
    }

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		
		// wait for the outboundChannel to be active, i.e. this will only happen once the channelActive() method has completed
		if (outboundChannel.isActive()) {
			
			LOG.info("Server received: " + msg);
			
			// Only forward HttpRequest messages to the back-end server.
			if (msg instanceof FullHttpRequest) {
	            
				FullHttpRequest completeMsg = (FullHttpRequest) msg;
				
				ChannelFuture future = outboundChannel.writeAndFlush(completeMsg);
	            
	            future.addListener(new ChannelFutureListener() {
	            	
	                @Override
	                public void operationComplete(ChannelFuture future) throws Exception {
	                	
	                    if (future.isSuccess()) {
	                    	LOG.info("successfully wrote to outbound channel");
	                        // was able to flush out data, start to read the next chunk
	                        ctx.channel().read();
	                    } else {
	                    	LOG.info("Unable to write to outbound channel due to : " + future.cause());
	                        future.channel().close();
	                    }
	                }
	                
	            });
			}			
            
        }
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		
		LOG.info("channelReadComplete triggered, so writing data");
		
		ctx.flush();
		
		/*
		ChannelFuture future = ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
		
		future.addListener(ChannelFutureListener.CLOSE);
		*/
	}
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		
        if (outboundChannel != null) {
        	HandlerUtil.closeOnFlush(outboundChannel);
        }
    }

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		
		LOG.info("caught exception : {}", cause);
		//ctx.close();
		
		HandlerUtil.closeOnFlush(ctx.channel());
	}
	
	

}
