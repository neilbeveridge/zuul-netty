package com.netflix.zuul.proxy.framework.api;

public interface HttpRequestHandler {

    void requestReceived(FrameworkHttpRequest request);

}
