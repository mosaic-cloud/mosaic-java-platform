
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
