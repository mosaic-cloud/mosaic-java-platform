
package eu.mosaic_cloud.examples.cloudlets.simple.tests;


import eu.mosaic_cloud.examples.cloudlets.simple.HelloWorldCloudlet;


public class HelloWorldCloudletTest
		extends BaseCloudletTest
{
	@Override
	public void setUp ()
	{
		this.setUp (HelloWorldCloudlet.LifeCycleHandler.class, HelloWorldCloudlet.HelloCloudletContext.class);
	}
}
