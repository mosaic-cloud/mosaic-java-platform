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
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.tests.TestLoggingHandler;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.core.utils.EncodingMetadata;
import eu.mosaic_cloud.platform.core.utils.PlainTextDataEncoder;
import eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
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
public class MemcachedDriverTest
{
	@Before
	public void setUp ()
	{
		final Transcript transcript = Transcript.create (this);
		final QueueingExceptionTracer exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final TranscriptExceptionTracer exceptions = TranscriptExceptionTracer.create (transcript, exceptionsQueue);
		this.exceptions = exceptions;
		BasicThreadingSecurityManager.initialize ();
		this.threadingContext = BasicThreadingContext.create (this, exceptions, exceptions.catcher);
		this.threadingContext.initialize ();
		final String host = System.getProperty (MemcachedDriverTest.MOSAIC_MEMCACHED_HOST, MemcachedDriverTest.MOSAIC_MEMCACHED_HOST_DEFAULT);
		final Integer port = Integer.valueOf (System.getProperty (MemcachedDriverTest.MOSAIC_MEMCACHED_PORT, MemcachedDriverTest.MOSAIC_MEMCACHED_PORT_DEFAULT));
		final IConfiguration configuration = PropertyTypeConfiguration.create ();
		configuration.addParameter ("memcached.host_1", host);
		configuration.addParameter ("memcached.port_1", port);
		configuration.addParameter ("kvstore.driver_name", "MEMCACHED");
		configuration.addParameter ("kvstore.driver_threads", 1);
		configuration.addParameter ("kvstore.bucket", "test");
		configuration.addParameter ("kvstore.user", "test");
		configuration.addParameter ("kvstore.passwd", "test");
		this.wrapper = MemcachedDriver.create (configuration, this.threadingContext);
		this.wrapper.registerClient (MemcachedDriverTest.keyPrefix, "test");
		this.encoder = PlainTextDataEncoder.DEFAULT_INSTANCE;
	}
	
	@After
	public void tearDown ()
	{
		this.wrapper.unregisterClient (MemcachedDriverTest.keyPrefix);
		this.wrapper.destroy ();
		this.threadingContext.destroy ();
	}
	
