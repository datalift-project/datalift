package org.datalift.projectmanager.rss;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Feed {
	private String title;
	private String id;
	private String link;
	private String updated;
	Iterator<Entry> entries;
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}

	public Iterator<Entry> getEntries() {
		return entries;
	}
	
	public void setEntries(Iterator<Entry> e) {
		this.entries = e;
	}
}
