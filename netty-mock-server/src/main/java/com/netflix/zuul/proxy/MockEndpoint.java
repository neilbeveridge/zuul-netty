package com.netflix.zuul.proxy;

import com.netflix.zuul.proxy.handler.HttpKeepAliveHandler;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.jboss.netty.util.CharsetUtil;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.jboss.netty.channel.Channels.pipeline;

public class MockEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(MockEndpoint.class);

    private static final int IDLE_TIMEOUT_READER = 0;
    private static final int IDLE_TIMEOUT_WRITER = 0;
    private static final int IDLE_TIMEOUT_BOTH = 10;

    private static final int PORT = 8081;

    private static final boolean IS_KEEP_ALIVE_SUPPORTED = true;

    private final ChannelHandler IDLE_STATE_HANDLER;
    private final ChannelHandler KEEP_ALIVE_HANDLER = new HttpKeepAliveHandler(IS_KEEP_ALIVE_SUPPORTED);

    private static final Timer TIMER = new HashedWheelTimer();

    public MockEndpoint() {
        IDLE_STATE_HANDLER = new IdleStateHandler(TIMER, IDLE_TIMEOUT_READER, IDLE_TIMEOUT_WRITER, IDLE_TIMEOUT_BOTH);
    }

    public void run() {
        // Configure the server.
        ServerBootstrap b = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

        b.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline p = pipeline();

                p.addLast("idle-detection", IDLE_STATE_HANDLER);
                p.addLast("http-decoder", new HttpRequestDecoder());
                p.addLast("http-encoder", new HttpResponseEncoder());
                p.addLast("keep-alive", KEEP_ALIVE_HANDLER);
                p.addLast("responder", new ChannelUpstreamHandler() {

                    @Override
                    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
                        if (e instanceof MessageEvent) {
                            final MessageEvent me = (MessageEvent) e;
                            LOG.debug("received: {}", me.getMessage().getClass().getSimpleName());

                            Map<String, List<String>> params = new HashMap<>();

                            StringBuffer buf = new StringBuffer();
                            buf.setLength(0);
                            buf.append("HI");

                            if (me.getMessage() instanceof HttpRequest) {
                                final HttpRequest request = (HttpRequest) me.getMessage();

                                LOG.debug("request: {}", request);

                                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
                                params = queryStringDecoder.getParameters();

                                if (!request.isChunked()) {
                                    writeOutput(e.getChannel(), buf, params.containsKey("chunked"));
                                }
                            } else if (me.getMessage() instanceof HttpChunk) {
                                final HttpChunk chunk = (HttpChunk) me.getMessage();
                                if (chunk.isLast()) {
                                    writeOutput(e.getChannel(), buf, params.containsKey("chunked"));
                                }
                            }
                        }
                    }
                });

                return p;
            }

        });

        b.setOption("child.tcpNoDelay", true);
        //b.setOption("receiveBufferSizePredictorFactory", new AdaptiveReceiveBufferSizePredictorFactory(1024, 8192, 131072));
        b.bind(new InetSocketAddress(PORT));

        LOG.info("server bound to port {}", PORT);
    }

    public static void main(String[] args) {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        LOG.info("Starting mock server...");
        new MockEndpoint().run();
    }

    private void writeOutput(final Channel channel, StringBuffer buffer, boolean isChunked) {
        ChannelBuffer output = ChannelBuffers.copiedBuffer(buffer.toString(), CharsetUtil.UTF_8);

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (isChunked) {
            response.setChunked(true);
            channel.write(response);
            LOG.debug("writing response: {}", response);
            LOG.debug("chunking");

            final int chunkCount = 10;
            final int chunkSize = output.readableBytes() / chunkCount;
            for (int i = 0; i < chunkCount; i++) {
                HttpChunk chunk = new DefaultHttpChunk(output.slice(i * chunkSize, i == chunkCount - 1 ? output.writerIndex() - i * chunkSize
                        : chunkSize));
                channel.write(chunk);
                LOG.debug("writing chunk: {}", chunk);
            }
            HttpChunk chunk = new DefaultHttpChunk(ChannelBuffers.EMPTY_BUFFER);
            channel.write(chunk);
            LOG.debug("wrote last chunk");
        } else {
            response.setContent(output);
            channel.write(response);
            LOG.debug("writing response: {}", response);
            LOG.debug("wrote response");
        }
    }

    /*private void buildBuffer(HttpRequest request) {
        buf.setLength(0);
        buf.append("WELCOME TO NEIL'S WEB SERVER\r\n");
        buf.append("===================================\r\n");

        buf.append("VERSION: " + request.getProtocolVersion() + "\r\n");
        buf.append("REQUEST_URI: " + request.getUri() + "\r\n\r\n");

        for (Map.Entry<String, String> h : request.getHeaders()) {
            buf.append("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n");
        }
        buf.append("\r\n");

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = queryStringDecoder.getParameters();
        if (!params.isEmpty()) {
            for (Entry<String, List<String>> p : params.entrySet()) {
                String key = p.getKey();
                List<String> vals = p.getValue();
                for (String val : vals) {
                    buf.append("PARAM: " + key + " = " + val + "\r\n");
                }
            }
            buf.append("\r\n");
        }

        ChannelBuffer content = request.getContent();
        if (content.readable()) {
            buf.append("CONTENT: " + content.toString(CharsetUtil.UTF_8) + "\r\n");
        }
    }*/
}
