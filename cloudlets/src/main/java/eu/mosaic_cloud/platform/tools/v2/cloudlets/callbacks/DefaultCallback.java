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

package eu.mosaic_cloud.platform.tools.v2.cloudlets.callbacks;


import eu.mosaic_cloud.platform.v2.cloudlets.core.Callback;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


public class DefaultCallback<TContext>
			extends Object
			implements
				Callback<TContext>
{
	protected DefaultCallback () {
		super ();
		this.transcript = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
	}
	
	public void initialize (@SuppressWarnings ("unused") final TContext context, final CloudletController<?> cloudlet) {
		Preconditions.checkNotNull (cloudlet);
		this.cloudlet = cloudlet;
	}
	
	protected void enforceCallbackArguments (final TContext context, final CallbackArguments arguments) {
		Preconditions.checkNotNull (arguments);
		this.enforceCloudlet (context, arguments.cloudlet);
	}
	
	protected void enforceCloudlet (@SuppressWarnings ("unused") final TContext context, final CloudletController<?> cloudlet) {
		Preconditions.checkNotNull (cloudlet);
		if ((this.cloudlet != null) && (this.cloudlet != cloudlet))
			throw (new IllegalArgumentException ());
	}
	
	protected CallbackCompletion<Void> handleUnhandledCallback (@SuppressWarnings ("rawtypes") final Class<? extends Callback> category, final String callback, final TContext context, final CallbackArguments arguments, final boolean positive, final boolean couldDestroy) {
		this.enforceCallbackArguments (context, arguments);
		this.traceCallback (category, callback, context, arguments, positive);
		if (positive)
			this.transcript.traceDebugging ("unhandled successfull callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, arguments.cloudlet, context);
		else
			this.transcript.traceWarning ("unhandled failure callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, arguments.cloudlet, context);
		if (!positive && couldDestroy)
			this.cloudlet.destroy ();
		return (DefaultCallback.Succeeded);
	}
	
	protected void traceCallback (@SuppressWarnings ("rawtypes") final Class<? extends Callback> category, final String callback, final TContext context, final CallbackArguments arguments, final boolean positive) {
		this.enforceCallbackArguments (context, arguments);
		if (positive)
			this.transcript.traceDebugging ("triggered (positive) callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, arguments.cloudlet, context);
		else
			this.transcript.traceWarning ("triggered (negative) callback `%{class}:%s` for cloudlet `%{object:identity}` within context `%{object:identity}`; ignoring!", category, callback, arguments.cloudlet, context);
	}
	
	@Deprecated
	protected CloudletController<?> cloudlet;
	@Deprecated
	protected final TranscriptExceptionTracer exceptions;
	@Deprecated
	protected final Transcript transcript;
	public static final CallbackCompletion<Void> Succeeded = CallbackCompletion.createOutcome ();
	protected static final CallbackCompletion<Void> NotImplemented = CallbackCompletion.createFailure (new UnsupportedOperationException ());
}
