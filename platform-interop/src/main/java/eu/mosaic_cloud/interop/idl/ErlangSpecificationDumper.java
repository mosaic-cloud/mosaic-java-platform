
package eu.mosaic_cloud.interop.idl;

import eu.mosaic_cloud.interop.amqp.AmqpSession;
import eu.mosaic_cloud.interop.kvstore.KeyValueSession;


public class ErlangSpecificationDumper
{
	public static final void main (final String[] arguments)
	{
		eu.mosaic_cloud.interoperability.tools.ErlangSpecificationDumper.main (arguments, AmqpSession.values ());
		eu.mosaic_cloud.interoperability.tools.ErlangSpecificationDumper.main (arguments, KeyValueSession.values ());
	}
}
