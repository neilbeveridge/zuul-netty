package com.netflix.zuul.proxy.core;

import com.netflix.zuul.proxy.IllegalRouteException;

import java.net.URI;

public interface ConnectionPool {

    Connection borrow(URI routeHost)
    throws IllegalRouteException;

    void release(Connection channel);

    void destroy(Connection channel);

}
