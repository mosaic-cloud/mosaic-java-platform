
package eu.mosaic_cloud.components.core;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;


public final class ComponentTcpSocketResourceDescriptor
		extends ComponentResourceDescriptor
{
	private ComponentTcpSocketResourceDescriptor (final String identifier, final InetSocketAddress address)
	{
		super (identifier);
		Preconditions.checkNotNull (address);
		this.address = address;
	}
	
	public static final ComponentTcpSocketResourceDescriptor create (final String identifier, final InetSocketAddress address)
	{
		return (new ComponentTcpSocketResourceDescriptor (identifier, address));
	}
	
	public static final ComponentTcpSocketResourceDescriptor create (final String identifier, final String ip, final int port, final String fqdn)
	{
		Preconditions.checkNotNull (ip);
		Preconditions.checkArgument ((port > 0) && (port < 65536));
		final InetAddress addressIp;
		try {
			addressIp = InetAddress.getByAddress (fqdn, InetAddresses.forString (ip).getAddress ());
		} catch (final UnknownHostException exception) {
			// NOTE: It is highly unlikely that this happens as the bytes are already verified and protected by an `IllegalArgumentException`...
			throw (new IllegalArgumentException (exception));
		}
		final InetSocketAddress address = new InetSocketAddress (addressIp, port);
		return (ComponentTcpSocketResourceDescriptor.create (identifier, address));
	}
	
	public final InetSocketAddress address;
}
