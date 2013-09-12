package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: nbeveridge
 * Date: 12/09/2013
 * Time: 19:25
 * To change this template use File | Settings | File Templates.
 */
public class IdleChannelWatchdog extends IdleStateAwareChannelHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IdleChannelWatchdog.class);

    private final String channelName;

    public IdleChannelWatchdog (String channelName) {
        this.channelName = channelName;
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
        LOG.info("closing {} channel after {} event was intercepted", channelName, e.getState().toString());

        e.getChannel().close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    LOG.info("{} channel closed cleanly", channelName);
                } else {
                    LOG.info("{} channel failed to close cleanly", channelName);
                }
            }
        });
    }

}
