package com.netflix.zuul.proxy.framework.api;

import org.jboss.netty.handler.codec.http.HttpMethod;

public interface HttpHandler {

    /**
     * The handler supports these HTTP Methods.
     * 
     * <ul>
     *  <li>Empty array means no HttpMethods are supported and this Handler will never be invoked.</li>
     *  <li>A NULL return value means all HttpMethods are supported.</li>
     * </ul>
     * 
     * @return array of HttpMethods supported
     */
    HttpMethod[] supportedMethods();

    /**
     * The handler supports these URIs.
     * 
     * <ul>
     *  <li>Empty array means no URIs are supported and this Handler will never be invoked.</li>
     *  <li>A NULL return value means all URIs are supported.</li>
     * </ul>
     * 
     * @return a whitelist array of URI strings that this Handler will support
     */
    String[] supportedURIs();

    /**
     * Whether this Handler is accepting requests.
     * 
     * <p>If false then this Handler will never be invoked.</p>
     * 
     * @return true if this Handler is accepting requests.
     */
    boolean isEnabled();

}
