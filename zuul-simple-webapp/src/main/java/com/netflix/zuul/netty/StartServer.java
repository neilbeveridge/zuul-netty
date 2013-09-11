package com.netflix.zuul.netty;

import com.google.common.base.Optional;
import com.netflix.zuul.netty.filter.ZuulFiltersLoader;
import com.netflix.zuul.netty.filter.ZuulRunner;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author HWEB
 */
public class StartServer {

    private static final String POST_PATH = "/Users/fgizaw/fa/hcom/innovation/zuul-netty/simple-webapp/src/main/filters";

    public static final Path FILTERS_ROOT_PATH = Paths.get(POST_PATH);

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Please give port as argument");
            System.exit(1);
        }
        int port = Integer.parseInt(Optional.fromNullable(args[0]).or("9090"));

        ZuulFiltersLoader zuulFiltersLoader = new ZuulFiltersLoader(FILTERS_ROOT_PATH);
        ZuulRunner zuulRunner = new ZuulRunner();
        zuulFiltersLoader.addFiltersListener(zuulRunner);

        zuulFiltersLoader.reload();
        ZuulServer server = new ZuulServer(port)
                .add(zuulRunner);
        server.start();

    }


}
