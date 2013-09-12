package com.netflix.zuul.netty.filter;

import com.netflix.zuul.netty.HttpHandler;

import java.nio.file.Path;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author HWEB
 */
public class ZuulRunner implements HttpHandler, FiltersListener {

    private final ConcurrentHashMap<Path, ZuulFilter> filtersByPath = new ConcurrentHashMap<>();
    private final SortedSet<ZuulFilter> preFilters = new ConcurrentSkipListSet<>();
    private final SortedSet<ZuulFilter> routeFilters = new ConcurrentSkipListSet<>();
    private final SortedSet<ZuulFilter> postFilters = new ConcurrentSkipListSet<>();

    @Override
    public void handle(RequestContext requestContext) {

        try {
            preRouting(requestContext);
        } catch (Exception e) {
            error(e);
            postRouting(requestContext);
            return;
        }
        try {
            routing(requestContext);
        } catch (Exception e) {
            error(e);
            postRouting(requestContext);
            return;
        }
        try {
            postRouting(requestContext);
        } catch (Exception e) {
            error(e);
            return;
        }

    }

    private void routing(RequestContext requestContext) {
        runFilters(requestContext, routeFilters);
    }

    private void postRouting(RequestContext requestContext) {
        runFilters(requestContext, postFilters);
    }

    private void preRouting(RequestContext requestContext) {
        runFilters(requestContext, preFilters);
    }

    private void runFilters(RequestContext requestContext, SortedSet<ZuulFilter> filters) {
        for (ZuulFilter f : filters) {
            f.execute(requestContext);
        }
    }

    private void error(Exception e) {
        e.printStackTrace();
    }


    @Override
    public void filterAdded(Path filterPath, ZuulFilter filter) {
        switch (filter.type()) {
            case "pre":
                preFilters.add(filter);
                break;
            case "route":
                routeFilters.add(filter);
                break;
            case "post":
                postFilters.add(filter);
                break;

        }
        filtersByPath.put(filterPath, filter);
    }

    @Override
    public void filterRemoved(Path filterPath, ZuulFilter filter) {
        preFilters.remove(filter);
        filtersByPath.remove(filterPath);
    }
}
