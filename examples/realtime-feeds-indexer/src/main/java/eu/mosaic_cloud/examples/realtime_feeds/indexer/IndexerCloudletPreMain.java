/*
 * #%L
 * mosaic-examples-realtime-feeds-indexer
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
package eu.mosaic_cloud.examples.realtime_feeds.indexer;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.cloudlets.container.CloudletContainerPreMain.CloudletContainerParameters;
import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

public class IndexerCloudletPreMain {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] arguments) throws Exception {
		Preconditions.checkArgument(arguments != null);
		Preconditions.checkArgument(arguments.length == 0, "invalid arguments");
		CloudletContainerParameters.classpath = null;
		CloudletContainerParameters.configFile = "indexer-cloudlet.prop";
		BasicComponentHarnessPreMain
				.main(new String[] { "eu.mosaic_cloud.cloudlets.runtime.ContainerComponentCallbacks" });
	}

}
