
package eu.mosaic_cloud.tools.configurations.implementations.basic.tests;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource.Resolution;
import eu.mosaic_cloud.tools.configurations.implementations.basic.SplicedConfigurationSource;

import org.junit.Assert;
import org.junit.Test;


public final class SplicedConfigurationSourceTests
{
	@Test (expected = IllegalArgumentException.class)
	public final void testAbsoluteRejection () {
		final ConfigurationSource source = SplicedConfigurationSource.create (SplicedConfigurationSourceTests.populateSource (), SplicedConfigurationSourceTests.a1);
		Assert.assertSame (source.resolve (ConfigurationIdentifier.resolveAbsolute ("/a1/string"), String.class).resolution, Resolution.Resolved);
	}
	
	@Test
	public final void testResolution () {
		final ConfigurationSource source = SplicedConfigurationSource.create (SplicedConfigurationSourceTests.populateSource (), SplicedConfigurationSourceTests.a1);
		Assert.assertSame (source.resolve (ConfigurationIdentifier.resolveRelative ("string"), String.class).resolution, Resolution.Resolved);
		Assert.assertSame (source.resolve (ConfigurationIdentifier.resolveRelative ("unknown"), String.class).resolution, Resolution.Unknown);
	}
	
	public static final ConfigurationSource populateSource () {
		return (PropertiesBackedConfigurationSourceTests.populateSource ());
	}
	
	protected static final ConfigurationIdentifier a1 = ConfigurationIdentifier.resolveAbsolute ("/a1");
	protected static final ConfigurationIdentifier a2 = ConfigurationIdentifier.resolveAbsolute ("/a2");
}
