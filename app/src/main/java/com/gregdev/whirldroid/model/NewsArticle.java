package com.gregdev.whirldroid.model;

import java.io.Serializable;
import java.util.Date;

import com.gregdev.whirldroid.Whirldroid;

public class NewsArticle implements Serializable {
	private static final long serialVersionUID = 3220142140607118787L;
	private String id;
	private String title;
	private String source;
	private String blurb;
	private Date date;

	public NewsArticle(String id, String title, String source, String blurb, Date date) {
		this.id     = id;
		this.title  = Whirldroid.removeCommonHtmlChars(title);
		this.source = Whirldroid.removeCommonHtmlChars(source);
		this.blurb  = Whirldroid.removeCommonHtmlChars(blurb);
		this.date   = date;
	}

	public String getId() {
		return id;
	}

	public String getBlurb() {
		return blurb;
	}

	public String getTitle() {
		return title;
	}

	public Date getDate() {
		return date;
	}
	
	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		return title;
	}
}