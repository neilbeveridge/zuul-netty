package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.core.InterruptsImpl;
import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandler;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandlerFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

public class HttpRequestFrameworkHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestFrameworkHandler.class);

    private final HttpRequestHandlerFactory httpRequestHandlerFactory;
    private final String tag;

    public HttpRequestFrameworkHandler(String tag, HttpRequestHandlerFactory httpRequestHandlerFactory) {
        this.httpRequestHandlerFactory = httpRequestHandlerFactory;
        this.tag = tag;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) e.getMessage();
            InterruptsImpl callback = new InterruptsImpl(request, e.getChannel());

            HttpRequestHandler requestHandler = httpRequestHandlerFactory.getInstance(tag, callback);

            if (requestHandler.isEnabled()) {
                if (requestHandler.supportedMethods() == null || Arrays.binarySearch(requestHandler.supportedMethods(), request.getMethod()) >= 0) {
                    if (requestHandler.supportedURIs() == null || Arrays.binarySearch(requestHandler.supportedURIs(), request.getUri()) >= 0) {
                        LOG.debug("handler: {} is calling request-handler: {}", tag, requestHandler.getClass().getSimpleName());
                        requestHandler.requestReceived(new HttpRequestAdapter(request));
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

    public static final class HttpRequestAdapter implements FrameworkHttpRequest {

        private final HttpRequest request;

        private HttpRequestAdapter(HttpRequest request) {
            this.request = request;
        }

        @Override
        public HttpMethod getMethod() {
            return request.getMethod();
        }

        @Override
        public String getUri() {
            return request.getUri();
        }

        @Override
        public String getHeader(String name) {
            return request.getHeader(name);
        }

        @Override
        public boolean containsHeader(String name) {
            return request.containsHeader(name);
        }

        @Override
        public void addHeader(String name, Object value) {
            request.addHeader(name, value);
        }

        @Override
        public void setUri(String uri) {
            request.setUri(uri);
        }

        @Override
        public List<Entry<String, String>> getHeaders() {
            return request.getHeaders();
        }

    }

}
