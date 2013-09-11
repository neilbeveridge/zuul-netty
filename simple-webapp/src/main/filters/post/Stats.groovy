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
package scripts.postProcess

import com.netflix.zuul.netty.filter.AbstractZuulFilter
import com.netflix.zuul.netty.filter.RequestContext

/**
 * @author Mikey Cohen
 * Date: 2/3/12
 * Time: 2:48 PM
 */
class Stats extends AbstractZuulFilter {

    Stats() {
        super("post", 2000)
    }

    @Override
    boolean shouldFilter(RequestContext requestContext) {
        return true
    }

    @Override
    Void doExecute(RequestContext requestContext) {
        dumpRoutingDebug(requestContext)
        dumpRequestDebug(requestContext)
    }

    public void dumpRequestDebug(RequestContext requestContext) {
        List<String> rd = requestContext.get("requestDebug");
        rd?.each {
            println("REQUEST_DEBUG::${it}");
        }
    }

    public void dumpRoutingDebug(RequestContext requestContext) {
        List<String> rd = requestContext.get("routingDebug");
        rd?.each {
            println("ZUUL_DEBUG::${it}");
        }
    }

}
