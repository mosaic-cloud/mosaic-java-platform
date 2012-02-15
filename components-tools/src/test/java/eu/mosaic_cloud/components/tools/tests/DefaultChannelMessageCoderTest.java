/*
 * #%L
 * mosaic-components-tools
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

package eu.mosaic_cloud.components.tools.tests;


import java.nio.ByteBuffer;

import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;
import org.junit.Assert;
import org.junit.Test;


public final class DefaultChannelMessageCoderTest
{
	@Test
	public final void test ()
			throws Throwable
	{
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.defaultInstance;
		final int tries = 16;
		for (int index = 0; index < tries; index++) {
			final ChannelMessage outboundMessage = RandomMessageGenerator.defaultInstance.generateChannelMessage ();
			final ByteBuffer packet = coder.encode (outboundMessage);
			final ChannelMessage inboundMessage = coder.decode (packet);
			Assert.assertEquals (outboundMessage.metaData, inboundMessage.metaData);
			Assert.assertEquals (outboundMessage.data, inboundMessage.data);
		}
	}
}
