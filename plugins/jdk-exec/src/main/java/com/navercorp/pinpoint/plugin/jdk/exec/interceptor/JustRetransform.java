package com.navercorp.pinpoint.plugin.jdk.exec.interceptor;

import com.navercorp.pinpoint.bootstrap.instrument.*;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor1;
import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.StaticAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.InterceptorScope;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;

import java.security.ProtectionDomain;
import java.util.concurrent.FutureTask;

/**
 * @author lisn
 */
public class JustRetransform implements StaticAroundInterceptor {
    private final Instrumentor instrumentor;
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    TransformCallback transformer1;
    TransformCallback transformer2;
    public JustRetransform(Instrumentor instrumentor, TransformCallback transformer1, TransformCallback transformer2) {
        this.instrumentor = instrumentor;
        this.transformer1 = transformer1;
        this.transformer2 = transformer2;
    }

    @Override
    public void before(Object target, String className, String methodName, String parameterDescription, Object[] args) {
        logger.info("calling retransform");
        instrumentor.retransform(java.util.concurrent.AbstractExecutorService.class, transformer1);
        instrumentor.retransform(FutureTask.class, transformer2);
        logger.info("retransform done");
    }

    @Override
    public void after(Object target, String className, String methodName, String parameterDescription, Object[] args, Object result, Throwable throwable) {

    }
}
