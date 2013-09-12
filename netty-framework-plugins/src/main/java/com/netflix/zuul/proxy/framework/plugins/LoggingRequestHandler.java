package com.netflix.zuul.proxy.framework.plugins;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandler;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandlerFactory;
import com.netflix.zuul.proxy.framework.api.Interrupts;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.handler.codec.http.HttpMethod.GET;

public class LoggingRequestHandler implements HttpRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingRequestHandler.class);
    private static final String[] SUPPORTED_URIS = null;
    private static final HttpMethod[] SUPPORTED_METHODS = new HttpMethod[]{GET};

    private final String tag;

    private LoggingRequestHandler(String tag) {
        this.tag = tag;
    }

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
        LOG.debug("{} - received request: {} {}", tag, request.getUri(), request.getHeaders());
    }

    public static final HttpRequestHandlerFactory FACTORY = new HttpRequestHandlerFactory() {
        @Override
        public HttpRequestHandler getInstance(String tag, Interrupts actionCallback) {
            return new LoggingRequestHandler(tag);
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
