package com.netflix.zuul.proxy.framework.api;


public interface HttpResponseHandlerFactory {

    HttpResponseHandler getInstance(String tag);

}
