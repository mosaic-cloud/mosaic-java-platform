
package eu.mosaic_cloud.tools.configurations.implementations.basic.tests;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource.Resolution;
import eu.mosaic_cloud.tools.configurations.implementations.basic.PropertiesBackedConfigurationSource;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;


public final class PropertiesBackedConfigurationSourceTests
{
	@Test
	public final void testParsing () {
		final PropertiesBackedConfigurationSource source = PropertiesBackedConfigurationSourceTests.populateSource ();
		Assert.assertEquals ("string", source.resolve (PropertiesBackedConfigurationSourceTests.a1.resolve ("string"), String.class).value);
		Assert.assertEquals (Integer.valueOf (1), source.resolve (PropertiesBackedConfigurationSourceTests.a1.resolve ("integer"), Integer.class).value);
		Assert.assertEquals (Long.valueOf (1), source.resolve (PropertiesBackedConfigurationSourceTests.a1.resolve ("long"), Long.class).value);
		Assert.assertEquals (Float.valueOf (1), source.resolve (PropertiesBackedConfigurationSourceTests.a1.resolve ("float"), Float.class).value);
		Assert.assertEquals (Double.valueOf (1), source.resolve (PropertiesBackedConfigurationSourceTests.a1.resolve ("double"), Double.class).value);
		Assert.assertEquals (Boolean.TRUE, source.resolve (PropertiesBackedConfigurationSourceTests.a1.resolve ("boolean"), Boolean.class).value);
	}
	
	@Test
	public final void testResolution () {
		final PropertiesBackedConfigurationSource source = PropertiesBackedConfigurationSourceTests.populateSource ();
		Assert.assertSame (source.resolve (PropertiesBackedConfigurationSourceTests.a2, String.class).resolution, Resolution.Unknown);
		Assert.assertSame (source.resolve (PropertiesBackedConfigurationSourceTests.a1.resolve ("string"), String.class).resolution, Resolution.Resolved);
	}
	
	public static final PropertiesBackedConfigurationSource populateSource () {
		final ImmutableMap.Builder<String, String> records = ImmutableMap.builder ();
		records.put ("/a1/string", "string");
		records.put ("/a1/integer", "1");
		records.put ("/a1/long", "1");
		records.put ("/a1/float", "1.0");
		records.put ("/a1/double", "1.0");
		records.put ("/a1/boolean", "true");
		return (PropertiesBackedConfigurationSource.clone (records.build ()));
	}
	
	protected static final ConfigurationIdentifier a1 = ConfigurationIdentifier.resolveAbsolute ("/a1");
	protected static final ConfigurationIdentifier a2 = ConfigurationIdentifier.resolveAbsolute ("/a2");
}
