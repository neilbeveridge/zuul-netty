package com.netflix.zuul.proxy.handler;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class HttpKeepAliveHandler extends IdleStateAwareChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpKeepAliveHandler.class);

    private final boolean isKeepAliveSupported;

    public HttpKeepAliveHandler(boolean isKeepAliveSupported) {
        this.isKeepAliveSupported = isKeepAliveSupported;
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) ctx.getAttachment();

        if (e.getMessage() instanceof HttpResponse) {

            HttpResponse response = (HttpResponse) e.getMessage();

            if (!response.isChunked()) {

                if (isKeepAliveSupported && isKeepAlive(request)) {

                    LOG.debug("keep-alive IS implemented for this connection");

                    // Add 'Content-Length' header only for a keep-alive connection.
                    response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
                    // Add keep alive header as per:
                    // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                    response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

                } else {

                    //not chunked and keep alive not supported, closing connection
                    LOG.debug("keep-alive NOT implemented for this connection");
                    e.getFuture().addListener(ChannelFutureListener.CLOSE);

                }

            } else {
                //response is chunked

                //don't send a content-length for chunked transfer-encoding
                response.removeHeader(CONTENT_LENGTH);

                //keep-alive explicitly off
                if (!isKeepAliveSupported || !isKeepAlive(request)) {
                    response.removeHeader(CONNECTION);
                    LOG.debug("keep-alive NOT implemented for this connection as it is explicitly turned off");
                } else {
                    LOG.debug("keep-alive IMPLIED for this connection as it is chunked");
                }
            }
        } else if (e.getMessage() instanceof HttpChunk) {
            LOG.debug("found chunk");
            if (((HttpChunk) e.getMessage()).isLast()) {

                if (!isKeepAliveSupported || !isKeepAlive(request)) {
                    //keep alive explicitly off
                    LOG.debug("reached the last chunk and keep alive is turned off so closing the connection");
                    e.getFuture().addListener(ChannelFutureListener.CLOSE);
                }

            }
        } else {
            LOG.debug("found something else");
        }

        super.writeRequested(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpRequest && isKeepAliveSupported) {
            ctx.setAttachment(e.getMessage());
        }
        super.messageReceived(ctx, e);
    }

}
