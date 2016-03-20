package com.navercorp.pinpoint.plugin.metamx.netty.httpclient;/*
 * Copyright 2014 NAVER Corp.
 *
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

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import java.security.ProtectionDomain;

/**
 * 
 * @author hamlet-lee
 *
 */
public class MetamxNettyHttpClientPlugin implements ProfilerPlugin, TransformTemplateAware {
    private PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        MetamxNettyHttpClientPluginConfig config = new MetamxNettyHttpClientPluginConfig(context.getConfig());
        if( !config.isProfile() ) {
            logger.info("MetamxNettyHttpClient: disabled");
            return;
        }
        logger.info("MetamxNettyHttpClient: enabled");
        transformTemplate.transform("com.metamx.http.client.NettyHttpClient", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                logger.info("MetamxNettyHttpClient: instrumenting");
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                InstrumentMethod go = target.getDeclaredMethod("go",
                        "com.metamx.http.client.Request",
                        "com.metamx.http.client.response.HttpResponseHandler",
                        "org.joda.time.Duration");

                go.addInterceptor("com.navercorp.pinpoint.plugin.metamx.netty.httpclient.interceptor.SendRequestInterceptor");
                logger.info("MetamxNettyHttpClient: instrument done");
                return target.toBytecode();
            }
        });
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
