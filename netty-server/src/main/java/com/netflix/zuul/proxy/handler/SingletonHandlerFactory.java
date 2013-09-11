package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelHandler;

public class SingletonHandlerFactory implements HandlerFactory {

    private final ChannelHandler handler;

    public SingletonHandlerFactory(ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    public ChannelHandler getInstance() {
        return handler;
    }

}
