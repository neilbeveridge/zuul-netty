package com.netflix.zuul.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.netty.filter.FiltersListener;
import com.netflix.zuul.netty.filter.ZuulFilter;
import com.netflix.zuul.netty.filter.ZuulPostFilter;
import com.netflix.zuul.netty.filter.ZuulPreFilter;
import com.netflix.zuul.proxy.handler.FrontEndServerHandler;
import com.netflix.zuul.proxy.handler.HttpRequestFrameworkHandler;
import com.netflix.zuul.proxy.handler.HttpResponseFrameworkHandler;

public class FrontendServerInitializer extends ChannelInitializer<SocketChannel> implements FiltersListener {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendServerInitializer.class);

    private final ConcurrentMap<ZuulPreFilter, Path> preFilters = new ConcurrentSkipListMap<>();
    private final ConcurrentMap<ZuulPostFilter, Path> postFilters = new ConcurrentSkipListMap<>();

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("codec", new HttpServerCodec());

        // TODO : determine what a sensible max size is for the HTTP message, here we've defined it as 4kb (which may be too small ... ?)
        // TODO : determine whether HttpObjectAggregator keeps chunks "on-heap".
        pipeline.addLast("aggregator", new HttpObjectAggregator(4 * 1024));

        addZuulPostFilters(pipeline, postFilters);
        addZuulPreFilters(pipeline, preFilters);

        pipeline.addLast("frontendServer", new FrontEndServerHandler());

        LOG.debug("Added handlers to channel pipeline : {}", pipeline.names());
    }

    private void addZuulPostFilters(ChannelPipeline pipeline, ConcurrentMap<ZuulPostFilter, Path> filters) {
        for (Map.Entry<ZuulPostFilter, Path> entry : filters.entrySet()) {
            String name = entry.getValue().toString();
            pipeline.addLast(name, new HttpResponseFrameworkHandler(name, entry.getKey()));
        }
    }

    private void addZuulPreFilters(ChannelPipeline pipeline, ConcurrentMap<ZuulPreFilter, Path> filters) {
        for (Map.Entry<ZuulPreFilter, Path> entry : filters.entrySet()) {
            String name = entry.getValue().toString();
            LOG.debug("PreFilter ChannelHandler added to pipeline {}", name);
            pipeline.addLast(name, new HttpRequestFrameworkHandler(name, entry.getKey()));
        }
    }

    @Override
    public void filterAdded(Path filterPath, ZuulFilter filter) {
        add(filterPath, filter);
    }

    @Override
    public void filterRemoved(Path filterPath, ZuulFilter filter) {
        remove(filter);
    }

    private void add(Path filterPath, ZuulFilter filter) {
        if (filter instanceof ZuulPreFilter) {
            preFilters.put((ZuulPreFilter) filter, filterPath);

        } else if (filter instanceof ZuulPostFilter) {
            postFilters.put((ZuulPostFilter) filter, filterPath);

        } else {
            throw new IllegalArgumentException("illegal filter type");
        }
    }

    private void remove(ZuulFilter filter) {
        if (filter instanceof ZuulPreFilter) {
            preFilters.remove(filter);

        } else if (filter instanceof ZuulPostFilter) {
            postFilters.remove(filter);

        } else {
            throw new IllegalArgumentException("illegal filter type");
        }
    }

}
