/*
 * #%L
 * tools-json
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

package eu.mosaic_cloud.json.core;


public interface JsonMapper
{
	public abstract <_Object_ extends Object> _Object_ decode (final Object structure, final Class<_Object_> mappingClass)
			throws Throwable;
	
	public abstract <_Object_ extends Object> Object encode (final _Object_ object, final Class<_Object_> mappingClass)
			throws Throwable;
}