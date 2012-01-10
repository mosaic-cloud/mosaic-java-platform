/*
 * #%L
 * mosaic-tools-callbacks
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

package eu.mosaic_cloud.callbacks.core;


public interface CallbackReactor
{
	public abstract <_Callbacks_ extends Callbacks> CallbackReference assign (final _Callbacks_ trigger, final _Callbacks_ delegate);
	
	public abstract <_Callbacks_ extends Callbacks> _Callbacks_ register (final Class<_Callbacks_> specification, final _Callbacks_ delegate);
	
	public abstract CallbackFuture resolve (final CallbackReference reference);
	
	public abstract void terminate ();
	
	public abstract <_Callbacks_ extends Callbacks> CallbackReference unregister (final _Callbacks_ trigger);
}
