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


import com.netflix.zuul.netty.debug.Debug
import com.netflix.zuul.netty.filter.AbstractZuulPreFilter
import com.netflix.zuul.netty.filter.AbstractZuulPreFilter

/**
 * @author mhawthorne
 */
class PreDecorationFilter extends AbstractZuulPreFilter {

    PreDecorationFilter() {
        super('pre', 5)
    }

    @Override
    Void doExecute(RequestContext requestContext) {
        // sets origin
        Debug.addRequestDebug(requestContext, "SETTING ROUTE HOST :: > http://apache.org/")
        requestContext.setRouteHost(new URL("http://apache.org/"));
        // sets custom header to send to the origin
        requestContext.addOriginResponseHeader("cache-control", "max-age=3600");

        return null
    }
}
