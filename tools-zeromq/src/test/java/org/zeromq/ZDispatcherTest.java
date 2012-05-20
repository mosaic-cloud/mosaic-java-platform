
package org.zeromq;


import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;


/**
 */
public class ZDispatcherTest
{
	@Test
	@Ignore
	public void dispatcherPerformanceTest ()
			throws InterruptedException
	{
		final int nMessages = 1000000;
		final CountDownLatch latch = new CountDownLatch (nMessages);
		final ZContext ctx = new ZContext ();
		final ZDispatcher dispatcher = new ZDispatcher ();
		final ZMQ.Socket in = ctx.createSocket (ZMQ.ROUTER);
		in.bind ("inproc://zmsg.test");
		final ZMQ.Socket out = ctx.createSocket (ZMQ.DEALER);
		out.connect ("inproc://zmsg.test");
		dispatcher.registerHandler (in, new ZDispatcher.ZMessageHandler () {
			@Override
			public void handleMessage (final ZDispatcher.ZSender sender, final ZMsg msg)
			{
				latch.countDown ();
			}
		}, new ZDispatcher.ZSender ());
		final long start = System.currentTimeMillis ();
		for (int i = 0; i < nMessages; i++) {
			final ZMsg msg = new ZMsg ();
			msg.addLast (UUID.randomUUID ().toString ());
			msg.send (out);
		}
		System.out.println (MessageFormat.format ("performanceTest message sent:{0}", nMessages));
		latch.await ();
		System.out.println (MessageFormat.format ("performanceTest throughput:{0} messages/seconds", nMessages / ((System.currentTimeMillis () - start) / 1000)));
		dispatcher.shutdown ();
		ctx.destroy ();
	}
	
	@Test
	public void singleMessage ()
			throws InterruptedException
	{
		final CountDownLatch latch = new CountDownLatch (1);
		final ZContext ctx = new ZContext ();
		final ZMQ.Socket logger = ctx.createSocket (ZMQ.PAIR);
		logger.bind ("inproc://zmsg.test");
		final ZMQ.Socket out = ctx.createSocket (ZMQ.PAIR);
		out.connect ("inproc://zmsg.test");
		final String mesgTxt = "Hello";
		final ZDispatcher dispatcher = new ZDispatcher ();
		final ZDispatcher.ZSender outSender = new ZDispatcher.ZSender ();
		dispatcher.registerHandler (out, new ZDispatcher.ZMessageHandler () {
			@Override
			public void handleMessage (final ZDispatcher.ZSender sender, final ZMsg msg)
			{}
		}, outSender);
		dispatcher.registerHandler (logger, new ZDispatcher.ZMessageHandler () {
			@Override
			public void handleMessage (final ZDispatcher.ZSender sender, final ZMsg msg)
			{
				Assert.assertEquals (mesgTxt, msg.poll ().toString ());
				latch.countDown ();
			}
		}, new ZDispatcher.ZSender ());
		final ZMsg msg = new ZMsg ();
		final ZFrame frame = new ZFrame (mesgTxt);
		msg.addFirst (frame);
		outSender.send (msg);
		latch.await (1, TimeUnit.SECONDS);
		Assert.assertEquals (0, latch.getCount ());
		dispatcher.shutdown ();
		ctx.destroy ();
	}
	
