package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.core.AttachedObjectContainerNetty;
import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: nbeveridge
 * Date: 12/09/2013
 * Time: 13:48
 * To change this template use File | Settings | File Templates.
 */
class HttpRequestFrameworkAdapter implements FrameworkHttpRequest {

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
        return request.getHeader(name);
    }

    @Override
    public boolean containsHeader(String name) {
        return request.containsHeader(name);
    }

    @Override
    public void addHeader(String name, Object value) {
        request.addHeader(name, value);
    }

    @Override
    public void setUri(String uri) {
        request.setUri(uri);
    }

    @Override
    public List<Map.Entry<String, String>> getHeaders() {
        return request.getHeaders();
    }

    @Override
    public void setRoute(URI route) {
        AttachedObjectContainerNetty.containerFor(context).attachObject(HttpProxyHandler.ROUTE_OBJECT, route);
    }

    @Override
    public void attachObject(String name, Object object) {
        AttachedObjectContainerNetty.containerFor(context).attachObject("user." + name, object);
    }

    @Override
    public <T extends Object> T detachObject(String name, Class<T> type) {
        return AttachedObjectContainerNetty.containerFor(context).detachObject("user." + name, type);
    }

    @Override
    public <T extends Object> T attachedObject(String name, Class<T> type) {
        return AttachedObjectContainerNetty.containerFor(context).attachedObject("user." + name, type);
    }

}
