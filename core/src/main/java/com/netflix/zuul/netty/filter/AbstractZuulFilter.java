package com.netflix.zuul.netty.filter;

/**
 * @author HWEB
 */
public abstract class AbstractZuulFilter implements ZuulFilter {

    private final String type;
    private final int order;

    public AbstractZuulFilter(String type, int order) {
        this.type = type;
        this.order = order;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public Void execute(RequestContext requestContext) {
        if (shouldFilter(requestContext)) {
            return doExecute(requestContext);
        }
        return null;
    }

    protected boolean shouldFilter(RequestContext requestContext) {
        return true;
    }

    @Override
    public int compareTo(Object o) {
        return order - ((ZuulFilter) o).order();
    }


    protected abstract Void doExecute(RequestContext requestContext);
}
