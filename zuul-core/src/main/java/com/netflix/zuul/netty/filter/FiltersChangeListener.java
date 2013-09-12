package com.netflix.zuul.netty.filter;

import java.nio.file.Path;
import java.util.EventListener;

/**
 * @author HWEB
 */
public interface FiltersChangeListener extends EventListener {

    void filterAdded(Path filterPah);

    void filterRemoved(Path filterPah);

    void filterChanged(Path filterPah);
}
