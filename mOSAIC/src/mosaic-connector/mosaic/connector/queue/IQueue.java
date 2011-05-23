package mosaic.connector.queue;

public interface IQueue {
	void connect(Object queueId);

	void publish(ITopic aTopic, IAmqpMessage aMessage);

	void subscribe(ITopic aTopic);
}
