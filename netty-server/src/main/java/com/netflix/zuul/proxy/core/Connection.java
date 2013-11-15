package com.netflix.zuul.proxy.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.URI;

public class Connection {

    private final URI route;
    private final ChannelFuture channelFuture;
    private final String id;

    public Connection(URI route, ChannelFuture channelFuture) {
        this.route = route;
        this.channelFuture = channelFuture;
        this.id = Integer.toHexString(channelFuture.channel().hashCode());
    }

    public URI getRoute() {
        return route;
    }

    public Channel getChannel() {
        return channelFuture.channel();
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public String getId() {
        return id;
    }

}
