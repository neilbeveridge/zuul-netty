package com.netflix.zuul.proxy.framework.api;

public interface HttpResponseHandler extends HttpHandler {

    int order();
    String type();
    void responseReceived(FrameworkHttpRequest request, FrameworkHttpResponse response);

}
