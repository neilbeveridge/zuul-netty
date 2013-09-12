package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionSurfacerHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionSurfacerHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.debug("channel failed {}", Integer.toHexString(e.getChannel().getId()), e.getCause());
    }

    @Override
    public void closeRequested(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOG.debug("channel close requested {}", Integer.toHexString(e.getChannel().getId()));
        super.closeRequested(ctx, e);
    }

}