	@Test
	public void testMessagesDispatchedToDifferentHandlersAreExecutedConcurrently ()
			throws InterruptedException,
				BrokenBarrierException,
				TimeoutException
	{
		final AtomicBoolean threadingIssueDetected = new AtomicBoolean (false);
		final Lock guardLock1 = new ReentrantLock ();
		final Lock guardLock2 = new ReentrantLock ();
		final CyclicBarrier handlersBarrier = new CyclicBarrier (3);
		final ZContext ctx = new ZContext ();
		final ZMQ.Socket socketOne = ctx.createSocket (ZMQ.PAIR);
		socketOne.bind ("inproc://zmsg.test");
		final ZMQ.Socket socketTwo = ctx.createSocket (ZMQ.PAIR);
		socketTwo.connect ("inproc://zmsg.test");
		final ZDispatcher dispatcher = new ZDispatcher ();
		final ZDispatcher.ZSender senderOne = new ZDispatcher.ZSender ();
		dispatcher.registerHandler (socketOne, new ZDispatcher.ZMessageHandler () {
			@Override
			public void handleMessage (final ZDispatcher.ZSender sender, final ZMsg msg)
			{
				try {
					if (guardLock1.tryLock ()) {
						handlersBarrier.await (1, TimeUnit.SECONDS);
					} else {
						threadingIssueDetected.set (true);
					}
				} catch (final Exception ex) {
					threadingIssueDetected.set (true);
				} finally {
					guardLock1.unlock ();
				}
			}
		}, senderOne);
		final ZDispatcher.ZSender senderTwo = new ZDispatcher.ZSender ();
		dispatcher.registerHandler (socketTwo, new ZDispatcher.ZMessageHandler () {
			@Override
			public void handleMessage (final ZDispatcher.ZSender sender, final ZMsg msg)
			{
				try {
					if (guardLock2.tryLock ()) {
						handlersBarrier.await (1, TimeUnit.SECONDS);
					} else {
						threadingIssueDetected.set (true);
					}
				} catch (final Exception ex) {
					threadingIssueDetected.set (true);
				} finally {
					guardLock2.unlock ();
				}
			}
		}, senderTwo);
		final ZMsg msg = new ZMsg ();
		final ZFrame frame = new ZFrame ("Hello");
		msg.addFirst (frame);
		senderOne.send (msg.duplicate ());
		senderOne.send (msg.duplicate ());
		senderTwo.send (msg.duplicate ());
		senderTwo.send (msg.duplicate ());
		handlersBarrier.await (1, TimeUnit.SECONDS);
		handlersBarrier.await (1, TimeUnit.SECONDS);
		Assert.assertFalse (threadingIssueDetected.get ());
		dispatcher.shutdown ();
		ctx.destroy ();
	}
	
	@Test
	public void testNoMessageAreSentAfterShutdown ()
			throws InterruptedException,
				BrokenBarrierException,
				TimeoutException
	{
		final AtomicBoolean shutdownIssueDetected = new AtomicBoolean (false);
		final CountDownLatch latch = new CountDownLatch (1);
		final ZContext ctx = new ZContext ();
		final ZMQ.Socket socketOne = ctx.createSocket (ZMQ.PAIR);
		socketOne.bind ("inproc://zmsg.test");
		final ZMQ.Socket socketTwo = ctx.createSocket (ZMQ.PAIR);
		socketTwo.connect ("inproc://zmsg.test");
		final ZDispatcher dispatcher = new ZDispatcher ();
		final CyclicBarrier handlersBarrier = new CyclicBarrier (2, new Runnable () {
			@Override
			public void run ()
			{
				if (latch.getCount () == 0) {
					dispatcher.shutdown ();
				}
			}
		});
		final ZDispatcher.ZSender senderOne = new ZDispatcher.ZSender ();
		dispatcher.registerHandler (socketOne, new ZDispatcher.ZMessageHandler () {
			@Override
			public void handleMessage (final ZDispatcher.ZSender sender, final ZMsg msg)
			{
				latch.countDown ();
				try {
					handlersBarrier.await (1, TimeUnit.SECONDS);
				} catch (final Exception e) {}
			}
		}, senderOne);
		final ZDispatcher.ZSender senderTwo = new ZDispatcher.ZSender ();
		dispatcher.registerHandler (socketTwo, new ZDispatcher.ZMessageHandler () {
			@Override
			public void handleMessage (final ZDispatcher.ZSender sender, final ZMsg msg)
			{
				sender.send (msg);
				shutdownIssueDetected.set (true);
			}
		}, senderTwo);
		final ZMsg msg = new ZMsg ();
		msg.add (new ZFrame ("Hello"));
		senderTwo.send (msg);
		handlersBarrier.await (1, TimeUnit.SECONDS);
		senderOne.send (msg);
		senderOne.send (msg);
		latch.await (1, TimeUnit.SECONDS);
		Assert.assertEquals (0, latch.getCount ());
		Assert.assertFalse (shutdownIssueDetected.get ());
		dispatcher.shutdown ();
		ctx.destroy ();
	}
}
