/*
 * #%L
 * mosaic-drivers-core
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.drivers.interop;


/**
 * Generic class holding connection information about a resource driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class DriverConnectionData
{
	/**
	 * Creates a new data class
	 * 
	 * @param host
	 *            the hostname or ip address of the machine running the resource
	 * @param port
	 *            the port on which the resource is listening
	 * @param driverName
	 *            driver name
	 */
	public DriverConnectionData (final String host, final int port, final String driverName)
	{
		super ();
		this.host = host;
		this.port = port;
		this.driverName = driverName;
	}
	
	/**
	 * Creates a new data class
	 * 
	 * @param host
	 *            the hostname or ip address of the machine running the resource
	 * @param port
	 *            the port on which the resource is listening
	 * @param driverName
	 *            driver name
	 * @param user
	 *            username for connecting to resource
	 * @param password
	 *            password for connecting to resource
	 */
	public DriverConnectionData (final String host, final int port, final String driverName, final String user, final String password)
	{
		this (host, port, driverName);
		this.user = user;
		this.password = password;
	}
	
	@Override
	public boolean equals (final Object obj)
	{
		boolean isEqual;
		isEqual = (this == obj);
		if (!isEqual && (obj instanceof DriverConnectionData)) {
			final DriverConnectionData other = (DriverConnectionData) obj;
			isEqual =
					(obj == null) || (this.getClass () != obj.getClass ()) || ((this.driverName == null) && (other.driverName != null)) || ((this.driverName != null) && (!this.driverName.equals (other.driverName))) || ((this.host == null) && (other.host != null)) || ((this.host != null) && (!this.host.equals (other.host))) || ((this.password == null) && (other.password != null)) || ((this.password != null) && (other.password == null))
							|| ((this.password != null) && ((other.password != null) && !this.password.equals (other.password))) || ((this.user == null) && (other.user != null)) || ((this.user != null) && (!this.user.equals (other.user)));
			isEqual ^= true;
		}
		return isEqual;
	}
	
	public String getDriverName ()
	{
		return this.driverName;
	}
	
	public String getHost ()
	{
		return this.host;
	}
	
	public String getPassword ()
	{
		return this.password;
	}
	
	public int getPort ()
	{
		return this.port;
	}
	
	public String getUser ()
	{
		return this.user;
	}
	
	@Override
	public int hashCode ()
	{ // NOPMD by georgiana on 10/12/11 3:11 PM
		final int prime = 31; // NOPMD by georgiana on 10/12/11 3:09 PM
		int result = 1; // NOPMD by georgiana on 10/12/11 3:11 PM
		result = (prime * result) + ((this.driverName == null) ? 0 : this.driverName.hashCode ());
		result = (prime * result) + ((this.host == null) ? 0 : this.host.hashCode ());
		result = (prime * result) + ((this.password == null) ? 0 : this.password.hashCode ());
		result = (prime * result) + this.port;
		result = (prime * result) + ((this.user == null) ? 0 : this.user.hashCode ());
		return result;
	}
	
	private final String driverName;
	private final String host;
	private String password = "";
	private final int port;
	private String user = "";
}
