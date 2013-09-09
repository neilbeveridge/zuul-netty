package com.netflix.zuul.netty;

import com.netflix.zuul.DynamicCodeCompiler;
import com.netflix.zuul.groovy.GroovyCompiler;
import com.netflix.zuul.netty.filter.ZuulFilter;

import java.io.File;
import java.lang.reflect.Modifier;

/**
 * @author HWEB
 */
public class ZuulRunner {

    public static final DynamicCodeCompiler COMPILER = new GroovyCompiler();

    public ZuulFilter putFilter(File file) throws Exception {
        Class clazz = COMPILER.compile(file);
        if (!Modifier.isAbstract(clazz.getModifiers())) {
            ZuulFilter zuulFilter = (ZuulFilter) clazz.newInstance();
            return zuulFilter;
        }

        return null;
    }
}
