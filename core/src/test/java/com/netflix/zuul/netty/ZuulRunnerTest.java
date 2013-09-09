package com.netflix.zuul.netty;

import com.netflix.zuul.netty.filter.ZuulFilter;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author HWEB
 */
public class ZuulRunnerTest {

    public static final Path FILTERS_ROOT_PATH = Paths.get(ZuulServerTest.class.getResource("/filters").getFile());

    final ZuulRunner runner = new ZuulRunner();

    @Test
    public void compilesGroovyFilter() throws Exception {
        ZuulFilter zuulFilter = runner.putFilter(FILTERS_ROOT_PATH.resolve("pre/DebugFilter.groovy").toFile());

        assertThat(zuulFilter.type(), is("pre"));
        assertThat(zuulFilter.order(), is(1));
    }
}
