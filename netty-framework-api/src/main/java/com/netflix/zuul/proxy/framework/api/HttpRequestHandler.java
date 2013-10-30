package com.netflix.zuul.proxy.framework.api;

import org.jboss.netty.channel.ChannelHandlerContext;

public interface HttpRequestHandler {

    void requestReceived(FrameworkHttpRequest request);

}
