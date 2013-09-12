package com.netflix.zuul.netty.filter;

/**
 */
public interface ZuulFilter<T> extends Ordered, Comparable<ZuulFilter<T>> {

    /**
     * @return
     */
    String type();

    /**
     * @param requestContext current request context
     * @return
     */
    T execute(RequestContext requestContext);

}
