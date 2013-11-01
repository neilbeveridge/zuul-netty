package com.netflix.zuul.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.JmxReporter;

public class HttpServer {

    private static final Timer TIMER = new HashedWheelTimer();
    private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

    private final int port;
    
    public HttpServer(int port) {
        this.port = port;
    }

    public synchronized void run() throws InterruptedException {
    	
        // Configure the server.
    	EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			
			bootstrap.group(bossGroup, workerGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			
			//b.setPipelineFactory(new CommonHttpPipeline(TIMER)); TODO : determine if CommonHttpPipeline needs to be kept.
			bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
			bootstrap.localAddress(port);
			
			bootstrap.childHandler(new CommonHttpPipeline(TIMER));
			
			// Start the server.
			ChannelFuture f = bootstrap.bind().sync();
			
			LOG.info("server bound to port {}", port);
 
			// Wait until the server socket is closed.
			f.channel().closeFuture().sync();
			
		} finally {
			
	        // Shut down all event loops to terminate all threads.
	        bossGroup.shutdownGracefully();
	        workerGroup.shutdownGracefully();
	        
	        // Wait until all threads are terminated.
	        bossGroup.terminationFuture().sync();
	        workerGroup.terminationFuture().sync();
	    }
    }

    public static void main(String[] args) throws InterruptedException {
        
    	// Log all channel events at DEBUG log level.
    	// TODO : determine if this instance will magically log, or we have to do some more coding ...
    	LoggingHandler loggingHandler = new LoggingHandler(HttpServer.class);
    	
        LOG.info("Starting server...");
        
        new HttpServer(80).run();
		
        JmxReporter.startDefault(Metrics.defaultRegistry());

        //ConsoleReporter.enable(1, TimeUnit.SECONDS);
    }
}
