package com.netflix.zuul.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

import com.netflix.zuul.proxy.handler.BackendClientChannelHandler;

public class BackendClientInitializer extends ChannelInitializer<SocketChannel> {

	private final Channel inboundChannel;
	
	public BackendClientInitializer(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
	}

	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		
		ChannelPipeline pipeline = channel.pipeline();
		
		pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(4 * 1024));
		pipeline.addLast(new BackendClientChannelHandler(inboundChannel));
	}
 }
