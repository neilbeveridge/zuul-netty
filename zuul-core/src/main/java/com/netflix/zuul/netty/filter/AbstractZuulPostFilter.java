package com.netflix.zuul.netty.filter;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.FrameworkHttpResponse;

/**
 * @author HWEB
 */
public abstract class AbstractZuulPostFilter implements ZuulPostFilter {

    private final String type;
    private final int order;

    public AbstractZuulPostFilter(String type, int order) {
        this.type = type;
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
    public boolean shouldFilter(FrameworkHttpRequest request, FrameworkHttpResponse response) {
        return true;
    }

    @Override
    public int compareTo(ZuulFilter o) {
        return order - o.order();
    }

}
