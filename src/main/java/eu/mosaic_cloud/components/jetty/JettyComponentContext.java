
package eu.mosaic_cloud.components.jetty;


import java.io.File;

import eu.mosaic_cloud.components.core.ComponentIdentifier;


final class JettyComponentContext
		extends Object
{
	private JettyComponentContext ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	static String appContextPath = "/";
	static File appTmp = new File ("./temporary");
	static File appWar;
	static ComponentIdentifier brokerGroup = ComponentIdentifier.resolve ("8cd74b5e4ecd322fd7bbfc762ed6cf7d601eede8");
	static String brokerPassword = "guest";
	static String brokerUsername = "guest";
	static String brokerVirtualHost = "/";
	static JettyComponentCallbacks callbacks;
	static boolean httpgRequestsAutodeclare = true;
	static String httpgRequestsExchange = "mosaic-http-requests";
	static String httpgRequestsQueue = "mosaic-http-requests";
	static String httpgRequestsRoutingKey = "#";
	static ComponentIdentifier selfGroup = ComponentIdentifier.resolve ("a2e40f0b2c041bc694ace68ace08420d40f9cbc0");
	static ComponentIdentifier selfIdentifier;
}
