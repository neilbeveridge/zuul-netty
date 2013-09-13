package com.netflix.zuul.proxy.framework.plugins;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandler;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandlerFactory;
import com.netflix.zuul.proxy.framework.api.Interrupts;

public class GlobalRewriteRequestHandler implements HttpRequestHandler {

    @Override
    public void requestReceived(FrameworkHttpRequest request) {
        switch (request.getUri()) {
            case "/foo":
                request.setUri(String.format("/bar"));
        }
    }

    public static final HttpRequestHandlerFactory FACTORY = new HttpRequestHandlerFactory() {
        @Override
        public HttpRequestHandler getInstance(String tag, Interrupts actionCallback) {
            return new GlobalRewriteRequestHandler();
        }
    };

}
