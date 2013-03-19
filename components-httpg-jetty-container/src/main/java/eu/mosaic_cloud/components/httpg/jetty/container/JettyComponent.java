/*
 * #%L
 * mosaic-components-httpg-jetty-container
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

package eu.mosaic_cloud.components.httpg.jetty.container;


import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public final class JettyComponent
		extends Object
{
	private JettyComponent ()
	{
		super ();
	}
	
	public final Future<ComponentCallReply> call (final ComponentIdentifier component, final ComponentCallRequest request)
	{
		final JettyComponentCallbacks callbacks = JettyComponentContext.callbacks;
		Preconditions.checkState (callbacks != null);
		return (callbacks.call (component, request));
	}
	
	public final Future<ComponentCallReply> call (final String component, final String operation, final Object inputs)
	{
		return (this.call (ComponentIdentifier.resolve (component), ComponentCallRequest.create (operation, inputs, ComponentCallReference.create ())));
	}
	
	public final void cast (final ComponentIdentifier component, final ComponentCastRequest request)
	{
		final JettyComponentCallbacks callbacks = JettyComponentContext.callbacks;
		Preconditions.checkState (callbacks != null);
		callbacks.cast (component, request);
	}
	
	public final void cast (final String component, final String operation, final Object inputs)
	{
		this.cast (ComponentIdentifier.resolve (component), ComponentCastRequest.create (operation, inputs));
	}
	
	public final ComponentIdentifier getGroup ()
	{
		final ComponentIdentifier group = JettyComponentContext.selfGroup;
		Preconditions.checkState (group != null);
		return (group);
	}
	
	public final ComponentIdentifier getIdentifier ()
	{
		final ComponentIdentifier identifier = JettyComponentContext.selfIdentifier;
		Preconditions.checkState (identifier != null);
		return (identifier);
	}
	
	public final boolean isStandalone ()
	{
		return (JettyComponentContext.callbacks != null);
	}
	
	public final void terminate ()
	{
		final JettyComponentCallbacks callbacks = JettyComponentContext.callbacks;
		Preconditions.checkState (callbacks != null);
		callbacks.terminate ();
	}
	
	public static final JettyComponent create ()
	{
		final JettyComponent component;
		synchronized (JettyComponent.currentComponent) {
			if (JettyComponent.currentComponent.get () == null) {
				component = new JettyComponent ();
				if (!JettyComponent.currentComponent.compareAndSet (null, component))
					throw (new AssertionError ());
			} else
				throw (new IllegalStateException ());
		}
		return (component);
	}
	
	public static final JettyComponent get ()
	{
		final JettyComponent component = JettyComponent.currentComponent.get ();
		Preconditions.checkState (component != null);
		return (component);
	}
	
	private static final AtomicReference<JettyComponent> currentComponent = Atomics.newReference (null);
}
