package mosaic.examples.feeds.test;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import mosaic.examples.feeds.FeedParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.syndication.io.FeedException;

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
			System.out.println("[1] Received: " + builder.toString());
			bytes = builder.toString().getBytes();// toBytes(builder.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
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
