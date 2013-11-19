package com.netflix.zuul.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMockEndPointServerInitializer extends ChannelInitializer<SocketChannel> {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpMockEndPointServerInitializer.class);
	private String responseToGive;
	
    public HttpMockEndPointServerInitializer(String responseToGive) {
		this.responseToGive = responseToGive;
	}

	@Override
    public void initChannel(SocketChannel ch) throws Exception {
    	
        // Create a default pipeline implementation.
        ChannelPipeline p = ch.pipeline();

        // Uncomment the following line if you want HTTPS
        //SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
        //engine.setUseClientMode(false);
        //p.addLast("ssl", new SslHandler(engine));

        p.addLast("decoder", new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        //p.addLast("aggregator", new HttpObjectAggregator(1048576));
        p.addLast("encoder", new HttpResponseEncoder());
        // Remove the following line if you don't want automatic content compression.
        //p.addLast("deflater", new HttpContentCompressor());
        p.addLast("handler", new HttpMockEndPointServerHandler(responseToGive));
        
        LOG.info("Added handlers to channel pipeline : " + p.names());
    }

}
