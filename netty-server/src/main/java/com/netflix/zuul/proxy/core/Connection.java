package com.netflix.zuul.proxy.core;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

public class Connection {

    private final Application application;
    private final ChannelFuture channelFuture;
    private final String id;

    public Connection(Application application, ChannelFuture channelFuture) {
        this.application = application;
        this.channelFuture = channelFuture;
        this.id = Integer.toHexString(channelFuture.getChannel().getId());
    }

    public Application getApplication() {
        return application;
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
