/*
 * #%L
 * mosaic-driver
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package eu.mosaic_cloud.driver.queue.amqp;

import eu.mosaic_cloud.core.ops.IOperationType;

/**
 * Operations defined for the AMQP protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum AmqpOperations implements IOperationType {
	DECLARE_EXCHANGE, DECLARE_QUEUE, BIND_QUEUE, CONSUME, PUBLISH, GET, ACK, CANCEL;
	private static final AmqpOperations[] COPY_OF_VALUES = values();

	/**
	 * Tests if given operation is supported by driver.
	 * 
	 * @param operation
	 *            name of operation
	 * @return <code>true</code> if operation is supported
	 */
	public static boolean isOperation(String operation) {
		for (AmqpOperations op : AmqpOperations.COPY_OF_VALUES) {
			if (op.name().equalsIgnoreCase(operation)) {
				return true; // NOPMD by georgiana on 10/12/11 3:35 PM
			}
		}
		return false;
	}
}
