package com.netflix.zuul.proxy;

import com.netflix.zuul.proxy.handler.HttpKeepAliveHandler;
import com.netflix.zuul.proxy.handler.HttpPrefixBasedTypeRouterHandler;
import com.netflix.zuul.proxy.handler.ServerTimingHandler;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;

import static org.jboss.netty.channel.Channels.pipeline;

public class CommonHttpPipeline implements ChannelPipelineFactory {

    //seconds until the TCP connection will close
    private static final int IDLE_TIMEOUT_READER = 0;
    private static final int IDLE_TIMEOUT_WRITER = 0;
    private static final int IDLE_TIMEOUT_BOTH = 10;

    private static final boolean IS_KEEP_ALIVE_SUPPORTED = true;

    private final ChannelHandler IDLE_STATE_HANDLER;
    private final ChannelHandler KEEP_ALIVE_HANDLER = new HttpKeepAliveHandler(IS_KEEP_ALIVE_SUPPORTED);
    private final ChannelHandler PREFIX_TYPE_ROUTER = new HttpPrefixBasedTypeRouterHandler();
    //private final ChannelHandler ACCEPT_TYPE_ROUTER = new HttpAcceptBasedTypeRouterHandler();

    public CommonHttpPipeline(Timer timer) {
        IDLE_STATE_HANDLER = new IdleStateHandler(timer, IDLE_TIMEOUT_READER, IDLE_TIMEOUT_WRITER, IDLE_TIMEOUT_BOTH);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();

        pipeline.addLast("idle-detection", IDLE_STATE_HANDLER);
        pipeline.addLast("http-decoder", new HttpRequestDecoder());
        pipeline.addLast("http-encoder", new HttpResponseEncoder());
        pipeline.addLast("edge-timer", new ServerTimingHandler("inbound"));
        pipeline.addLast("http-keep-alive", KEEP_ALIVE_HANDLER);
        pipeline.addLast("http-deflater", new HttpContentCompressor());
        pipeline.addLast("http-pipeline-router", PREFIX_TYPE_ROUTER);

        return pipeline;
    }
}
