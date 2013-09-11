package com.netflix.zuul.proxy.core;

public interface ConnectionPool {

    Connection borrow(Application application);

    void release(Connection channel);

    void destroy(Connection channel);

}
