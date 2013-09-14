package com.netflix.zuul.proxy;

import com.netflix.zuul.proxy.core.CommonsConnectionPool;
import com.netflix.zuul.proxy.core.ConnectionPool;
import com.netflix.zuul.proxy.framework.plugins.GlobalRedirectRequestHandler;
import com.netflix.zuul.proxy.framework.plugins.GlobalRewriteRequestHandler;
import com.netflix.zuul.proxy.framework.plugins.LoggingRequestHandler;
import com.netflix.zuul.proxy.framework.plugins.LoggingResponseHandler;
import com.netflix.zuul.proxy.handler.*;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;

import java.util.concurrent.Executors;

import static org.jboss.netty.channel.Channels.pipeline;

public class CommonHttpPipeline implements ChannelPipelineFactory {

    //seconds until the TCP connection will close
    private static final int IDLE_TIMEOUT_READER = 0;
    private static final int IDLE_TIMEOUT_WRITER = 0;
    private static final int IDLE_TIMEOUT_BOTH = 10;

    private static final boolean IS_KEEP_ALIVE_SUPPORTED = true;
    private static final boolean IS_REQUEST_CHUNKED_ENABLED = true;

    private static final ChannelHandler HTTP_GEO_LOCATION = new HttpRequestFrameworkHandler("http-geo-location", LoggingRequestHandler.FACTORY);
    private static final ChannelHandler HTTP_GEO_REDIRECTION = new HttpRequestFrameworkHandler("http-geo-redirection", LoggingRequestHandler.FACTORY);
    private static final ChannelHandler HTTP_CONTEXT_RESOLVER = new HttpRequestFrameworkHandler("http-context-resolver",
            LoggingRequestHandler.FACTORY);
    private static final ChannelHandler HTTP_GLOBAL_REWRITE = new HttpRequestFrameworkHandler("http-global-rewrite",
            GlobalRewriteRequestHandler.FACTORY);
    private static final ChannelHandler HTTP_GLOBAL_REDIRECT = new HttpRequestFrameworkHandler("http-global-redirect",
            GlobalRedirectRequestHandler.FACTORY);
    private static final ChannelHandler HTTP_APP_RESOLVER = new HttpAppResolvingHandler();
    private static final ChannelHandler HTTP_APP_REWRITE = new HttpRequestFrameworkHandler("http-app-rewrite", LoggingRequestHandler.FACTORY);
    private static final ChannelHandler HTTP_APP_REDIRECT = new HttpRequestFrameworkHandler("http-app-redirect", LoggingRequestHandler.FACTORY);
    private static final ChannelHandler HTTP_RESPONSE_LOGGER = new HttpResponseFrameworkHandler("http-response-logger",
            LoggingResponseHandler.FACTORY);
    private static final ChannelHandler APP_EXECUTION_HANDLER = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(128, 1048576, 1048576));
    private static final ChannelFactory OUTBOUND_CHANNEL_FACTORY = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());

    private final ConnectionPool outboundConnectionPool;
    private final ChannelHandler idleStateHandler;
    private final ChannelHandler KEEP_ALIVE_HANDLER = new HttpKeepAliveHandler(IS_KEEP_ALIVE_SUPPORTED);

    public CommonHttpPipeline(Timer timer) {
        idleStateHandler = new IdleStateHandler(timer, IDLE_TIMEOUT_READER, IDLE_TIMEOUT_WRITER, IDLE_TIMEOUT_BOTH);
        outboundConnectionPool = new CommonsConnectionPool(timer, OUTBOUND_CHANNEL_FACTORY);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();

        pipeline.addLast("app-execution-handler", APP_EXECUTION_HANDLER);

        pipeline.addLast("idle-detection", idleStateHandler);
        pipeline.addLast("http-decoder", new HttpRequestDecoder());
        pipeline.addLast("http-encoder", new HttpResponseEncoder());
        pipeline.addLast("http-deflater", new HttpContentCompressor());
        pipeline.addLast("edge-timer", new ServerTimingHandler("inbound"));
        pipeline.addLast("http-keep-alive", KEEP_ALIVE_HANDLER);
        pipeline.addLast("idle-watchdog", new IdleChannelWatchdog("inbound"));

        pipeline.addLast("app-http-response-logger", HTTP_RESPONSE_LOGGER);

        //request handlers
        //pipeline.addLast("app-execution-handler", APP_EXECUTION_HANDLER);
        //pipeline.addLast("app-http-geo-location", HTTP_GEO_LOCATION);
        //pipeline.addLast("app-http-geo-redirection", HTTP_GEO_REDIRECTION);
        //pipeline.addLast("app-http-context-resolver", HTTP_CONTEXT_RESOLVER);
        //pipeline.addLast("app-http-global-rewrite", HTTP_GLOBAL_REWRITE);
        //pipeline.addLast("app-http-global-redirect", HTTP_GLOBAL_REDIRECT);
        pipeline.addLast("app-http-app-resolver", HTTP_APP_RESOLVER);
        //pipeline.addLast("app-http-app-rewrite", HTTP_APP_REWRITE);
        //pipeline.addLast("app-http-app-redirect", HTTP_APP_REDIRECT);

        pipeline.addLast("proxy", new HttpProxyHandler(outboundConnectionPool, IS_REQUEST_CHUNKED_ENABLED));

        return pipeline;
    }
}
