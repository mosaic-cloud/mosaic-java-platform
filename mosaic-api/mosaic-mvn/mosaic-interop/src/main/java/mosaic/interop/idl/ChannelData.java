package mosaic.interop.idl;

/**
 * Data class holding interoperability channel parameters.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ChannelData {
	String channelIdentifier;
	String channelEndpoint;

	/**
	 * Creates a new channel data object.
	 * 
	 * @param channelIdentifier
	 *            the identifier of the channel
	 * @param channelEndpoint
	 *            the endpoint (<host>:<port>) where the channel is accepting
	 *            requests
	 */
	public ChannelData(String channelIdentifier, String channelEndpoint) {
		super();
		this.channelIdentifier = channelIdentifier;
		this.channelEndpoint = channelEndpoint;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((channelEndpoint == null) ? 0 : channelEndpoint.hashCode());
		result = prime
				* result
				+ ((channelIdentifier == null) ? 0 : channelIdentifier
						.hashCode());
		return result;
	}

	public String getChannelIdentifier() {
		return channelIdentifier;
	}

	public String getChannelEndpoint() {
		return channelEndpoint;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChannelData other = (ChannelData) obj;
		if (channelEndpoint == null) {
			if (other.channelEndpoint != null)
				return false;
		} else if (!channelEndpoint.equals(other.channelEndpoint))
			return false;
		if (channelIdentifier == null) {
			if (other.channelIdentifier != null)
				return false;
		} else if (!channelIdentifier.equals(other.channelIdentifier))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return channelIdentifier + "(" + channelEndpoint + ")";
	}

}