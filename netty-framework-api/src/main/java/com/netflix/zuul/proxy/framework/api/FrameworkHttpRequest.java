package com.netflix.zuul.proxy.framework.api;

import org.jboss.netty.handler.codec.http.HttpMethod;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface FrameworkHttpRequest extends AttachedObjectContainer {

    /**
     * Returns the method of this request.
     */
    HttpMethod getMethod();
    
    /**
     * Returns the URI (or path) of this request.
     */
    String getUri();
    
    /**
     * Sets the URI (or path) of this request.
     */
    void setUri(String uri);
    
    /**
     * Returns the header value with the specified header name.  If there are
     * more than one header value for the specified header name, the first
     * value is returned.
     *
     * @return the header value or {@code null} if there is no such header
     */
    String getHeader(String name);
    
    /**
     * Returns {@code true} if and only if there is a header with the specified
     * header name.
     */
    boolean containsHeader(String name);
    
    /**
     * Adds a new header with the specified name and value.
     */
    void addHeader(String name, Object value);
    
    /**
     * Returns the all header names and values that this message contains.
     *
     * @return the {@link List} of the header name-value pairs.  An empty list
     *         if there is no header in this message.
     */
    List<Map.Entry<String, String>> getHeaders();

    /**
     * Set the target URI for this request.
     * @param route URI representing the host to proxy this request to. Only the scheme, host and port are taken into consideration when establishing the connection.
     */
    void setRoute (URI route);
    
}
