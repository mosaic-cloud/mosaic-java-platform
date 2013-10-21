
package eu.mosaic_cloud.tools.configurations.core.tests;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;

import org.junit.Assert;
import org.junit.Test;


public final class ConfigurationIdentifierTests
{
	@Test
	public final void testIdentity () {
		{
			final ConfigurationIdentifier i1 = ConfigurationIdentifier.resolveAbsolute ("/a");
			final ConfigurationIdentifier i2 = ConfigurationIdentifier.resolveAbsolute ("/a");
			Assert.assertSame (i1, i2);
		}
		{
			final ConfigurationIdentifier i1 = ConfigurationIdentifier.resolveAbsolute ("/a");
			final ConfigurationIdentifier i2 = ConfigurationIdentifier.resolveAbsolute ("/b");
			Assert.assertNotSame (i1, i2);
		}
		{
			final ConfigurationIdentifier i1 = ConfigurationIdentifier.resolveRelative ("a");
			final ConfigurationIdentifier i2 = ConfigurationIdentifier.resolveRelative ("a");
			Assert.assertSame (i1, i2);
		}
		{
			final ConfigurationIdentifier i1 = ConfigurationIdentifier.resolveRelative ("a");
			final ConfigurationIdentifier i2 = ConfigurationIdentifier.resolveRelative ("b");
			Assert.assertNotSame (i1, i2);
		}
		{
			final ConfigurationIdentifier i1 = ConfigurationIdentifier.resolveAbsolute ("/a");
			final ConfigurationIdentifier i2 = ConfigurationIdentifier.resolveRelative ("a");
			Assert.assertNotSame (i1, i2);
		}
	}
	
	@Test (expected = IllegalArgumentException.class)
	public final void testParsingAbsoluteFailure () {
		ConfigurationIdentifier.resolveAbsolute ("a");
	}
	
	@Test (expected = IllegalArgumentException.class)
	public final void testParsingRelativeFailure () {
		ConfigurationIdentifier.resolveRelative ("/a");
	}
	
	@Test
	public final void testResolution () {
		final ConfigurationIdentifier a = ConfigurationIdentifier.resolveAbsolute ("/a");
		final ConfigurationIdentifier r0 = ConfigurationIdentifier.resolveRelative ("r0");
		final ConfigurationIdentifier r1 = ConfigurationIdentifier.resolveRelative ("r1");
		final ConfigurationIdentifier r2 = ConfigurationIdentifier.resolveRelative ("r2");
		{
			final ConfigurationIdentifier o1 = a.resolve ("r1");
			final ConfigurationIdentifier o2 = ConfigurationIdentifier.resolveAbsolute ("/a/r1");
			Assert.assertSame (o1, o2);
		}
		{
			final ConfigurationIdentifier o1 = a.resolve ("r1").resolve ("r2");
			final ConfigurationIdentifier o2 = ConfigurationIdentifier.resolveAbsolute ("/a/r1/r2");
			Assert.assertSame (o1, o2);
		}
		{
			final ConfigurationIdentifier o1 = a.resolve (r1).resolve (r2);
			final ConfigurationIdentifier o2 = a.resolve (r1.resolve (r2));
			Assert.assertSame (o1, o2);
		}
		{
			final ConfigurationIdentifier o1 = r0.resolve ("r1");
			final ConfigurationIdentifier o2 = ConfigurationIdentifier.resolveRelative ("r0/r1");
			Assert.assertSame (o1, o2);
		}
		{
			final ConfigurationIdentifier o1 = r0.resolve ("r1").resolve ("r2");
			final ConfigurationIdentifier o2 = ConfigurationIdentifier.resolveRelative ("r0/r1/r2");
			Assert.assertSame (o1, o2);
		}
		{
			final ConfigurationIdentifier o1 = r0.resolve (r1).resolve (r2);
			final ConfigurationIdentifier o2 = r0.resolve (r1.resolve (r2));
			Assert.assertSame (o1, o2);
		}
	}
}
