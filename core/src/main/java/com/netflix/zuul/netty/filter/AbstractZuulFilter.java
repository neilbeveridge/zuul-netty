package com.netflix.zuul.netty.filter;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * @author HWEB
 */
public class AbstractZuulFilter implements ZuulFilter {

    private final String type;
    private final int order;

    public AbstractZuulFilter(String type, int order) {
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
    public boolean shouldFilter(HttpRequest request, HttpResponse response) {
        return false;
    }

    @Override
    public Void execute(HttpRequest request, HttpResponse response) {
        return null;
    }
}
