package com.netflix.zuul.netty.filter;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author HWEB
 */
public class ZuulFiltersLoaderTest {

    public static final Path FILTERS_ROOT_PATH = Paths.get(ZuulFiltersLoaderTest.class.getResource("/filters").getFile());

    @Test
    @Ignore
    public void notifiesListenersOfNewFilters() throws Exception {
        ZuulFiltersLoader zuulFiltersLoader = new ZuulFiltersLoader(FILTERS_ROOT_PATH);
        FiltersListener filtersListener = mock(FiltersListener.class);
        zuulFiltersLoader.addFiltersListener(filtersListener);

        zuulFiltersLoader.reload();

        verify(filtersListener).filterAdded(any(Path.class), any(ZuulFilter.class));
    }
}
