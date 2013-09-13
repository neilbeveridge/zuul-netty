package com.netflix.zuul.proxy.framework.api;

/**
 * @author HWEB
 */
public abstract class AbstractHttpRequestHandler implements HttpRequestHandler {

    public static final String TYPE = "pre";

    private final int order;

    protected AbstractHttpRequestHandler(int order) {
        this.order = order;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void requestReceived(FrameworkHttpRequest request) {
        if (shouldHandle(request)) {
            doHandle(request);
        }
    }

    protected abstract void doHandle(FrameworkHttpRequest request);

    private boolean shouldHandle(FrameworkHttpRequest request) {
        return true;
    }
}
