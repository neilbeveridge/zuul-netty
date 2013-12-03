package com.netflix.zuul.proxy.handler;

import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * A dummy NO-OP handler that is required to be defined as a Handler for a Bootstrap in
 * com.netflix.zuul.proxy.core.CommonsConnectionPool.createApplicationPool(...).new PoolableObjectFactory() {...}.makeObject()
 * to prevent a java.lang.IllegalStateException: "handler not set" being thrown during bootstrap.connect().
 * 
 * @author deepakgupta
 *
 */
public class PlaceholderHandler extends ChannelInboundHandlerAdapter {

}
