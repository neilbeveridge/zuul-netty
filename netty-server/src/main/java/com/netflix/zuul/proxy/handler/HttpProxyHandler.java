package com.netflix.zuul.proxy.handler;

import com.netflix.zuul.proxy.IllegalRouteException;
import com.netflix.zuul.proxy.core.Connection;
import com.netflix.zuul.proxy.core.ConnectionPool;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.netflix.zuul.proxy.core.Route.ROUTE_HEADER;

public class HttpProxyHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpProxyHandler.class);

    private static final String HANDLER_NAME = "http-pipe-back";
    final static String WENT_AWAY = "outbound-went-away";

    private final boolean isChunkedRequestsSupported;
    private final List<HttpChunk> queuedChunks = new ArrayList<>();
    private final ConnectionPool connectionPool;

    private volatile boolean doneSendingRequestAndBuffer = false;
    private volatile Connection outboundConnection;

    private HttpProxyHandler(ConnectionPool connectionPool, boolean isChunkedRequestsSupported) {
        this.isChunkedRequestsSupported = isChunkedRequestsSupported;
        this.connectionPool = connectionPool;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getChannel().close();
        LOG.info("inbound channel failed", e.getCause());
        destroyConnection();
    }

    private void disposeConnection() {
        final Connection connection = outboundConnection;
        if (connection != null) {
            LOG.debug("disposing connection {}", connection.getId());
            this.outboundConnection = null;
            removeHandler(connection);
            connectionPool.release(connection);
        }
    }

    private void destroyConnection() {
        final Connection connection = outboundConnection;
        if (connection != null) {
            LOG.debug("destroying connection {}", connection.getId());
            this.outboundConnection = null;
            removeHandler(connection);
            connectionPool.destroy(connection);
        }
    }

    private void removeHandler(Connection connection) {
        LOG.debug("trying to remove handler on connection {}", connection == null ? WENT_AWAY : connection.getId());
        if (connection.getChannel().getPipeline().get(HANDLER_NAME) != null) {
            connection.getChannel().getPipeline().remove(HANDLER_NAME);
        }
    }

    @Override
    public void setInterestOpsRequested(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel inboundChannel = e.getChannel();

        //support for throttling driven by socket saturation
        if (outboundConnection != null) {
            if (!inboundChannel.isWritable()) {
                outboundConnection.getChannel().setReadable(false);
            } else {
                outboundConnection.getChannel().setReadable(true);
            }
        }

        super.setInterestOpsRequested(ctx, e);
    }

    private boolean outboundNull() {
        return outboundConnection == null;
    }

    private class OutboundHandler extends SimpleChannelUpstreamHandler {
        private final Channel inboundChannel;

        public OutboundHandler(Channel inboundChannel) {
            this.inboundChannel = inboundChannel;
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            LOG.debug("outbound disconnected");
            super.channelDisconnected(ctx, e);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

            //it is possible that the client has gone away so check before writing to the channel
            if (inboundChannel.isConnected()) {

                LOG.debug("return write isConnected:{}, type={} - {}", outboundNull() ? WENT_AWAY : outboundConnection.getChannel().isConnected(), e
                        .getMessage().getClass().getSimpleName(), outboundNull() ? WENT_AWAY : outboundConnection.getId());

                if (e.getMessage() instanceof HttpResponse) {
                    final HttpResponse response = (HttpResponse) e.getMessage();
                    LOG.debug("response payload {} {}", outboundNull() ? WENT_AWAY : outboundConnection.getId(), dumpBuffer(response.getContent()));
                } else if (e.getMessage() instanceof HttpChunk) {
                    final HttpChunk chunk = (HttpChunk) e.getMessage();
                    LOG.debug("chunk payload {} {}", outboundNull() ? WENT_AWAY : outboundConnection.getId(), dumpBuffer(chunk.getContent()));
                }

                if (e.getMessage() instanceof HttpResponse) {
                    LOG.debug("response is chunked: {} {}", ((HttpResponse) e.getMessage()).isChunked(), outboundNull() ? WENT_AWAY
                            : outboundConnection.getId());
                }

                if (e.getMessage() instanceof HttpResponse && !((HttpResponse) e.getMessage()).isChunked()) {
                    disposeConnection(); //not chunked so we're done
                } else if (e.getMessage() instanceof HttpChunk && ((HttpChunk) e.getMessage()).isLast()) {
                    disposeConnection(); //chunked and is last chunk so we're done
                }

                //it is important to dispose of the outbound connection before writing here to stop the client sending the next request before it's finished
                inboundChannel.write(e.getMessage());

            } else {

                LOG.debug("client disconnected, assuming bad outbound state as well, closing connection {}", outboundNull() ? WENT_AWAY
                        : outboundConnection.getId());

                inboundChannel.close();
                destroyConnection();

            }

        }

        @Override
        public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            final Channel outboundChannel = e.getChannel();

            //support for throttling driven by socket saturation
            if (!outboundChannel.isWritable()) {
                inboundChannel.setReadable(false);
            } else {
                inboundChannel.setReadable(true);
            }

            super.channelInterestChanged(ctx, e);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            inboundChannel.close();
            destroyConnection();
        }

    }

    private Connection newConnection(URL hostRoute, final Channel inboundChannel) throws IllegalRouteException {

        //suspend inbound channel whilst we connect
        inboundChannel.setReadable(false);

        final Connection outboundConnection = connectionPool.borrow(hostRoute);

        //always associate the outbound connection with this inbound connection
        outboundConnection.getChannel().getPipeline().addLast(HANDLER_NAME, new OutboundHandler(inboundChannel));

        outboundConnection.getChannelFuture().addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                inboundChannel.setReadable(true);
            }

        });

        this.outboundConnection = outboundConnection;

        return outboundConnection;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent message) throws Exception {
        final Channel inboundChannel = message.getChannel();

        try {
            if (message.getMessage() instanceof HttpRequest) {

                final HttpRequest request = (HttpRequest) message.getMessage();
                handleRequest(request, inboundChannel);

            } else if (message.getMessage() instanceof HttpChunk) {

                if (!isChunkedRequestsSupported) {
                    LOG.warn("requests with chunked transfer encoding are not supported");
                    inboundChannel.close();
                    return;
                }

                final HttpChunk chunk = (HttpChunk) message.getMessage();
                handleChunk(chunk, inboundChannel);

            }
        } catch (IllegalRouteException e) {
            //TODO: do something better than dropping the connection when the route is bad
            inboundChannel.close();
            LOG.warn("dropped connection for bad route: {}", e.getRoute());
        }
    }

    private URL getRoute(HttpRequest request)
            throws IllegalRouteException {
        URL route;
        String sRoute = request.getHeader(ROUTE_HEADER);
        try {
            route = new URL(sRoute);
        } catch (MalformedURLException e) {
            throw new IllegalRouteException(sRoute);
        }

        LOG.debug("found route: {}", route);

        return route;
    }

    private void handleRequest(final HttpRequest request, final Channel inboundChannel)
            throws IllegalRouteException {
        this.doneSendingRequestAndBuffer = false;

        URL routeHost = getRoute(request);

        //establish connection if not already pending or if it has gone away
        if (!connected() || !routeHost.equals(outboundConnection.getRouteHost())) {
            disposeConnection();
            final Connection connection = newConnection(routeHost, inboundChannel);

            connection.getChannelFuture().addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    final Channel outboundChannel = future.getChannel();

                    LOG.debug("writing request to connection {}", connection.getId());

                    //write request onto established connection
                    performWrite(inboundChannel, outboundChannel, request);

                    //don't do lock acquisition unless we really must - this is the trunk road
                    if (request.isChunked()) {
                        synchronized (queuedChunks) {
                            performChunkWrite(inboundChannel, outboundChannel, queuedChunks);
                            HttpProxyHandler.this.doneSendingRequestAndBuffer = true;
                        }
                    }

                    HttpProxyHandler.this.doneSendingRequestAndBuffer = true;
                }

            });

        } else {
            //write onto established connection
            performWrite(inboundChannel, outboundConnection.getChannel(), request);

            this.doneSendingRequestAndBuffer = true;
        }
    }

    private void handleChunk(final HttpChunk chunk, final Channel inboundChannel) {

        //accept some lock acquisition here since request chunking is rare
        //need to lock before doneSendingRequestAndBuffer test to avoid race condition
        synchronized (queuedChunks) {

            if (!doneSendingRequestAndBuffer) {

                //buffer the chunks until the request has been sent
                queuedChunks.add(chunk);

                LOG.debug("buffering chunk");

            } else {

                //write onto established connection
                performWrite(inboundChannel, outboundConnection.getChannel(), chunk);

                LOG.debug("writing un-buffered chunk");

            }

        }
    }

    private void performChunkWrite(Channel inboundChannel, Channel outboundChannel, List<HttpChunk> buffer) {
        for (HttpChunk chunk : buffer) {
            LOG.debug("writing buffered chunk");
            performWrite(inboundChannel, outboundChannel, chunk);
        }
        buffer.clear();
    }

    private void performWrite(Channel inboundChannel, Channel outboundChannel, final Object request) {
        //perform the request write to the outbound channel
        if (outboundChannel.isConnected()) {
            outboundChannel.write(request);
        } else {
            LOG.warn("write failed: not connected");
            inboundChannel.close();
        }

    }

    private static String dumpBuffer(ChannelBuffer buffer) {
        StringBuffer output = new StringBuffer();
        while (buffer.readable()) {
            try {
                output.append(new String(new byte[]{buffer.readByte()}, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return output.toString();
    }

    private boolean connected() {
        return outboundConnection != null && outboundConnection.getChannel().isConnected();
    }

    public static final class Factory implements com.netflix.zuul.proxy.handler.HandlerFactory {

        private final ConnectionPool connectionPool;
        private final boolean isChunkedRequestsSupported;

        public Factory(ConnectionPool connectionPool, boolean isChunkedRequestsSupported) {
            this.connectionPool = connectionPool;
            this.isChunkedRequestsSupported = isChunkedRequestsSupported;
        }

        @Override
        public ChannelHandler getInstance() {
            return new HttpProxyHandler(connectionPool, isChunkedRequestsSupported);
        }

    }

}
