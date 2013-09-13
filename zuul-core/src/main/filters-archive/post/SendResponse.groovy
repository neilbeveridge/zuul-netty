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



import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicIntProperty
import com.netflix.config.DynamicPropertyFactory
import com.netflix.util.Pair
import com.netflix.zuul.constants.ZuulConstants
import com.netflix.zuul.netty.debug.Debug
import com.netflix.zuul.netty.filter.AbstractZuulPreFilter

import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponse

class SendResponseFilter extends AbstractZuulPreFilter {

    static DynamicBooleanProperty INCLUDE_DEBUG_HEADER =
        DynamicPropertyFactory.getInstance().getBooleanProperty(ZuulConstants.ZUUL_INCLUDE_DEBUG_HEADER, false);

    static DynamicIntProperty INITIAL_STREAM_BUFFER_SIZE =
        DynamicPropertyFactory.getInstance().getIntProperty(ZuulConstants.ZUUL_INITIAL_STREAM_BUFFER_SIZE, 1024);

    static DynamicBooleanProperty SET_CONTENT_LENGTH = DynamicPropertyFactory.getInstance().getBooleanProperty(ZuulConstants.ZUUL_SET_CONTENT_LENGTH, false);

    SendResponseFilter() {
        super('post', 1000)
    }


    boolean shouldFilter(RequestContext currentContext) {
        return !currentContext.getZuulResponseHeaders().isEmpty() ||
                currentContext.getResponseDataStream() != null ||
                currentContext.responseBody != null
    }


    Void doExecute(RequestContext requestContext) {
        addResponseHeaders(requestContext)
        writeResponse(requestContext)
    }

    void writeResponse(RequestContext context) {
        // there is no body to send
        if (context.getResponseBody() == null && context.getResponseDataStream() == null) return;

        HttpResponse response = context.getResponse()
        HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_ENCODING, "UTF-8")
        context.writeResponse()
    }


    private void addResponseHeaders(RequestContext context) {
        HttpResponse response = context.getResponse();
        List<Pair<String, String>> zuulResponseHeaders = context.getZuulResponseHeaders();
        String debugHeader = ""

        List<String> rd

        rd = (List<String>) context.get("routingDebug");
        rd?.each {
            debugHeader += "[[[${it}]]]";
        }

        /*
        rd = (List<String>) RequestContext.getCurrentContext().get("requestDebug");
        rd?.each {
            debugHeader += "[[[REQUEST_DEBUG::${it}]]]";
        }
        */

        if (INCLUDE_DEBUG_HEADER.get()) HttpHeaders.setHeader(response, "X-Zuul-Debug-Header", debugHeader)

        if (Debug.debugRequest(context)) {
            zuulResponseHeaders?.each { Pair<String, String> it ->
                HttpHeaders.setHeader(response, it.first(), it.second())
                Debug.addRequestDebug(context, "OUTBOUND: <  " + it.first() + ":" + it.second())
            }
        } else {
            zuulResponseHeaders?.each { Pair<String, String> it ->
                HttpHeaders.setHeader(response, it.first(), it.second())
            }
        }


        Integer contentLength = context.getOriginContentLength()

        // only inserts Content-Length if origin provides it and origin response is not gzipped
        if (SET_CONTENT_LENGTH.get()) {
            if (contentLength != null && !ctx.getResponseGZipped())
                HttpHeaders.setContentLength(response, contentLength)
        }
    }

}