package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.core.Application;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;

import static com.netflix.zuul.proxy.core.Application.APP_HEADER;
import static com.netflix.zuul.proxy.core.Application.DEFAULT_APPLICATION;

public class HttpAppResolvingHandler extends SimpleChannelUpstreamHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        //only set this on request - proceeding chunks will be routed on same outbound connection
        if (e.getMessage() instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) e.getMessage();

            for (Application app : Application.values()) {
                if (request.getUri().startsWith(app.getPrefix())) {
                    request.setHeader(APP_HEADER, app.name());
                    break;
                }
            }

            for (Application app : Application.values()) {
                for (String uri : app.getUris()) {
                    if (request.getUri().equals(uri)) {
                        request.setHeader(APP_HEADER, app.name());
                        break;
                    }
                }
            }

            if (!request.containsHeader(APP_HEADER)) {
                request.setHeader(APP_HEADER, DEFAULT_APPLICATION.name());
            }
        }
        
        super.messageReceived(ctx, e);
    }
}
