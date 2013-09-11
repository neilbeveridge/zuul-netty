package com.netflix.zuul.proxy.framework.api;

public interface HttpRequestHandler extends HttpHandler {

    void requestReceived(FrameworkHttpRequest request);

}
