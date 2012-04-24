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

package eu.mosaic_cloud.drivers.kvstore;


import eu.mosaic_cloud.platform.core.ops.IOperationType;


/**
 * Basic operations supported by a key-value stores and memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum KeyValueOperations
		implements
			IOperationType
{
	ADD,
	APPEND,
	CAS,
	DELETE,
	GET,
	GET_BULK,
	LIST,
	PREPEND,
	REPLACE,
	SET;
}
