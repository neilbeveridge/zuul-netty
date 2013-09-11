package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.netflix.zuul.proxy.InboundPipelineDefinitions.makeApplicationPipeline;
import static com.netflix.zuul.proxy.InboundPipelineDefinitions.makeAssetPipeline;

public class HttpPrefixBasedTypeRouterHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpPrefixBasedTypeRouterHandler.class);
    private static final String ASSETS_URI_PREFIX = "/asset";

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        //the initial request is used to setup the pipeline, subsequent chunks follow the same pipeline
        if (e.getMessage() instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) e.getMessage();

            if (request.getUri().startsWith(ASSETS_URI_PREFIX)) {
                LOG.info("router - request IS for an asset: {}", request.getUri());
                makeAssetPipeline(ctx.getPipeline());
            } else {
                LOG.info("router - request NOT for an asset: {}", request.getUri());
                makeApplicationPipeline(ctx.getPipeline());
            }
        } else {
        }

        super.messageReceived(ctx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        //LOG.warn("exception bubbled up to edge of proxy: {}", e);
        super.exceptionCaught(ctx, e);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        super.writeRequested(ctx, e);
    }

}
