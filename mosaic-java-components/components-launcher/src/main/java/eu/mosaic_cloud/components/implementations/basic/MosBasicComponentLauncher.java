
package eu.mosaic_cloud.components.implementations.basic;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.json.tools.DefaultJsonCoder;
import eu.mosaic_cloud.tools.ClasspathExporter;
import org.slf4j.LoggerFactory;


public final class MosBasicComponentLauncher
{
	private MosBasicComponentLauncher ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String arguments[], final ClassLoader loader)
			throws Throwable
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (MosBasicComponentLauncher.class);
		Preconditions.checkArgument ((arguments != null) && (arguments.length == 5), "invalid arguments: expected <class> <ip> <http-port> <logback-port> <mos-url>");
		final String clasz = arguments[0];
		final InetSocketAddress httpAddress = new InetSocketAddress (arguments[1], Integer.parseInt (arguments[2]));
		final InetSocketAddress logbackAddress = new InetSocketAddress (arguments[1], Integer.parseInt (arguments[3]));
		final URL controller = new URL (arguments[4]);
		final ClasspathExporter exporter = ClasspathExporter.create (httpAddress, Objects.firstNonNull (loader, ClassLoader.getSystemClassLoader ()), AbortingExceptionTracer.defaultInstance);
		final SimpleSocketServer appender = new SimpleSocketServer ((LoggerContext) LoggerFactory.getILoggerFactory (), logbackAddress.getPort ());
		final String[] identifier = new String[] {null};
		final boolean[] shouldStop = new boolean[] {false};
		Runtime.getRuntime ().addShutdownHook (new Thread () {
			@Override
			public final void run ()
			{
				shouldStop[0] = true;
				logger.debug ("starting safety hook...");
				new Thread () {
					@Override
					public final void run ()
					{
						try {
							Thread.sleep (2000);
						} catch (final InterruptedException exception) {}
						Runtime.getRuntime ().halt (1);
					}
				}.start ();
				logger.debug ("stopping component...");
				if (identifier[0] != null)
					try {
						final URL stopUrl = new URL (controller, String.format ("/processes/stop?key=%s", identifier[0]));
						stopUrl.openStream ().close ();
					} catch (final Throwable exception) {
						logger.error ("failed stopping component", exception);
					}
				logger.debug ("stopping exporter...");
				try {
					exporter.stopServer ();
				} catch (final Throwable exception) {
					logger.error ("failed stopping exporter", exception);
				}
				logger.debug ("sopping appender...");
				try {
					appender.close ();
				} catch (final Throwable exception) {
					logger.error ("failed stopping appender", exception);
				}
			}
		});
		logger.debug ("starting exporter...");
		try {
			exporter.startServer ();
		} catch (final Throwable exception) {
			logger.error ("failed starting exporter", exception);
			Runtime.getRuntime ().halt (1);
			return;
		}
		logger.debug ("starting appender...");
		try {
			appender.start ();
		} catch (final Throwable exception) {
			logger.error ("failed starting appender", exception);
			Runtime.getRuntime ().halt (1);
			return;
		}
		logger.debug ("creating component...");
		try {
			final List<String> configuration = Arrays.asList (clasz, String.format ("http://%s:%d/", httpAddress.getAddress ().getHostAddress (), Integer.valueOf (httpAddress.getPort ())), String.format ("%s:%d", logbackAddress.getAddress ().getHostAddress (), Integer.valueOf (logbackAddress.getPort ())));
			final String[] createParameters = new String[] {String.format ("type=%s", URLEncoder.encode ("#mosaic-components:java-container", "UTF-8")), String.format ("configuration=%s", DefaultJsonCoder.defaultInstance.encodeToString (configuration)), "count=1"};
			final URL createUrl = new URL (controller, String.format ("/processes/create?%s", Joiner.on ("&").join (Arrays.asList (createParameters))));
			logger.debug ("{}", createUrl.toString ());
			try {
				final InputStream createStream = createUrl.openStream ();
				final String createResponse = new String (ByteStreams.toByteArray (createStream), Charset.forName ("UTF-8"));
				try {
					final Map<?, ?> createResponseMap = (Map<?, ?>) DefaultJsonCoder.defaultInstance.decodeFromString (createResponse);
					Preconditions.checkState (Boolean.TRUE.equals (createResponseMap.get ("ok")));
					Preconditions.checkState (((List<?>) createResponseMap.get ("keys")).size () > 0);
					identifier[0] = (String) ((List<?>) createResponseMap.get ("keys")).get (0);
					Preconditions.checkNotNull (identifier);
				} catch (final Throwable exception) {
					logger.error (String.format ("failed creating component: %s", createResponse), exception);
					Runtime.getRuntime ().halt (1);
					return;
				}
			} catch (final Throwable exception) {
				logger.error ("failed creating component", exception);
				Runtime.getRuntime ().halt (1);
				return;
			}
		} catch (final Throwable exception) {
			logger.error ("failed creating component", exception);
			Runtime.getRuntime ().halt (1);
			return;
		}
		logger.info ("started: {}", identifier);
		new BufferedReader (new InputStreamReader (System.in)).readLine ();
		System.exit (0);
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		MosBasicComponentLauncher.main (arguments, null);
	}
}
