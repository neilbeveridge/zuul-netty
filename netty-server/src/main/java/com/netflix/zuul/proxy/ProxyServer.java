package com.netflix.zuul.proxy;

import com.netflix.zuul.netty.filter.FiltersChangeNotifier;
import com.netflix.zuul.netty.filter.ZuulFiltersLoader;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.JmxReporter;

import io.netty.bootstrap.ServerBootstrap;

import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @author HWEB
 */
public class ProxyServer {
    private static final Timer TIMER = new HashedWheelTimer();
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServer.class);
	private static final String PROPERTY_WORKERS = "com.netflix.zuul.workers.inbound";

    private final int port;
    private Channel channel;
    private ServerBootstrap bootstrap;
    private FiltersChangeNotifier filtersChangeNotifier;

    public ProxyServer(int port) {
        this.port = port;
    }

    public ProxyServer setFiltersChangeNotifier(FiltersChangeNotifier filtersChangeNotifier) {
        this.filtersChangeNotifier = filtersChangeNotifier;
        return this;
    }


    public FutureTask<ProxyServer> run() {
    	
        FutureTask<ProxyServer> future = new FutureTask<>(new Callable<ProxyServer>() {

            @Override
            public ProxyServer call() throws Exception {
            	
            	// Configure the server.
            	EventLoopGroup bossGroup = new NioEventLoopGroup();
				
				EventLoopGroup workerGroup = null;
				if (System.getProperty(PROPERTY_WORKERS) != null) {
                    int inboundWorkers = Integer.parseInt(System.getProperty(PROPERTY_WORKERS));
					workerGroup = new NioEventLoopGroup(inboundWorkers);
				} else {
					workerGroup = new NioEventLoopGroup();
				}
                
                try {
        			ServerBootstrap bootstrap = new ServerBootstrap();
        			
        			bootstrap.group(bossGroup, workerGroup);
        			bootstrap.channel(NioServerSocketChannel.class);
        			
        			//TODO : determine if CommonHttpPipeline needs to be kept.
        			FiltersChangeNotifier changeNotifier = filtersChangeNotifier != null ? filtersChangeNotifier : FiltersChangeNotifier.IGNORE;
                    CommonHttpPipeline commonHttpPipeline = new CommonHttpPipeline(TIMER);
                    changeNotifier.addFiltersListener(commonHttpPipeline);
        			
        			
        			bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        			bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
        			bootstrap.localAddress(port);
        			
        			bootstrap.childHandler(commonHttpPipeline);
        			
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
                
                // TODO : determine why "this" needs to be returned.
                return ProxyServer.this;
            }
        });

        final Thread thread = new Thread(future, "Proxy Server");
        thread.start();
        return future;
    }

    public boolean isRunning() {
        return channel != null && channel.isActive();
    }


    
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String filtersPath = "/Users/nbeveridge/Development/git/zuul-netty/zuul-core/src/main/filters/pre";
        if (args.length >= 2) {
            port = Integer.parseInt(args[0]);
            filtersPath = args[1];
        }

        // Log all channel events at DEBUG log level.
    	// TODO : determine if this instance will magically log, or we have to do some more coding ...
    	LoggingHandler loggingHandler = new LoggingHandler(ProxyServer.class);
    	
        LOG.info("Starting server...");

        ZuulFiltersLoader changeNotifier = new ZuulFiltersLoader(Paths.get(filtersPath));
        ProxyServer proxyServer = new ProxyServer(port).setFiltersChangeNotifier(changeNotifier);

        proxyServer.run().get();
        changeNotifier.reload();

        JmxReporter.startDefault(Metrics.defaultRegistry());

        //ConsoleReporter.enable(1, TimeUnit.SECONDS);
    }


}
