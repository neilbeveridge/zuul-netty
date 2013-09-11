package com.netflix.zuul.netty.debug;


import com.netflix.zuul.netty.filter.RequestContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HWEB
 */
public class Debug {

    public static void setDebugRequest(RequestContext currentContext, boolean bDebug) {
        currentContext.setDebugRequest(bDebug);
    }

    public static void setDebugRequestHeadersOnly(RequestContext currentContext, boolean bHeadersOnly) {
        currentContext.setDebugRequestHeadersOnly(bHeadersOnly);
    }

    public static boolean debugRequestHeadersOnly(RequestContext currentContext) {
        return currentContext.debugRequestHeadersOnly();
    }


    public static void setDebugRouting(RequestContext currentContext, boolean bDebug) {
        currentContext.setDebugRouting(bDebug);
    }


    public static boolean debugRequest(RequestContext currentContext) {
        return currentContext.debugRequest();
    }

    public static boolean debugRouting(RequestContext currentContext) {
        return currentContext.debugRouting();
    }

    public static void addRoutingDebug(RequestContext currentContext, String line) {
        List<String> rd = getRoutingDebug(currentContext);
        rd.add(line);
    }

    /**
     * @return Returns the list of routiong debug messages
     */
    public static List<String> getRoutingDebug(RequestContext currentContext) {
        List<String> rd = (List<String>) currentContext.get("routingDebug");
        if (rd == null) {
            rd = new ArrayList<>();
            currentContext.set("routingDebug", rd);
        }
        return rd;
    }

    /**
     * Adds a line to the  Request debug messages
     *
     * @param line
     */
    public static void addRequestDebug(RequestContext currentContext, String line) {
        List<String> rd = getRequestDebug(currentContext);
        rd.add(line);
    }

    /**
     * @return returns the list of request debug messages
     */
    public static List<String> getRequestDebug(RequestContext currentContext) {
        List<String> rd = (List<String>) currentContext.get("requestDebug");
        if (rd == null) {
            rd = new ArrayList<>();
            currentContext.set("requestDebug", rd);
        }
        return rd;
    }

}
