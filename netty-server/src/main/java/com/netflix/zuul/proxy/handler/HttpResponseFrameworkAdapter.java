package com.netflix.zuul.proxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;

import java.util.List;
import java.util.Map;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpResponse;

/**
 * Created with IntelliJ IDEA.
 * User: nbeveridge
 * Date: 12/09/2013
 * Time: 13:49
 * To change this template use File | Settings | File Templates.
 */
class HttpResponseFrameworkAdapter implements FrameworkHttpResponse {

    private final HttpResponse response;
    private final ChannelHandlerContext context;

    HttpResponseFrameworkAdapter(ChannelHandlerContext context, HttpResponse response) {
        this.response = response;
        this.context = context;
    }

    @Override
    public String getHeader(String name) {
        return response.headers().get(name);
    }

    @Override
    public boolean containsHeader(String name) {
        return response.headers().contains(name);
    }

    @Override
    public void addHeader(String name, Object value) {
        response.headers().add(name, value);
    }

    @Override
    public List<Map.Entry<String, String>> getHeaders() {
        return response.headers().entries();
    }

    @Override
    public void attachObject(String name, Object object) {
        context.channel().attr(new AttributeKey<>("user." + name)).set(object);
    }

    @Override
    public <T extends Object> T detachObject(String name, Class<T> type) {
        return (T) context.channel().attr(new AttributeKey<>("user." + name)).getAndSet(null);
    }

    @Override
    public <T extends Object> T attachedObject(String name, Class<T> type) {
        return (T) context.channel().attr(new AttributeKey<>("user." + name)).get();
    }
}
