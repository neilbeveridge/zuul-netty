package com.netflix.zuul.netty.filter;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 */
public interface ZuulFilter<T> extends Ordered {

    /**
     * @return
     */
    String type();

    /**
     * a "true" return from this method means that the run() method should be invoked
     *
     * @return true if the run() method should be invoked. false will not invoke the run() method
     */
    boolean shouldFilter(HttpRequest request, HttpResponse response);

    /**
     * @param request
     * @param response
     */
    T execute(HttpRequest request, HttpResponse response);
}
