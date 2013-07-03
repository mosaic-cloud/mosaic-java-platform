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


import java.nio.ByteBuffer;
import java.util.Map;

import com.google.common.base.Preconditions;


public final class ChannelMessage
			extends Object
{
	private ChannelMessage (final ChannelMessageType type, final Map<String, Object> metaData, final ByteBuffer data) {
		super ();
		Preconditions.checkNotNull (type);
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		this.type = type;
		this.metaData = metaData;
		this.data = data;
	}
	
	public final ByteBuffer data;
	public final Map<String, Object> metaData;
	public final ChannelMessageType type;
	
	public static final ChannelMessage create (final ChannelMessageType type, final Map<String, Object> metaData, final ByteBuffer data) {
		return (new ChannelMessage (type, metaData, data));
	}
}
