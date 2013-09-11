package com.netflix.zuul.proxy.framework.api;


public interface HttpRequestHandlerFactory {

    HttpRequestHandler getInstance(String tag, com.netflix.zuul.proxy.framework.api.Interrupts interrupts);

}
