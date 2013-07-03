/*
 * #%L
 * mosaic-drivers-core
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

package eu.mosaic_cloud.drivers;


public final class ConfigProperties
{
	private ConfigProperties ()
	{}
	
	public static final String AmqpDriver_0 = "amqp.driver_threads";
	public static final String AmqpDriver_1 = "amqp.host";
	public static final String AmqpDriver_2 = "amqp.port";
	public static final String AmqpDriver_3 = "amqp.user";
	public static final String AmqpDriver_4 = "amqp.passwd";
	public static final String AmqpDriver_5 = "amqp.virtual_host";
	public static final String AmqpDriver_6 = "amqp.max_reconnection_tries";
	public static final String AmqpDriver_7 = "amqp.min_reconnection_time";
	
	public static final String KVStoreDriver_0 = "kvstore.host";
	public static final String KVStoreDriver_1 = "kvstore.port";
	public static final String KVStoreDriver_2 = "kvstore.driver_threads";
	public static final String KVStoreDriver_3 = "kvstore.bucket";
	public static final String KVStoreDriver_4 = "kvstore.passwd";
	public static final String KVStoreDriver_5 = "kvstore.user";
	public static final String KVStoreDriver_6 = "kvstore.driver_name";
	
	public static final String AmqpDriverComponentCallbacks_0 = "resource.group.identifier";
	public static final String AmqpDriverComponentCallbacks_1 = "self.group.identifier";
	public static final String AmqpDriverComponentCallbacks_2 = "mosaic-rabbitmq:get-broker-endpoint";
	public static final String AmqpDriverComponentCallbacks_3 = "interop.driver.endpoint";
	public static final String AmqpDriverComponentCallbacks_4 = "interop.driver.identity";
	public static final String AmqpDriverComponentCallbacks_5 = "mosaic-component:get.channel.data";
	
	public static final String KVDriverComponentCallbacks_0 = "resource.group.identifier";
	public static final String KVDriverComponentCallbacks_1 = "self.group.identifier";
	public static final String KVDriverComponentCallbacks_2 = "mosaic-riak-kv:get-store-pb-endpoint";
	public static final String KVDriverComponentCallbacks_3 = "interop.driver.endpoint";
	public static final String KVDriverComponentCallbacks_4 = "interop.driver.identity";
	public static final String KVDriverComponentCallbacks_5 = "mosaic-component:get.channel.data";
	public static final String KVDriverComponentCallbacks_6 = "mosaic-riak-kv:get-store-http-endpoint";
}
