package com.netflix.zuul.proxy.core;

public enum Application {
    HWA("/hw", "localhost", 8081, new String[]{}),
    SHA("/sp", "localhost", 8081, new String[]{}),
    PDA("/pd", "localhost", 8081, new String[]{}),
    BKA("/ba", "localhost", 8081, new String[]{}),
    LPA("/lp", "localhost", 8081, new String[]{});

    public static final String APP_HEADER = "X-hcom-app";
    public static final Application DEFAULT_APPLICATION = LPA;

    private String prefix;
    private String host;
    private int port;
    private String[] uris;

    private Application(String prefix, String host, int port, String[] uris) {
        this.prefix = prefix;
        this.host = host;
        this.port = port;
        this.uris = uris;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPrefix() {
        return prefix;
    }

    public String[] getUris() {
        return uris;
    }
}
