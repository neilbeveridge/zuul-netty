package com.netflix.zuul.netty;

import com.netflix.zuul.netty.filter.RequestContext;

/**
 * @author HWEB
 */
public interface HttpHandler {

    void handle(RequestContext requestContext);
}
