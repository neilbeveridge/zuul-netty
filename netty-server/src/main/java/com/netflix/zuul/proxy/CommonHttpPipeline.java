package com.netflix.zuul.proxy;

import com.netflix.zuul.netty.filter.FiltersListener;
import com.netflix.zuul.netty.filter.ZuulFilter;
import com.netflix.zuul.netty.filter.ZuulPostFilter;
import com.netflix.zuul.netty.filter.ZuulPreFilter;
import com.netflix.zuul.proxy.core.CommonsConnectionPool;
import com.netflix.zuul.proxy.core.ConnectionPool;
import com.netflix.zuul.proxy.framework.plugins.LoggingResponseHandler;
import com.netflix.zuul.proxy.handler.*;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CommonHttpPipeline implements ChannelPipelineFactory, FiltersListener {
    private static final Logger LOG = LoggerFactory.getLogger(CommonHttpPipeline.class);

    private final ConcurrentMap<ZuulPreFilter, Path> preFilters = new ConcurrentSkipListMap<>();
    private final ConcurrentMap<ZuulPostFilter, Path> postFilters = new ConcurrentSkipListMap<>();

    private static final String PROPERTY_STAGE_WORKERS = "com.netflix.zuul.workers.stage";
    private static final String PROPERTY_OUTBOUND_WORKERS = "com.netflix.zuul.workers.outbound";

    //seconds until the TCP connection will close
    private static final int IDLE_TIMEOUT_READER = 0;
    private static final int IDLE_TIMEOUT_WRITER = 0;
    private static final int IDLE_TIMEOUT_BOTH = 10;

    private static final boolean IS_KEEP_ALIVE_SUPPORTED = true;
    private static final boolean IS_REQUEST_CHUNKED_ENABLED = false;

    private static final ChannelHandler HTTP_APP_RESOLVER = new HttpAppResolvingHandler();
    private static final ChannelHandler HTTP_RESPONSE_LOGGER = new HttpResponseFrameworkHandler("http-response-logger",
            LoggingResponseHandler.FACTORY.getInstance("http-response-logger"));
    private static final ChannelHandler APP_EXECUTION_HANDLER;
    private static final ChannelFactory OUTBOUND_CHANNEL_FACTORY;
    private static final ChannelHandler IDLE_CHANNEL_WATCHDOG_HANDLER = new IdleChannelWatchdog("inbound");

    private final ConnectionPool outboundConnectionPool;
    private final ChannelHandler idleStateHandler;
    private final ChannelHandler KEEP_ALIVE_HANDLER = new HttpKeepAliveHandler(IS_KEEP_ALIVE_SUPPORTED);

    private static final String START_OF_POST_FILTERS = "idle-watchdog";
    private static final String START_OF_PRE_FILTERS = "app-http-response-logger";

    static {
        int stageWorkers = System.getProperty(PROPERTY_STAGE_WORKERS)!=null?Integer.parseInt(System.getProperty(PROPERTY_STAGE_WORKERS)):Runtime.getRuntime().availableProcessors();
        APP_EXECUTION_HANDLER = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(stageWorkers, 5*1024*1024, 250*1024*1024, 100, TimeUnit.MILLISECONDS));
        LOG.info("stage worker threads max set to {}", stageWorkers);

        if (System.getProperty(PROPERTY_OUTBOUND_WORKERS) != null) {
            int outboundWorkers = Integer.parseInt(System.getProperty(PROPERTY_OUTBOUND_WORKERS));
            OUTBOUND_CHANNEL_FACTORY = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool(), outboundWorkers);
            LOG.info("outbound worker threads max set to {}", outboundWorkers);
        } else {
            OUTBOUND_CHANNEL_FACTORY = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        }

    }


    public CommonHttpPipeline(Timer timer) {
        this.idleStateHandler = new IdleStateHandler(timer, IDLE_TIMEOUT_READER, IDLE_TIMEOUT_WRITER, IDLE_TIMEOUT_BOTH);
        this.outboundConnectionPool = new CommonsConnectionPool(timer, OUTBOUND_CHANNEL_FACTORY);
    }


    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        //offload from worker threads
        pipeline.addLast("app-execution-handler", APP_EXECUTION_HANDLER);

        //httpfu io
        pipeline.addLast("socket-suspension", new SocketSuspensionHandler());
        pipeline.addLast("idle-detection", idleStateHandler);
        pipeline.addLast("http-decoder", new HttpRequestDecoder());
        pipeline.addLast("http-encoder", new HttpResponseEncoder());

        //httpfu
        pipeline.addLast("http-deflater", new HttpContentCompressor());
        pipeline.addLast("edge-timer", new ServerTimingHandler("inbound"));
        pipeline.addLast("http-keep-alive", KEEP_ALIVE_HANDLER);
        pipeline.addLast("idle-watchdog", IDLE_CHANNEL_WATCHDOG_HANDLER);

        //response handlers
        addZuulPostFilters(pipeline, postFilters);

        pipeline.addLast("app-http-response-logger", HTTP_RESPONSE_LOGGER);

        //request handlers
        addZuulPreFilters(pipeline, preFilters);

        //proxy
        pipeline.addLast("proxy", new HttpProxyHandler(outboundConnectionPool, IS_REQUEST_CHUNKED_ENABLED));

        return pipeline;
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
            pipeline.addLast(name, new HttpRequestFrameworkHandler(name, entry.getKey()));
        }
    }

    @Override
    public void filterAdded(Path filterPath, ZuulFilter filter) {
        add(filterPath, filter);
    }

    @Override
    public void filterRemoved(Path filterPath, ZuulFilter filter) {
        remove(filterPath, filter);
    }

    private void add(Path filterPath, ZuulFilter filter) {
        if (filter instanceof ZuulPreFilter) {
            ZuulPreFilter typedFilter = (ZuulPreFilter) filter;
            preFilters.put(typedFilter, filterPath);

        } else if (filter instanceof ZuulPostFilter) {
            ZuulPostFilter typedFilter = (ZuulPostFilter) filter;
            postFilters.put(typedFilter, filterPath);
        } else {
            throw new IllegalArgumentException("illegal filter type");
        }
    }

    private void remove(Path filterPath, ZuulFilter filter) {
        if (filter instanceof ZuulPreFilter) {
            preFilters.remove(filter);
        } else if (filter instanceof ZuulPostFilter) {
            postFilters.remove(filter);
        } else {
            throw new IllegalArgumentException("illegal filter type");
        }
    }

}
