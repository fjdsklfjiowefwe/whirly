package com.gregdev.whirldroid.model;

import com.gregdev.whirldroid.Whirldroid;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Forum implements Serializable {

	private static final long serialVersionUID = 3624082881579843124L;
	
	private int id;
	private String title;
	private int order;
	private String section;
	private int page_count;
	private Map<String, Integer> groups;
	private List<Thread> threads;

	public Forum(int id, String title, int order, String section) {
		this.id = id;
		this.title = title;
		this.order = order;
		this.section = section;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}
	
	public int getPageCount() {
		return page_count;
	}
	
	public void setPageCount(int page_count) {
		this.page_count = page_count;
	}

	public Map<String, Integer> getGroups() {
		return groups;
	}

	public void setGroups(Map<String, Integer> groups) {
		this.groups = groups;
	}

	public List<Thread> getThreads() {
		return threads;
	}

	public void setThreads(List<Thread> threads) {
		this.threads = threads;
	}

	public boolean equals(Forum forum) {
		Whirldroid.log("Whirldroidm " + forum.getId());
		Whirldroid.log("Whirldroidm " + this.getId());
		Whirldroid.log("Whirldroidm");
		return this.getId() == forum.getId();
	}

}
