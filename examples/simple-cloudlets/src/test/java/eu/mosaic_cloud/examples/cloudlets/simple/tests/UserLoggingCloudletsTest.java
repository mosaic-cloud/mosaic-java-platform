
package eu.mosaic_cloud.examples.cloudlets.simple.tests;

import eu.mosaic_cloud.examples.cloudlets.simple.LoggingCloudlet;
import eu.mosaic_cloud.examples.cloudlets.simple.UserCloudlet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class UserLoggingCloudletsTest {

    public class LoggingCloudletTest extends BaseCloudletTest {

        @Override
        public void setUp() {
            this.doRun = UserLoggingCloudletsTest.doRun;
            this.setUp(LoggingCloudlet.LifeCycleHandler.class,
                    LoggingCloudlet.LoggingCloudletContext.class, "logging-cloudlet.properties");
        }
    }

    public static class UserCloudletTest extends BaseCloudletTest {

        @Override
        public void setUp() {
            this.doRun = UserLoggingCloudletsTest.doRun;
            this.setUp(UserCloudlet.LifeCycleHandler.class, UserCloudlet.UserCloudletContext.class,
                    "user-cloudlet.properties");
        }
    }

    static boolean doRun = false;

    @Test
    public void test() {
        UserLoggingCloudletsTest.doRun = true;
        try {
            final ParallelComputer computer = new ParallelComputer(true, false);
            final Result result = JUnitCore.runClasses(computer, UserCloudletTest.class,
                    LoggingCloudletTest.class);
            Assert.assertTrue(result.wasSuccessful());
        } finally {
            UserLoggingCloudletsTest.doRun = false;
        }
    }
}
