package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class SocketSuspensionHandler extends SimpleChannelHandler {

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        e.getChannel().setReadable(true);

        super.writeRequested(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        e.getChannel().setReadable(false);

        super.messageReceived(ctx, e);
    }

}
