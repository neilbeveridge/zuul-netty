package com.netflix.zuul.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import com.netflix.zuul.proxy.handler.BackendClientChannelHandler;

public class BackendClientInitializer extends ChannelInitializer<SocketChannel> {
	
	private static final Logger LOG = LoggerFactory.getLogger(BackendClientInitializer.class);

	private final Channel inboundChannel;
	
	public BackendClientInitializer(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
	}

	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		
		ChannelPipeline pipeline = channel.pipeline();
		
		pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(4 * 1024));
        pipeline.addLast("loggingHandler", new LoggingHandler(LogLevel.DEBUG));
		pipeline.addLast("backEndClientChannelHandler", new BackendClientChannelHandler(inboundChannel));
		
		LOG.debug("Added handlers to channel pipeline : {}", pipeline.names());
	}
 }
