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

    private static final String[] HOSTS = [
        "ip-172-31-22-187.us-west-1.compute.internal",
        "ip-172-31-29-235.us-west-1.compute.internal",
        "ip-172-31-31-215.us-west-1.compute.internal",
        "ip-172-31-31-213.us-west-1.compute.internal",
        "ip-172-31-31-214.us-west-1.compute.internal"
    ]

    PreDecorationFilter() {
        super(5)
    }

    @Override
    void requestReceived(FrameworkHttpRequest request) {
        request.addHeader(Route.ROUTE_HEADER, HOSTS[ThreadLocalRandom.current().nextInt(HOSTS.length)]);
    }
}
