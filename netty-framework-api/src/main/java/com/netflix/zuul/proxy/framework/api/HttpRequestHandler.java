package com.netflix.zuul.proxy.framework.api;

public interface HttpRequestHandler extends HttpHandler {

    int order();

    String type();

    void requestReceived(FrameworkHttpRequest request);

}
