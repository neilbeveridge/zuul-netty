package com.netflix.zuul.proxy.framework.api;

public interface Interrupts {

    void movedPermanently (String location);
    void temporaryRedirect (String location);
    
}
