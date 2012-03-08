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

import eu.mosaic_cloud.components.core.ComponentIdentifier;


final class JettyComponentContext
		extends Object
{
	private JettyComponentContext ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	static String applicationContextPath = "/";
	static File applicationTemporary = new File ("./application.tmp");
	static File applicationWar = null;
	static ComponentIdentifier brokerGroup = ComponentIdentifier.resolve ("8cd74b5e4ecd322fd7bbfc762ed6cf7d601eede8");
	static String brokerPassword = "guest";
	static String brokerUsername = "guest";
	static String brokerVirtualHost = "/";
	static JettyComponentCallbacks callbacks = null;
	static boolean httpgRequestsAutodeclare = true;
	static String httpgRequestsExchange = "mosaic-http-requests";
	static String httpgRequestsQueue = "mosaic-http-requests";
	static String httpgRequestsRoutingKey = "#";
	static ComponentIdentifier selfGroup = ComponentIdentifier.resolve ("a2e40f0b2c041bc694ace68ace08420d40f9cbc0");
	static ComponentIdentifier selfIdentifier = null;
}
