/*
 * #%L
 * mosaic-tools-exceptions
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

package eu.mosaic_cloud.tools.exceptions.tools;


import java.io.PrintStream;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;


public final class AbortingExceptionTracer
		extends BaseExceptionTracer
{
	private AbortingExceptionTracer (final PrintStream transcript)
	{
		super ();
		Preconditions.checkNotNull (transcript);
		this.transcript = transcript;
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception)
	{
		final boolean abort;
		switch (resolution) {
			case Handled :
				abort = false;
				break;
			case Deferred :
				abort = false;
				break;
			case Ignored :
				abort = true;
				break;
			default:
				abort = true;
				break;
		}
		if (abort) {
			this.trace (exception);
			this.abort ();
		}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		this.trace (resolution, exception);
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.trace (resolution, exception);
	}
	
	private final void abort ()
	{
		AbortingExceptionTracer.exiter.maybeStart ();
	}
	
	private final void trace (final Throwable exception)
	{
		AbortingExceptionTracer.trace (exception, this.transcript);
	}
	
	private final PrintStream transcript;
	
	static final void trace (final Throwable exception, final PrintStream transcript)
	{
		try {
			exception.printStackTrace (transcript);
		} catch (final Throwable exception1) {
			// intentional
		}
	}
	
	public static final AbortingExceptionTracer defaultInstance = new AbortingExceptionTracer (AbortingExceptionTracer.defaultTranscript);
	private static final int defaultExitCode = 254;
	private static final long defaultExitTimeout = 2000;
	private static final PrintStream defaultTranscript = System.err;
	private static final Exiter exiter = new Exiter (AbortingExceptionTracer.defaultExitCode, AbortingExceptionTracer.defaultExitTimeout, AbortingExceptionTracer.defaultTranscript);
	
	private static final class Exiter
			extends Thread
	{
		public Exiter (final int code, final long timeout, final PrintStream transcript)
		{
			super ();
			Preconditions.checkArgument ((code >= 0) && (code <= 255));
			Preconditions.checkArgument (timeout > 0);
			Preconditions.checkNotNull (transcript);
			this.code = code;
			this.timeout = timeout;
			this.transcript = transcript;
			this.setName (AbortingExceptionTracer.class.getCanonicalName () + "#exiter");
			this.setDaemon (true);
		}
		
		public final void maybeStart ()
		{
			synchronized (this) {
				if (!this.isAlive ())
					try {
						this.start ();
					} catch (final Throwable exception1) {
						AbortingExceptionTracer.trace (exception1, this.transcript);
						try {
							Runtime.getRuntime ().halt (this.code);
						} catch (final Throwable exception2) {
							AbortingExceptionTracer.trace (exception2, this.transcript);
						}
					}
			}
		}
		
		@Override
		public final void run ()
		{
			System.exit (this.code);
			for (int timeoutStep = 0; timeoutStep < this.timeout / Exiter.defaultTimeoutResolution; timeoutStep++)
				try {
					Thread.sleep (Exiter.defaultTimeoutResolution);
				} catch (final InterruptedException exception1) {
					// intentional
				}
			Runtime.getRuntime ().halt (this.code);
		}
		
		private final int code;
		private final long timeout;
		private final PrintStream transcript;
		private static final long defaultTimeoutResolution = 100;
	}
}
