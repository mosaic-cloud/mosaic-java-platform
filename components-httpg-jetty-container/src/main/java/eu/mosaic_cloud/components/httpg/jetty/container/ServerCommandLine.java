/*
 * #%L
 * mosaic-components-httpg-jetty-container
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

package eu.mosaic_cloud.components.httpg.jetty.container;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import eu.mosaic_cloud.components.httpg.jetty.connector.AmqpConnector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;


public class ServerCommandLine
{
	public static Server createServer (final Properties props, final ClassLoader loader)
	{
		final Server jettyServer = new Server ();
		final String userName = props.getProperty ("username");
		final String userPassword = props.getProperty ("password");
		final String hostName = props.getProperty ("server");
		final String routingKey = props.getProperty ("routing-key");
		final String exchangeName = props.getProperty ("exchange");
		final String queueName = props.getProperty ("queue");
		final int amqpPort = Integer.parseInt (props.getProperty ("port"));
		final String amqpVirtualHost = props.getProperty ("virtual-host");
		final boolean amqpAutoDeclare = props.containsKey ("auto-declare");
		final String tmp = props.getProperty ("tmp");
		final String webAppDir = props.getProperty ("webapp");
		final String ctxPath = props.getProperty ("app-context", "/");
		final String listen = props.getProperty ("jetty-socket-connector-port");
		/*
		 * Setup connectors
		 */
		final AmqpConnector amqpConnector = new AmqpConnector (exchangeName, routingKey, queueName, hostName, userName, userPassword, amqpPort, amqpVirtualHost, amqpAutoDeclare);
		final Connector[] connectors;
		if ((listen != null) && !listen.isEmpty ()) {
			final int port = Integer.parseInt (listen);
			final SocketConnector socketConnector = new SocketConnector ();
			socketConnector.setPort (port);
			connectors = new Connector[] {socketConnector, amqpConnector};
		} else {
			connectors = new Connector[] {amqpConnector};
		}
		jettyServer.setConnectors (connectors);
		/*
		 * Check if user want's to register webapps
		 */
		if (webAppDir != null) {
			final WebAppContext webapp = new WebAppContext ();
			final WebAppClassLoader classloader;
			try {
				classloader = new WebAppClassLoader (loader, webapp);
			} catch (final Throwable exception) {
				throw (new Error (exception));
			}
			if (!"embedded".equals (webAppDir)) {
				webapp.setWar (webAppDir);
				webapp.setExtractWAR (true);
				webapp.setCopyWebInf (true);
				webapp.setCopyWebDir (true);
			} else {
				final String resourceBase = loader.getResource ("WEB-INF/web.xml").toString ().replaceAll ("WEB-INF/web.xml$", "");
				webapp.setResourceBase (resourceBase);
				webapp.setWar (null);
				webapp.setExtractWAR (false);
				webapp.setCopyWebInf (false);
				webapp.setCopyWebDir (false);
			}
			webapp.setClassLoader (classloader);
			webapp.setContextPath (ctxPath);
			webapp.setParentLoaderPriority (false);
			webapp.addServlet (DefaultServlet.class, "/");
			webapp.setInitParameter ("dirAllowed", "false");
			webapp.setInitParameter ("welcomeServlets", "true");
			webapp.setInitParameter ("redirectWelcome", "true");
			if ((tmp != null) && (!tmp.isEmpty ()))
				webapp.setTempDirectory (new File (tmp));
			jettyServer.setHandler (webapp);
		}
		/*
		 * Start the toy
		 */
		return (jettyServer);
	}
	
	public static void main (final String[] args)
			throws Exception
	{
		final Properties props = ServerCommandLine.getConfig (args);
		final Server server = ServerCommandLine.createServer (props, ClassLoader.getSystemClassLoader ());
		server.start ();
		server.join ();
	}
	
	private static Properties getConfig (final String[] args)
	{
		CommandLine line = null;
		final CommandLineParser parser = new GnuParser ();
		final Properties props = new Properties ();
		final Options options = new Options ();
		final Option helpOption = new Option ("h", "help", false, "print this message");
		final Option propertyFileOption = new Option ("f", "property-file", true, "Use property file");
		final Option amqpServerOption = new Option ("s", "server", true, "The AMQP server");
		final Option amqpServerPortOption = new Option ("t", "port", true, "The AMQP server port");
		final Option amqpUserNameOption = new Option ("u", "username", true, "The user used for connecting");
		final Option amqpUserPasswordOption = new Option ("p", "password", true, "Password for the user in cleartext. Warning! thnk twice b4 using da option!");
		final Option amqpExchangeOption = new Option ("e", "exchange", true, "The exchange name");
		final Option amqpQueueOption = new Option ("q", "queue", true, "The queue name");
		final Option amqpRoutingKeyOption = new Option ("r", "routing-key", true, "The routing key");
		final Option amqpAutodeclareQueue = new Option ("a", "auto-declare", false, "auto declare the queue");
		final Option jettyClassicConnector = new Option ("c", "jetty-socket-connector-port", true, "Also start the normal on the specified port");
		final Option webapp = new Option ("w", "webapp", true, "The location of the war file");
		final Option webappContext = new Option ("q", "app-context", true, "App context path");
		final Option tmp = new Option ("T", "tmp", true, "temporary folder");
		options.addOption (helpOption);
		options.addOption (amqpServerOption);
		options.addOption (amqpServerPortOption);
		options.addOption (amqpRoutingKeyOption);
		options.addOption (amqpExchangeOption);
		options.addOption (amqpQueueOption);
		options.addOption (amqpUserNameOption);
		options.addOption (amqpUserPasswordOption);
		options.addOption (amqpAutodeclareQueue);
		options.addOption (propertyFileOption);
		options.addOption (jettyClassicConnector);
		options.addOption (webapp);
		options.addOption (webappContext);
		options.addOption (tmp);
		try {
			line = parser.parse (options, args);
		} catch (final ParseException exp) {
			System.err.println ("Parsing failed.  Reason: " + exp.getMessage ());
			ServerCommandLine.printHelpAndExit (options);
		}
		if (line.hasOption ("help")) {
			ServerCommandLine.printHelpAndExit (options);
		}
		if (line.hasOption ("property-file")) {
			final String fname = line.getOptionValue ("property-file");
			try {
				final FileInputStream fstream = new FileInputStream (fname);
				props.load (fstream);
				fstream.close ();
			} catch (final FileNotFoundException e) {
				System.err.println ("Could not open file: " + e.getMessage ());
				System.exit (1);
			} catch (final IOException e) {
				System.err.println ("Could not read property file: " + e.getMessage ());
				System.exit (1);
			}
		}
		/*
		 * Add defaults
		 */
		props.setProperty ("server", "127.0.0.1");
		props.setProperty ("port", "5672");
		props.setProperty ("virtual-host", "/");
		props.setProperty ("exchange", "mosaic-http-requests");
		props.setProperty ("routing-key", "#");
		props.setProperty ("username", "guest");
		props.setProperty ("password", "guest");
		props.setProperty ("queue", "mosaic-http-requests");
		/*
		 * Add command line options to property
		 */
		for (final Option o : line.getOptions ()) {
			if (o.getValue () != null) {
				props.setProperty (o.getLongOpt (), o.getValue ());
			}
		}
		if (line.hasOption ("auto-declare")) {
			props.setProperty ("auto-declare", "");
		}
		return props;
	}
	
	private static void printHelpAndExit (final Options options)
	{
		final HelpFormatter formatter = new HelpFormatter ();
		formatter.printHelp ("jetty-amqp-connector", options);
		System.exit (1);
	}
}
