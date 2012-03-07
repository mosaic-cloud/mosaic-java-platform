/*
 * #%L
 * mosaic-platform-interop
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.platform.interop.tools;

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
    public boolean equals(Object obj) {
        boolean isEqual;
        isEqual = (this == obj);
        if (!isEqual) {
            if (obj instanceof ChannelData) {
                final ChannelData other = (ChannelData) obj;
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

    public String getChannelEndpoint() {
        return this.channelEndpoint;
    }

    public String getChannelIdentifier() {
        return this.channelIdentifier;
    }

    @Override
    public int hashCode() {
        final int prime = 31; // NOPMD by georgiana on 9/27/11 7:58 PM
        int result = 1; // NOPMD by georgiana on 9/27/11 7:58 PM
        result = (prime * result)
                + ((this.channelEndpoint == null) ? 0 : this.channelEndpoint.hashCode());
        result = (prime * result)
                + ((this.channelIdentifier == null) ? 0 : this.channelIdentifier.hashCode());
        return result;
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
