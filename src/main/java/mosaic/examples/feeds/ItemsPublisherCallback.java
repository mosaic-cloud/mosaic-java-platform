package mosaic.examples.feeds;

import mosaic.cloudlet.component.tests.AuthenticationToken;
import mosaic.cloudlet.resources.amqp.DefaultAmqpPublisherCallback;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

public class ItemsPublisherCallback extends
		DefaultAmqpPublisherCallback<IndexerCloudletState, AuthenticationToken> {

}
