package com.netflix.zuul.proxy.handler;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.BackendClientInitializer;
import com.netflix.zuul.proxy.IllegalRouteException;
import com.netflix.zuul.proxy.core.Connection;
import com.netflix.zuul.proxy.core.ConnectionPool;

/**
 * @author HWEB
 */
public class FrontEndServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(FrontEndServerHandler.class);

	private static final String BACK_END_CLIENT_INITIALIZER = "backEndClientInitializer";

    private final String remoteHost;
    private final int remotePort;

    // outboundChannel is volatile since it is shared between threads (this handler & BackendClientChannelHandler).
    private volatile Channel outboundChannel;
    
    private volatile Connection outboundConnection;
    
	private ConnectionPool connectionPool;

    public FrontEndServerHandler(ConnectionPool connectionPool) {
    	LOG.debug("connectionPool : {}", connectionPool);
        this.remoteHost = "localhost";
        this.remotePort = 8081;
        this.connectionPool = connectionPool;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	
    	LOG.debug("channel became active for channel : {}", ctx.channel());

        // get a reference to the inbound channel, which allows us to read data from external clients.
        final Channel inboundChannel = ctx.channel();
        
        disposeConnection();

        // connect to the back-end server
        Connection connection = obtainConnection(inboundChannel);
        
        ChannelFuture future = connection.getChannelFuture();

        // get a reference to the outbound channel, which allows us to write data to the back-end server.
        outboundChannel = future.channel();

        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    LOG.debug("successfully connected to remote server {} on port {}", remoteHost, remotePort);
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    LOG.debug("Unable to connect to remote server {} on port {}", remoteHost, remotePort);
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }

        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {

        // wait for the outboundChannel to be active, i.e. this will only happen once the channelActive() method has completed
        if (outboundChannel.isActive()) {
        	
        	LOG.debug("outboundChannel : {} isActive for ctx : {}", outboundChannel, ctx);

            // Only forward HttpRequest messages to the back-end server.
            if (msg instanceof FullHttpRequest) {

                final Channel inboundChannel = ctx.channel();
                URI route = inboundChannel.attr(Routing.ROUTE_KEY).get();
                LOG.debug("route = {} in channel={}", route, inboundChannel.hashCode());

                if (routeIsCorrect(route)) {
                    routeToBackend(ctx, msg);
                } else {
                    send404NotFoundResponse(ctx);
                }
            }
        }
    }
    
    private Connection obtainConnection(final Channel inboundChannel) throws IllegalRouteException, URISyntaxException {

    	URI hostRoute = new URI("http://" + this.remoteHost + ":" + this.remotePort);
    	
    	LOG.debug("trying to borrow a connection from the pool for hostRoute : {}", hostRoute);
    	
    	outboundConnection = connectionPool.borrow(hostRoute);

        //always associate the outbound connection with this inbound connection
        ChannelPipeline pipeline = outboundConnection.getChannel().pipeline();
        
        pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(4 * 1024));
        pipeline.addLast("loggingHandler", new LoggingHandler(LogLevel.DEBUG));
		pipeline.addLast("backEndClientChannelHandler", new BackendClientChannelHandler(inboundChannel));
               
		LOG.debug("For channel {}, Added handlers to channel pipeline : {}", inboundChannel, pipeline.names());
		
        return outboundConnection;
    }

    private void send404NotFoundResponse(ChannelHandlerContext ctx) {
        outboundChannel.close();

        Channel inboundChannel = ctx.channel();

        inboundChannel.writeAndFlush(create404Response());

        HandlerUtil.closeOnFlush(inboundChannel);
    }

    private FullHttpResponse create404Response() {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
        response.headers().set(CONTENT_LENGTH, 0);
        return response;
    }

    private void routeToBackend(final ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest completeMsg = (FullHttpRequest) msg;
        
        ChannelPipeline pipeline = outboundChannel.pipeline();
        
        LOG.debug("For outboundChannel : {}, handlers in pipeline : {}",outboundChannel, pipeline.names());

        ChannelFuture future = outboundChannel.writeAndFlush(completeMsg);

        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {

                if (future.isSuccess()) {
                    LOG.debug("successfully wrote to outbound channel");
                    ctx.channel().read();
                } else {
                    LOG.debug("Unable to write to outbound channel due to : ", future.cause());
                    future.channel().close();
                }
            }
        });
    }

    private boolean routeIsCorrect(URI route) {
        return remoteHost.equals(route.getHost()) && remotePort == route.getPort();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        LOG.debug("channelReadComplete triggered, so writing data");

        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        if (outboundChannel != null) {
            HandlerUtil.closeOnFlush(outboundChannel);
        }
    }
    
    private void disposeConnection() {
        final Connection connection = outboundConnection;
        if (connection != null) {
            LOG.debug("disposing connection {}", connection.getId());
            this.outboundConnection = null;
            //removeHandler(connection);
            returnConnectionToPool();
        }
    }
    
    private void returnConnectionToPool() {
		
    	LOG.debug("releasing outboundConnection : {} back to the connectionPool : {}", outboundConnection.getId(), connectionPool);
		connectionPool.release(outboundConnection);
	}

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        LOG.debug("caught exception : {}", cause);

        HandlerUtil.closeOnFlush(ctx.channel());
    }

}
