package com.netflix.zuul.netty.filter;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.netflix.util.Pair;
import com.netflix.zuul.constants.ZuulHeaders;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author HWEB
 */
public class RequestContext extends ConcurrentHashMap<String, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(RequestContext.class);

    protected static Class<? extends RequestContext> contextClass = RequestContext.class;

    private static RequestContext testContext = null;
    private final HttpRequest request;
    private final HttpResponse response;
    private final ChannelHandlerContext channelHandlerContext;

    public RequestContext(ChannelHandlerContext channelHandlerContext, HttpRequest request, HttpResponse response) {
        this.channelHandlerContext = channelHandlerContext;
        this.request = request;
        this.response = response;
        setRequest(request);
        setResponse(response);
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        setRequestQueryParams(queryStringDecoder.parameters());
    }

    /**
     * Override the default RequestContext
     *
     * @param clazz
     */
    public static void setContextClass(Class<? extends RequestContext> clazz) {
        contextClass = clazz;
    }

    /**
     * set an overriden "test" context
     *
     * @param context
     */
    public static void testSetCurrentContext(RequestContext context) {
        testContext = context;
    }


    /**
     * Convenience method to return a boolean value for a given key
     *
     * @param key
     * @return true or false depending what was set. default is false
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Convenience method to return a boolean value for a given key
     *
     * @param key
     * @param defaultResponse
     * @return true or false depending what was set. default defaultResponse
     */
    public boolean getBoolean(String key, boolean defaultResponse) {
        Boolean b = (Boolean) get(key);
        if (b != null) {
            return b.booleanValue();
        }
        return defaultResponse;
    }

    /**
     * sets a key value to Boolen.TRUE
     *
     * @param key
     */
    public void set(String key) {
        put(key, Boolean.TRUE);
    }

    /**
     * puts the key, value into the map. a null value will remove the key from the map
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        if (value != null) put(key, value);
        else remove(key);
    }

    /**
     * true if  zuulEngineRan
     *
     * @return
     */
    public boolean getZuulEngineRan() {
        return getBoolean("zuulEngineRan");
    }

    /**
     * sets zuulEngineRan to true
     */
    public void setZuulEngineRan() {
        put("zuulEngineRan", true);
    }

    /**
     * @return the HttpServletRequest from the "request" key
     */
    public HttpRequest getRequest() {
        return (HttpRequest) get("request");
    }

    /**
     * sets the HttpServletRequest into the "request" key
     *
     * @param request
     */
    private void setRequest(HttpRequest request) {
        put("request", request);
    }

    /**
     * @return the HttpServletResponse from the "response" key
     */
    public HttpResponse getResponse() {
        return (HttpResponse) get("response");
    }

    private void setResponse(HttpResponse response) {
        set("response", response);
    }

    /**
     * returns a set throwable
     *
     * @return a set throwable
     */
    public Throwable getThrowable() {
        return (Throwable) get("throwable");

    }

    /**
     * sets a throwable
     *
     * @param th
     */
    public void setThrowable(Throwable th) {
        put("throwable", th);

    }

    /**
     * sets  debugRouting
     *
     * @param bDebug
     */
    public void setDebugRouting(boolean bDebug) {
        set("debugRouting", bDebug);
    }

    /**
     * @return "debugRouting"
     */
    public boolean debugRouting() {
        return getBoolean("debugRouting");
    }

    /**
     * sets "debugRequestHeadersOnly" to bHeadersOnly
     *
     * @param bHeadersOnly
     */
    public void setDebugRequestHeadersOnly(boolean bHeadersOnly) {
        set("debugRequestHeadersOnly", bHeadersOnly);

    }

    /**
     * @return "debugRequestHeadersOnly"
     */
    public boolean debugRequestHeadersOnly() {
        return getBoolean("debugRequestHeadersOnly");
    }

    /**
     * sets "debugRequest"
     *
     * @param bDebug
     */
    public void setDebugRequest(boolean bDebug) {
        set("debugRequest", bDebug);
    }

    /**
     * gets debugRequest
     *
     * @return debugRequest
     */
    public boolean debugRequest() {
        return getBoolean("debugRequest");
    }

    /**
     * removes "routeHost" key
     */
    public void removeRouteHost() {
        remove("routeHost");
    }

    /**
     * sets routeHost
     *
     * @param routeHost a URL
     */
    public void setRouteHost(URL routeHost) {
        set("routeHost", routeHost);
    }

    /**
     * @return "routeHost" URL
     */
    public URL getRouteHost() {
        return (URL) get("routeHost");
    }


    /**
     * sets the "responseBody" value as a String. This is the response sent back to the client.
     *
     * @param body
     */
    public void setResponseBody(String body) {
        set("responseBody", body);
    }

    /**
     * @return the String response body to be snt back to the requesting client
     */
    public String getResponseBody() {
        return (String) get("responseBody");
    }

    /**
     * sets the InputStream of the response into the responseDataStream
     *
     * @param responseDataStream
     */
    public void setResponseDataStream(InputStream responseDataStream) {
        set("responseDataStream", responseDataStream);
    }

    /**
     * sets the flag responseGZipped if the response is gzipped
     *
     * @param gzipped
     */
    public void setResponseGZipped(boolean gzipped) {
        put("responseGZipped", gzipped);
    }

    /**
     * @return true if responseGZipped is true (the response is gzipped)
     */
    public boolean getResponseGZipped() {
        return getBoolean("responseGZipped", true);
    }

    /**
     * @return the InputStream Response
     */
    public InputStream getResponseDataStream() {
        return (InputStream) get("responseDataStream");
    }

    /**
     * If this value if true then the response should be sent to the client.
     *
     * @return
     */
    public boolean sendZuulResponse() {
        return getBoolean("sendZuulResponse", true);
    }

    /**
     * sets the sendZuulResponse boolean
     *
     * @param bSend
     */
    public void setSendZuulResponse(boolean bSend) {
        set("sendZuulResponse", Boolean.valueOf(bSend));
    }

    /**
     * returns the response status code. Default is 200
     *
     * @return
     */
    public int getResponseStatusCode() {
        return get("responseStatusCode") != null ? (Integer) get("responseStatusCode") : 200;
    }


    /**
     * Use this instead of response.setStatusCode()
     *
     * @param nStatusCode
     */
    public void setResponseStatusCode(int nStatusCode) {
        getResponse().setStatus(HttpResponseStatus.valueOf(nStatusCode));
        set("responseStatusCode", nStatusCode);
    }

    /**
     * add a header to be sent to the origin
     *
     * @param name
     * @param value
     */
    public void addZuulRequestHeader(String name, String value) {
        getZuulRequestHeaders().put(name.toLowerCase(), value);
    }

    /**
     * return the list of requestHeaders to be sent to the origin
     *
     * @return the list of requestHeaders to be sent to the origin
     */
    public Map<String, String> getZuulRequestHeaders() {
        if (get("zuulRequestHeaders") == null) {
            HashMap<String, String> zuulRequestHeaders = new HashMap<String, String>();
            putIfAbsent("zuulRequestHeaders", zuulRequestHeaders);
        }
        return (Map<String, String>) get("zuulRequestHeaders");
    }

    /**
     * add a header to be sent to the response
     *
     * @param name
     * @param value
     */
    public void addZuulResponseHeader(String name, String value) {
        getZuulResponseHeaders().add(new Pair<String, String>(name, value));
    }

    /**
     * returns the current response header list
     *
     * @return a List<Pair<String, String>>  of response headers
     */
    public List<Pair<String, String>> getZuulResponseHeaders() {
        if (get("zuulResponseHeaders") == null) {
            List<Pair<String, String>> zuulRequestHeaders = new ArrayList<Pair<String, String>>();
            putIfAbsent("zuulResponseHeaders", zuulRequestHeaders);
        }
        return (List<Pair<String, String>>) get("zuulResponseHeaders");
    }

    /**
     * the Origin response headers
     *
     * @return the List<Pair<String, String>> of headers sent back from the origin
     */
    public List<Pair<String, String>> getOriginResponseHeaders() {
        if (get("originResponseHeaders") == null) {
            List<Pair<String, String>> originResponseHeaders = new ArrayList<Pair<String, String>>();
            putIfAbsent("originResponseHeaders", originResponseHeaders);
        }
        return (List<Pair<String, String>>) get("originResponseHeaders");
    }

    /**
     * adds a header to the origin response headers
     *
     * @param name
     * @param value
     */
    public void addOriginResponseHeader(String name, String value) {
        getOriginResponseHeaders().add(new Pair<String, String>(name, value));
    }

    /**
     * returns the content-length of the origin response
     *
     * @return the content-length of the origin response
     */
    public Integer getOriginContentLength() {
        return (Integer) get("originContentLength");
    }

    /**
     * sets the content-length from the origin response
     *
     * @param v
     */
    public void setOriginContentLength(Integer v) {
        set("originContentLength", v);
    }

    /**
     * sets the content-length from the origin response
     *
     * @param v parses the string into an int
     */
    public void setOriginContentLength(String v) {
        try {
            final Integer i = Integer.valueOf(v);
            set("originContentLength", i);
        } catch (NumberFormatException e) {
            LOG.warn("error parsing origin content length", e);
        }
    }

    /**
     * @return true if the request body is chunked
     */
    public boolean isChunkedRequestBody() {
        final Object v = get("chunkedRequestBody");
        return (v != null) ? (Boolean) v : false;
    }

    /**
     * sets chunkedRequestBody to true
     */
    public void setChunkedRequestBody() {
        this.set("chunkedRequestBody", Boolean.TRUE);
    }

    /**
     * @return true is the client request can accept gzip encoding. Checks the "accept-encoding" header
     */
    public boolean isGzipRequested() {
        final String requestEncoding = this.getRequest().headers().get(ZuulHeaders.ACCEPT_ENCODING);
        return requestEncoding != null && requestEncoding.toLowerCase().contains("gzip");
    }


    public String getQueryString() {
        String uri = request.getUri();
        if (uri.indexOf('?') > 0) {
            return uri.substring(uri.indexOf('?'));
        }
        return null;
    }

    /**
     * @return Map<String, List<String>>  of the request Query Parameters
     */
    public Map<String, List<String>> getRequestQueryParams() {
        return (Map<String, List<String>>) get("requestQueryParams");
    }

    public String getRequestParameter(String name) {
        List<String> values = getRequestQueryParams().get(name);
        return values != null && values.size() > 0 ? values.get(0) : null;
    }

    /**
     * sets the request query params list
     *
     * @param qp Map<String, List<String>> qp
     */
    public void setRequestQueryParams(Map<String, List<String>> qp) {
        put("requestQueryParams", qp);
    }


    public void writeResponse() throws IOException {
        String content = CharStreams.toString(new InputStreamReader(getResponseDataStream(), Charsets.UTF_8));
        System.out.println(content);
        channelHandlerContext.writeAndFlush(response);
    }

}