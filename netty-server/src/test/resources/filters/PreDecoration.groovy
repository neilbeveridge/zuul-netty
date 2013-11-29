/*
 * Copyright 2013 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */


import com.netflix.zuul.netty.filter.AbstractZuulPreFilter
import com.netflix.zuul.proxy.framework.api.FrameworkHttpRequest
import com.netflix.zuul.proxy.framework.api.Route

import java.util.concurrent.ThreadLocalRandom


/**
 * @author mhawthorne
 */
class PreDecorationFilter extends AbstractZuulPreFilter {
    private URI mockBackEnd = new URI ("http://localhost:8081");
    private URI invalidRoute = new URI ("http://localhost:8082");

    PreDecorationFilter() {
        super(5)

        System.out.println("DEBUG PreDecorationFilter constructed");
    }

    @Override
    void requestReceived(FrameworkHttpRequest request) {
        System.out.println("URI received by script: " + request.getUri());

        URI route;
        
        if("/example".equals(request.getUri())) {
          route = mockBackEnd;
        } else {
          route = invalidRoute;
        }

        System.out.println("DEBUG PreDecorationFilter requestReceived, route = " + route);
        request.setRoute(route);
    }
}
