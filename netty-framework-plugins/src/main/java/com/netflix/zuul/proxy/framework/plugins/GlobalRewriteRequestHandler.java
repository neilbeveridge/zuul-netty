package com.netflix.zuul.proxy.framework.plugins;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandler;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandlerFactory;
import com.netflix.zuul.proxy.framework.api.Interrupts;
import org.jboss.netty.handler.codec.http.HttpMethod;

import static org.jboss.netty.handler.codec.http.HttpMethod.GET;

public class GlobalRewriteRequestHandler implements HttpRequestHandler {

    private static final String[] SUPPORTED_URIS = new String[]{"/foo"};
    private static final HttpMethod[] SUPPORTED_METHODS = new HttpMethod[]{GET};

    @Override
    public int order() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String type() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

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

    @Override
    public HttpMethod[] supportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public String[] supportedURIs() {
        return SUPPORTED_URIS;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
