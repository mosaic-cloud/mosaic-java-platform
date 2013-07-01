/*
 * #%L
 * mosaic-drivers-stubs-riak
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.drivers.kvstore.RiakDriver;
import eu.mosaic_cloud.drivers.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.drivers.ops.IResult;
import eu.mosaic_cloud.drivers.ops.tests.TestLoggingHandler;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
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
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public abstract class RiakDriverTest
{
	public RiakDriverTest (final String portDefault)
	{
		final Transcript transcript = Transcript.create (this);
		final QueueingExceptionTracer exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final TranscriptExceptionTracer exceptions = TranscriptExceptionTracer.create (transcript, exceptionsQueue);
		this.exceptions = exceptions;
		BasicThreadingSecurityManager.initialize ();
		this.threadingContext = BasicThreadingContext.create (this, exceptions, exceptions.catcher);
		this.threadingContext.initialize ();
		final String host = System.getProperty (RiakDriverTest.MOSAIC_RIAK_HOST, RiakDriverTest.MOSAIC_RIAK_HOST_DEFAULT);
		final Integer port = Integer.valueOf (System.getProperty (RiakDriverTest.MOSAIC_RIAK_PORT, portDefault));
		this.configuration = PropertyTypeConfiguration.create ();
		this.configuration.addParameter ("kvstore.host", host);
		this.configuration.addParameter ("kvstore.port", port);
		this.configuration.addParameter ("kvstore.driver_threads", 1);
		this.configuration.addParameter ("kvstore.bucket", "tests");
	}
	
	@Before
	public void setUp ()
			throws Exception
	{
		this.wrapper = RiakDriver.create (this.configuration, this.threadingContext);
		this.wrapper.registerClient (RiakDriverTest.keyPrefix, "test");
		this.encoder = PlainTextDataEncoder.DEFAULT_INSTANCE;
	}
	
	@After
	public void tearDown ()
	{
		this.wrapper.unregisterClient (RiakDriverTest.keyPrefix);
		this.wrapper.destroy ();
		this.threadingContext.destroy ();
	}
	
	public void testConnection ()
	{
		Assert.assertNotNull (this.wrapper);
	}
	
	public void testDelete ()
	{
		final String k1 = RiakDriverTest.keyPrefix + "_key_fantastic";
		final IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean> ("delete 1");
		final IResult<Boolean> r1 = this.wrapper.invokeDeleteOperation (RiakDriverTest.keyPrefix, k1, handler1);
		try {
			Assert.assertTrue (r1.getResult ());
		} catch (final InterruptedException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		} catch (final ExecutionException e) {
			this.exceptions.traceIgnoredException (e);
			Assert.fail ();
		}
		Threading.sleep (1000);
		final IOperationCompletionHandler<KeyValueMessage> handler3 = new TestLoggingHandler<KeyValueMessage> ("check deleted");
		final IResult<KeyValueMessage> r3 = this.wrapper.invokeGetOperation (RiakDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler3);
		try {
			Assert.assertNull (r3.getResult ().getData ());
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
		this.testDriverName ();
		this.testConnection ();
		this.testSet ();
		this.testGet ();
		// FIXME there is some conflict between json jars so list won't work with REST driver
		//        this.testList();
		this.testDelete ();
	}
	
	public void testDriverName ()
	{
		final String driverName = ConfigUtils.resolveParameter (this.configuration, "kvstore.driver_name", String.class, "");
		Assert.assertFalse (driverName.isEmpty ());
	}
	
	public void testGet ()
			throws IOException,
				ClassNotFoundException,
				EncodingException
	{
		final String k1 = RiakDriverTest.keyPrefix + "_key_famous";
		final IOperationCompletionHandler<KeyValueMessage> handler = new TestLoggingHandler<KeyValueMessage> ("get");
		final IResult<KeyValueMessage> r1 = this.wrapper.invokeGetOperation (RiakDriverTest.keyPrefix, k1, new EncodingMetadata ("text/plain", "identity"), handler);
		try {
			final KeyValueMessage mssg = r1.getResult ();
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
		final String k1 = RiakDriverTest.keyPrefix + "_key_fantastic";
		final String k2 = RiakDriverTest.keyPrefix + "_key_famous";
		final IOperationCompletionHandler<List<String>> handler = new TestLoggingHandler<List<String>> ("list");
		final IResult<List<String>> r1 = this.wrapper.invokeListOperation (RiakDriverTest.keyPrefix, handler);
		try {
			final List<String> lresult = r1.getResult ();
			Assert.assertNotNull (lresult);
			Assert.assertTrue (lresult.contains (k1));
			Assert.assertTrue (lresult.contains (k2));
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
		final String k1 = RiakDriverTest.keyPrefix + "_key_fantastic";
		final byte[] b1 = this.encoder.encode ("fantastic", new EncodingMetadata ("text/plain", "identity")).data;
		KeyValueMessage mssg = new KeyValueMessage (k1, b1, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean> ("set 1");
		final IResult<Boolean> r1 = this.wrapper.invokeSetOperation (RiakDriverTest.keyPrefix, mssg, handler1);
		Assert.assertNotNull (r1);
		final String k2 = RiakDriverTest.keyPrefix + "_key_famous";
		final byte[] b2 = this.encoder.encode ("famous", new EncodingMetadata ("text/plain", "identity")).data;
		mssg = new KeyValueMessage (k2, b2, "identity", "text/plain");
		final IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean> ("set 2");
		final IResult<Boolean> r2 = this.wrapper.invokeSetOperation (RiakDriverTest.keyPrefix, mssg, handler2);
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
		RiakDriverTest.keyPrefix = UUID.randomUUID ().toString ();
	}
	
	protected final IConfiguration configuration;
	private DataEncoder<String> encoder;
	private final BaseExceptionTracer exceptions;
	private final BasicThreadingContext threadingContext;
	private RiakDriver wrapper;
	public static final String MOSAIC_RIAK_PORT_PB_DEFAULT = "22652";
	public static final String MOSAIC_RIAK_PORT_REST_DEFAULT = "24637";
	private static String keyPrefix;
	private static final String MOSAIC_RIAK_HOST = "mosaic.tests.resources.riak.host";
	private static final String MOSAIC_RIAK_HOST_DEFAULT = "127.0.0.1";
	private static final String MOSAIC_RIAK_PORT = "mosaic.tests.resources.riak.port";
}
