package mosaic.connector.queue;

/**
 * This class defines an outbound message and all information required to
 * publish it.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpOutboundMessage implements IAmqpMessage {
	private final String callback;
	private final String contentEncoding;
	private final String contentType;
	private final String correlation;
	private final byte[] data;
	private final boolean durable;
	private final String exchange;
	private final String identifier;
	private final boolean immediate;
	private final boolean mandatory;
	private final String routingKey;

	public AmqpOutboundMessage(String exchange, String routingKey, byte[] data,
			boolean mandatory, boolean immediate, boolean durable) {
		this(exchange, routingKey, data, mandatory, immediate, durable, null,
				null, null, null, null);
	}

	/**
	 * Constructs a message.
	 * 
	 * @param exchange
	 *            the exchange to publish the message to
	 * @param routingKey
	 *            the routing key
	 * @param data
	 *            the message body
	 * @param mandatory
	 *            <code>true</code> if we are requesting a mandatory publish
	 * @param immediate
	 *            <code>true</code> if we are requesting an immediate publish
	 * @param durable
	 *            <code>true</code> if delivery mode should be 2
	 * @param callback
	 *            the address of the Node to send replies to
	 * @param contentEncoding
	 * @param contentType
	 *            tThe RFC-2046 MIME type for the Message content (such as
	 *            "text/plain")
	 * @param correlation
	 *            this is a client-specific id that may be used to mark or
	 *            identify Messages between clients. The server ignores this
	 *            field.
	 * @param identifier
	 *            message-id is an optional property which uniquely identifies a
	 *            Message within the Message system. The Message producer is
	 *            usually responsible for setting the message-id in such a way
	 *            that it is assured to be globally unique. The server MAY
	 *            discard a Message as a duplicate if the value of the
	 *            message-id matches that of a previously received Message sent
	 *            to the same Node.
	 * 
	 */
	public AmqpOutboundMessage(String exchange, String routingKey, byte[] data,
			boolean mandatory, boolean immediate, boolean durable,
			String callback, String contentEncoding, String contentType,
			String correlation, String identifier) {
		super();
		this.callback = callback;
		this.contentEncoding = contentEncoding;
		this.contentType = contentType;
		this.correlation = correlation;
		this.data = data;
		this.durable = durable;
		this.exchange = exchange;
		this.identifier = identifier;
		this.immediate = immediate;
		this.mandatory = mandatory;
		this.routingKey = routingKey;
	}

	public String getCallback() {
		return callback;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public String getContentType() {
		return contentType;
	}

	public String getCorrelation() {
		return correlation;
	}

	public byte[] getData() {
		return data;
	}

	public boolean isDurable() {
		return durable;
	}

	public String getExchange() {
		return exchange;
	}

	public String getIdentifier() {
		return identifier;
	}

	public boolean isImmediate() {
		return immediate;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public String getRoutingKey() {
		return routingKey;
	}

}
