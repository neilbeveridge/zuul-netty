package com.netflix.zuul.proxy;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServerTest {
	private static final String BOOKING_CONFIRMED = "booking confirmed";

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyServerTest.class);
	
	private static final String LOCAL_HOST = "localhost";
	private static final int REMOTE_PORT = 8081;
	private static final int LOCAL_PORT = 8080;
	
	private ProxyServer underTest;
	private MockEndpoint backEndService;
	
	@Before
	public void setup() {
		
		
	}
	
	@Test
	public void canProxyRequestToBookingService() throws Exception {
		
		// given
		Future<Void> afterProxyServer = setUpProxyServer();
		Future<Void> afterMockBackEnd = setUpMockBackEndService(BOOKING_CONFIRMED);
		
		// TODO : provide a better way of synching on startup of both mock & proxy server.
		Thread.sleep(5000);
		
		// when
		String responseBody = sendRequest("http://localhost:8080/booking");
		
		// then
		
       
        assertEquals(BOOKING_CONFIRMED, responseBody);
        
        // shutdown
        
        // TODO have separate start/stop methods for servers so we don't have to kill them like this
        
        afterProxyServer.cancel(true);
        afterMockBackEnd.cancel(true);
    }

    private Future<Void> setUpMockBackEndService(String responseToGive) {
    	backEndService = new MockEndpoint(REMOTE_PORT, responseToGive);
    	
    	return Executors.newFixedThreadPool(1).submit(new Callable<Void>() {
    		@Override
    		public Void call() throws Exception {
    			backEndService.run();
    			return null;
    		}
    	});
	}

	private Future<Void> setUpProxyServer() throws Exception {
		underTest = new ProxyServer(LOCAL_PORT, LOCAL_HOST, REMOTE_PORT);

		return Executors.newFixedThreadPool(1).submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				underTest.bootstrap();
				return null;
			}
		});
	}

	public String sendRequest(final String uri) throws Exception {
		URL url = new URL(uri.toString());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	
		StringBuilder a = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
				
		while (true) {
			String line = reader.readLine();

			if (line == null)
				break;

			a.append(line);
		}
		String content = a.toString();

		return content;
	}

}
