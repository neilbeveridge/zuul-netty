package com.netflix.zuul.proxy.core;

import com.netflix.zuul.proxy.IllegalRouteException;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.net.URL;

public class PrototypeConnectionPool implements ConnectionPool {

    private final ClientBootstrap bootstrap;

    public PrototypeConnectionPool(ClientBootstrap clientBootstrap) {
        this.bootstrap = clientBootstrap;
    }

    @Override
    public Connection borrow(URL routeHost)
            throws IllegalRouteException {
        final ChannelFuture future = bootstrap.connect(new InetSocketAddress(routeHost.getHost(), routeHost.getPort()));

        return new Connection(routeHost, future);
    }

    @Override
    public void release(Connection channel) {
    }

    @Override
    public void destroy(Connection channel) {
    }

}
