/*
 * #%L
 * mosaic-examples-simple-cloudlets
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

package eu.mosaic_cloud.examples.cloudlets.simple.tests;

import eu.mosaic_cloud.examples.cloudlets.simple.PingCloudlet;
import eu.mosaic_cloud.examples.cloudlets.simple.PongCloudlet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class PingPongCloudletsTest {

    public static class PingCloudletTest extends BaseCloudletTest {

        @Override
        public void setUp() {
            this.doRun = PingPongCloudletsTest.doRun;
            this.setUp(PingCloudlet.LifeCycleHandler.class, PingCloudlet.PingCloudletContext.class,
                    "ping-cloudlet.properties");
        }
    }

    public static class PongCloudletTest extends BaseCloudletTest {

        @Override
        public void setUp() {
            this.doRun = PingPongCloudletsTest.doRun;
            this.setUp(PongCloudlet.LifeCycleHandler.class, PongCloudlet.PongCloudletContext.class,
                    "pong-cloudlet.properties");
        }
    }

    static boolean doRun = false;

    @Test
    public void test() {
        PingPongCloudletsTest.doRun = true;
        try {
            final ParallelComputer computer = new ParallelComputer(true, false);
            final Result result = JUnitCore.runClasses(computer, PingCloudletTest.class,
                    PongCloudletTest.class);
            Assert.assertTrue(result.wasSuccessful());
        } finally {
            PingPongCloudletsTest.doRun = false;
        }
    }
}
