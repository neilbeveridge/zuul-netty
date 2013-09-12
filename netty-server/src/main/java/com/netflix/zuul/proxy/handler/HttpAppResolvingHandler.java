package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static com.netflix.zuul.proxy.core.Route.ROUTE_HEADER;

public class HttpAppResolvingHandler extends SimpleChannelUpstreamHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpAppResolvingHandler.class);
    private static final String STATIC_ROUTE = "http://ec2-54-215-170-232.us-west-1.compute.amazonaws.com:80";
    //private static final String STATIC_ROUTE = "http://uk.hotels.com:80";

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        //only set this on request - proceeding chunks will be routed on same outbound connection
        if (e.getMessage() instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) e.getMessage();

            request.setHeader(ROUTE_HEADER, STATIC_ROUTE);
            LOG.debug("setting header {} to {}", ROUTE_HEADER, STATIC_ROUTE);

            request.setHeader("host", new URL(STATIC_ROUTE).getHost());
        }

        super.messageReceived(ctx, e);
    }
}
