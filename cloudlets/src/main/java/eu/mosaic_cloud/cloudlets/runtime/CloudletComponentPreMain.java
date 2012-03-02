/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

import com.google.common.base.Preconditions;

public class CloudletComponentPreMain {

    public static class CloudletContainerParameters {

        public static String classpath;

        public static String configFile;

        public static int noInstances = 1;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] arguments) throws Throwable {
        Preconditions.checkArgument(arguments != null);
        Preconditions.checkArgument((arguments.length == 2) || (arguments.length == 3),
                "invalid arguments: <cloudlet jar> <cloudlet descriptor> [<no_of_instances>]");
        CloudletComponentPreMain.CloudletContainerParameters.classpath = arguments[0];
        CloudletComponentPreMain.CloudletContainerParameters.configFile = arguments[1];
        if (arguments.length == 3) {
            CloudletComponentPreMain.CloudletContainerParameters.noInstances = Integer
                    .parseInt(arguments[2]);
        }
        BasicComponentHarnessPreMain.main(new String[] {
            "eu.mosaic_cloud.cloudlets.runtime.ContainerComponentCallbacks" });
    }
}
