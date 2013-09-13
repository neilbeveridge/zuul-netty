package com.netflix.zuul.netty.filter;

/**
 * @author HWEB
 */
public interface FiltersChangeNotifier {

    FiltersChangeNotifier IGNORE = new FiltersChangeNotifier() {
        @Override
        public void addFiltersListener(FiltersListener filtersListener) {
        }
    };

    void addFiltersListener(FiltersListener filtersListener);
}
