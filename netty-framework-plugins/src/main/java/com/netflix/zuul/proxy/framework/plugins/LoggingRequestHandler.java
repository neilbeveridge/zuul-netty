package com.netflix.zuul.proxy.framework.plugins;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandler;
import com.netflix.zuul.proxy.framework.api.HttpRequestHandlerFactory;
import com.netflix.zuul.proxy.framework.api.Interrupts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingRequestHandler implements HttpRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingRequestHandler.class);

    private final String tag;

    private LoggingRequestHandler(String tag) {
        this.tag = tag;
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


}
