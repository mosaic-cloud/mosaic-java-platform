/*
 * #%L
 * mosaic-components-core
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
