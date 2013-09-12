package com.netflix.zuul.proxy.core;

import com.netflix.zuul.proxy.IllegalRouteException;

import java.net.URL;

public interface ConnectionPool {

    Connection borrow(URL routeHost)
    throws IllegalRouteException;

    void release(Connection channel);

    void destroy(Connection channel);

}
