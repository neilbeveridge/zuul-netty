package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.core.InterruptsImpl;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class HttpRequestFrameworkHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestFrameworkHandler.class);

    private final HttpRequestHandler requestHandler;
    private final String tag;

    public HttpRequestFrameworkHandler(String tag, HttpRequestHandler httpRequestHandler) {
        this.requestHandler = httpRequestHandler;
        this.tag = tag;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) e.getMessage();
            InterruptsImpl callback = new InterruptsImpl(request, e.getChannel());

            if (requestHandler.isEnabled()) {
                if (requestHandler.supportedMethods() == null || Arrays.binarySearch(requestHandler.supportedMethods(), request.getMethod()) >= 0) {
                    if (requestHandler.supportedURIs() == null || Arrays.binarySearch(requestHandler.supportedURIs(), request.getUri()) >= 0) {
                        LOG.debug("handler: {} is calling request-handler: {}", tag, requestHandler.getClass().getSimpleName());
                        requestHandler.requestReceived(new HttpRequestFrameworkAdapter(request));
                    }
                }
            }

            if (callback.isInterrupted()) {
                //plugin requested that execution is interrupted i.e. not passed down the pipeline
                return;
            }
        } else if (e.getMessage() instanceof HttpChunk) {
            LOG.debug("encountered a chunk, not passed to handler");
        }

        super.messageReceived(ctx, e);
    }

}
