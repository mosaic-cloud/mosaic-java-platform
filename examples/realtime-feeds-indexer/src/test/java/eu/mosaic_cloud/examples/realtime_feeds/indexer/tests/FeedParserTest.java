/*
 * #%L
 * mosaic-examples-realtime-feeds-indexer
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
package eu.mosaic_cloud.examples.realtime_feeds.indexer.tests;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.sun.syndication.io.FeedException;
import eu.mosaic_cloud.examples.realtime_feeds.indexer.FeedParser;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FeedParserTest {

	private FeedParser parser;

	@Before
	public void setUp() {
		this.parser = new FeedParser();
	}

	@After
	public void tearDown() {

	}

	private byte[] readAtom(String feedsUrl) {
		InputStreamReader streamReader = null;
		BufferedReader reader = null;
		StringBuilder builder = new StringBuilder();
		String line;
		byte[] bytes = null;
		try {
			streamReader = new InputStreamReader(
					(new URL(feedsUrl)).openStream());
			reader = new BufferedReader(streamReader);
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			bytes = builder.toString().getBytes();
		} catch (Exception e) {
			Assert.fail();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Assert.fail();
				}
			}
		}
		return bytes;
	}

	@Test
	public void test() {
		byte[] entry = readAtom("http://search.twitter.com/search.atom?q=%22cloud%22");
		try {
			this.parser.parseFeed(entry);
		} catch (IOException e) {
			fail(e.getMessage());
		} catch (FeedException e) {
			fail(e.getMessage());
		}
	}

}
