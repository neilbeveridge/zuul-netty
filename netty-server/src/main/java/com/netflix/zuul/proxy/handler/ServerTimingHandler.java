package com.netflix.zuul.proxy.handler;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ServerTimingHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ServerTimingHandler.class);
    private static final double NANO_TO_MS = 1000000d;
    private final String tag;
    private final Timer timer = Metrics.newTimer(ServerTimingHandler.class, "inbound-requests", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private TimerContext context;
    private long start;

    public ServerTimingHandler(String tag) {
        this.tag = tag;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpRequest) {
            this.context = timer.time();
            this.start = System.nanoTime();
        }

        super.messageReceived(ctx, e);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpChunk) {
            if (((HttpChunk) e.getMessage()).isLast()) {
                context.stop();
                logDifference(System.nanoTime() - start);
                LOG.debug("saw last chunk");
            }
        } else if (e.getMessage() instanceof HttpMessage) {
            if (!((HttpMessage) e.getMessage()).isChunked()) {
                context.stop();
                logDifference(System.nanoTime() - start);
            }
            LOG.debug("headers: {}", ((HttpMessage) e.getMessage()).getHeaders());
        } else {
            context.stop();
            logDifference(System.nanoTime() - start);
        }

        LOG.debug("saw message {}", e.getMessage());

        super.writeRequested(ctx, e);
    }

    private void logDifference(long nano) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("timer {} - exchange took: {}ms", tag, String.format("%.2f", nano / NANO_TO_MS));
        }
    }

}
