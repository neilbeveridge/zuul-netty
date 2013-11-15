package com.netflix.zuul.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author HWEB
 */
public class ProxyServer {
    private static final Timer TIMER = new HashedWheelTimer();
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServer.class);
	private static final String PROPERTY_WORKERS = "com.netflix.zuul.workers.inbound";

	private final int localPort;
    private final String remoteHost;
    private final int remotePort;

    public ProxyServer(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public void bootstrap() throws Exception {
        LOG.info(
                "Proxying *:" + localPort + " to " +
                remoteHost + ':' + remotePort + " ...");

        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new FrontendServerInitializer(remoteHost, remotePort))
             .childOption(ChannelOption.AUTO_READ, false); // TODO : determine what this option does.
             
            ChannelFuture future = b.bind(localPort);
            
            // ChannelFutureListener added to confirm whether the bind was successful. 
            future.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					
					if (future.isSuccess()) {
						LOG.info("Successfully bound to port : {}", localPort);
					} else {
						LOG.info("Could not bind to port : {}", localPort);
					}
				}
			});
            
            // block until the future completes
            // TODO : determine whether it's better to replace the ChannelFutureListener with this blocking code.
            future.sync();
            
            // Wait until the server socket is closed.
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

 

    public static void main(String[] args) throws Exception {
    	
    	// Validate command line options.
        if (args.length != 3) {
            System.err.println(
                    "Usage: " + ProxyServer.class.getSimpleName() +
                    " <local port> <remote host> <remote port>");
            return;
        }

        // Parse command line options.
        int localPort = Integer.parseInt(args[0]);
        String remoteHost = args[1];
        int remotePort = Integer.parseInt(args[2]);

        new ProxyServer(localPort, remoteHost, remotePort).bootstrap();
    }


}
