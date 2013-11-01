package com.netflix.zuul.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.proxy.handler.HttpKeepAliveHandler;


public class MockEndpoint {
	
	/*
	private final class ServerHandler extends ChannelInboundHandlerAdapter {
		
		@Override
	    public void channelRead(ChannelHandlerContext ctx, Object message) {
			
			
                LOG.debug("received: {}", message.getClass().getSimpleName());

                Map<String, List<String>> params = new HashMap<>();

                StringBuffer buf = new StringBuffer();
                buf.setLength(0);
                buf.append("HI");

                if (message instanceof HttpRequest) {
                    final HttpRequest request = (HttpRequest) message;

                    LOG.debug("request: {}", request);

                    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
                    params = queryStringDecoder.parameters();

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
	*/
	
    private final class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    	
		private static final int ONE_MEGA_BYTE = 1048576;

		@Override
		protected void initChannel(SocketChannel channel) throws Exception {
			
			ChannelPipeline p = channel.pipeline();

		    p.addLast("idle-detection", IDLE_STATE_HANDLER);
		    p.addLast("http-decoder", new HttpRequestDecoder());
		    
		    // The line below is required to handle HTTP chunked transfer encoding. (see en.wikipedia.org/wiki/Chunked_transfer_encoding)
		    // TODO : test that HttpObjectAggregator handles both chunked AND non-chunked HTTP messages.
		    p.addLast("aggregator", new HttpObjectAggregator(ONE_MEGA_BYTE));
		    
		    p.addLast("http-encoder", new HttpResponseEncoder());
		    p.addLast("keep-alive", KEEP_ALIVE_HANDLER);
		    
		    // TODO : determine if this is required, since it just seems to handle HTTP chunked transfer encoding, which
		    //        should be handled by HttpObjectAggregator.
		    //p.addLast("responder", new ServerHandler() );
		}
	}

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
        IDLE_STATE_HANDLER = new IdleStateHandler(IDLE_TIMEOUT_READER, IDLE_TIMEOUT_WRITER, IDLE_TIMEOUT_BOTH);
    }

    public void run() throws InterruptedException {
    	
    	// Configure the server.
    	EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			
			bootstrap.group(bossGroup, workerGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			
			bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
			bootstrap.localAddress(PORT);
			
			bootstrap.childHandler(new ServerChannelInitializer());
			
			// Start the server.
			ChannelFuture f = bootstrap.bind().sync();
			
			LOG.info("server bound to port {}", PORT);
 
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
    	LoggingHandler loggingHandler = new LoggingHandler(MockEndpoint.class);

        LOG.info("Starting mock server...");
        
        new MockEndpoint().run();
    }

    /*
    private void writeOutput(final Channel channel, StringBuffer buffer, boolean isChunked) {
        ByteBuf output = Unpooled.copiedBuffer(buffer.toString(), CharsetUtil.UTF_8);

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpHeaders headers = response.headers();
        headers.add(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (isChunked) {
            headers.add(HttpHeaders.Names.CONTENT_TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
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
    */

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
