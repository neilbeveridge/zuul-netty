package com.netflix.zuul.proxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.core.InterruptsImpl;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandler;

public class HttpRequestFrameworkHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestFrameworkHandler.class);

    private final HttpRequestHandler requestHandler;
    private final String tag;

    public HttpRequestFrameworkHandler(String tag, HttpRequestHandler httpRequestHandler) {
        LOG.debug("requestHandler = {}", httpRequestHandler);
        this.requestHandler = httpRequestHandler;
        this.tag = tag;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        LOG.debug("Channel Active");

        super.channelActive(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        LOG.debug("Channel Read with message {} of type {}", message, message.getClass().getSimpleName());

        if (message instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) message;
            InterruptsImpl callback = new InterruptsImpl(request, context.channel());
            LOG.debug("handler: {} is calling request-handler: {}", tag, requestHandler.getClass().getSimpleName());
            requestHandler.requestReceived(new HttpRequestFrameworkAdapter(context, request));


            if (callback.isInterrupted()) {
                LOG.debug("Interrupted");
                //plugin requested that execution is interrupted i.e. not passed down the pipeline
                return;
            }
        }

        LOG.debug("Just before ctx.read()");

        context.fireChannelRead(message);
    }
}
