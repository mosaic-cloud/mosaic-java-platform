
package eu.mosaic_cloud.tools;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.transcript.core.Transcript;
import eu.mosaic_cloud.transcript.tools.TranscriptExceptionTracer;


public final class ClasspathExporter
		extends Object
		implements
			HttpHandler
{
	private ClasspathExporter (final InetSocketAddress address, final ClassLoader loader, final ExceptionTracer exceptions)
	{
		super ();
		Preconditions.checkNotNull (address);
		Preconditions.checkNotNull (loader);
		this.transcript = Transcript.create (this);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
		this.executor = Executors.newCachedThreadPool (DefaultThreadPoolFactory.create (this, true, Thread.NORM_PRIORITY, this.exceptions));
		this.address = address;
		this.loader = loader;
	}
	
	@Override
	public void handle (final HttpExchange exchange)
	{
		String path = null;
		try {
			path = exchange.getRequestURI ().getPath ();
			this.transcript.traceInformation ("serving resource `%s`...", path);
			final InputStream input = this.loader.getResourceAsStream (path.substring (1));
			if (input != null) {
				exchange.sendResponseHeaders (200, 0);
				final OutputStream output = exchange.getResponseBody ();
				ByteStreams.copy (input, output);
				output.close ();
				input.close ();
			} else
				exchange.sendResponseHeaders (404, -1);
		} catch (final Throwable exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while serving resource `%s`; ignoring!", path);
		}
	}
	
	public final void start ()
	{
		try {
			this.server = HttpServer.create (this.address, 0);
		} catch (final IOException exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while creating the http server; aborting!");
		}
		this.server.setExecutor (this.executor);
		this.server.createContext ("/", this);
		this.server.start ();
	}
	
	public final void stop ()
	{
		this.server.stop (5000);
	}
	
	private final InetSocketAddress address;
	private final TranscriptExceptionTracer exceptions;
	private final ExecutorService executor;
	private final ClassLoader loader;
	private HttpServer server;
	private final Transcript transcript;
	
	public static final ClasspathExporter create (final InetSocketAddress address, final ClassLoader loader, final ExceptionTracer exceptions)
	{
		return (new ClasspathExporter (address, loader, exceptions));
	}
	
	public static final void main (final String[] arguments)
	{
		ClasspathExporter.main (arguments, null);
	}
	
	public static final void main (final String[] arguments, final ClassLoader loader)
	{
		Preconditions.checkArgument ((arguments != null) && ((arguments.length == 0) || (arguments.length == 2)), "invalid arguments: expected <ip> <port>");
		final InetSocketAddress address;
		if (arguments.length == 0)
			address = new InetSocketAddress (27665);
		else {
			Preconditions.checkArgument (arguments.length == 2, "invalid arguments; expected: <ip> <port>");
			address = new InetSocketAddress (arguments[0], Integer.parseInt (arguments[1]));
		}
		final ClasspathExporter exporter = ClasspathExporter.create (address, Objects.firstNonNull (loader, ClassLoader.getSystemClassLoader ()), AbortingExceptionTracer.defaultInstance);
		exporter.start ();
		while (true) {
			try {
				Thread.sleep (1000);
			} catch (final InterruptedException exception) {
				break;
			}
		}
		exporter.stop ();
	}
}
