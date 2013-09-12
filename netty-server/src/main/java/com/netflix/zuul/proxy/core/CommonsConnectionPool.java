package com.netflix.zuul.proxy.core;

import com.netflix.zuul.proxy.HttpOutboundPipeline;
import com.netflix.zuul.proxy.IllegalRouteException;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_GROW;

public class CommonsConnectionPool implements com.netflix.zuul.proxy.core.ConnectionPool {

    private static final Logger LOG = LoggerFactory.getLogger(CommonsConnectionPool.class);

    private static final long TIME_BETWEEN_BACKGROUND_EVICTIONS = 1000L;
    private static final long EVICT_IDLE_AFTER = -1; // -ve for no time-based eviction

    private final Counter aliveConnections = Metrics.newCounter(CommonsConnectionPool.class, "alive-connections");
    private volatile Map<URL, ObjectPool<ChannelFuture>> pool = new HashMap<>();
    private final ChannelFactory channelFactory;
    private Lock poolCreationRaceLock = new ReentrantLock();

    private GenericObjectPool<ChannelFuture> createApplicationPool(final URL routeHost)
    throws IllegalRouteException {

        //only the connection oriented parts of the URL should be set - avoid accidental creation of a ton of connection pools
        try {
            if (!new URL(routeHost.getProtocol(), routeHost.getHost(), routeHost.getPort(), "").equals(routeHost)) {
                LOG.info("route {} should only define protocol, host and port", routeHost);
                throw new IllegalRouteException(routeHost.toString());
            }
            if (routeHost.getPort() == -1) {
                LOG.info("route {} must define a port", routeHost);
                throw new IllegalRouteException(routeHost.toString());
            }
        } catch (MalformedURLException e) {
            throw new IllegalRouteException(routeHost.toString());
        }

        final GenericObjectPool<ChannelFuture> applicationPool = new GenericObjectPool<ChannelFuture>(new PoolableObjectFactory<ChannelFuture>() {

            @Override
            public void activateObject(ChannelFuture future) throws Exception {
                // not supported
            }

            @Override
            public void destroyObject(ChannelFuture future) throws Exception {
                LOG.debug("destroying connection {}", Integer.toHexString(future.getChannel().getId()));

                // if an error has closed the channel already Netty incorrectly throws an
                // exception without this check see https://github.com/netty/netty/issues/724
                if (future.getChannel().isOpen()) {
                    future.getChannel().close();
                }

                if (future.isSuccess()) {
                    aliveConnections.dec();
                }
            }

            @Override
            public ChannelFuture makeObject() throws Exception {
                ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
                bootstrap.setPipelineFactory(new HttpOutboundPipeline());

                final ChannelFuture future = bootstrap.connect(new InetSocketAddress(routeHost.getHost(), routeHost.getPort()));
                LOG.info("attempting connection to remote host {}:{} on connection {}", routeHost.getHost(), routeHost.getPort(),
                        Integer.toHexString(future.getChannel().getId()));

                future.addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            aliveConnections.inc();
                        } else {
                            LOG.warn("connection to remote host FAILED {}:{} on connection {}", routeHost.getHost(), routeHost.getPort(),
                                    Integer.toHexString(future.getChannel().getId()));
                        }
                    }

                });

                return future;
            }

            @Override
            public void passivateObject(ChannelFuture future) throws Exception {
                // not supported
            }

            @Override
            public boolean validateObject(ChannelFuture future) {
                boolean isValid = !future.isDone() || future.isDone() && future.isSuccess() && future.getChannel().isConnected();
                if (!isValid) {
                    LOG.debug("connection not valid {}", Integer.toHexString(future.getChannel().getId()));
                }
                return isValid;
            }

        });

        applicationPool.setLifo(false); // fifo seeks to short circuit timeouts at the target - otherwise the target will disconnect less used connections
        applicationPool.setMaxActive(-1);
        applicationPool.setMaxIdle(-1);
        applicationPool.setMaxWait(-1);
        applicationPool.setMinEvictableIdleTimeMillis(EVICT_IDLE_AFTER);
        applicationPool.setNumTestsPerEvictionRun(-10);
        applicationPool.setTestOnBorrow(true);
        applicationPool.setTestOnReturn(true);
        applicationPool.setTestWhileIdle(true);
        applicationPool.setTimeBetweenEvictionRunsMillis(TIME_BETWEEN_BACKGROUND_EVICTIONS);
        applicationPool.setWhenExhaustedAction(WHEN_EXHAUSTED_GROW);

        Metrics.newGauge(CommonsConnectionPool.class, String.format("pool-%s-active", routeHost.toString().toLowerCase()), new Gauge<Integer>() {
            @Override
            public Integer value() {
                return applicationPool.getNumActive();
            }
        });
        Metrics.newGauge(CommonsConnectionPool.class, String.format("pool-%s-idle", routeHost.toString().toLowerCase()), new Gauge<Integer>() {
            @Override
            public Integer value() {
                return applicationPool.getNumIdle();
            }
        });

        return applicationPool;
    }

    public CommonsConnectionPool(ChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    @Override
    public Connection borrow(URL routeHost)
    throws IllegalRouteException {

        //only take out the lock if absolutely necessary
        if (!pool.containsKey(routeHost)) {
            poolCreationRaceLock.lock();
            if (!pool.containsKey(routeHost)) {
                this.pool.put(routeHost, createApplicationPool(routeHost));
            }
            poolCreationRaceLock.unlock();
        }

        try {
            ChannelFuture future = pool.get(routeHost).borrowObject();
            Connection connection = new Connection(routeHost, future);
            LOG.debug("borrowing connection {}", connection.getId());
            return connection;
        } catch (NoSuchElementException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void release(Connection connection) {
        LOG.debug("releasing connection {}", connection.getId());
        try {
            pool.get(connection.getRouteHost()).returnObject(connection.getChannelFuture());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy(Connection connection) {
        LOG.debug("destroying connection {}", connection.getId());
        try {
            pool.get(connection.getRouteHost()).invalidateObject(connection.getChannelFuture());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
