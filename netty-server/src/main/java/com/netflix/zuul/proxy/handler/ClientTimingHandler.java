package com.netflix.zuul.proxy.handler;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ClientTimingHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClientTimingHandler.class);
    private static final double NANO_TO_MS = 1000000d;
    private final String tag;
    private final Timer connectTimer = Metrics.newTimer(com.netflix.zuul.proxy.handler.ServerTimingHandler.class, "outbound-connect", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private final Timer exchangeTimer = Metrics.newTimer(com.netflix.zuul.proxy.handler.ServerTimingHandler.class, "outbound-exchange", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private TimerContext connectContext;
    private TimerContext exchangeContext;

    private long connectStart;
    private long writeStart;

    public ClientTimingHandler(String tag) {
        this.tag = tag;
    }

    @Override
    public void connectRequested(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        this.connectStart = System.nanoTime();
        this.connectContext = connectTimer.time();
        super.connectRequested(ctx, e);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOG.info("timer {} - connect took: {}ms", tag, String.format("%.2f", (System.nanoTime() - connectStart) / NANO_TO_MS));
        connectContext.stop();
        super.channelConnected(ctx, e);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        this.writeStart = System.nanoTime();
        this.exchangeContext = exchangeTimer.time();
        super.writeRequested(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpChunk) {
            if (((HttpChunk) e.getMessage()).isLast()) {
                logDifference(System.nanoTime() - writeStart);
                exchangeContext.stop();
            }
        } else if (e.getMessage() instanceof HttpMessage) {
            if (!((HttpMessage) e.getMessage()).isChunked()) {
                logDifference(System.nanoTime() - writeStart);
                exchangeContext.stop();
            }
        } else {
            logDifference(System.nanoTime() - writeStart);
            exchangeContext.stop();
        }

        super.messageReceived(ctx, e);
    }

    private void logDifference(long nano) {
        LOG.info("timer {} - exchange took: {}ms", tag, String.format("%.2f", nano / NANO_TO_MS));
    }

}
