package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import static com.netflix.zuul.proxy.InboundPipelineDefinitions.makeApplicationPipeline;
import static com.netflix.zuul.proxy.InboundPipelineDefinitions.makeAssetPipeline;

public class HttpAcceptBasedTypeRouterHandler extends SimpleChannelUpstreamHandler {

    private static final Pattern ASSETS_ACCEPTED_REGEX = Pattern.compile(".*?(text/css|text/javascript|image).*?");
    private static final Logger LOG = LoggerFactory.getLogger(HttpAcceptBasedTypeRouterHandler.class);

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();

        if (ASSETS_ACCEPTED_REGEX.matcher(request.getHeader(HttpHeaders.Names.ACCEPT)).matches()) {
            LOG.info("router - request IS for an asset: {}", request.getUri());
            makeAssetPipeline(ctx.getPipeline());
        } else {
            LOG.info("router - request NOT for an asset: {}", request.getUri());
            makeApplicationPipeline(ctx.getPipeline());
        }
        
        super.messageReceived(ctx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        //LOG.warn("exception bubbled up to edge of proxy: {}", e);
        super.exceptionCaught(ctx, e);
    }
}
