/*
 * #%L
 * mosaic-drivers-stubs-kv-common
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

package eu.mosaic_cloud.drivers.kvstore.interop;


import eu.mosaic_cloud.drivers.interop.DriverConnectionData;


/**
 * Generic class holding connection information about a key-value resource
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KvDriverConnectionData
		extends DriverConnectionData
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
	 * @param bucket
	 *            bucket name
	 */
	public KvDriverConnectionData (final String host, final int port, final String driverName, final String bucket)
	{
		super (host, port, driverName);
		this.bucket = bucket;
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
	 * @param bucket
	 *            bucket name
	 */
	public KvDriverConnectionData (final String host, final int port, final String driverName, final String user, final String password, final String bucket)
	{
		super (host, port, driverName, user, password);
		this.bucket = bucket;
	}
	
	@Override
	public boolean equals (final Object obj)
	{
		boolean isEqual = (this == obj);
		if (!isEqual) {
			isEqual = (!super.equals (obj)) || (this.getClass () != obj.getClass ());
			if (!isEqual) {
				final KvDriverConnectionData other = (KvDriverConnectionData) obj;
				isEqual = ((this.bucket == null) && (other.bucket != null)) || ((this.bucket != null) && (!this.bucket.equals (other.bucket)));
			}
			isEqual ^= true;
		}
		return isEqual;
	}
	
	public String getBucket ()
	{
		return this.bucket;
	}
	
	@Override
	public int hashCode ()
	{
		final int prime = 31;
		int result = super.hashCode ();
		result = (prime * result) + ((this.bucket == null) ? 0 : this.bucket.hashCode ());
		return result;
	}
	
	private final String bucket;
}
