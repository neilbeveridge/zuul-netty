package com.netflix.zuul.proxy.framework.api;

public interface HttpResponseHandler extends HttpHandler {

    void responseReceived(FrameworkHttpResponse response);

}
