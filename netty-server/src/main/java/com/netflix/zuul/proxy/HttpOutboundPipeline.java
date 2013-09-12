package com.netflix.zuul.proxy;

import com.netflix.zuul.proxy.handler.ClientTimingHandler;
import com.netflix.zuul.proxy.handler.ExceptionSurfacerHandler;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

import static org.jboss.netty.channel.Channels.pipeline;

public class HttpOutboundPipeline implements ChannelPipelineFactory {

    private static final int RESPONSE_MAX_INITIAL_LINE_LENGTH = 4096;
    private static final int RESPONSE_MAX_HEADER_SIZE = (1024*16);
    private static final int RESPONSE_MAX_CHUNK_SIZE = (1024*16);

    private static final ChannelHandler EXCEPTION_SURFACER = new ExceptionSurfacerHandler();

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();

        pipeline.addLast("logging", new LoggingHandler(InternalLogLevel.DEBUG));
        pipeline.addLast("http-deflater", new HttpContentCompressor());
        pipeline.addLast("decoder", new HttpResponseDecoder(/*RESPONSE_MAX_INITIAL_LINE_LENGTH, RESPONSE_MAX_HEADER_SIZE, RESPONSE_MAX_CHUNK_SIZE*/));
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("http-inflater", new HttpContentDecompressor());
        pipeline.addLast("remote-hop-timer", new ClientTimingHandler("outbound"));
        pipeline.addLast("exception-surfacer", EXCEPTION_SURFACER);

        return pipeline;
    }

}
