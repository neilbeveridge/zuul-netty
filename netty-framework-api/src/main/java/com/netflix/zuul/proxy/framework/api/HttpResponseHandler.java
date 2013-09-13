package com.netflix.zuul.proxy.framework.api;

public interface HttpResponseHandler {

    void responseReceived(FrameworkHttpRequest request, FrameworkHttpResponse response);

}