	public void testAdd ()
			throws IOException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		final String k2 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		final byte[] b1 = this.encoder.encode ("wrong", new EncodingMetadata ("text/plain", "identity"));
		final byte[] b2 = this.encoder.encode ("fabulous", new EncodingMetadata ("text/plain", "identity"));
		final KeyValueMessage mssg1 = new KeyValueMessage (k1, b1, "identity", "text/plain");
		final KeyValueMessage mssg2 = new KeyValueMessage (k2, b2, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean> ("add1");
		final IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean> ("add2");
		final IResult<Boolean> r1 = this.wrapper.invokeAddOperation (MemcachedDriverTest.keyPrefix, mssg1, 30, handler1);
		final IResult<Boolean> r2 = this.wrapper.invokeAddOperation (MemcachedDriverTest.keyPrefix, mssg2, 30, handler2);
		try {
			Assert.assertFalse (r1.getResult ());
			Assert.assertTrue (r2.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testAppend ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		final byte[] b1 = this.encoder.encode (" and miraculous", new EncodingMetadata ("text/plain", "identity"));
		final KeyValueMessage mssg1 = new KeyValueMessage (k1, b1, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("append");
		final IResult<Boolean> r1 = this.wrapper.invokeAppendOperation (MemcachedDriverTest.keyPrefix, mssg1, handler);
		try {
			Assert.assertTrue (r1.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
		final IOperationCompletionHandler<KeyValueMessage> handler1 = new TestLoggingHandler<KeyValueMessage> ("Get after append");
		final IResult<KeyValueMessage> r2 = this.wrapper.invokeGetOperation (MemcachedDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler1);
		try {
			final KeyValueMessage mssg = r2.getResult ();
			Assert.assertEquals ("fantabulous and miraculous", this.encoder.decode (mssg.getData (), new EncodingMetadata ("text/plain", "identity")));
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testCAS ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		final byte[] b1 = this.encoder.encode ("replaced by dummy", new EncodingMetadata ("text/plain", "identity"));
		final KeyValueMessage mssg1 = new KeyValueMessage (k1, b1, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("cas");
		final IResult<Boolean> r1 = this.wrapper.invokeCASOperation (MemcachedDriverTest.keyPrefix, mssg1, handler);
		try {
			Assert.assertTrue (r1.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
		final IOperationCompletionHandler<KeyValueMessage> handler1 = new TestLoggingHandler<KeyValueMessage> ("Get after cas");
		final IResult<KeyValueMessage> r2 = this.wrapper.invokeGetOperation (MemcachedDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler1);
		try {
			final KeyValueMessage mssg = r2.getResult ();
			Assert.assertEquals ("replaced by dummy", this.encoder.decode (mssg.getData (), new EncodingMetadata ("text/plain", "identity")));
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testConnection ()
	{
		Assert.assertNotNull (this.wrapper);
	}
	
	public void testDelete ()
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("delete");
		final IResult<Boolean> r1 = this.wrapper.invokeDeleteOperation (MemcachedDriverTest.keyPrefix, k1, handler);
		try {
			Assert.assertTrue (r1.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
		final IOperationCompletionHandler<KeyValueMessage> handler1 = new TestLoggingHandler<KeyValueMessage> ("Get after delete");
		final IResult<KeyValueMessage> r2 = this.wrapper.invokeGetOperation (MemcachedDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler1);
		try {
			Assert.assertNull (r2.getResult ().getData ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	@Test
	public void testDriver ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		this.testConnection ();
		this.testSet ();
		this.testGet ();
		this.testGetBulk ();
		this.testAdd ();
		this.testReplace ();
		this.testList ();
		this.testDelete ();
	}
	
	public void testGet ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		final IOperationCompletionHandler<KeyValueMessage> handler = new TestLoggingHandler<KeyValueMessage> ("get");
		final IResult<KeyValueMessage> r1 = this.wrapper.invokeGetOperation (MemcachedDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler);
		try {
			final KeyValueMessage mssg = r1.getResult ();
			Assert.assertEquals ("fantastic", this.encoder.decode (mssg.getData (), new EncodingMetadata ("text/plain", "identity")));
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testGetBulk ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		final String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
		final List<String> keys = new ArrayList<String> ();
		keys.add (k1);
		keys.add (k2);
		final IOperationCompletionHandler<Map<String, KeyValueMessage>> handler = new TestLoggingHandler<Map<String, KeyValueMessage>> ("getBulk");
		final IResult<Map<String, KeyValueMessage>> r1 = this.wrapper.invokeGetBulkOperation (MemcachedDriverTest.keyPrefix, keys, new EncodingMetadata ("text/plain", "identity"), handler);
		try {
			KeyValueMessage mssg = r1.getResult ().get (k1);
			Assert.assertEquals ("fantastic", this.encoder.decode (mssg.getData (), new EncodingMetadata ("text/plain", "identity")));
			mssg = r1.getResult ().get (k2);
			Assert.assertEquals ("famous", this.encoder.decode (mssg.getData (), new EncodingMetadata ("text/plain", "identity")));
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testList ()
	{
		final IOperationCompletionHandler<List<String>> handler = new TestLoggingHandler<List<String>> ("list");
		final IResult<List<String>> r1 = this.wrapper.invokeListOperation (MemcachedDriverTest.keyPrefix, handler);
		try {
			Assert.assertNull (r1.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testPrepend ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		final byte[] b1 = this.encoder.encode ("it is ", new EncodingMetadata ("text/plain", "identity"));
		final KeyValueMessage mssg1 = new KeyValueMessage (k1, b1, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("prepend");
		final IResult<Boolean> r1 = this.wrapper.invokePrependOperation (MemcachedDriverTest.keyPrefix, mssg1, handler);
		try {
			Assert.assertTrue (r1.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
		final IOperationCompletionHandler<KeyValueMessage> handler1 = new TestLoggingHandler<KeyValueMessage> ("Get after prepend");
		final IResult<KeyValueMessage> r2 = this.wrapper.invokeGetOperation (MemcachedDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler1);
		try {
			final KeyValueMessage mssg = r2.getResult ();
			Assert.assertEquals ("it is fantabulous and miraculous", this.encoder.decode (mssg.getData (), new EncodingMetadata ("text/plain", "identity")));
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testReplace ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		final byte[] b1 = this.encoder.encode ("fantabulous", new EncodingMetadata ("text/plain", "identity"));
		final KeyValueMessage mssg1 = new KeyValueMessage (k1, b1, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("replace");
		final IResult<Boolean> r1 = this.wrapper.invokeReplaceOperation (MemcachedDriverTest.keyPrefix, mssg1, 30, handler);
		try {
			Assert.assertTrue (r1.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
		final IOperationCompletionHandler<KeyValueMessage> handler1 = new TestLoggingHandler<KeyValueMessage> ("Get after replace");
		final IResult<KeyValueMessage> r2 = this.wrapper.invokeGetOperation (MemcachedDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler1);
		try {
			final KeyValueMessage mssg = r2.getResult ();
			Assert.assertEquals ("fantabulous", this.encoder.decode (mssg.getData (), new EncodingMetadata ("text/plain", "identity")));
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	public void testSet ()
			throws IOException,
				EncodingException
	{
		final String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		final byte[] bytes1 = this.encoder.encode ("fantastic", new EncodingMetadata ("text/plain", "identity"));
		KeyValueMessage mssg = new KeyValueMessage (k1, bytes1, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean> ("set 1");
		final IResult<Boolean> r1 = this.wrapper.invokeSetOperation (MemcachedDriverTest.keyPrefix, mssg, 30, handler1);
		Assert.assertNotNull (r1);
		final String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
		final byte[] bytes2 = this.encoder.encode ("famous", new EncodingMetadata ("text/plain", "identity"));
		mssg = new KeyValueMessage (k2, bytes2, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean> ("set 2");
		final IResult<Boolean> r2 = this.wrapper.invokeSetOperation (MemcachedDriverTest.keyPrefix, mssg, 30, handler2);
		Assert.assertNotNull (r2);
		try {
			Assert.assertTrue (r1.getResult ());
			Assert.assertTrue (r2.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
	{
		MemcachedDriverTest.keyPrefix = UUID.randomUUID ().toString ();
	}
	
	private DataEncoder<String> encoder;
	private BaseExceptionTracer exceptions;
	private BasicThreadingContext threadingContext;
	private MemcachedDriver wrapper;
	private static String keyPrefix;
	private static final String MOSAIC_MEMCACHED_HOST = "mosaic.tests.resources.memcached.host";
	private static final String MOSAIC_MEMCACHED_HOST_DEFAULT = "127.0.0.1";
	private static final String MOSAIC_MEMCACHED_PORT = "mosaic.tests.resources.memcached.port";
	private static final String MOSAIC_MEMCACHED_PORT_DEFAULT = "8091";
}
