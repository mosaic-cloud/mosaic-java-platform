
package org.zeromq;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 */
public class ZDispatcher
{
	public ZDispatcher ()
	{
		this.dispatcherExecutor = Executors.newCachedThreadPool ();
	}
	
	public ZDispatcher (final ExecutorService dispatcherExecutor)
	{
		this.dispatcherExecutor = dispatcherExecutor;
	}
	
	public void registerHandler (final ZMQ.Socket socket, final ZMessageHandler messageHandler, final ZSender sender)
	{
		this.registerHandler (socket, messageHandler, sender, Executors.newFixedThreadPool (Runtime.getRuntime ().availableProcessors ()));
	}
	
	public void registerHandler (final ZMQ.Socket socket, final ZMessageHandler messageHandler, final ZSender sender, final ExecutorService threadpool)
	{
		final SocketDispatcher socketDispatcher = new SocketDispatcher (socket, messageHandler, sender, threadpool);
		if (this.dispatchers.putIfAbsent (socket, socketDispatcher) != null) {
			throw new IllegalArgumentException ("This socket already have a message handler");
		}
		socketDispatcher.start ();
		this.dispatcherExecutor.execute (socketDispatcher);
	}
	
	public void shutdown ()
	{
		this.dispatcherExecutor.shutdown ();
		for (final SocketDispatcher socketDispatcher : this.dispatchers.values ()) {
			socketDispatcher.shutdown ();
		}
		this.dispatchers.clear ();
	}
	
	public void unregisterHandler (final ZMQ.Socket socket)
	{
		final SocketDispatcher removedDispatcher = this.dispatchers.remove (socket);
		if (removedDispatcher == null) {
			throw new IllegalArgumentException ("This socket doesn't have a message handler");
		}
		removedDispatcher.shutdown ();
	}
	
	private final ExecutorService dispatcherExecutor;
	private final ConcurrentMap<ZMQ.Socket, SocketDispatcher> dispatchers = new ConcurrentHashMap<ZMQ.Socket, SocketDispatcher> ();
	
	public interface ZMessageHandler
	{
		public void handleMessage (ZDispatcher.ZSender sender, ZMsg msg);
	}
	
	public final static class ZSender
	{
		public final boolean send (final ZMsg msg)
		{
			return this.out.add (msg);
		}
		
		private final BlockingQueue<ZMsg> out = new LinkedBlockingQueue<ZMsg> ();
	}
	
	private static final class SocketDispatcher
			implements
				Runnable
	{
		public SocketDispatcher (final ZMQ.Socket socket, final ZMessageHandler handler, final ZSender sender, final ExecutorService handleThreadpool)
		{
			this.socket = socket;
			this.handler = handler;
			this.sender = sender;
			this.threadpool = handleThreadpool;
		}
		
		@Override
		public void run ()
		{
			while (this.active) {
				this.doReceive ();
				this.doHandle ();
				this.doSend ();
			}
			this.threadpool.shutdown ();
			this.shutdownLatch.countDown ();
		}
		
		public void shutdown ()
		{
			try {
				this.active = false;
				this.shutdownLatch.await ();
			} catch (final InterruptedException e) {}
		}
		
		public void start ()
		{
			this.active = true;
		}
		
		private void doHandle ()
		{
			if (!this.in.isEmpty () && this.busy.compareAndSet (false, true)) {
				this.threadpool.submit (new Runnable () {
					@Override
					public void run ()
					{
						final ZMessageBuffer messages = SocketDispatcher.messages.get ();
						messages.drainFrom (SocketDispatcher.this.in);
						SocketDispatcher.this.busy.set (false);
						for (int i = 0; i <= messages.lastValidIndex; i++) {
							if (SocketDispatcher.this.active) {
								SocketDispatcher.this.handler.handleMessage (SocketDispatcher.this.sender, messages.buffer[i]);
							}
						}
					}
				});
			}
		}
		
		private void doReceive ()
		{
			ZMsg msg;
			int remainingBuffer = SocketDispatcher.BUFFER_SIZE;
			while (this.active && (remainingBuffer-- > 0) && ((msg = ZMsg.recvMsg (this.socket, ZMQ.DONTWAIT)) != null) && (msg.size () > 0) && msg.getFirst ().hasData ()) {
				this.in.add (msg);
			}
		}
		
		private void doSend ()
		{
			ZMsg msg;
			int remainingBuffer = SocketDispatcher.BUFFER_SIZE;
			while (this.active && (remainingBuffer-- > 0) && ((msg = this.sender.out.poll ()) != null)) {
				msg.send (this.socket);
			}
		}
		
		private volatile boolean active = false;
		private final AtomicBoolean busy = new AtomicBoolean (false);
		private final ZMessageHandler handler;
		private final BlockingQueue<ZMsg> in = new LinkedBlockingQueue<ZMsg> ();
		private final ZSender sender;
		private final CountDownLatch shutdownLatch = new CountDownLatch (1);
		private final ZMQ.Socket socket;
		private final ExecutorService threadpool;
		private static final int BUFFER_SIZE = 1024;
		private static final ThreadLocal<ZMessageBuffer> messages = new ThreadLocal<ZMessageBuffer> () {
			@Override
			protected ZMessageBuffer initialValue ()
			{
				return new ZMessageBuffer ();
			}
		};
		
		private static class ZMessageBuffer
		{
			private void drainFrom (final BlockingQueue<ZMsg> in)
			{
				int lastIndex = this.lastValidIndex = -1;
				ZMsg msg;
				while ((++lastIndex < this.buffer.length) && ((msg = in.poll ()) != null)) {
					this.buffer[lastIndex] = msg;
					this.lastValidIndex = lastIndex;
				}
			}
			
			private final ZMsg[] buffer = new ZMsg[SocketDispatcher.BUFFER_SIZE];
			private int lastValidIndex;
		}
	}
}
