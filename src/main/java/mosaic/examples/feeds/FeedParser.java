package mosaic.examples.feeds;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class FeedParser {

	/** Parses RSS or Atom to instantiate a SyndFeed. */
	private SyndFeedInput input;

	public FeedParser() {
		this.input = new SyndFeedInput(true);
		this.input.setPreserveWireFeed(true);
	}

	public Timeline parseFeed(byte[] xmlEntry) throws IOException,
			FeedException {
		// Load the feed, regardless of RSS or Atom type
		SyndFeed feed = this.input.build(new XmlReader(
				new ByteArrayInputStream(xmlEntry)));

		// check feed type, only ATOM is accepted
		Feed atomFeed = null;
		if (feed.getFeedType().toLowerCase().startsWith("atom")) {
			atomFeed = (Feed) feed.originalWireFeed();
		} else
			throw new FeedException("Only ATOM feeds can be parsed.");
		Timeline timeline = new Timeline(atomFeed.getId(), feed.getLink(),
				atomFeed.getUpdated().getTime());

		@SuppressWarnings("unchecked")
		List<SyndEntry> entries = feed.getEntries();
		for (SyndEntry entry : entries) {
			String authorName = null;
			String authorEmail = null;
			String authorURI = null;
			SyndPerson author = null;
			if (entry.getAuthors().size() > 0) {
				author = (SyndPerson) entry.getAuthors().get(0);
				authorName = author.getName();
				authorEmail = author.getEmail();
				authorURI = author.getUri();
			}
			String content = null;
			String contentType = null;
			SyndContent contentOb = null;
			if (entry.getContents().size() > 0) {
				contentOb = (SyndContent) entry.getContents().get(0);
				content = contentOb.getValue();
				contentType = contentOb.getType();
			}
			String title = entry.getTitleEx().getValue();
			String titleType = entry.getTitleEx().getType();

			// System.out.println("Feed:\n\t" + entry.getUri() + "\n\t" + title
			// + " " + titleType + "\n\t" + content + " " + contentType
			// + "\n\t" + entry.getUpdatedDate().getTime() + "\n\t"
			// + authorName + " " + authorEmail + " " + authorURI);
			timeline.addEntry(entry.getUri(), title, titleType, content,
					contentType, entry.getUpdatedDate().getTime(), authorName,
					authorEmail, authorURI);
		}
		return timeline;
	}

}
