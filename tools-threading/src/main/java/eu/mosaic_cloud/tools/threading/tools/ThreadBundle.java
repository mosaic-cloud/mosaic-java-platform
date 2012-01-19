
package eu.mosaic_cloud.tools.threading.tools;


import java.util.concurrent.ConcurrentSkipListSet;


public final class ThreadBundle
		extends Object
{
	public ThreadBundle ()
	{
		super ();
		this.threads = new ConcurrentSkipListSet<ThreadBundle.Thread> ();
	}
	
	public final void join ()
	{
		while (true)
			try {
				for (final Thread thread : this.threads)
					thread.join (1);
				break;
			} catch (final InterruptedException exception) {
				for (final Thread thread : this.threads)
					thread.interrupt ();
			}
	}
	
	public final void shouldStop ()
	{
		for (final Thread thread : this.threads)
			thread.shouldStop ();
	}
	
	public final void start ()
	{
		for (final Thread thread : this.threads)
			thread.start ();
	}
	
	/*private*/final ConcurrentSkipListSet<Thread> threads;
	
	public abstract class Thread
			extends java.lang.Thread
			implements
				Comparable<Thread>
	{
		protected Thread ()
		{
			super ();
			ThreadBundle.this.threads.add (this);
			this.shouldStop = false;
		}
		
		@Override
		public final int compareTo (final Thread thread)
		{
			final int thisHash = System.identityHashCode (this);
			final int threadHash = System.identityHashCode (thread);
			if (thisHash < threadHash)
				return (-1);
			if (thisHash > threadHash)
				return (1);
			return (0);
		}
		
		public final void shouldStop ()
		{
			this.shouldStop = true;
		}
		
		@Override
		public final synchronized void start ()
		{
			super.start ();
		}
		
		protected final boolean shouldRun ()
		{
			return (!this.shouldStop);
		}
		
		private boolean shouldStop;
	}
}
