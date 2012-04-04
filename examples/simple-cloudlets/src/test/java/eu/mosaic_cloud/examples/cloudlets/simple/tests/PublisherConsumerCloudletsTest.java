
package eu.mosaic_cloud.examples.cloudlets.simple.tests;

import eu.mosaic_cloud.examples.cloudlets.simple.ConsumerCloudlet;
import eu.mosaic_cloud.examples.cloudlets.simple.PublisherCloudlet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class PublisherConsumerCloudletsTest {

    public static class ConsumerCloudletTest extends BaseCloudletTest {

        @Override
        public void setUp() {
            this.doRun = PublisherConsumerCloudletsTest.doRun;
            this.setUp(ConsumerCloudlet.LifeCycleHandler.class,
                    ConsumerCloudlet.ConsumerCloudletContext.class, "consumer-cloudlet.properties");
        }
    }

    public static class PublisherCloudletTest extends BaseCloudletTest {

        @Override
        public void setUp() {
            this.doRun = PublisherConsumerCloudletsTest.doRun;
            this.setUp(PublisherCloudlet.LifeCycleHandler.class,
                    PublisherCloudlet.PublisherCloudletContext.class,
                    "publisher-cloudlet.properties");
        }
    }

    static boolean doRun = false;

    @Test
    public void test() {
        PublisherConsumerCloudletsTest.doRun = true;
        try {
            final ParallelComputer computer = new ParallelComputer(true, false);
            final Result result = JUnitCore.runClasses(computer, PublisherCloudletTest.class,
                    ConsumerCloudletTest.class);
            Assert.assertTrue(result.wasSuccessful());
        } finally {
            PublisherConsumerCloudletsTest.doRun = false;
        }
    }
}
