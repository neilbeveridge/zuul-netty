package com.netflix.zuul.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An HTTP server that sends random HTTP number.
 */
public class MockEndpoint {
	
	private static final Logger LOG = LoggerFactory.getLogger(MockEndpoint.class);

    private final int port;

	private String responseToGive;

    public MockEndpoint(int port) {
        this.port = port;
    }

	public MockEndpoint(int port, String responseToGive) {
		this.port = port;
		this.responseToGive = responseToGive;
	}

	public void run() throws Exception {
		
		// Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new HttpMockEndPointServerInitializer(responseToGive));

            Channel ch = b.bind(port).sync().channel();
            
            LOG.info("bound to port {}", port);
            
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }

		String responseToGive = null;
		if (args.length > 1)
			responseToGive = args[1];

		if (responseToGive == null) {
			new MockEndpoint(port).run();
		} else {
			new MockEndpoint(port, responseToGive).run();
		}
    }
}
