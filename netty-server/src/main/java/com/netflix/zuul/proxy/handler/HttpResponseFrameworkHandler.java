package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.framework.api.HttpResponseHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseFrameworkHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseFrameworkHandler.class);

    private final HttpResponseHandler responseHandler;
    private final String tag;

    public HttpResponseFrameworkHandler(String tag, HttpResponseHandler httpResponseHandler) {
        this.responseHandler = httpResponseHandler;
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

            LOG.debug("handler: {} is calling response-handler: {}", tag, responseHandler.getClass().getSimpleName());
            responseHandler.responseReceived(new HttpRequestFrameworkAdapter(request), new HttpResponseFrameworkAdapter(response));

            ctx.setAttachment(null);
        } else if (e.getMessage() instanceof HttpChunk) {
            LOG.debug("encountered a chunk, not passed to handler");
        }

        super.writeRequested(ctx, e);
    }

}
