/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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

package eu.mosaic_cloud.connector.kvstore.tests;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import com.google.common.base.Preconditions;

import eu.mosaic_cloud.connector.kvstore.KeyValueStoreConnector;
import eu.mosaic_cloud.core.Serial;
import eu.mosaic_cloud.core.SerialJunitRunner;
import eu.mosaic_cloud.core.TestLoggingHandler;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.ops.IResult;
import eu.mosaic_cloud.core.utils.PojoDataEncoder;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith (SerialJunitRunner.class)
@Serial
@Ignore
public class KeyValueConnectorOnlyTest
{
	public void testConnection ()
	{
		Assert.assertNotNull (KeyValueConnectorOnlyTest.connector);
	}
	
	@Test
	public void testConnector ()
			throws IOException,
				ClassNotFoundException
	{
		this.testConnection ();
		this.testSet ();
		this.testGet ();
		this.testDelete ();
	}
	
	public void testDelete ()
	{
		final String k1 = KeyValueConnectorOnlyTest.keyPrefix + "_key_fantastic";
		final List<IOperationCompletionHandler<Boolean>> handlers = KeyValueConnectorOnlyTest.getHandlers ("delete");
		final IResult<Boolean> r1 = KeyValueConnectorOnlyTest.connector.delete (k1, handlers, null);
		try {
			Assert.assertTrue (r1.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
		} catch (final Exception e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail ();
		}
		final List<IOperationCompletionHandler<String>> handlers1 = KeyValueConnectorOnlyTest.getHandlers ("get after delete");
		final IResult<String> r2 = KeyValueConnectorOnlyTest.connector.get (k1, handlers1, null);
		try {
			Assert.assertNull (r2.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
		} catch (final Exception e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail ();
		}
	}
	
	public void testGet ()
			throws IOException,
				ClassNotFoundException
	{
		final String k1 = KeyValueConnectorOnlyTest.keyPrefix + "_key_fantastic";
		final List<IOperationCompletionHandler<String>> handlers = KeyValueConnectorOnlyTest.getHandlers ("get");
		final IResult<String> r1 = KeyValueConnectorOnlyTest.connector.get (k1, handlers, null);
		try {
			Assert.assertEquals ("fantastic", r1.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS).toString ());
		} catch (final Exception e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail ();
		}
	}
	
	public void testSet ()
			throws IOException
	{
		final String k1 = KeyValueConnectorOnlyTest.keyPrefix + "_key_fantastic";
		final List<IOperationCompletionHandler<Boolean>> handlers1 = KeyValueConnectorOnlyTest.getHandlers ("set 1");
		final IResult<Boolean> r1 = KeyValueConnectorOnlyTest.connector.set (k1, "fantastic", handlers1, null);
		Assert.assertNotNull (r1);
		final String k2 = KeyValueConnectorOnlyTest.keyPrefix + "_key_famous";
		final List<IOperationCompletionHandler<Boolean>> handlers2 = KeyValueConnectorOnlyTest.getHandlers ("set 2");
		final IResult<Boolean> r2 = KeyValueConnectorOnlyTest.connector.set (k2, "famous", handlers2, null);
		Assert.assertNotNull (r2);
		try {
			Assert.assertTrue (r1.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
			Assert.assertTrue (r2.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
		} catch (final Exception e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail ();
		}
	}
	
	public static void main (final String[] arguments)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length == 0));
		KeyValueConnectorOnlyTest.setUpBeforeClass ();
		new KeyValueConnectorOnlyTest ().testConnector ();
		KeyValueConnectorOnlyTest.tearDownAfterClass ();
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
			throws Throwable
	{
		final IConfiguration config = PropertyTypeConfiguration.create (KeyValueConnectorOnlyTest.class.getClassLoader (), "kv-test.prop");
		KeyValueConnectorOnlyTest.connector = KeyValueStoreConnector.create (config, new PojoDataEncoder<String> (String.class));
		KeyValueConnectorOnlyTest.keyPrefix = UUID.randomUUID ().toString ();
	}
	
	@AfterClass
	public static void tearDownAfterClass ()
			throws Throwable
	{
		KeyValueConnectorOnlyTest.connector.destroy ();
	}
	
	private static <T> List<IOperationCompletionHandler<T>> getHandlers (final String testName)
	{
		final IOperationCompletionHandler<T> handler = new TestLoggingHandler<T> (testName);
		final List<IOperationCompletionHandler<T>> list = new ArrayList<IOperationCompletionHandler<T>> ();
		list.add (handler);
		return list;
	}
	
	private static KeyValueStoreConnector<String> connector;
	private static String keyPrefix;
	private static String storeType;
	private static final long timeout = 1000 * 1000;
}
