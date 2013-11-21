package com.netflix.zuul.proxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;

/**
 * Created with IntelliJ IDEA.
 * User: nbeveridge
 * Date: 12/09/2013
 * Time: 13:48
 * To change this template use File | Settings | File Templates.
 */
class HttpRequestFrameworkAdapter implements FrameworkHttpRequest {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestFrameworkAdapter.class);

    private final HttpRequest request;
    private final ChannelHandlerContext context;

    HttpRequestFrameworkAdapter(ChannelHandlerContext context, HttpRequest request) {
        this.request = request;
        this.context = context;
    }

    @Override
    public HttpMethod getMethod() {
        return request.getMethod();
    }

    @Override
    public String getUri() {
        return request.getUri();
    }

    @Override
    public String getHeader(String name) {
        return request.headers().get(name);
    }

    @Override
    public boolean containsHeader(String name) {
        return request.headers().contains(name);
    }

    @Override
    public void addHeader(String name, Object value) {
        request.headers().add(name, value);
    }

    @Override
    public void setUri(String uri) {
        request.setUri(uri);
    }

    @Override
    public List<Map.Entry<String, String>> getHeaders() {
        return request.headers().entries();
    }

    @Override
    public void setRoute(URI route) {
        LOG.debug("channel = {}, route = {}", context.channel().hashCode(), route);

        context.channel().attr(Routing.ROUTE_KEY).set(route);
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
