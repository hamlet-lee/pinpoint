/**
 * Copyright 2014 NAVER Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.plugin.metamx.netty.httpclient.interceptor;

/**
 * @author hamlet-lee
 */


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.jaxrs.smile.SmileMediaTypes;
import com.google.common.collect.Multimap;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor1;
import com.metamx.http.client.Request;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.util.InterceptorUtils;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.plugin.metamx.netty.httpclient.MetamxNettyHttpClientConstants;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import java.net.URL;
import java.util.Collection;

/**
 *
 * @author hamlet-lee
 *
 */
public class SendRequestInterceptor implements AroundInterceptor1 {
    private PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final MethodDescriptor descriptor;
    private final TraceContext traceContext;
    public static final String APPLICATION_JSON = "application/json";
    private static ObjectMapper smileMapper = new ObjectMapper( new SmileFactory());

    public SendRequestInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.descriptor = descriptor;
        this.traceContext = traceContext;
    }

    @Override
    public void before(Object target, Object arg0) {
        if( logger.isDebugEnabled()) {
            logger.debug("MetamxNettyHttpClient: before go()");
        }
        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        Request request = (Request) arg0;
        Multimap<String, String> headers = request.getHeaders();
        if (trace.canSampled()) {
            SpanEventRecorder recorder = trace.traceBlockBegin();

            // RPC call trace have to be recorded with a service code in RPC client code range.
            recorder.recordServiceType(MetamxNettyHttpClientConstants.SERVICE_TYPE);

            // You have to issue a TraceId the receiver of this request will use.
            TraceId nextId = trace.getTraceId().getNextTraceId();

            // Then record it as next span id.
            recorder.recordNextSpanId(nextId.getSpanId());

            // Finally, pass some tracing data to the server.
            // How to put them in a message is protocol specific.
            headers.put(Header.HTTP_TRACE_ID.toString(), nextId.getTransactionId());
            headers.put(Header.HTTP_SPAN_ID.toString(), Long.toString(nextId.getSpanId()));
            headers.put(Header.HTTP_PARENT_SPAN_ID.toString(), Long.toString(nextId.getParentSpanId()));
            headers.put(Header.HTTP_PARENT_APPLICATION_TYPE.toString(), Short.toString(traceContext.getServerTypeCode()));
            headers.put(Header.HTTP_PARENT_APPLICATION_NAME.toString(), traceContext.getApplicationName());
            headers.put(Header.HTTP_FLAGS.toString(), Short.toString(nextId.getFlags()));


            URL url = request.getUrl();
            int port = url.getPort();
            String serverAddress = url.getHost();
            recorder.recordAttribute(AnnotationKey.HTTP_URL, url);
            String endpoint = serverAddress + ":" + port;

            //need this to build map?
            headers.put(Header.HTTP_HOST.toString(), endpoint);

            recorder.recordDestinationId(endpoint);
            recorder.recordEndPoint(endpoint);
            String entityString = null;
            if( request.getMethod().equals(HttpMethod.POST)) {
                Collection<String> strs = request.getHeaders().get(HttpHeaders.Names.CONTENT_TYPE);
                String contentType = null;
                for(String s : strs) {
                    contentType = s;
                }
                byte[] bytes = request.getContent().array();
                if( SmileMediaTypes.APPLICATION_JACKSON_SMILE.equals(contentType)){
                    try {
                        entityString = smileMapper.readTree(bytes).toString();
                    }catch(Exception e){
                        if(logger.isDebugEnabled()){
                            logger.error("error", e);
                        }
                        entityString = "parse BSON error!";
                    }
                }else if( APPLICATION_JSON.equals(contentType) ||
                        contentType != null && contentType.toLowerCase().contains("text")){
                    entityString = new String(bytes);
                }else{
                    entityString = new String(bytes);
                }
            }
            if( entityString != null) {
                recorder.recordAttribute(AnnotationKey.HTTP_PARAM_ENTITY, entityString);
            }
        } else {
            // If sampling this transaction is disabled, pass only that infomation to the server.
            headers.put(Header.HTTP_SAMPLED.toString(), "1");
        }
    }

    @Override
    public void after(Object target, Object arg0, Object result, Throwable throwable) {
        if( logger.isDebugEnabled()) {
            logger.debug("MetamxNettyHttpClient: after go()");
        }
        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();

            recorder.recordApi(descriptor);

            if (throwable == null) {
                // RPC client have to record end point (server address)
            } else {
                recorder.recordException(throwable);
            }
        } finally {
            trace.traceBlockEnd();
        }
    }
}
