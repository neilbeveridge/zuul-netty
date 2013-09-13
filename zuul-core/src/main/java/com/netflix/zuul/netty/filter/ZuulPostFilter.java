package com.netflix.zuul.netty.filter;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.FrameworkHttpResponse;
import com.netflix.zuul.proxy.framework.api.HttpResponseHandler;

/**
 * Created with IntelliJ IDEA.
 * User: root
 * Date: 9/13/13
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ZuulPostFilter extends ZuulFilter, HttpResponseHandler {

    boolean shouldFilter(FrameworkHttpRequest request, FrameworkHttpResponse response);

}
