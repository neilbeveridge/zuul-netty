package com.netflix.zuul.proxy;

import com.netflix.zuul.proxy.handler.ClientTimingHandler;
import com.netflix.zuul.proxy.handler.ExceptionSurfacerHandler;
import com.netflix.zuul.proxy.handler.IdleChannelWatchdog;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.logging.InternalLogLevel;

import io.netty.util.Timer;

public class HttpOutboundPipeline extends ChannelInitializer<SocketChannel> {

    private static final int RESPONSE_MAX_INITIAL_LINE_LENGTH = 4096;
    private static final int RESPONSE_MAX_HEADER_SIZE = (1024*8);
    private static final int RESPONSE_MAX_CHUNK_SIZE = (1024*8);

    //seconds until the TCP connection will close
    private static final int IDLE_TIMEOUT_READER = 0;
    private static final int IDLE_TIMEOUT_WRITER = 0;
    private static final int IDLE_TIMEOUT_BOTH = 10;

    private static final ChannelHandler EXCEPTION_SURFACER = new ExceptionSurfacerHandler();
    private final ChannelHandler IDLE_STATE_HANDLER;

    public HttpOutboundPipeline (Timer timer) {
        IDLE_STATE_HANDLER = new IdleStateHandler(timer, IDLE_TIMEOUT_READER, IDLE_TIMEOUT_WRITER, IDLE_TIMEOUT_BOTH);
    }

	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		
		ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("idle-detection", IDLE_STATE_HANDLER);
        pipeline.addLast("logging", new LoggingHandler(InternalLogLevel.DEBUG));
        pipeline.addLast("http-deflater", new HttpContentCompressor());
        pipeline.addLast("decoder", new HttpResponseDecoder(RESPONSE_MAX_INITIAL_LINE_LENGTH, RESPONSE_MAX_HEADER_SIZE, RESPONSE_MAX_CHUNK_SIZE));
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("http-inflater", new HttpContentDecompressor());
        pipeline.addLast("remote-hop-timer", new ClientTimingHandler("outbound"));
        pipeline.addLast("exception-surfacer", EXCEPTION_SURFACER);
        pipeline.addLast("idle-watchdog", new IdleChannelWatchdog("outbound"));
		
	}

}
