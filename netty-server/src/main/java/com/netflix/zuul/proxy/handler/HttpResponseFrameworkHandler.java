package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpResponse;
import com.netflix.zuul.proxy.framework.api.HttpResponseHandler;
import com.netflix.zuul.proxy.framework.api.HttpResponseHandlerFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

public class HttpResponseFrameworkHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseFrameworkHandler.class);

    private final HttpResponseHandlerFactory httpResponseHandlerFactory;
    private final String tag;

    public HttpResponseFrameworkHandler(String tag, HttpResponseHandlerFactory httpResponseHandlerFactory) {
        this.httpResponseHandlerFactory = httpResponseHandlerFactory;
        this.tag = tag;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        LOG.debug("attaching message: {}", e.getMessage().getClass().getSimpleName());
        if (e.getMessage() instanceof HttpRequest) {
            ctx.setAttachment(e.getMessage());
        }
        super.messageReceived(ctx, e);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) e.getMessage();
            HttpRequest request = (HttpRequest) ctx.getAttachment();

            HttpResponseHandler responseHandler = httpResponseHandlerFactory.getInstance(tag);

            if (responseHandler.isEnabled()) {
                if (responseHandler.supportedMethods() == null || Arrays.binarySearch(responseHandler.supportedMethods(), request.getMethod()) >= 0) {
                    if (responseHandler.supportedURIs() == null || Arrays.binarySearch(responseHandler.supportedURIs(), request.getUri()) >= 0) {
                        LOG.debug("handler: {} is calling response-handler: {}", tag, responseHandler.getClass().getSimpleName());
                        responseHandler.responseReceived(new HttpResponseAdapter(response));
                    }
                }
            }

            ctx.setAttachment(null);
        } else if (e.getMessage() instanceof HttpChunk) {
            LOG.debug("encountered a chunk, not passed to handler");
        }

        super.writeRequested(ctx, e);
    }

    public static final class HttpResponseAdapter implements FrameworkHttpResponse {

        private final HttpResponse response;

        private HttpResponseAdapter(HttpResponse response) {
            this.response = response;
        }

        @Override
        public String getHeader(String name) {
            return response.getHeader(name);
        }

        @Override
        public boolean containsHeader(String name) {
            return response.containsHeader(name);
        }

        @Override
        public void addHeader(String name, Object value) {
            response.addHeader(name, value);
        }

        @Override
        public List<Entry<String, String>> getHeaders() {
            return response.getHeaders();
        }

    }

}
