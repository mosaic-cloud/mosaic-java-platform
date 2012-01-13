/*
 * #%L
 * mosaic-drivers
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
package eu.mosaic_cloud.driver.interop.kvstore;

import eu.mosaic_cloud.driver.interop.DriverConnectionData;

/**
 * Generic class holding connection information about a key-value resource
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KVDriverConnectionData extends DriverConnectionData {

	private final String bucket;

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
	public KVDriverConnectionData(String host, int port, String driverName,
			String bucket) {
		super(host, port, driverName);
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
	public KVDriverConnectionData(String host, int port, String driverName,
			String user, String password, String bucket) {
		super(host, port, driverName, user, password);
		this.bucket = bucket;
	}

	public String getBucket() {
		return this.bucket;
	}

	@Override
	public int hashCode() {
		final int prime = 31; // NOPMD by georgiana on 10/12/11 2:19 PM
		int result = super.hashCode(); // NOPMD by georgiana on 10/12/11 2:19 PM
		result = (prime * result)
				+ ((this.bucket == null) ? 0 : this.bucket.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		boolean isEqual = (this == obj);
		if (!isEqual) {
			isEqual = (!super.equals(obj)) || (getClass() != obj.getClass());
			if (!isEqual) {
				KVDriverConnectionData other = (KVDriverConnectionData) obj;
				isEqual = ((this.bucket == null) && (other.bucket != null))
						|| (this.bucket != null && (!this.bucket
								.equals(other.bucket)));
			}
			isEqual ^= true;
		}
		return isEqual;
	}
}
