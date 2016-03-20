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
package com.navercorp.pinpoint.plugin.metamx.netty.httpclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.metamx.common.lifecycle.Lifecycle;
import com.metamx.http.client.*;
import com.metamx.http.client.netty.HttpClientPipelineFactory;
import com.metamx.http.client.pool.ChannelResourceFactory;
import com.metamx.http.client.pool.ResourcePool;
import com.metamx.http.client.pool.ResourcePoolConfig;
import com.metamx.http.client.response.ClientResponse;
import com.metamx.http.client.response.HttpResponseHandler;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifier;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.navercorp.pinpoint.test.plugin.Dependency;
import com.navercorp.pinpoint.test.plugin.JvmVersion;
import com.navercorp.pinpoint.test.plugin.PinpointPluginTestSuite;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.nio.NioClientBossPool;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.Timer;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.metamx.http.client.Request;

import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.annotation;
import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.event;


/**
 * @author hamlet-lee
 *
 */
@RunWith(PinpointPluginTestSuite.class)
@JvmVersion({7,8})
@Dependency({ "io.netty:netty:3.10.4.Final",
        "com.metamx:http-client:1.0.4",
        "log4j:log4j:[1.2.16]",
        "com.fasterxml.jackson.jaxrs:jackson-jaxrs-smile-provider:2.4.6",
        "com.fasterxml.jackson.core:jackson-databind:2.4.6",
        "javax.ws.rs:jsr311-api:1.1.1"})
public class MetamxNettyHttpClientIT {
    private Lifecycle lifeCycle;
    @Before
    public void setup() throws Exception {
        lifeCycle = new Lifecycle();
        lifeCycle.start();
    }

    @After
    public void teardown(){
        lifeCycle.stop();
    }

    public static ClientBootstrap createBootstrap(Lifecycle lifecycle, Timer timer)
    {
        // Default from NioClientSocketChannelFactory.DEFAULT_BOSS_COUNT, which is private:
        final int bossCount = 1;

        // Default from SelectorUtil.DEFAULT_IO_THREADS, which is private:
        final int workerCount = Runtime.getRuntime().availableProcessors() * 2;

        final NioClientBossPool bossPool = new NioClientBossPool(
                Executors.newCachedThreadPool(
                        new ThreadFactoryBuilder()
                                .setDaemon(true)
                                .setNameFormat("HttpClient-Netty-Boss-%s")
                                .build()
                ),
                bossCount,
                timer,
                ThreadNameDeterminer.CURRENT
        );

        final NioWorkerPool workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(
                        new ThreadFactoryBuilder()
                                .setDaemon(true)
                                .setNameFormat("HttpClient-Netty-Worker-%s")
                                .build()
                ),
                workerCount,
                ThreadNameDeterminer.CURRENT
        );

