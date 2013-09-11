package com.netflix.zuul.proxy.framework.plugins;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpResponse;
import com.netflix.zuul.proxy.framework.api.HttpResponseHandler;
import com.netflix.zuul.proxy.framework.api.HttpResponseHandlerFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.handler.codec.http.HttpMethod.GET;

public class LoggingResponseHandler implements HttpResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingResponseHandler.class);
    private static final String[] SUPPORTED_URIS = null;
    private static final HttpMethod[] SUPPORTED_METHODS = new HttpMethod[]{GET};

    private final String tag;

    public LoggingResponseHandler(String tag) {
        this.tag = tag;
    }

    @Override
    public void responseReceived(FrameworkHttpResponse response) {
        LOG.debug("{} - received response: {}", tag, response.getHeaders());
        response.addHeader("X-hcom-some-plugin-header", "some data");
    }

    public static final HttpResponseHandlerFactory FACTORY = new HttpResponseHandlerFactory() {
        @Override
        public HttpResponseHandler getInstance(String tag) {
            return new LoggingResponseHandler(tag);
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
