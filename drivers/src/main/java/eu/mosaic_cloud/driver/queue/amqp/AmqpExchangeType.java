/*
 * #%L
 * mosaic-drivers
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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

/**
 * Possible AMQP exchange types.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum AmqpExchangeType {
	/**
	 * With a direct exchange a message goes to the queues whose binding key
	 * exactly matches the routing key of the message.
	 */
	DIRECT("direct"),
	/**
	 * A fanout exchange broadcasts all the messages it receives to all the
	 * queues it knows.
	 */
	FANOUT("fanout"),
	/**
	 * With a topic exchange a message goes to the queues whose binding key
	 * matches the routing key of the message.
	 */
	TOPIC("topic");

	private String amqpName;

	AmqpExchangeType(String amqpName) {
		this.amqpName = amqpName;
	}

	/**
	 * Returns the AMQP name of the exchange type.
	 * 
	 * @return
	 */
	public String getAmqpName() {
		return this.amqpName;
	}
}
