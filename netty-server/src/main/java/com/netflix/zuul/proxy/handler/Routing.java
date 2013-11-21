package com.netflix.zuul.proxy.handler;

import io.netty.util.AttributeKey;

import java.net.URI;

public final class Routing {
    public static final AttributeKey<URI> ROUTE_KEY = new AttributeKey<>("core.route");

    private Routing() {

    }
}
