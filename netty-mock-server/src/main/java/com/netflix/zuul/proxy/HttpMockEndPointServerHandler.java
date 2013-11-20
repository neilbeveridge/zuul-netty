package com.netflix.zuul.proxy;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMockEndPointServerHandler extends ChannelInboundHandlerAdapter {

    private static final String BOOKING_PATH = "/ba";

    private static final Logger LOG = LoggerFactory.getLogger(HttpMockEndPointServerHandler.class);

    private static final Random RANDOM_GENERATOR = new Random();

    private final String responseToGive;

    public HttpMockEndPointServerHandler(String responseToGive) {
        this.responseToGive = responseToGive;

        if (responseToGive == null) {
            LOG.debug("Response to give: random number");
        } else {
            LOG.debug("Response to give: string '{}'", responseToGive);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            LOG.info("got request : {}", req);
            String uri = req.getUri();
            LOG.debug("URI of request: {}", uri);

            if (is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }

            boolean keepAlive = isKeepAlive(req);
            FullHttpResponse response;

            if (uri.equals(BOOKING_PATH)) {
                byte[] content = getResponseContent();

                response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(content));
                response.headers().set(CONTENT_TYPE, "text/plain");
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            } else {
                response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
                response.headers().set(CONTENT_LENGTH, 0);
            }

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                ctx.write(response);
            }

        }
    }

    private byte[] getResponseContent() {
        if (responseToGive != null) {
            return responseToGive.getBytes();
        }

        return generateRandomContent();
    }

    private byte[] generateRandomContent() {
        int random = RANDOM_GENERATOR.nextInt();
        byte[] content = Integer.toString(random).getBytes();

        LOG.info("content int = {}, bytes = {}", random, content);
        return content;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
