/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.tools.v1.callbacks;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.core.Callback;
import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;


public class DefaultCallback<TContext>
			extends Object
			implements
				Callback<TContext>
{
	protected DefaultCallback (final CloudletController<TContext> cloudlet) {
		super ();
		this.cloudlet = cloudlet;
		this.transcript = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
	}
	
	protected CallbackCompletion<Void> chain (final CallbackCompletion<?> ... dependents) {
		return (CallbackCompletion.createChained (dependents));
	}
	
	protected void enforceCloudlet (final CloudletController<?> cloudlet) {
		if ((this.cloudlet != null) && (this.cloudlet != cloudlet))
			throw (new IllegalArgumentException ());
	}
	
	protected CallbackCompletion<Void> handleUnhandledCallback (final Class<? extends Callback> category, final String callback, final TContext context, final CallbackArguments arguments, final boolean positive, final boolean couldDestroy) {
		final CloudletController<?> cloudlet = arguments.getCloudlet ();
		this.enforceCloudlet (cloudlet);
		this.traceCallback (category, callback, context, arguments, positive);
		if (positive)
			this.transcript.traceDebugging ("unhandled successfull callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, cloudlet, context);
		else
			this.transcript.traceWarning ("unhandled failure callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, cloudlet, context);
		if (!positive && couldDestroy)
			cloudlet.destroy ();
		return (Callback.SUCCESS);
	}
	
	protected void traceCallback (final Class<? extends Callback> category, final String callback, final TContext context, final CallbackArguments arguments, final boolean positive) {
		final CloudletController<?> cloudlet = arguments.getCloudlet ();
		this.enforceCloudlet (cloudlet);
		if (positive)
			this.transcript.traceDebugging ("triggered (positive) callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, cloudlet, context);
		else
			this.transcript.traceWarning ("triggered (negative) callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, cloudlet, context);
	}
	
	@Deprecated
	protected final CloudletController<TContext> cloudlet;
	@Deprecated
	protected final TranscriptExceptionTracer exceptions;
	@Deprecated
	protected final Transcript transcript;
}
