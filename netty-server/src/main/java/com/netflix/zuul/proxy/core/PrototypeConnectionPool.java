package com.netflix.zuul.proxy.core;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;

public class PrototypeConnectionPool implements ConnectionPool {

    private final ClientBootstrap bootstrap;

    public PrototypeConnectionPool(ClientBootstrap clientBootstrap) {
        this.bootstrap = clientBootstrap;
    }

    @Override
    public Connection borrow(Application application) {
        final ChannelFuture future = bootstrap.connect(new InetSocketAddress(application.getHost(), application.getPort()));

        return new Connection(application, future);
    }

    @Override
    public void release(Connection channel) {
    }

    @Override
    public void destroy(Connection channel) {
    }

}
