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
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;

public class InboundPipelineDefinitions {

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
    private static final ChannelHandler ASSET_EXECUTION_HANDLER = new ExecutionHandler(
            new OrderedMemoryAwareThreadPoolExecutor(128, 1048576, 1048576));
    private static final ChannelHandler APP_EXECUTION_HANDLER = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(128, 1048576, 1048576));

    private static final ChannelFactory OUTBOUND_CHANNEL_FACTORY = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());
    private static final ConnectionPool OUTBOUND_CONNECTION_POOL = new CommonsConnectionPool(OUTBOUND_CHANNEL_FACTORY);

    private static final Map<String, HandlerFactory> ASSET_PIPELINE = new LinkedHashMap<>();
    private static final Map<String, HandlerFactory> APP_PIPELINE = new LinkedHashMap<>();

    static {
        //ASSETS//
        
        //response handlers
        //put response handlers first as they may rely on having seen a request which a request handler might interrupt
        ASSET_PIPELINE.put("asset-http-response-logger", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_RESPONSE_LOGGER));

        //request handlers
        ASSET_PIPELINE.put("asset-execution-handler", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(ASSET_EXECUTION_HANDLER));
        ASSET_PIPELINE.put("asset-http-geo-location", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_GEO_LOCATION));
        
        //sink
        //not setup
        
        //APPS//
        
        //response handlers
        //put response handlers first as they may rely on having seen a request which a request handler might interrupt
        APP_PIPELINE.put("app-http-response-logger", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_RESPONSE_LOGGER));

        //request handlers
        APP_PIPELINE.put("app-execution-handler", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(APP_EXECUTION_HANDLER));
        APP_PIPELINE.put("app-http-geo-location", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_GEO_LOCATION));
        APP_PIPELINE.put("app-http-geo-redirection", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_GEO_REDIRECTION));
        APP_PIPELINE.put("app-http-context-resolver", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_CONTEXT_RESOLVER));
        APP_PIPELINE.put("app-http-global-rewrite", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_GLOBAL_REWRITE));
        APP_PIPELINE.put("app-http-global-redirect", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_GLOBAL_REDIRECT));
        APP_PIPELINE.put("app-http-app-resolver", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_APP_RESOLVER));
        APP_PIPELINE.put("app-http-app-rewrite", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_APP_REWRITE));
        APP_PIPELINE.put("app-http-app-redirect", new com.netflix.zuul.proxy.handler.SingletonHandlerFactory(HTTP_APP_REDIRECT));

        //sink
        APP_PIPELINE.put("app-http-reverse-proxy", new HttpProxyHandler.Factory(OUTBOUND_CONNECTION_POOL, true));
    }

    public static void makeApplicationPipeline(ChannelPipeline pipeline) {
        removeAssetPipeline(pipeline);

        try {
            for (Entry<String, HandlerFactory> handler : APP_PIPELINE.entrySet()) {
                pipeline.addLast(handler.getKey(), handler.getValue().getInstance());
            }
        } catch (IllegalArgumentException e) {
            //assume that this is already an application pipeline
        }
    }

    private static void removeApplicationPipeline(ChannelPipeline pipeline) {
        try {
            for (String name : APP_PIPELINE.keySet()) {
                pipeline.remove(name);
            }
        } catch (NoSuchElementException e) {
            //assume that this isn't an application pipeline
        }
    }

    public static void makeAssetPipeline(ChannelPipeline pipeline) {
        removeApplicationPipeline(pipeline);

        try {
            for (Entry<String, HandlerFactory> handler : ASSET_PIPELINE.entrySet()) {
                pipeline.addLast(handler.getKey(), handler.getValue().getInstance());
            }
        } catch (IllegalArgumentException e) {
            //assume that this is already an asset pipeline
        }
    }

    private static void removeAssetPipeline(ChannelPipeline pipeline) {
        try {
            for (String name : ASSET_PIPELINE.keySet()) {
                pipeline.remove(name);
            }
        } catch (NoSuchElementException e) {
            //assume that this isn't an asset pipeline
        }
    }

}
