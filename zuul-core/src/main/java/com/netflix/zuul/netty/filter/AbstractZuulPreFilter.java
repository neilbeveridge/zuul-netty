package com.netflix.zuul.netty.filter;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;

/**
 * @author HWEB
 */
public abstract class AbstractZuulPreFilter implements ZuulPreFilter {

    private final String type = "pre";
    private final int order;

    public AbstractZuulPreFilter(int order) {
        this.order = order;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public boolean shouldFilter(FrameworkHttpRequest request) {
        return true;
    }

    @Override
    public int compareTo(ZuulFilter o) {
        return order - o.order();
    }

}
