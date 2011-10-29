
package mosaic.interop.idl;

import mosaic.interop.kvstore.KeyValueSession;

import mosaic.interop.amqp.AmqpSession;

public class ErlangSpecificationDumper
{
	public static final void main (final String[] arguments)
	{
		eu.mosaic_cloud.interoperability.tools.ErlangSpecificationDumper.main (arguments, AmqpSession.values ());
		eu.mosaic_cloud.interoperability.tools.ErlangSpecificationDumper.main (arguments, KeyValueSession.values ());
	}
}
