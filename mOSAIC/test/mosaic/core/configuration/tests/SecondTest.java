package mosaic.core.configuration.tests;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SecondTest {
	private static PropertyTypeConfiguration configuration;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		configuration = PropertyTypeConfiguration.create(
				SecondTest.class.getClassLoader(), "amqp-test.prop");
	}

	@Test
	public void testProps() {
		ConfigurationIdentifier id0 = ConfigurationIdentifier
				.resolveAbsolute("interop/req");
		IConfiguration interopReqConfig = configuration
				.spliceConfiguration(id0);
		String exchange = ConfigUtils.resolveParameter(interopReqConfig,
				"amqp.exchange", String.class, "");
		String exchangeAbs = ConfigUtils.resolveParameter(interopReqConfig,
				"/interop.req.amqp.exchange", String.class, "");

		Assert.assertEquals("amqp", exchange);
	}

}
