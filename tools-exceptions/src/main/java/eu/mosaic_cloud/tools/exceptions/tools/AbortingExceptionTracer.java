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

import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;


public final class AbortingExceptionTracer
		extends BaseExceptionTracer
{
	private AbortingExceptionTracer (final PrintStream transcript)
	{
		super ();
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
		final PrintStream finalTranscript = ((transcript != null) ? transcript : System.err);
		try {
			exception.printStackTrace (finalTranscript);
		} catch (final Throwable exception1) {
			// intentional
		}
	}
	
	public static final int defaultExitCode = 254;
	public static final long defaultExitTimeout = 2000;
	public static final AbortingExceptionTracer defaultInstance = new AbortingExceptionTracer (null);
	private static final long defaultExitTimeoutResolution = 100;
	private static final Exiter exiter = new Exiter (null);
	
	private static final class Exiter
			extends Object
	{
		public Exiter (final PrintStream transcript)
		{
			super ();
			this.transcript = transcript;
			this.exitThread = new Thread (new Runnable () {
				@Override
				public final void run ()
				{
					Exiter.this.exit ();
				}
			}, AbortingExceptionTracer.class.getCanonicalName () + "#exiter");
			this.haltThread = new Thread (new Runnable () {
				@Override
				public final void run ()
				{
					Exiter.this.halt ();
				}
			}, AbortingExceptionTracer.class.getCanonicalName () + "#halter");
			this.exitThread.setDaemon (true);
			Runtime.getRuntime ().addShutdownHook (this.haltThread);
		}
		
		public final void maybeStart ()
		{
			synchronized (this) {
				if (!this.exitThread.isAlive ())
					try {
						this.exitThread.start ();
					} catch (final Throwable exception1) {
						AbortingExceptionTracer.trace (exception1, this.transcript);
						try {
							Runtime.getRuntime ().halt (AbortingExceptionTracer.defaultExitCode);
						} catch (final Throwable exception2) {
							AbortingExceptionTracer.trace (exception2, this.transcript);
						}
					}
			}
		}
		
		private final void exit ()
		{
			try {
				Runtime.getRuntime ().exit (AbortingExceptionTracer.defaultExitCode);
			} catch (final Throwable exception) {
				AbortingExceptionTracer.trace (exception, this.transcript);
			}
		}
		
		private final void halt ()
		{
			for (int timeoutStep = 0; timeoutStep < AbortingExceptionTracer.defaultExitTimeout / AbortingExceptionTracer.defaultExitTimeoutResolution; timeoutStep++)
				try {
					Thread.sleep (AbortingExceptionTracer.defaultExitTimeoutResolution);
				} catch (final InterruptedException exception1) {
					// intentional
				}
			try {
				Runtime.getRuntime ().halt (AbortingExceptionTracer.defaultExitCode);
			} catch (final Throwable exception) {
				AbortingExceptionTracer.trace (exception, this.transcript);
			}
		}
		
		private final Thread exitThread;
		private final Thread haltThread;
		private final PrintStream transcript;
	}
}
