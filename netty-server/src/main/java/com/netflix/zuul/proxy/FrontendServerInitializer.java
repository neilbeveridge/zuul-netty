package com.netflix.zuul.proxy;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.handler.FrontEndServerHandler;

public class FrontendServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger LOG = LoggerFactory.getLogger(FrontendServerInitializer.class);

    private final String remoteHost;
    private final int remotePort;

    public FrontendServerInitializer(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        ChannelPipeline p = ch.pipeline();

        p.addLast("codec", new HttpServerCodec());

        // TODO : determine what a sensible max size is for the HTTP message, here we've defined it as 4kb (which may be too small ... ?)
        // TODO : determine whether HttpObjectAggregator keeps chunks "on-heap".
        p.addLast("aggregator", new HttpObjectAggregator(4 * 1024));

        p.addLast("frontendServer", new FrontEndServerHandler(remoteHost, remotePort));

        LOG.info("Added handlers to channel pipeline : " + p.names());
    }

}
