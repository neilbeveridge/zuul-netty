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
package scripts.preProcess

import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.netflix.config.DynamicStringProperty
import com.netflix.zuul.constants.ZuulConstants
import com.netflix.zuul.netty.filter.AbstractZuulFilter
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse

class DebugFilter extends AbstractZuulFilter {

    static final DynamicBooleanProperty routingDebug = DynamicPropertyFactory.getInstance().getBooleanProperty(ZuulConstants.ZUUL_DEBUG_REQUEST, true)
    static final DynamicStringProperty debugParameter = DynamicPropertyFactory.getInstance().getStringProperty(ZuulConstants.ZUUL_DEBUG_PARAMETER, "d")

    DebugFilter() {
        super('pre', 1)
    }


    boolean shouldFilter(HttpRequest request, HttpResponse response) {
        if ("true".equals(request.getParameter(debugParameter.get()))) return true;
        return routingDebug.get();

    }

    public Void execute(HttpRequest request, HttpResponse response) {

        return null;
    }


}