        final ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(bossPool, workerPool));

        bootstrap.setOption("keepAlive", true);
        bootstrap.setPipelineFactory(new HttpClientPipelineFactory());

        InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());

        try {
            lifecycle.addMaybeStartHandler(
                    new Lifecycle.Handler()
                    {
                        @Override
                        public void start() throws Exception
                        {
                        }

                        @Override
                        public void stop()
                        {
                            bootstrap.releaseExternalResources();
                        }
                    }
            );
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return bootstrap;
    }

    public HttpClient get()
    {
        final HashedWheelTimer timer = new HashedWheelTimer(
                new ThreadFactoryBuilder().setDaemon(true)
                        .setNameFormat("HttpClient-Timer-%s")
                        .build(),
                ThreadNameDeterminer.CURRENT,
                100,
                TimeUnit.MILLISECONDS,
                512
        );

        return new NettyHttpClient(
                new ResourcePool(
                        new ChannelResourceFactory(
                                createBootstrap(lifeCycle, timer),
                                null,
                                timer,
                                -1
                        ),
                        new ResourcePoolConfig(3)
                )
        );
    }

    HttpResponseHandler<InputStream, InputStream> getResponseHandler() {
        return new HttpResponseHandler<InputStream, InputStream>()
        {
            private long responseStartTime;
            private final AtomicLong byteCount = new AtomicLong(0);
            private final BlockingQueue<InputStream> queue = new LinkedBlockingQueue<InputStream>();
            private final AtomicBoolean done = new AtomicBoolean(false);

            @Override
            public ClientResponse<InputStream> handleResponse(HttpResponse response)
            {
                try {
                    queue.put(new ChannelBufferInputStream(response.getContent()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw Throwables.propagate(e);
                }

                byteCount.addAndGet(response.getContent().readableBytes());
                return ClientResponse.<InputStream>finished(
                        new SequenceInputStream(
                                new Enumeration<InputStream>()
                                {
                                    @Override
                                    public boolean hasMoreElements()
                                    {
                                        // Done is always true until the last stream has be put in the queue.
                                        // Then the stream should be spouting good InputStreams.
                                        synchronized (done) {
                                            return !done.get() || !queue.isEmpty();
                                        }
                                    }

                                    @Override
                                    public InputStream nextElement()
                                    {
                                        try {
                                            return queue.take();
                                        }
                                        catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            throw Throwables.propagate(e);
                                        }
                                    }
                                }
                        )
                );
            }

            @Override
            public ClientResponse<InputStream> handleChunk(
                    ClientResponse<InputStream> clientResponse, HttpChunk chunk
            )
            {
                final ChannelBuffer channelBuffer = chunk.getContent();
                final int bytes = channelBuffer.readableBytes();
                if (bytes > 0) {
                    try {
                        queue.put(new ChannelBufferInputStream(channelBuffer));
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw Throwables.propagate(e);
                    }
                    byteCount.addAndGet(bytes);
                }
                return clientResponse;
            }

            @Override
            public ClientResponse<InputStream> done(ClientResponse<InputStream> clientResponse)
            {
                synchronized (done) {
                    try {
                        // An empty byte array is put at the end to give the SequenceInputStream.close() as something to close out
                        // after done is set to true, regardless of the rest of the stream's state.
                        queue.put(ByteSource.empty().openStream());
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw Throwables.propagate(e);
                    }
                    catch (IOException e) {
                        // This should never happen
                        throw Throwables.propagate(e);
                    }
                    finally {
                        done.set(true);
                    }
                }
                return ClientResponse.<InputStream>finished(clientResponse.getObj());
            }

            @Override
            public void exceptionCaught(final ClientResponse<InputStream> clientResponse, final Throwable e)
            {
                // Don't wait for lock in case the lock had something to do with the error
                synchronized (done) {
                    done.set(true);
                    // Make a best effort to put a zero length buffer into the queue in case something is waiting on the take()
                    // If nothing is waiting on take(), this will be closed out anyways.
                    queue.offer(
                            new InputStream()
                            {
                                @Override
                                public int read() throws IOException
                                {
                                    throw new IOException(e);
                                }
                            }
                    );
                }
            }
        };
    }

    @Test
    public void test() throws Exception {
        String url = "http://www.baidu.com/";
        HttpClient httpClient = get();



        ListenableFuture<InputStream> future = httpClient.go(new Request(
                        HttpMethod.GET,
                        new URL(url)),
                getResponseHandler()
        );

        try {
            future.get();
        }catch(Throwable e){}

        Method go = com.metamx.http.client.NettyHttpClient.class.getMethod("go",
                com.metamx.http.client.Request.class,
                com.metamx.http.client.response.HttpResponseHandler.class,
                org.joda.time.Duration.class);

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();

        verifier.verifyTraceCount(1);
        verifier.verifyTrace(event("METAMX-HTTP", go, annotation("http.url", "http://www.baidu.com/")));
    }

    @Test
    public void testPost() throws Exception {
        String url = "http://www.baidu.com/";
        HttpClient httpClient = get();



        ListenableFuture<InputStream> future = httpClient.go(new Request(
                        HttpMethod.POST,
                        new URL(url)).setContent("hello".getBytes()),
                getResponseHandler()
        );

        try {
            future.get();
        }catch(Throwable e){
            // Channel disconnected
        }

        Method go = com.metamx.http.client.NettyHttpClient.class.getMethod("go",
                com.metamx.http.client.Request.class,
                com.metamx.http.client.response.HttpResponseHandler.class,
                org.joda.time.Duration.class);

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();

        verifier.verifyTraceCount(1);
        verifier.verifyTrace(event("METAMX-HTTP", go, annotation("http.url", "http://www.baidu.com/"),
                annotation("http.entity", "hello")));
    }

    @Test
    public void testBson() throws Exception {
        String url = "http://www.baidu.com/";
        HttpClient httpClient = get();


        byte[] bsonBytes = new byte[]{58, 41, 10, 5, -6, -5};
        ListenableFuture<InputStream> future = httpClient.go(new Request(
                        HttpMethod.POST,
                    new URL(url)).setContent(bsonBytes).setHeader("Content-Type", "application/x-jackson-smile"),
                getResponseHandler()
        );

        try {
            future.get();
        }catch(Throwable e){
            // Channel disconnected
        }

        Method go = com.metamx.http.client.NettyHttpClient.class.getMethod("go",
                com.metamx.http.client.Request.class,
                com.metamx.http.client.response.HttpResponseHandler.class,
                org.joda.time.Duration.class);

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();

        verifier.verifyTraceCount(1);
        verifier.verifyTrace(event("METAMX-HTTP", go, annotation("http.url", "http://www.baidu.com/"),
                annotation("http.entity", "{}")));
    }
}
