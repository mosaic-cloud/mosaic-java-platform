package mosaic.interop.idl;

/**
 * Data class holding interoperability channel parameters.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ChannelData {
	private final String channelIdentifier;
	private final String channelEndpoint;

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
		final int prime = 31; // NOPMD by georgiana on 9/27/11 7:58 PM
		int result = 1; // NOPMD by georgiana on 9/27/11 7:58 PM
		result = (prime * result)
				+ ((this.channelEndpoint == null) ? 0 : this.channelEndpoint
						.hashCode());
		result = (prime * result)
				+ ((this.channelIdentifier == null) ? 0
						: this.channelIdentifier.hashCode());
		return result;
	}

	public String getChannelIdentifier() {
		return this.channelIdentifier;
	}

	public String getChannelEndpoint() {
		return this.channelEndpoint;
	}

	@Override
	public boolean equals(Object obj) {
		boolean isEqual;
		isEqual = (this == obj);
		if (!isEqual) {
			if (obj instanceof ChannelData) {
				ChannelData other = (ChannelData) obj;
				isEqual = (obj == null)
						|| (getClass() != obj.getClass())
						|| ((this.channelEndpoint == null) && (other.channelEndpoint != null))
						|| ((this.channelEndpoint != null) && !this.channelEndpoint
								.equals(other.channelEndpoint))
						|| ((this.channelIdentifier == null) && (other.channelIdentifier != null))
						|| ((this.channelIdentifier != null) && !this.channelIdentifier
								.equals(other.channelIdentifier));
				isEqual ^= true;
			}
		}

		return isEqual;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.channelIdentifier + "(" + this.channelEndpoint + ")";
	}

}