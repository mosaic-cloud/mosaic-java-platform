package eu.mosaic.JettyAmqpConnector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.DeploymentManager.AppEntry;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
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
		try {
			line = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			System.exit(1);
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
		props.setProperty("port", "5672");
		props.setProperty("server", "127.0.0.1");
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

	private static void startServer(Properties props) throws Exception {
		Server jettyServer = new Server();
		String userName = props.getProperty("username");
		String userPassword = props.getProperty("password");
		String hostName = props.getProperty("server");
		String routingKey = props.getProperty("routing-key");
		String exchangeName = props.getProperty("exchange");
		String queueName = props.getProperty("queue");
		int amqpPort = Integer.parseInt(props.getProperty("port"));

		/*
		 * Setup connectors
		 */
		AmqpConnector amqpConnector = new AmqpConnector(exchangeName,
				routingKey, queueName, hostName, userName, userPassword,
				amqpPort, props.containsKey("auto-declare"));
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
			webapp.setContextPath(ctxPath);
			webapp.setWar(webAppDir);
			jettyServer.setHandler(webapp);

		}

		/*
		 * Start the toy
		 */
		System.err.println("Starting server!");
		System.err.println("Queue name: " + queueName);
		jettyServer.start();
		jettyServer.join();
	}

	public static void main(String[] args) throws Exception {
		Properties props = getConfig(args);
		startServer(props);

	}

}
