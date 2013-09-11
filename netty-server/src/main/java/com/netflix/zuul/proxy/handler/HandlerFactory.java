package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelHandler;

public interface HandlerFactory {
    ChannelHandler getInstance();
}
