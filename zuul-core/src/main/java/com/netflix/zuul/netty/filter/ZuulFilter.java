package com.netflix.zuul.netty.filter;

/**
 */
public interface ZuulFilter extends Ordered, Comparable<ZuulFilter> {

    /**
     * @return
     */
    String type();

}
