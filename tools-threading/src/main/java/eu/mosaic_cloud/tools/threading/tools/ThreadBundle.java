/*
 * #%L
 * mosaic-tools-threading
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

package eu.mosaic_cloud.tools.threading.tools;


import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;

import eu.mosaic_cloud.tools.threading.core.ThreadController;

import com.google.common.base.Preconditions;


public final class ThreadBundle<_Thread_ extends Thread>
		extends Object
		implements
			ThreadController,
			Iterable<_Thread_>
{
	private ThreadBundle ()
	{
		super ();
		this.collector = new ReferenceQueue<_Thread_> ();
		this.threads = new ConcurrentSkipListSet<ThreadReference<_Thread_>> (new ThreadComparator ());
	}
	
	@Override
	public final boolean await ()
	{
		this.collect ();
		return (Threading.join (this));
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		this.collect ();
		return (Threading.join (this, timeout));
	}
	
	@Override
	public final void interrupt ()
	{
		this.collect ();
		Threading.interrupt (this);
	}
	
	@Override
	public final Iterator<_Thread_> iterator ()
	{
		this.collect ();
		return (new ThreadIterator<_Thread_> (this.threads.iterator ()));
	}
	
	public final void register (final _Thread_ thread)
	{
		Preconditions.checkNotNull (thread);
		this.threads.add (new ThreadReference<_Thread_> (thread, this.collector));
	}
	
	public final int start ()
	{
		this.collect ();
		return (Threading.start (this));
	}
	
	private final void collect ()
	{
		while (true) {
			final Reference<? extends _Thread_> reference = this.collector.poll ();
			if (reference == null)
				break;
			final _Thread_ thread = reference.get ();
			if (thread == null)
				continue;
			this.threads.remove (new ThreadReference<_Thread_> (thread));
		}
	}
	
	public static final <_Thread_ extends Thread> ThreadBundle<_Thread_> create ()
	{
		return (new ThreadBundle<_Thread_> ());
	}
	
	private final ReferenceQueue<_Thread_> collector;
	private final ConcurrentSkipListSet<ThreadReference<_Thread_>> threads;
	
	private static final class ThreadComparator
			extends Object
			implements
				Comparator<ThreadReference<?>>
	{
		ThreadComparator ()
		{
			super ();
		}
		
		@Override
		public final int compare (final ThreadReference<?> left, final ThreadReference<?> right)
		{
			return ((left.hashCode () - right.hashCode ()));
		}
	}
	
	private static final class ThreadIterator<_Thread_ extends Thread>
			extends Object
			implements
				Iterator<_Thread_>
	{
		ThreadIterator (final Iterator<ThreadReference<_Thread_>> threads)
		{
			super ();
			this.threads = threads;
		}
		
		@Override
		public final boolean hasNext ()
		{
			if (this.current == null)
				this.advance ();
			return (this.current != null);
		}
		
		@Override
		public final _Thread_ next ()
		{
			if (this.current == null)
				this.advance ();
			if (this.current == null)
				throw (new NoSuchElementException ());
			final _Thread_ current = this.current;
			this.current = null;
			return (current);
		}
		
		@Override
		public final void remove ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		private final void advance ()
		{
			this.current = null;
			while (true) {
				if (!this.threads.hasNext ())
					break;
				this.current = this.threads.next ().get ();
				if (this.current != null)
					break;
			}
		}
		
		private _Thread_ current;
		private final Iterator<ThreadReference<_Thread_>> threads;
	}
	
	private static final class ThreadReference<_Thread_ extends Thread>
			extends Object
	{
		ThreadReference (final _Thread_ thread)
		{
			super ();
			this.threadIdentifier = thread.getId ();
			this.objectIdentifier = System.identityHashCode (thread);
			this.reference = null;
		}
		
		ThreadReference (final _Thread_ thread, final ReferenceQueue<? super _Thread_> collector)
		{
			super ();
			Preconditions.checkNotNull (thread);
			this.reference = new WeakReference<_Thread_> (thread, collector);
			this.threadIdentifier = thread.getId ();
			this.objectIdentifier = System.identityHashCode (thread);
		}
		
		@Override
		public final boolean equals (final Object object)
		{
			if (this == object)
				return (true);
			if (!(object instanceof ThreadReference))
				return (false);
			final ThreadReference<?> other = (ThreadReference<?>) object;
			return ((this.objectIdentifier == other.objectIdentifier) && (this.threadIdentifier == other.threadIdentifier));
		}
		
		public final _Thread_ get ()
		{
			return (this.reference.get ());
		}
		
		@Override
		public final int hashCode ()
		{
			return (this.objectIdentifier);
		}
		
		private final int objectIdentifier;
		private final WeakReference<_Thread_> reference;
		private final long threadIdentifier;
	}
}
