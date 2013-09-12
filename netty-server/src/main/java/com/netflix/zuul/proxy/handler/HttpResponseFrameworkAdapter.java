package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: nbeveridge
 * Date: 12/09/2013
 * Time: 13:49
 * To change this template use File | Settings | File Templates.
 */
class HttpResponseFrameworkAdapter implements FrameworkHttpResponse {

    private final HttpResponse response;

    HttpResponseFrameworkAdapter(HttpResponse response) {
        this.response = response;
    }

    @Override
    public String getHeader(String name) {
        return response.getHeader(name);
    }

    @Override
    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    @Override
    public void addHeader(String name, Object value) {
        response.addHeader(name, value);
    }

    @Override
    public List<Map.Entry<String, String>> getHeaders() {
        return response.getHeaders();
    }

}
