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


import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.core.ICallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.slf4j.Logger;


/**
 * Default callback class.
 * 
 * @author Georgiana Macariu
 * @param <TContext>
 *            the type of the context of the cloudlet using this callback
 */
public class DefaultCallback<TContext>
			implements
				ICallback<TContext>
{
	protected DefaultCallback () {
		super ();
		this.transcript = Transcript.create (this, true);
		this.logger = this.transcript.adaptAs (Logger.class);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
	}
	
	/**
	 * Handles any unhandled callback.
	 * 
	 * @param arguments
	 *            the arguments of the callback
	 * @param callbackType
	 *            a string describing the type of callback (e.g. initialize)
	 * @param positive
	 *            <code>true</code> if callback corresponds to successful termination of the operation
	 * @param couldDestroy
	 *            <code>true</code> if cloudlet can be destroyed here
	 */
	protected CallbackCompletion<Void> handleUnhandledCallback (final CallbackArguments arguments, final String callbackType, final boolean positive, final boolean couldDestroy) {
		this.traceUnhandledCallback (arguments, callbackType, positive);
		if (!positive && couldDestroy) {
			arguments.getCloudlet ().destroy ();
		}
		return CallbackCompletion.createOutcome ();
	}
	
	/**
	 * Traces unhandled callbacks.
	 * 
	 * @param arguments
	 *            the arguments of the callback
	 * @param callbackType
	 *            a string describing the type of callback (e.g. initialize)
	 * @param positive
	 *            <code>true</code> if callback corresponds to successful termination of the operation
	 */
	protected void traceUnhandledCallback (final CallbackArguments arguments, final String callbackType, final boolean positive) {
		if (positive) {
			this.transcript.traceDebugging ("unhandled successfull callback `%s` for cloudlet `%{object:identity}`; ignoring!", callbackType, arguments.getCloudlet ());
		} else {
			this.transcript.traceWarning ("unhandled failure callback `%s` for cloudlet `%{object:identity}`; ignoring!", callbackType, arguments.getCloudlet ());
		}
	}
	
	protected final TranscriptExceptionTracer exceptions;
	protected final Logger logger;
	protected final Transcript transcript;
}
