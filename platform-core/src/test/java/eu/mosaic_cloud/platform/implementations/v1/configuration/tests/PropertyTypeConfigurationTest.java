/*
 * #%L
 * mosaic-platform-core
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

package eu.mosaic_cloud.platform.implementations.v1.configuration.tests;


import java.io.FileInputStream;

import eu.mosaic_cloud.platform.implementations.v1.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.implementations.v1.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.v1.core.configuration.IConfiguration;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class PropertyTypeConfigurationTest
{
	@Test
	public void testConfigurationIdentifierResolve ()
	{
		final ConfigurationIdentifier id0 = ConfigurationIdentifier.resolveAbsolute ("mosaic");
		Assert.assertNotNull (id0);
		Assert.assertEquals ("Absolute for root resolve absolute failed", "/mosaic", id0.getIdentifier ());
		final ConfigurationIdentifier id1 = ConfigurationIdentifier.resolveAbsolute ("mosaic/int");
		Assert.assertNotNull (id1);
		Assert.assertEquals ("Absolute for non-root resolve absolute failed", "/mosaic/int", id1.getIdentifier ());
		final ConfigurationIdentifier id2 = ConfigurationIdentifier.resolveRelative ("boolean");
		Assert.assertNotNull (id2);
		Assert.assertEquals ("Relative for non-root resolve relative failed", "boolean", id2.getIdentifier ());
		final ConfigurationIdentifier id3 = ConfigurationIdentifier.resolveRelative (id0, "boolean");
		Assert.assertNotNull (id3);
		Assert.assertEquals ("Absolute for non-root resolve relative with absolute path failed", "/mosaic/boolean", id3.getIdentifier ());
		final ConfigurationIdentifier id4 = id0.resolve (id2);
		Assert.assertNotNull (id4);
		Assert.assertEquals ("Absolute for instance resolve failed", "/mosaic/boolean", id4.getIdentifier ());
		final ConfigurationIdentifier id5 = id0.resolve ("real");
		Assert.assertNotNull (id5);
		Assert.assertEquals ("Absolute for instance resolve failed", "/mosaic/real", id5.getIdentifier ());
	}
	
	@Test
	public void testEquals ()
	{
		Assert.assertTrue (PropertyTypeConfigurationTest.configuration.equals (PropertyTypeConfigurationTest.fileConfiguration));
	}
	
	@Test
	public void testGetParameter ()
	{
		ConfigurationIdentifier id;
		id = ConfigurationIdentifier.resolveAbsolute ("mosaic/int");
		final Integer value = PropertyTypeConfigurationTest.configuration.getParameter (id, Integer.class).getValue (0);
		Assert.assertEquals (1, value.intValue ());
		id = ConfigurationIdentifier.resolveAbsolute ("mosaic/real");
		final Double dvalue = PropertyTypeConfigurationTest.configuration.getParameter (id, Double.class).getValue (0.0);
		Assert.assertEquals (2.0, dvalue.doubleValue (), 0.0);
		id = ConfigurationIdentifier.resolveAbsolute ("mosaic/boolean");
		final Boolean bvalue = PropertyTypeConfigurationTest.configuration.getParameter (id, Boolean.class).getValue (false);
		Assert.assertEquals (true, bvalue.booleanValue ());
		id = ConfigurationIdentifier.resolveAbsolute ("mosaic/string");
		final String svalue = PropertyTypeConfigurationTest.configuration.getParameter (id, String.class).getValue ("");
		Assert.assertEquals ("abac", svalue);
	}
	
	@Test
	public void testPropertyTypeConfiguration ()
	{
		Assert.assertNotNull (PropertyTypeConfigurationTest.configuration);
		Assert.assertNotNull (PropertyTypeConfigurationTest.fileConfiguration);
		Assert.assertNotNull (PropertyTypeConfigurationTest.systemConfiguration);
	}
	
	@Test
	public void testSpliceConfiguration ()
	{
		final IConfiguration mConfiguration = PropertyTypeConfigurationTest.configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("mosaic"));
		final int intValue = ConfigUtils.resolveParameter (mConfiguration, "int", Integer.class, 0);
		Assert.assertEquals (1, intValue);
		final String strValue = ConfigUtils.resolveParameter (mConfiguration, "string", String.class, "");
		Assert.assertEquals ("abac", strValue);
		final boolean boolValue = ConfigUtils.resolveParameter (mConfiguration, "boolean", Boolean.class, false);
		Assert.assertTrue (boolValue);
		final double realValue = ConfigUtils.resolveParameter (mConfiguration, "real", Double.class, 0.0);
		Assert.assertEquals (2.0, realValue, 0.0);
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
			throws Exception
	{
		PropertyTypeConfigurationTest.configuration = PropertyTypeConfiguration.create (PropertyTypeConfigurationTest.class.getClassLoader (), "configuration-test.properties");
		PropertyTypeConfigurationTest.fileConfiguration = PropertyTypeConfiguration.create (new FileInputStream ("src/test/resources/configuration-test.properties"));
		PropertyTypeConfigurationTest.systemConfiguration = PropertyTypeConfiguration.create ();
	}
	
	private static PropertyTypeConfiguration configuration;
	private static PropertyTypeConfiguration fileConfiguration;
	private static PropertyTypeConfiguration systemConfiguration;
}
