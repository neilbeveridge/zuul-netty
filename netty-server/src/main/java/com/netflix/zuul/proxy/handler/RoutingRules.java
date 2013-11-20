package com.netflix.zuul.proxy.handler;

public class RoutingRules {
    private final String sourceUri;
    private final String destinationUri;

    public RoutingRules(String sourceUri, String destinationUri) {
        if (sourceUri == null) {
            throw new IllegalArgumentException("Null sourceUri");
        }
        if (destinationUri == null) {
            throw new IllegalArgumentException("Null destination");
        }

        this.sourceUri = sourceUri;
        this.destinationUri = destinationUri;
    }

    public String destinationForSource(String sourceUri) {
        if (this.sourceUri.equals(sourceUri)) {
            return destinationUri;
        }

        return null;
    }
}
