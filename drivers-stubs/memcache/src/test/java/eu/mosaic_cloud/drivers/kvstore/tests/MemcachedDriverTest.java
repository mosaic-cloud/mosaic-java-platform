/*
 * #%L
 * mosaic-drivers-stubs-memcache
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.drivers.kvstore.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.drivers.kvstore.MemcachedDriver;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.tests.TestLoggingHandler;
import eu.mosaic_cloud.platform.core.utils.SerDesUtils;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MemcachedDriverTest {

    private static final String MOSAIC_MEMCACHED_HOST = "mosaic.tests.resources.memcached.host";
    private static final String MOSAIC_MEMCACHED_HOST_DEFAULT = "127.0.0.1";
    private static final String MOSAIC_MEMCACHED_PORT = "mosaic.tests.resources.memcached.port";
    private static final String MOSAIC_MEMCACHED_PORT_DEFAULT = "8091";
    private BasicThreadingContext threadingContext;
    private MemcachedDriver wrapper;
    private static String keyPrefix;

    @BeforeClass
    public static void setUpBeforeClass() {
        MemcachedDriverTest.keyPrefix = UUID.randomUUID().toString();
    }

    @Before
    public void setUp() {
        final Transcript transcript = Transcript.create(this);
        final QueueingExceptionTracer exceptionsQueue = QueueingExceptionTracer
                .create(NullExceptionTracer.defaultInstance);
        final TranscriptExceptionTracer exceptions = TranscriptExceptionTracer.create(transcript,
                exceptionsQueue);
        BasicThreadingSecurityManager.initialize();
        this.threadingContext = BasicThreadingContext.create(this, exceptions, exceptions.catcher);
        this.threadingContext.initialize();

        final String host = System.getProperty(MemcachedDriverTest.MOSAIC_MEMCACHED_HOST,
                MemcachedDriverTest.MOSAIC_MEMCACHED_HOST_DEFAULT);
        final Integer port = Integer.valueOf(System.getProperty(
                MemcachedDriverTest.MOSAIC_MEMCACHED_PORT,
                MemcachedDriverTest.MOSAIC_MEMCACHED_PORT_DEFAULT));

        final IConfiguration configuration = PropertyTypeConfiguration.create();
        configuration.addParameter("memcached.host_1", host);
        configuration.addParameter("memcached.port_1", port);
        configuration.addParameter("kvstore.driver_name", "MEMCACHED");
        configuration.addParameter("kvstore.driver_threads", 1);
        configuration.addParameter("kvstore.bucket", "test");
        configuration.addParameter("kvstore.user", "test");
        configuration.addParameter("kvstore.passwd", "test");

        this.wrapper = MemcachedDriver.create(configuration, this.threadingContext);
        this.wrapper.registerClient(MemcachedDriverTest.keyPrefix, "test");
    }

    @After
    public void tearDown() {
        this.wrapper.unregisterClient(MemcachedDriverTest.keyPrefix);
        this.wrapper.destroy();
        this.threadingContext.destroy();
    }

    public void testAdd() throws IOException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
        final String k2 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
        final byte[] b1 = SerDesUtils.pojoToBytes("wrong");
        final byte[] b2 = SerDesUtils.pojoToBytes("fabulous");
        final IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
                "add1");
        final IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
                "add2");
        final IResult<Boolean> r1 = this.wrapper.invokeAddOperation(MemcachedDriverTest.keyPrefix,
                k1, 30, b1, handler1);
        final IResult<Boolean> r2 = this.wrapper.invokeAddOperation(MemcachedDriverTest.keyPrefix,
                k2, 30, b2, handler2);
        try {
            Assert.assertFalse(r1.getResult());
            Assert.assertTrue(r2.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    public void testAppend() throws IOException, ClassNotFoundException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
        final byte[] b1 = SerDesUtils.pojoToBytes(" and miraculous");
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
                "append");
        final IResult<Boolean> r1 = this.wrapper.invokeAppendOperation(
                MemcachedDriverTest.keyPrefix, k1, b1, handler);
        try {
            Assert.assertTrue(r1.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
        final IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
                "Get after append");
        final IResult<byte[]> r2 = this.wrapper.invokeGetOperation(MemcachedDriverTest.keyPrefix,
                k1, handler1);
        try {
            Assert.assertEquals("fantabulous and miraculous", SerDesUtils.toObject(r2.getResult())
                    .toString());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    public void testCAS() throws IOException, ClassNotFoundException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
        final byte[] b1 = SerDesUtils.pojoToBytes("replaced by dummy");
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>("cas");
        final IResult<Boolean> r1 = this.wrapper.invokeCASOperation(MemcachedDriverTest.keyPrefix,
                k1, b1, handler);
        try {
            Assert.assertTrue(r1.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
        final IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
                "Get after cas");
        final IResult<byte[]> r2 = this.wrapper.invokeGetOperation(MemcachedDriverTest.keyPrefix,
                k1, handler1);
        try {
            Assert.assertEquals("replaced by dummy", SerDesUtils.toObject(r2.getResult())
                    .toString());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    public void testConnection() {
        Assert.assertNotNull(this.wrapper);
    }

    public void testDelete() {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
                "delete");
        final IResult<Boolean> r1 = this.wrapper.invokeDeleteOperation(
                MemcachedDriverTest.keyPrefix, k1, handler);
        try {
            Assert.assertTrue(r1.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
        final IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
                "Get after delete");
        final IResult<byte[]> r2 = this.wrapper.invokeGetOperation(MemcachedDriverTest.keyPrefix,
                k1, handler1);
        try {
            Assert.assertNull(r2.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    @Test
    public void testDriver() throws IOException, ClassNotFoundException {
        testConnection();
        testSet();
        testGet();
        testGetBulk();
        testAdd();
        testReplace();
        testDelete();
    }

    public void testGet() throws IOException, ClassNotFoundException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
        final IOperationCompletionHandler<byte[]> handler = new TestLoggingHandler<byte[]>("get");
        final IResult<byte[]> r1 = this.wrapper.invokeGetOperation(MemcachedDriverTest.keyPrefix,
                k1, handler);
        try {
            Assert.assertEquals("fantastic", SerDesUtils.toObject(r1.getResult()));
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    public void testGetBulk() throws IOException, ClassNotFoundException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
        final String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
        final List<String> keys = new ArrayList<String>();
        keys.add(k1);
        keys.add(k2);
        final IOperationCompletionHandler<Map<String, byte[]>> handler = new TestLoggingHandler<Map<String, byte[]>>(
                "getBulk");
        final IResult<Map<String, byte[]>> r1 = this.wrapper.invokeGetBulkOperation(
                MemcachedDriverTest.keyPrefix, keys, handler);
        try {
            Assert.assertEquals("fantastic", SerDesUtils.toObject(r1.getResult().get(k1))
                    .toString());
            Assert.assertEquals("famous", SerDesUtils.toObject(r1.getResult().get(k2)).toString());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    public void testPrepend() throws IOException, ClassNotFoundException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
        final byte[] b1 = SerDesUtils.pojoToBytes("it is ");
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
                "prepend");
        final IResult<Boolean> r1 = this.wrapper.invokePrependOperation(
                MemcachedDriverTest.keyPrefix, k1, b1, handler);
        try {
            Assert.assertTrue(r1.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
        final IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
                "Get after prepend");
        final IResult<byte[]> r2 = this.wrapper.invokeGetOperation(MemcachedDriverTest.keyPrefix,
                k1, handler1);
        try {
            Assert.assertEquals("it is fantabulous and miraculous",
                    SerDesUtils.toObject(r2.getResult()).toString());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    public void testReplace() throws IOException, ClassNotFoundException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
        final byte[] b1 = SerDesUtils.pojoToBytes("fantabulous");
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
                "replace");
        final IResult<Boolean> r1 = this.wrapper.invokeReplaceOperation(
                MemcachedDriverTest.keyPrefix, k1, 30, b1, handler);
        try {
            Assert.assertTrue(r1.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
        final IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
                "Get after replace");
        final IResult<byte[]> r2 = this.wrapper.invokeGetOperation(MemcachedDriverTest.keyPrefix,
                k1, handler1);
        try {
            Assert.assertEquals("fantabulous", SerDesUtils.toObject(r2.getResult()).toString());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }

    public void testSet() throws IOException {
        final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
        final byte[] bytes1 = SerDesUtils.pojoToBytes("fantastic");
        final IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
                "set 1");
        final IResult<Boolean> r1 = this.wrapper.invokeSetOperation(MemcachedDriverTest.keyPrefix,
                k1, 30, bytes1, handler1);
        Assert.assertNotNull(r1);
        final String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
        final byte[] bytes2 = SerDesUtils.pojoToBytes("famous");
        final IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
                "set 2");
        final IResult<Boolean> r2 = this.wrapper.invokeSetOperation(MemcachedDriverTest.keyPrefix,
                k2, 30, bytes2, handler2);
        Assert.assertNotNull(r2);
        try {
            Assert.assertTrue(r1.getResult());
            Assert.assertTrue(r2.getResult());
        } catch (final InterruptedException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        } catch (final ExecutionException e) {
            ExceptionTracer.traceIgnored(e);
            Assert.fail();
        }
    }
}
