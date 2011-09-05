
package eu.mosaic_cloud.tools;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.transcript.core.Transcript;
import eu.mosaic_cloud.transcript.tools.TranscriptExceptionTracer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;


public final class ClasspathExporter
		extends AbstractHandler
{
	private ClasspathExporter (final InetSocketAddress address, final ClassLoader loader, final ExceptionTracer exceptions)
	{
		super ();
		Preconditions.checkNotNull (address);
		Preconditions.checkNotNull (loader);
		this.transcript = Transcript.create (this);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
		this.address = address;
		this.loader = loader;
	}
	
	@Override
	public void handle (final String path, final HttpServletRequest request, final HttpServletResponse response, final int dispatch)
	{
		try {
			this.transcript.traceDebugging ("serving resource `%s`...", path);
			final InputStream input = this.loader.getResourceAsStream (path.substring (1));
			if (input != null) {
				final byte[] data = ByteStreams.toByteArray (input);
				input.close ();
				response.setContentLength (data.length);
				final OutputStream output = response.getOutputStream ();
				output.write (data);
				output.close ();
			} else
				response.sendError (404);
		} catch (final Throwable exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while serving resource `%s`; ignoring!", path);
		}
	}
	
	public final void startServer ()
	{
		try {
			this.server = new Server (this.address.getPort ());
			this.server.setHandler (this);
			this.server.start ();
		} catch (final Exception exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while starting the http server; aborting!");
		}
	}
	
	public final void stopServer ()
	{
		try {
			this.server.stop ();
		} catch (final Exception exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while stopping the http server; aborting!");
		}
	}
	
	private final InetSocketAddress address;
	private final TranscriptExceptionTracer exceptions;
	private final ClassLoader loader;
	private Server server;
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
		else
			address = new InetSocketAddress (arguments[0], Integer.parseInt (arguments[1]));
		final ClasspathExporter exporter = ClasspathExporter.create (address, Objects.firstNonNull (loader, ClassLoader.getSystemClassLoader ()), AbortingExceptionTracer.defaultInstance);
		exporter.startServer ();
		while (true) {
			try {
				Thread.sleep (1000);
			} catch (final InterruptedException exception) {
				break;
			}
		}
		exporter.stopServer ();
	}
}
