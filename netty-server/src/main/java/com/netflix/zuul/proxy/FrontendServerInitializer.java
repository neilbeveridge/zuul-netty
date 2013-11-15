package com.netflix.zuul.proxy;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.handler.FrontEndServerHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class FrontendServerInitializer extends ChannelInitializer<SocketChannel> {

	private static final Logger LOG = LoggerFactory.getLogger(FrontendServerInitializer.class);
	
	private final String remoteHost;
    private final int remotePort;

    public FrontendServerInitializer(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		
		ChannelPipeline p = ch.pipeline();

		p.addLast(new LoggingHandler(LogLevel.INFO));
		
		p.addLast("requestDecoder", new HttpRequestDecoder());
		p.addLast("responseEncoder", new HttpResponseEncoder());
		
		p.addLast("responeDecoder", new HttpResponseDecoder());
		p.addLast("requestEncoder", new HttpRequestEncoder());
		
		// TODO : determine what a sensible max size is for the HTTP message, here we've defined it as 4kb (which may be too small ... ?)
		// TODO : determine whether HttpObjectAggregator keeps chunks "on-heap".
		p.addLast("aggregator", new HttpObjectAggregator(4 * 1024)); 
		
		p.addLast("frontendServer", new FrontEndServerHandler(remoteHost, remotePort));
		
		LOG.info("Added handlers to channel pipeline : " + p.names());
	}

}
