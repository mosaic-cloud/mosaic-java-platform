/*
 * #%L
 * mosaic-components-httpg-jetty-connector
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
package eu.mosaic_cloud.components.httpg.jetty.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

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
import org.eclipse.jetty.webapp.WebAppContext;

public class ServerCommandLine {
	private static void printHelpAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("jetty-amqp-connector", options);
		System.exit(1);
	}

	private static Properties getConfig(String[] args) {
		CommandLine line = null;
		CommandLineParser parser = new GnuParser();
		Properties props = new Properties();
		Options options = new Options();
		Option helpOption = new Option("h", "help", false, "print this message");
		Option propertyFileOption = new Option("f", "property-file", true,
				"Use property file");
		Option amqpServerOption = new Option("s", "server", true,
				"The AMQP server");
		Option amqpServerPortOption = new Option("t", "port", true,
				"The AMQP server port");
		Option amqpUserNameOption = new Option("u", "username", true,
				"The user used for connecting");
		Option amqpUserPasswordOption = new Option("p", "password", true,
				"Password for the user in cleartext. Warning! thnk twice b4 using da option!");
		Option amqpExchangeOption = new Option("e", "exchange", true,
				"The exchange name");
		Option amqpQueueOption = new Option("q", "queue", true,
				"The queue name");
		Option amqpRoutingKeyOption = new Option("r", "routing-key", true,
				"The routing key");
		Option amqpAutodeclareQueue = new Option("a", "auto-declare", false,
				"auto declare the queue");
		Option jettyClassicConnector = new Option("c",
				"jetty-socket-connector-port", true,
				"Also start the normal on the specified port");
		Option webapp = new Option("w", "webapp", true,
				"The location of the war file");
		Option webappContext = new Option("q", "app-context", true, "App context path");
		Option tmp = new Option("T", "tmp", true, "temporary folder");

		options.addOption(helpOption);
		options.addOption(amqpServerOption);
		options.addOption(amqpServerPortOption);
		options.addOption(amqpRoutingKeyOption);
		options.addOption(amqpExchangeOption);
		options.addOption(amqpQueueOption);
		options.addOption(amqpUserNameOption);
		options.addOption(amqpUserPasswordOption);
		options.addOption(amqpAutodeclareQueue);
		options.addOption(propertyFileOption);
		options.addOption(jettyClassicConnector);
		options.addOption(webapp);
		options.addOption(webappContext);
		options.addOption(tmp);
		try {
			line = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			printHelpAndExit(options);
		}

		if (line.hasOption("help")) {
			printHelpAndExit(options);
		}

		if (line.hasOption("property-file")) {
			String fname = line.getOptionValue("property-file");
			try {
				FileInputStream fstream = new FileInputStream(fname);
				props.load(fstream);
				fstream.close();
			} catch (FileNotFoundException e) {
				System.err.println("Could not open file: " + e.getMessage());
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Could not read property file: "
						+ e.getMessage());
				System.exit(1);
			}
		}

		/*
		 * Add defaults
		 */
		props.setProperty("server", "127.0.0.1");
		props.setProperty("port", "5672");
		props.setProperty("virtual-host", "/");
		props.setProperty("exchange", "mosaic-http-requests");
		props.setProperty("routing-key", "#");
		props.setProperty("username", "guest");
		props.setProperty("password", "guest");
		props.setProperty("queue", "mosaic-http-requests");

		/*
		 * Add command line options to property
		 */
		for (Option o : line.getOptions()) {
			if (o.getValue() != null) {
				props.setProperty(o.getLongOpt(), o.getValue());
			}
		}

		if (line.hasOption("auto-declare")) {
			props.setProperty("auto-declare", "");
		}

		return props;
	}

	public static Server createServer(Properties props, final ClassLoader loader) {
		Server jettyServer = new Server();
		String userName = props.getProperty("username");
		String userPassword = props.getProperty("password");
		String hostName = props.getProperty("server");
		String routingKey = props.getProperty("routing-key");
		String exchangeName = props.getProperty("exchange");
		String queueName = props.getProperty("queue");
		int amqpPort = Integer.parseInt(props.getProperty("port"));
		String amqpVirtualHost = props.getProperty("virtual-host");
		String tmp = props.getProperty("tmp");
		
		/*
		 * Setup connectors
		 */
		AmqpConnector amqpConnector = new AmqpConnector(exchangeName,
				routingKey, queueName, hostName, userName, userPassword,
				amqpPort, amqpVirtualHost, props.containsKey("auto-declare"));
		Connector[] connectors = null;
		if (props.containsKey("jetty-socket-connector-port")) {
			int port = Integer.parseInt(props
					.getProperty("jetty-socket-connector-port"));
			SocketConnector socketConnector = new SocketConnector();
			socketConnector.setPort(port);
			connectors = new Connector[] { socketConnector, amqpConnector };
		} else {
			connectors = new Connector[] { amqpConnector };
		}
		jettyServer.setConnectors(connectors);

		/*
		 * Check if user want's to register webapps
		 */
		if (props.containsKey("webapp")) {
			final String webAppDir = props.getProperty("webapp");
			final String ctxPath = props.getProperty("app-context", "/");
			
			WebAppContext webapp = new WebAppContext();
			if ((tmp != null) && (!tmp.isEmpty()))
				webapp.setTempDirectory(new File(tmp));
			webapp.setContextPath(ctxPath);
			if (!"embedded".equals (webAppDir))
				webapp.setWar(webAppDir);
			else {
				StringBuilder classPath = new StringBuilder();
				try {
					Enumeration<URL> resources = loader.getResources("");
					while (resources.hasMoreElements()) {
						if (classPath.length() > 0)
							classPath.append(';');
						classPath.append(resources.nextElement().toString());
					}
				} catch (final Throwable exception) {
					throw (new Error(exception));
				}
				webapp.setResourceBase(loader.getResource("WEB-INF/web.xml").toString().replaceAll("WEB-INF/web.xml$", ""));
				webapp.setExtraClasspath(classPath.toString());
				webapp.setClassLoader(loader);
			}
			webapp.setParentLoaderPriority(false);
			webapp.addServlet(DefaultServlet.class, "/");
			webapp.setInitParameter("dirAllowed", "false");
			webapp.setInitParameter("welcomeServlets", "true");
			webapp.setInitParameter("redirectWelcome", "true");
			webapp.setExtractWAR(false);
			jettyServer.setHandler(webapp);
		}

		/*
		 * Start the toy
		 */
		return (jettyServer);
	}

	public static void main(String[] args) throws Exception {
		Properties props = getConfig(args);
		Server server = createServer(props, ServerCommandLine.class.getClassLoader());
		server.start();
		server.join ();
	}

}
