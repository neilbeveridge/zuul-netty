package com.netflix.zuul.proxy.framework.plugins;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import com.netflix.zuul.proxy.framework.api.FrameworkHttpResponse;
import com.netflix.zuul.proxy.framework.api.HttpResponseHandler;
import com.netflix.zuul.proxy.framework.api.HttpResponseHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingResponseHandler implements HttpResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingResponseHandler.class);

    private final String tag;

    public LoggingResponseHandler(String tag) {
        this.tag = tag;
    }

    @Override
    public void responseReceived(FrameworkHttpRequest request, FrameworkHttpResponse response) {
        LOG.debug("{} - received response: {}", tag, response.getHeaders());
        response.addHeader("X-hcom-some-plugin-header", "some data");
    }

    public static final HttpResponseHandlerFactory FACTORY = new HttpResponseHandlerFactory() {
        @Override
        public HttpResponseHandler getInstance(String tag) {
            return new LoggingResponseHandler(tag);
        }
    };

}
