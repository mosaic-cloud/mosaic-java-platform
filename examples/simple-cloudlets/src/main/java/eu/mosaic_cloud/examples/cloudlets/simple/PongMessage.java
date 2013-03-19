/*
 * #%L
 * mosaic-examples-simple-cloudlets
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

package eu.mosaic_cloud.examples.cloudlets.simple;


public class PongMessage
{
	public PongMessage ()
	{}
	
	public PongMessage (final String key, final PingPongData value)
	{
		this.key = key;
		this.value = value;
	}
	
	public String getKey ()
	{
		return this.key;
	}
	
	public PingPongData getValue ()
	{
		return this.value;
	}
	
	public void setKey (final String key)
	{
		this.key = key;
	}
	
	public void setValue (final PingPongData value)
	{
		this.value = value;
	}
	
	private String key;
	private PingPongData value;
}
