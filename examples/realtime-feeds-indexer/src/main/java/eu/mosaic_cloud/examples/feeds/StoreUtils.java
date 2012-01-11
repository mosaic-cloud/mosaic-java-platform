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
package eu.mosaic_cloud.examples.feeds;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StoreUtils {

	public static final String generateKey(final String string) {
		try {
			final MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(string.getBytes(), 0, string.length());
			final BigInteger i = new BigInteger(1, md5.digest());
			final String timelineKey = String.format("%1$032X", i)
					.toLowerCase();
			return timelineKey;
		} catch (final Exception e) {
			throw new IllegalStateException();
		}
	}

	public static final String generateFeedKey(String url) {
		// return (new crypto.Hash ("md5") .update (_url) .digest ("hex"));
		String key = null;
		try {
			MessageDigest md5;
			md5 = MessageDigest.getInstance("MD5");
			md5.update(url.getBytes(), 0, url.length());
			BigInteger i = new BigInteger(1, md5.digest());
			key = String.format("%1$032X", i).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException();
		}
		return key;
	}

	public static final String generateFeedTaskKey(String url, String type) {
		// return (new crypto.Hash ("md5") .update (_url) .update ("#") .update
		// (_type) .digest ("hex"));
		return generateTwoStringKey(url, type);
	}

	public static final String generateFeedTimelineKey(String url, int sequence) {
		// return (new crypto.Hash ("md5") .update (_url) .update ("#") .update
		// (printf ("%08x", _sequence)) .digest ("hex"));
		String seq = String.format("%1$08x", sequence).toLowerCase();
		return generateTwoStringKey(url, seq);
	}

	public static final String generateFeedItemKey(String url, String itemId) {
		// return (new crypto.Hash ("md5") .update (_url) .update ("#") .update
		// (_id) .digest ("hex"));
		return generateTwoStringKey(url, itemId);
	}

	private static String generateTwoStringKey(String string1, String string2) {
		String key = null;
		try {
			MessageDigest md5;
			md5 = MessageDigest.getInstance("MD5");
			md5.update(string1.getBytes(), 0, string1.length());
			md5.update("#".getBytes(), 0, 1);
			md5.update(string2.getBytes(), 0, string2.length());
			BigInteger i = new BigInteger(1, md5.digest());
			key = String.format("%1$032X", i).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException();
		}
		return key;
	}
}
