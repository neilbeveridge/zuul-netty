package com.netflix.zuul.proxy.core;

import java.net.URI;

import com.netflix.zuul.proxy.IllegalRouteException;

public interface ConnectionPool {

    Connection borrow(URI routeHost) throws IllegalRouteException;

    void release(Connection channel);

    void destroy(Connection channel);

}
