package mosaic.driver.queue;

/**
 * Possible AMQP exchange types.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum AmqpExchangeType {
	/**
	 * With a direct exchange a message goes to the queues whose binding key
	 * exactly matches the routing key of the message.
	 */
	DIRECT("direct"),
	/**
	 * A fanout exchange broadcasts all the messages it receives to all the
	 * queues it knows.
	 */
	FANOUT("fanout"),
	/**
	 * With a topic exchange a message goes to the queues whose binding key
	 * matches the routing key of the message.
	 */
	TOPIC("topic");

	private String amqpName;

	AmqpExchangeType(String amqpName) {
		this.amqpName = amqpName;
	}
	
	/**
	 * Returns the AMQP name of the exchange type.
	 * @return
	 */
	public String getAmqpName() {
		return this.amqpName;
	}
}
