package com.netflix.zuul.proxy.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.framework.api.HttpResponseHandler;

public class HttpResponseFrameworkHandler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseFrameworkHandler.class);

    private final HttpResponseHandler responseHandler;
    private final String tag;

    public HttpResponseFrameworkHandler(String tag, HttpResponseHandler httpResponseHandler) {
        this.responseHandler = httpResponseHandler;
        this.tag = tag;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        LOG.debug("attaching message: {}", message.getClass().getSimpleName());
        if (message instanceof HttpRequest) {
            context.attr(new AttributeKey<>("request")).set(message);
        }
        super.channelRead(context, message);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) message;
            HttpRequest request = (HttpRequest) context.attr(new AttributeKey<>("request")).get();

            LOG.debug("handler: {} is calling response-handler: {}", tag, responseHandler.getClass().getSimpleName());
            responseHandler.responseReceived(new HttpRequestFrameworkAdapter(context, request), new HttpResponseFrameworkAdapter(context, response));

            context.attr(new AttributeKey<>("request")).set(null);
        }

        super.write(context, message, promise);
    }
}
