package com.netflix.zuul.proxy.core;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import java.net.URL;

public class Connection {

    private final URL routeHost;
    private final ChannelFuture channelFuture;
    private final String id;

    public Connection(URL routeHost, ChannelFuture channelFuture) {
        this.routeHost = routeHost;
        this.channelFuture = channelFuture;
        this.id = Integer.toHexString(channelFuture.getChannel().getId());
    }

    public URL getRouteHost() {
        return routeHost;
    }

    public Channel getChannel() {
        return channelFuture.getChannel();
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public String getId() {
        return id;
    }

}
